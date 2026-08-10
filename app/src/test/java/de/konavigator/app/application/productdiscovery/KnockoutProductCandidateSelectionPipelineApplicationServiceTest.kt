package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.FxRateProviderResult
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitCalculator
import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionPolicy
import de.konavigator.app.domain.currency.FxRateQuote
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateSelectionPipelineApplicationServiceTest {
    private val evaluationTime = 1_000L

    @Test
    fun discoveryStopRemainsNestedAndStopsLaterStages() = runTest {
        val fxProvider = FakeFxRateProvider()
        val result = calculationStopped(
            service(
                catalogResult = KnockoutProductSpecificationCatalogResult.Success(emptyList()),
                fxProvider = fxProvider
            ).execute(request())
        )

        val calculationStop = result.calculationPipelineResult
        assertTrue(
            calculationStop is KnockoutProductCandidateCalculationPipelineApplicationResult
                .DiscoveryStopped
        )
        calculationStop as KnockoutProductCandidateCalculationPipelineApplicationResult
            .DiscoveryStopped
        assertSame(
            KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates,
            calculationStop.discoveryResult
        )
        assertTrue(fxProvider.calls.isEmpty())
    }

    @Test
    fun marketDataStopRemainsNestedAndStopsLaterStages() = runTest {
        val candidate = snapshot("A")
        val result = calculationStopped(
            service(
                catalogResult = catalogSuccess(candidate),
                availabilityResult = availabilitySuccess("A")
            ).execute(request())
        )

        val calculationStop = result.calculationPipelineResult
        assertTrue(
            calculationStop is KnockoutProductCandidateCalculationPipelineApplicationResult
                .MarketDataStopped
        )
        calculationStop as KnockoutProductCandidateCalculationPipelineApplicationResult
            .MarketDataStopped
        assertEquals(
            KnockoutProductCandidateMarketDataResult.MarketDataNotFound("A"),
            calculationStop.marketDataResult
        )
    }

    @Test
    fun dataQualityGateStopRemainsNested() = runTest {
        val blocked = snapshot("BLOCKED", basePrice = 0.0)
        val result = calculationStopped(
            successfulDiscoveryService(listOf(blocked)).execute(request())
        )

        val calculationStop = result.calculationPipelineResult
        assertTrue(
            calculationStop is KnockoutProductCandidateCalculationPipelineApplicationResult
                .NoStructurallyEligibleCandidates
        )
        calculationStop as KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoStructurallyEligibleCandidates
        assertSame(
            blocked,
            calculationStop.blockedDataQualityCandidates.single()
                .candidateWithMarketData.specificationSnapshot
        )
    }

    @Test
    fun noCurrencyConvertibleCandidatesStopBeforePlannedEntrySelection() = runTest {
        val crossCurrency = snapshot(
            productIsin = "CROSS",
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        )
        val result = currencyStopped(
            successfulDiscoveryService(
                candidates = listOf(crossCurrency),
                fxProvider = FakeFxRateProvider(FxRateProviderResult.NotFound)
            ).execute(request())
        )

        assertEquals(1, result.calculationPipelineResult.successfulCandidates.size)
        assertTrue(
            result.currencyConversionResult is
                KnockoutProductCandidateCurrencyConversionApplicationResult
                    .NoCurrencyConvertibleCandidates
        )
        val currencyStop = result.currencyConversionResult as
            KnockoutProductCandidateCurrencyConversionApplicationResult
                .NoCurrencyConvertibleCandidates
        assertSame(
            result.calculationPipelineResult.successfulCandidates.single(),
            currencyStop.failedCandidates.single().candidateWithCalculation
        )
    }

    @Test
    fun invalidTargetLeverageResultRemainsNestedWithoutFlattening() = runTest {
        val candidate = snapshot("INVALID-TARGET")
        val result = evaluated(
            successfulDiscoveryService(listOf(candidate)).execute(
                request(targetLeverage = 0.0)
            )
        )

        assertTrue(
            result.plannedEntrySelectionResult is
                KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                    .NoValidTargetLeveragePlanCandidates
        )
    }

    @Test
    fun existingEntryFailureResultRemainsNestedWithoutFlattening() = runTest {
        val candidate = snapshot(
            productIsin = "INVALID-ENTRY",
            knockoutBarrier = 110.0
        )
        val result = evaluated(
            successfulDiscoveryService(listOf(candidate)).execute(request())
        )

        assertTrue(
            result.plannedEntrySelectionResult is
                KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                    .NoSuccessfulExistingEntryCalculationCandidates
        )
    }

    @Test
    fun completeSameCurrencyApplicationPathSelectsPrimaryAndAlternative() = runTest {
        val primary = snapshot("PRIMARY")
        val alternative = snapshot("ALTERNATIVE")
        val fxProvider = FakeFxRateProvider()
        val inputSnapshots = listOf(primary, alternative)
        val originalSnapshots = inputSnapshots.toList()
        val request = request()

        val result = evaluated(
            successfulDiscoveryService(inputSnapshots, fxProvider = fxProvider)
                .execute(request)
        )
        val planned = plannedEvaluated(result.plannedEntrySelectionResult)
        val selection = selected(planned.targetSelectionResult)

        assertSame(
            primary,
            originalSnapshot(selection.primaryCandidate)
        )
        assertSame(
            alternative,
            originalSnapshot(selection.alternativeCandidates.single())
        )
        assertTrue(fxProvider.calls.isEmpty())
        assertEquals(originalSnapshots, inputSnapshots)
        assertEquals("underlying", request.underlyingId)
        assertEquals(TradeDirection.LONG, request.direction)
        assertEquals("broker", request.brokerId)
        assertEquals(setOf("issuer"), request.enabledIssuerIds)
        assertEquals(MarketDataCalculationType.MID, request.calculationType)
        assertEquals(evaluationTime, request.evaluationTimeEpochMillis)
        assertEquals(100L, request.maxFxAgeMillis)
        assertEquals(100.0, request.underlyingPrice, 0.0)
        assertEquals(100.0, request.plannedEntryPrice, 0.0)
        assertEquals(5.0, request.targetLeverage, 0.0)
        assertEquals(1.0, request.maxRelativeLeverageDeviationPercent, 0.0)
        assertEquals(1.0, request.maxBarrierDeviationPercentOfPlannedEntry, 0.0)
    }

    @Test
    fun completeCrossCurrencyApplicationPathPreservesEvidenceAndConversionReference() = runTest {
        val candidate = snapshot(
            productIsin = "CROSS-SELECTED",
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        )
        val quote = FxRateQuote(
            underlyingCurrency = currencyCode("USD"),
            productCurrency = currencyCode("EUR"),
            underlyingCurrencyPerProductCurrencyRate = 1.1,
            sourceId = "SYNTH_FX",
            observedAtEpochMillis = 950L
        )
        val fxProvider = FakeFxRateProvider(FxRateProviderResult.Success(quote))

        val result = evaluated(
            successfulDiscoveryService(listOf(candidate), fxProvider = fxProvider)
                .execute(request())
        )
        val converted = result.currencyConversionResult.successfulCandidates.single()
        val planned = plannedEvaluated(result.plannedEntrySelectionResult)
        val selection = selected(planned.targetSelectionResult)
        val selectedInput = originalInput(selection.primaryCandidate)

        assertSame(converted.targetLeverageInput, selectedInput)
        assertSame(
            converted.targetLeverageInput.currencyConversion,
            selectedInput.currencyConversion
        )
        assertEquals(
            KnockoutProductCandidateCurrencyConversionEvidence.CrossCurrency(
                sourceId = "SYNTH_FX",
                observedAtEpochMillis = 950L
            ),
            converted.evidence
        )
        assertEquals(
            listOf(currencyCode("USD") to currencyCode("EUR")),
            fxProvider.calls
        )
    }

    @Test
    fun mixedDiagnosticsRemainNestedWhileSuccessfulCandidatesReachSelection() = runTest {
        val earlyBlocked = snapshot(
            productIsin = "EARLY-BLOCKED",
            basePrice = 0.0,
            underlyingCurrency = "CHF",
            productCurrency = "EUR"
        )
        val currencyFailure = snapshot(
            productIsin = "FX-FAILURE",
            underlyingCurrency = "GBP",
            productCurrency = "EUR"
        )
        val primary = snapshot("PRIMARY")
        val alternative = snapshot("ALTERNATIVE")
        val fxProvider = FakeFxRateProvider(FxRateProviderResult.NotFound)

        val result = evaluated(
            successfulDiscoveryService(
                listOf(earlyBlocked, currencyFailure, primary, alternative),
                fxProvider = fxProvider
            ).execute(request())
        )
        val selection = selected(
            plannedEvaluated(result.plannedEntrySelectionResult).targetSelectionResult
        )

        assertSame(
            earlyBlocked,
            result.calculationPipelineResult.blockedDataQualityCandidates.single()
                .candidateWithMarketData.specificationSnapshot
        )
        assertSame(
            currencyFailure,
            result.currencyConversionResult.failedCandidates.single()
                .candidateWithCalculation.candidateWithSourceEvaluation
                .candidateWithFreshness.candidateWithCalculationAvailability
                .candidateWithDataQuality.candidateWithMarketData.specificationSnapshot
        )
        assertSame(primary, originalSnapshot(selection.primaryCandidate))
        assertSame(alternative, originalSnapshot(selection.alternativeCandidates.single()))
        assertEquals(
            listOf(currencyCode("GBP") to currencyCode("EUR")),
            fxProvider.calls
        )
    }

    @Test
    fun duplicateSnapshotsAndOrderRemainThroughAllThreeServices() = runTest {
        val duplicate = snapshot("DUPLICATE")
        val input = listOf(duplicate, duplicate)

        val result = evaluated(successfulDiscoveryService(input).execute(request()))
        val converted = result.currencyConversionResult.successfulCandidates
        val selection = selected(
            plannedEvaluated(result.plannedEntrySelectionResult).targetSelectionResult
        )

        assertEquals(2, result.calculationPipelineResult.successfulCandidates.size)
        assertSame(
            duplicate,
            originalSnapshot(result.calculationPipelineResult.successfulCandidates[0])
        )
        assertSame(
            duplicate,
            originalSnapshot(result.calculationPipelineResult.successfulCandidates[1])
        )
        assertEquals(2, converted.size)
        assertSame(
            converted[0].targetLeverageInput,
            originalInput(selection.primaryCandidate)
        )
        assertSame(
            converted[1].targetLeverageInput,
            originalInput(selection.alternativeCandidates.single())
        )
    }

    private fun service(
        catalogResult: KnockoutProductSpecificationCatalogResult,
        availabilityResult: KnockoutProductBrokerAvailabilityResult = availabilitySuccess(),
        marketDataResponses: Map<String, RepositoryResult<KnockoutProductMarketData>> = emptyMap(),
        fxProvider: FakeFxRateProvider = FakeFxRateProvider()
    ) = KnockoutProductCandidateSelectionPipelineApplicationService(
        calculationPipelineApplicationService = calculationPipelineService(
            catalogResult,
            availabilityResult,
            marketDataResponses
        ),
        currencyConversionApplicationService =
            KnockoutProductCandidateCurrencyConversionApplicationService(
                fxRateProvider = fxProvider,
                currencyConversionPolicy = CurrencyConversionPolicy()
            ),
        plannedEntrySelectionApplicationService = plannedEntrySelectionService()
    )

    private fun successfulDiscoveryService(
        candidates: List<KnockoutProductSpecificationSnapshot>,
        fxProvider: FakeFxRateProvider = FakeFxRateProvider()
    ): KnockoutProductCandidateSelectionPipelineApplicationService = service(
        catalogResult = catalogSuccess(*candidates.toTypedArray()),
        availabilityResult = availabilitySuccess(
            *candidates.map { it.specification.productIsin }.toTypedArray()
        ),
        marketDataResponses = candidates.associate { candidate ->
            candidate.specification.productIsin to RepositoryResult.Success(
                marketData(
                    productIsin = candidate.specification.productIsin,
                    currency = candidate.specification.productCurrency
                )
            )
        },
        fxProvider = fxProvider
    )

    private fun calculationPipelineService(
        catalogResult: KnockoutProductSpecificationCatalogResult,
        availabilityResult: KnockoutProductBrokerAvailabilityResult,
        marketDataResponses: Map<String, RepositoryResult<KnockoutProductMarketData>>
    ) = KnockoutProductCandidateCalculationPipelineApplicationService(
        discoveryApplicationService = KnockoutProductDiscoveryApplicationService(
            catalogRepository = FakeCatalogRepository(catalogResult),
            brokerAvailabilityRepository = FakeBrokerAvailabilityRepository(availabilityResult),
            issuerSelectionFilter = KnockoutProductIssuerSelectionFilter()
        ),
        marketDataApplicationService = KnockoutProductCandidateMarketDataApplicationService(
            FakeMarketDataRepository(marketDataResponses)
        ),
        dataQualityApplicationService = KnockoutProductCandidateDataQualityApplicationService(),
        dataQualityGate = KnockoutProductCandidateDataQualityGate(),
        calculationAvailabilityApplicationService =
            KnockoutProductCandidateCalculationAvailabilityApplicationService(),
        calculationAvailabilityGate = KnockoutProductCandidateCalculationAvailabilityGate(),
        freshnessApplicationService = KnockoutProductCandidateFreshnessApplicationService(
            MarketDataFreshnessPolicy(
                MarketDataFreshnessThresholds(100L, 100L, 100L, 100L)
            )
        ),
        freshnessGate = KnockoutProductCandidateFreshnessGate(),
        sourceEvaluationApplicationService =
            KnockoutProductCandidateSourceEvaluationApplicationService(sourcePolicy()),
        sourceGate = KnockoutProductCandidateSourceGate(),
        calculationApplicationService = KnockoutProductCandidateCalculationApplicationService(),
        calculationGate = KnockoutProductCandidateCalculationGate()
    )

    private fun plannedEntrySelectionService() =
        KnockoutProductCandidatePlannedEntrySelectionApplicationService(
            KnockoutProductCandidateTargetLeverageApplicationService(TradeCalculationEngine),
            KnockoutProductCandidateTargetLeverageGate(),
            KnockoutProductCandidateExistingEntryCalculationApplicationService(
                ExistingKnockoutProductEntryCalculator
            ),
            KnockoutProductCandidateExistingEntryCalculationGate(),
            KnockoutProductCandidateTargetSelectionApplicationService(
                KnockoutProductCandidateTargetDeviationApplicationService(
                    ExistingKnockoutProductTargetDeviationCalculator
                ),
                KnockoutProductCandidateTargetDeviationGate(),
                KnockoutProductCandidateTargetFitApplicationService(
                    ExistingKnockoutProductTargetFitCalculator
                ),
                KnockoutProductCandidateTargetFitGate(),
                KnockoutProductCandidateTargetFitRanker(),
                KnockoutProductCandidateTargetFitSelector()
            )
        )

    private fun request(
        targetLeverage: Double = 5.0
    ) = KnockoutProductCandidateSelectionPipelineApplicationRequest(
        underlyingId = "underlying",
        direction = TradeDirection.LONG,
        brokerId = "broker",
        enabledIssuerIds = setOf("issuer"),
        calculationType = MarketDataCalculationType.MID,
        evaluationTimeEpochMillis = evaluationTime,
        maxFxAgeMillis = 100L,
        underlyingPrice = 100.0,
        plannedEntryPrice = 100.0,
        targetLeverage = targetLeverage,
        maxRelativeLeverageDeviationPercent = 1.0,
        maxBarrierDeviationPercentOfPlannedEntry = 1.0
    )

    private fun snapshot(
        productIsin: String,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 80.0,
        underlyingCurrency: String = "EUR",
        productCurrency: String = "EUR"
    ) = KnockoutProductSpecificationSnapshot(
        specification = KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = "SYN001",
            issuerId = "issuer",
            underlyingId = "underlying",
            direction = TradeDirection.LONG,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = 0.1,
            underlyingCurrency = underlyingCurrency,
            productCurrency = productCurrency
        ),
        sourceId = "SYNTH_SPEC",
        retrievedAtEpochMillis = 1_000L,
        sourceTimestampEpochMillis = 900L
    )

    private fun marketData(
        productIsin: String,
        currency: String
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = 9.0,
        ask = 10.0,
        bidTimestampEpochMillis = 995L,
        askTimestampEpochMillis = 995L,
        currency = currency,
        sourceId = "ALLOWED"
    )

    private fun sourcePolicy() = MarketDataSourcePolicy(
        MarketDataSourcePolicyConfig(
            listOf(
                MarketDataSourceRule(
                    sourceId = "ALLOWED",
                    supportedCalculationTypes = setOf(MarketDataCalculationType.MID)
                )
            )
        )
    )

    private fun catalogSuccess(
        vararg candidates: KnockoutProductSpecificationSnapshot
    ) = KnockoutProductSpecificationCatalogResult.Success(candidates.toList())

    private fun availabilitySuccess(
        vararg productIsins: String
    ) = KnockoutProductBrokerAvailabilityResult.Success(productIsins.toSet())

    private fun calculationStopped(
        result: KnockoutProductCandidateSelectionPipelineApplicationResult
    ): KnockoutProductCandidateSelectionPipelineApplicationResult.CalculationPipelineStopped {
        assertTrue(
            result is KnockoutProductCandidateSelectionPipelineApplicationResult
                .CalculationPipelineStopped
        )
        return result as KnockoutProductCandidateSelectionPipelineApplicationResult
            .CalculationPipelineStopped
    }

    private fun currencyStopped(
        result: KnockoutProductCandidateSelectionPipelineApplicationResult
    ): KnockoutProductCandidateSelectionPipelineApplicationResult.CurrencyConversionStopped {
        assertTrue(
            result is KnockoutProductCandidateSelectionPipelineApplicationResult
                .CurrencyConversionStopped
        )
        return result as KnockoutProductCandidateSelectionPipelineApplicationResult
            .CurrencyConversionStopped
    }

    private fun evaluated(
        result: KnockoutProductCandidateSelectionPipelineApplicationResult
    ): KnockoutProductCandidateSelectionPipelineApplicationResult
        .PlannedEntrySelectionEvaluated {
        assertTrue(
            result is KnockoutProductCandidateSelectionPipelineApplicationResult
                .PlannedEntrySelectionEvaluated
        )
        return result as KnockoutProductCandidateSelectionPipelineApplicationResult
            .PlannedEntrySelectionEvaluated
    }

    private fun plannedEvaluated(
        result: KnockoutProductCandidatePlannedEntrySelectionApplicationResult
    ): KnockoutProductCandidatePlannedEntrySelectionApplicationResult.TargetSelectionEvaluated {
        assertTrue(
            result is KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .TargetSelectionEvaluated
        )
        return result as KnockoutProductCandidatePlannedEntrySelectionApplicationResult
            .TargetSelectionEvaluated
    }

    private fun selected(
        result: KnockoutProductCandidateTargetSelectionApplicationResult
    ): KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates {
        assertTrue(
            result is KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates
        )
        return result as KnockoutProductCandidateTargetSelectionApplicationResult
            .SelectedCandidates
    }

    private fun originalInput(
        candidate: KnockoutProductCandidateWithTargetFit
    ) = candidate.candidateWithTargetDeviation
        .candidateWithExistingEntryCalculation
        .candidateWithTargetLeveragePlan
        .input

    private fun originalSnapshot(
        candidate: KnockoutProductCandidateWithTargetFit
    ) = originalSnapshot(originalInput(candidate).candidateWithCalculation)

    private fun originalSnapshot(
        candidate: KnockoutProductCandidateWithCalculation
    ) = candidate.candidateWithSourceEvaluation.candidateWithFreshness
        .candidateWithCalculationAvailability.candidateWithDataQuality
        .candidateWithMarketData.specificationSnapshot

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid test currency: ${result.error}")
        }

    private class FakeCatalogRepository(
        private val result: KnockoutProductSpecificationCatalogResult
    ) : KnockoutProductSpecificationCatalogRepository {
        override suspend fun findCandidates(
            query: KnockoutProductSpecificationCatalogQuery
        ) = result
    }

    private class FakeBrokerAvailabilityRepository(
        private val result: KnockoutProductBrokerAvailabilityResult
    ) : KnockoutProductBrokerAvailabilityRepository {
        override suspend fun findTradableProductIsins(
            query: KnockoutProductBrokerAvailabilityQuery
        ) = result
    }

    private class FakeMarketDataRepository(
        private val responses: Map<String, RepositoryResult<KnockoutProductMarketData>>
    ) : KnockoutProductMarketDataRepository {
        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductMarketData> =
            responses[productIsin] ?: RepositoryResult.NotFound
    }

    private class FakeFxRateProvider(
        private val result: FxRateProviderResult = FxRateProviderResult.NotFound
    ) : FxRateProvider {
        val calls = mutableListOf<Pair<CurrencyCode, CurrencyCode>>()

        override suspend fun findRate(
            underlyingCurrency: CurrencyCode,
            productCurrency: CurrencyCode
        ): FxRateProviderResult {
            calls += underlyingCurrency to productCurrency
            return result
        }
    }
}
