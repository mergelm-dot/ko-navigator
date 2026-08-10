package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateCalculationPipelineApplicationServiceTest {
    private val calculationType = MarketDataCalculationType.MID
    private val evaluationTime = 1_000L

    @Test
    fun noCatalogCandidatesRemainTypedDiscoveryStop() = runTest {
        val result = service(
            catalogResult = KnockoutProductSpecificationCatalogResult.Success(emptyList())
        ).execute(request())

        assertSame(
            KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates,
            discoveryStop(result).discoveryResult
        )
    }

    @Test
    fun noBrokerTradableCandidatesRemainTypedDiscoveryStop() = runTest {
        val candidate = snapshot("A")
        val result = service(
            catalogResult = catalogSuccess(candidate),
            availabilityResult = availabilitySuccess()
        ).execute(request())

        assertSame(
            KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates,
            discoveryStop(result).discoveryResult
        )
    }

    @Test
    fun noEnabledIssuerCandidatesRemainTypedDiscoveryStop() = runTest {
        val candidate = snapshot("A")
        val result = service(
            catalogResult = catalogSuccess(candidate),
            availabilityResult = availabilitySuccess("A")
        ).execute(request(enabledIssuerIds = emptySet()))

        assertSame(
            KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates,
            discoveryStop(result).discoveryResult
        )
    }

    @Test
    fun technicalDiscoveryFailureRemainsUnchanged() = runTest {
        val result = service(
            catalogResult = KnockoutProductSpecificationCatalogResult.DataAccessFailure
        ).execute(request())

        assertSame(
            KnockoutProductDiscoveryApplicationResult.CatalogDataAccessFailure,
            discoveryStop(result).discoveryResult
        )
    }

    @Test
    fun marketDataNotFoundRemainsUnchanged() = runTest {
        val candidate = snapshot("A")
        val result = successfulDiscoveryService(
            candidates = listOf(candidate),
            marketDataResponses = emptyMap()
        ).execute(request())

        assertEquals(
            KnockoutProductCandidateMarketDataResult.MarketDataNotFound("A"),
            marketDataStop(result).marketDataResult
        )
    }

    @Test
    fun marketDataInvalidDataRemainsUnchanged() = runTest {
        val candidate = snapshot("A")
        val result = successfulDiscoveryService(
            candidates = listOf(candidate),
            marketDataResponses = mapOf("A" to RepositoryResult.InvalidData)
        ).execute(request())

        assertEquals(
            KnockoutProductCandidateMarketDataResult.MarketDataInvalidData("A"),
            marketDataStop(result).marketDataResult
        )
    }

    @Test
    fun allBlockedDataQualityCandidatesStopPipeline() = runTest {
        val blocked = snapshot("A", basePrice = 0.0)
        val result = execute(listOf(blocked), listOf(marketData("A")))

        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .NoStructurallyEligibleCandidates
        )
        result as KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoStructurallyEligibleCandidates
        assertSame(blocked, snapshot(result.blockedDataQualityCandidates.single()))
    }

    @Test
    fun allCalculationUnavailableCandidatesStopPipeline() = runTest {
        val unavailable = snapshot("A")
        val result = execute(
            listOf(unavailable),
            listOf(marketData("A", bid = null, bidTimestamp = null))
        )

        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .NoCalculationAvailableCandidates
        )
        result as KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoCalculationAvailableCandidates
        assertTrue(result.blockedDataQualityCandidates.isEmpty())
        assertSame(unavailable, snapshot(result.calculationUnavailableCandidates.single()))
    }

    @Test
    fun exactEvaluationTimeCanStopAllNotFreshCandidates() = runTest {
        val stale = snapshot("A")
        val result = execute(
            candidates = listOf(stale),
            marketData = listOf(marketData("A", bidTimestamp = 950L, askTimestamp = 950L)),
            request = request(evaluationTimeEpochMillis = 1_051L)
        )

        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .NoFreshCandidates
        )
        result as KnockoutProductCandidateCalculationPipelineApplicationResult.NoFreshCandidates
        assertSame(stale, snapshot(result.notFreshCandidates.single()))
    }

    @Test
    fun allSourceBlockedCandidatesStopPipeline() = runTest {
        val blocked = snapshot("A")
        val result = execute(
            listOf(blocked),
            listOf(marketData("A", sourceId = "BLOCKED"))
        )

        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .NoSourceAllowedCandidates
        )
        result as KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoSourceAllowedCandidates
        assertSame(blocked, snapshot(result.sourceBlockedCandidates.single()))
    }

    @Test
    fun calculatorInvalidInputsAreBlockedByDataQualityBeforeCalculation() = runTest {
        val calculatorInvalid = snapshot("A")
        val result = execute(
            listOf(calculatorInvalid),
            listOf(marketData("A", bid = 11.0, ask = 10.0))
        )

        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .NoStructurallyEligibleCandidates
        )
        result as KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoStructurallyEligibleCandidates
        assertSame(
            calculatorInvalid,
            snapshot(result.blockedDataQualityCandidates.single())
        )
    }

    @Test
    fun oneCandidateCompletesEntirePipeline() = runTest {
        val candidate = snapshot("A")
        val result = success(execute(listOf(candidate), listOf(marketData("A"))))

        assertSame(candidate, snapshot(result.successfulCandidates.single()))
        val outcome = result.successfulCandidates.single().calculationOutcome
        assertTrue(outcome is KnockoutProductCandidateCalculationOutcome.Success)
        assertTrue(
            (outcome as KnockoutProductCandidateCalculationOutcome.Success).value is
                MarketDataCalculationValue.MidPrice
        )
    }

    @Test
    fun calculatorInvalidCandidateIsBlockedWhileValidCandidateReachesCalculationSuccess() =
        runTest {
        val calculatorInvalid = snapshot("A")
        val unavailable = snapshot("B")
        val stale = snapshot("C")
        val sourceBlocked = snapshot("D")
        val successful = snapshot("E")
        val candidates = listOf(
            calculatorInvalid,
            unavailable,
            stale,
            sourceBlocked,
            successful
        )
        val data = listOf(
            marketData("A", bid = 11.0, ask = 10.0),
            marketData("B", bid = null, bidTimestamp = null),
            marketData("C", bidTimestamp = 800L, askTimestamp = 800L),
            marketData("D", sourceId = "BLOCKED"),
            marketData("E")
        )

        val result = success(execute(candidates, data))

        assertSame(successful, snapshot(result.successfulCandidates.single()))
        assertSame(calculatorInvalid, snapshot(result.blockedDataQualityCandidates.single()))
        assertSame(unavailable, snapshot(result.calculationUnavailableCandidates.single()))
        assertSame(stale, snapshot(result.notFreshCandidates.single()))
        assertSame(sourceBlocked, snapshot(result.sourceBlockedCandidates.single()))
        assertTrue(result.failedCalculationCandidates.isEmpty())
    }

    @Test
    fun successfulCandidatesPreserveDiscoveryOrder() = runTest {
        val first = snapshot("A")
        val second = snapshot("B")
        val result = success(
            execute(
                listOf(first, second),
                listOf(marketData("A"), marketData("B"))
            )
        )

        assertEquals(listOf(first, second), result.successfulCandidates.map(::snapshot))
    }

    @Test
    fun duplicateReferencesAndRequestValuesRemainUnchanged() = runTest {
        val duplicate = snapshot("A")
        val marketData = marketData("A")
        val enabledIssuerIds = linkedSetOf("issuer")
        val request = request(
            underlyingId = " underlying ",
            direction = TradeDirection.LONG,
            brokerId = " broker ",
            enabledIssuerIds = enabledIssuerIds
        )
        val result = success(
            execute(listOf(duplicate, duplicate), listOf(marketData), request)
        )

        assertEquals(2, result.successfulCandidates.size)
        assertSame(duplicate, snapshot(result.successfulCandidates[0]))
        assertSame(duplicate, snapshot(result.successfulCandidates[1]))
        assertSame(marketData, marketData(result.successfulCandidates[0]))
        assertSame(marketData, marketData(result.successfulCandidates[1]))
        assertEquals(" underlying ", request.underlyingId)
        assertEquals(TradeDirection.LONG, request.direction)
        assertEquals(" broker ", request.brokerId)
        assertSame(enabledIssuerIds, request.enabledIssuerIds)
        assertEquals(calculationType, request.calculationType)
        assertEquals(evaluationTime, request.evaluationTimeEpochMillis)
    }

    private suspend fun execute(
        candidates: List<KnockoutProductSpecificationSnapshot>,
        marketData: List<KnockoutProductMarketData>,
        request: KnockoutProductCandidateCalculationPipelineApplicationRequest = request()
    ): KnockoutProductCandidateCalculationPipelineApplicationResult =
        successfulDiscoveryService(
            candidates = candidates,
            marketDataResponses = marketData.associate { data ->
                data.productIsin to RepositoryResult.Success(data)
            }
        ).execute(request)

    private fun service(
        catalogResult: KnockoutProductSpecificationCatalogResult,
        availabilityResult: KnockoutProductBrokerAvailabilityResult = availabilitySuccess(),
        marketDataResponses: Map<String, RepositoryResult<KnockoutProductMarketData>> = emptyMap()
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

    private fun successfulDiscoveryService(
        candidates: List<KnockoutProductSpecificationSnapshot>,
        marketDataResponses: Map<String, RepositoryResult<KnockoutProductMarketData>>
    ) = service(
        catalogResult = KnockoutProductSpecificationCatalogResult.Success(candidates),
        availabilityResult = KnockoutProductBrokerAvailabilityResult.Success(
            candidates.map { it.specification.productIsin }.toSet()
        ),
        marketDataResponses = marketDataResponses
    )

    private fun request(
        underlyingId: String = "underlying",
        direction: TradeDirection = TradeDirection.LONG,
        brokerId: String = "broker",
        enabledIssuerIds: Set<String> = setOf("issuer"),
        calculationType: MarketDataCalculationType = this.calculationType,
        evaluationTimeEpochMillis: Long = evaluationTime
    ) = KnockoutProductCandidateCalculationPipelineApplicationRequest(
        underlyingId = underlyingId,
        direction = direction,
        brokerId = brokerId,
        enabledIssuerIds = enabledIssuerIds,
        calculationType = calculationType,
        evaluationTimeEpochMillis = evaluationTimeEpochMillis
    )

    private fun catalogSuccess(
        vararg candidates: KnockoutProductSpecificationSnapshot
    ) = KnockoutProductSpecificationCatalogResult.Success(candidates.toList())

    private fun availabilitySuccess(
        vararg productIsins: String
    ) = KnockoutProductBrokerAvailabilityResult.Success(productIsins.toSet())

    private fun discoveryStop(
        result: KnockoutProductCandidateCalculationPipelineApplicationResult
    ): KnockoutProductCandidateCalculationPipelineApplicationResult.DiscoveryStopped {
        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .DiscoveryStopped
        )
        return result as KnockoutProductCandidateCalculationPipelineApplicationResult
            .DiscoveryStopped
    }

    private fun marketDataStop(
        result: KnockoutProductCandidateCalculationPipelineApplicationResult
    ): KnockoutProductCandidateCalculationPipelineApplicationResult.MarketDataStopped {
        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .MarketDataStopped
        )
        return result as KnockoutProductCandidateCalculationPipelineApplicationResult
            .MarketDataStopped
    }

    private fun success(
        result: KnockoutProductCandidateCalculationPipelineApplicationResult
    ): KnockoutProductCandidateCalculationPipelineApplicationResult
        .SuccessfulCalculationCandidates {
        assertTrue(
            result is KnockoutProductCandidateCalculationPipelineApplicationResult
                .SuccessfulCalculationCandidates
        )
        return result as KnockoutProductCandidateCalculationPipelineApplicationResult
            .SuccessfulCalculationCandidates
    }

    private fun snapshot(
        candidate: KnockoutProductCandidateWithDataQuality
    ) = candidate.candidateWithMarketData.specificationSnapshot

    private fun snapshot(
        candidate: KnockoutProductCandidateWithCalculationAvailability
    ) = snapshot(candidate.candidateWithDataQuality)

    private fun snapshot(
        candidate: KnockoutProductCandidateWithFreshness
    ) = snapshot(candidate.candidateWithCalculationAvailability)

    private fun snapshot(
        candidate: KnockoutProductCandidateWithSourceEvaluation
    ) = snapshot(candidate.candidateWithFreshness)

    private fun snapshot(
        candidate: KnockoutProductCandidateWithCalculation
    ) = snapshot(candidate.candidateWithSourceEvaluation)

    private fun marketData(
        candidate: KnockoutProductCandidateWithCalculation
    ) = candidate.candidateWithSourceEvaluation.candidateWithFreshness
        .candidateWithCalculationAvailability.candidateWithDataQuality
        .candidateWithMarketData.marketData

    private fun snapshot(
        productIsin: String,
        basePrice: Double = 80.0
    ) = KnockoutProductSpecificationSnapshot(
        specification = KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = "SYN001",
            issuerId = "issuer",
            underlyingId = "underlying",
            direction = TradeDirection.LONG,
            basePrice = basePrice,
            knockoutBarrier = 82.0,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        ),
        sourceId = "SPEC_SOURCE",
        retrievedAtEpochMillis = 1_000L,
        sourceTimestampEpochMillis = 900L
    )

    private fun marketData(
        productIsin: String,
        bid: Double? = 9.0,
        ask: Double? = 10.0,
        bidTimestamp: Long? = 995L,
        askTimestamp: Long? = 995L,
        sourceId: String = "ALLOWED"
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestamp,
        askTimestampEpochMillis = askTimestamp,
        currency = "EUR",
        sourceId = sourceId
    )

    private fun sourcePolicy() = MarketDataSourcePolicy(
        MarketDataSourcePolicyConfig(
            listOf(
                MarketDataSourceRule(
                    sourceId = "ALLOWED",
                    supportedCalculationTypes = setOf(
                        MarketDataCalculationType.PURCHASE_PRICE,
                        MarketDataCalculationType.SALE_PRICE,
                        MarketDataCalculationType.SPREAD,
                        MarketDataCalculationType.MID
                    )
                )
            )
        )
    )

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
}
