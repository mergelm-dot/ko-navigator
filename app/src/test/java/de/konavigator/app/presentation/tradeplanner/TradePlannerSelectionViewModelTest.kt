package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationOutcome
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationPipelineApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionEvidence
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidatePlannedEntrySelectionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationRequest
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetLeverageInput
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetSelectionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithCalculation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithCalculationAvailability
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithCurrencyConversion
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithDataQuality
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithExistingEntryCalculation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithFreshness
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithMarketData
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithSourceEvaluation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithTargetDeviation
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithTargetFit
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithTargetLeveragePlan
import de.konavigator.app.application.productdiscovery.KnockoutProductDiscoveryApplicationResult
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitResult
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TradePlannerSelectionViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateContainsStableIdsDefaultsLongAndIdle() {
        val state = viewModel().uiState.value

        assertEquals(null, state.selectedUnderlyingId)
        assertEquals(null, state.selectedBrokerId)
        assertEquals(emptySet<String>(), state.enabledIssuerIds)
        assertEquals("100,00", state.currentUnderlyingPriceInput)
        assertEquals("95,00", state.plannedEntryPriceInput)
        assertEquals("3", state.targetLeverageInput)
        assertEquals(TradeDirection.LONG, state.direction)
        assertSame(TradePlannerSelectionUiSubmission.Idle, state.submission)
    }

    @Test
    fun callbacksStoreExactValuesAndIssuerSetIsDefensivelyCopied() {
        val viewModel = viewModel()
        val issuers = linkedSetOf("issuer-b", "issuer-a")

        viewModel.onUnderlyingSelected(" Underlying-ID ")
        viewModel.onBrokerSelected(" Broker-ID ")
        viewModel.onEnabledIssuerIdsChanged(issuers)
        viewModel.onCurrentPriceChanged(" 123,4500 ")
        viewModel.onPlannedEntryPriceChanged(" 118.765400 ")
        viewModel.onTargetLeverageChanged(" 4,250001 ")
        viewModel.onDirectionChanged(TradeDirection.SHORT)
        issuers += "external-change"

        val state = viewModel.uiState.value
        assertEquals(" Underlying-ID ", state.selectedUnderlyingId)
        assertEquals(" Broker-ID ", state.selectedBrokerId)
        assertEquals(setOf("issuer-b", "issuer-a"), state.enabledIssuerIds)
        assertNotSame(issuers, state.enabledIssuerIds)
        assertEquals(" 123,4500 ", state.currentUnderlyingPriceInput)
        assertEquals(" 118.765400 ", state.plannedEntryPriceInput)
        assertEquals(" 4,250001 ", state.targetLeverageInput)
        assertEquals(TradeDirection.SHORT, state.direction)
        assertSame(TradePlannerSelectionUiSubmission.Idle, state.submission)
    }

    @Test
    fun everyRelevantInputChangeResetsSubmissionToIdle() {
        val changes = listOf<(TradePlannerSelectionViewModel) -> Unit>(
            { it.onUnderlyingSelected("underlying") },
            { it.onBrokerSelected("broker") },
            { it.onEnabledIssuerIdsChanged(setOf("issuer")) },
            { it.onCurrentPriceChanged("101") },
            { it.onPlannedEntryPriceChanged("94") },
            { it.onTargetLeverageChanged("4") },
            { it.onDirectionChanged(TradeDirection.SHORT) }
        )

        changes.forEach { change ->
            val viewModel = viewModel()
            viewModel.onCalculateClicked()
            assertTrue(
                viewModel.uiState.value.submission is
                    TradePlannerSelectionUiSubmission.InvalidInput
            )

            change(viewModel)

            assertSame(TradePlannerSelectionUiSubmission.Idle, viewModel.uiState.value.submission)
        }
    }

    @Test
    fun missingIdsReturnStableErrorsWithoutExecutorOrEvaluationTime() {
        val executor = RecordingExecutor(noCatalogResult())
        val timeProvider = RecordingTimeProvider(1_111L)
        val viewModel = viewModel(executor, timeProvider)
        viewModel.onUnderlyingSelected("   ")
        viewModel.onBrokerSelected(null)

        viewModel.onCalculateClicked()

        assertEquals(
            listOf(
                TradePlannerSelectionUiInputError.UNDERLYING_REQUIRED,
                TradePlannerSelectionUiInputError.BROKER_REQUIRED
            ),
            invalidErrors(viewModel)
        )
        assertTrue(executor.requests.isEmpty())
        assertEquals(0, timeProvider.calls)
    }

    @Test
    fun numericRequiredAndInvalidValuesAreBlockedInStableCombinedOrder() {
        val invalidValues = listOf("not-a-number", "NaN", "Infinity", "-Infinity", "0", "-1")
        invalidValues.forEach { input ->
            val current = validViewModel()
            current.onCurrentPriceChanged(input)
            current.onCalculateClicked()
            assertEquals(
                listOf(TradePlannerSelectionUiInputError.CURRENT_PRICE_INVALID),
                invalidErrors(current)
            )

            val planned = validViewModel()
            planned.onPlannedEntryPriceChanged(input)
            planned.onCalculateClicked()
            assertEquals(
                listOf(TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_INVALID),
                invalidErrors(planned)
            )
        }
        (invalidValues + "1").forEach { input ->
            val leverage = validViewModel()
            leverage.onTargetLeverageChanged(input)
            leverage.onCalculateClicked()
            assertEquals(
                listOf(TradePlannerSelectionUiInputError.TARGET_LEVERAGE_INVALID),
                invalidErrors(leverage)
            )
        }

        val combined = validViewModel()
        combined.onCurrentPriceChanged("  ")
        combined.onPlannedEntryPriceChanged("NaN")
        combined.onTargetLeverageChanged("1")
        combined.onCalculateClicked()
        assertEquals(
            listOf(
                TradePlannerSelectionUiInputError.CURRENT_PRICE_REQUIRED,
                TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_INVALID,
                TradePlannerSelectionUiInputError.TARGET_LEVERAGE_INVALID
            ),
            invalidErrors(combined)
        )
    }

    @Test
    fun fullRequestPreservesIdsSettingsTimeAndUnroundedCommaOrPointValues() = runTest {
        val executor = RecordingExecutor(noCatalogResult())
        val timeProvider = RecordingTimeProvider(9_876_543_210L)
        val settings = TradePlannerSelectionExecutionSettings(
            calculationType = MarketDataCalculationType.SPREAD,
            maxFxAgeMillis = -98_765L,
            maxRelativeLeverageDeviationPercent = -2.3456789,
            maxBarrierDeviationPercentOfPlannedEntry = -4.5678912
        )
        val viewModel = viewModel(executor, timeProvider, settings)
        val issuerIds = setOf("issuer-a", "issuer-b")
        viewModel.onUnderlyingSelected(" underlying-test-1 ")
        viewModel.onBrokerSelected(" broker-test-1 ")
        viewModel.onEnabledIssuerIdsChanged(issuerIds)
        viewModel.onDirectionChanged(TradeDirection.SHORT)
        viewModel.onCurrentPriceChanged(" 123,456789 ")
        viewModel.onPlannedEntryPriceChanged(" 118.765432 ")
        viewModel.onTargetLeverageChanged(" 4,250001 ")

        viewModel.onCalculateClicked()

        assertSame(TradePlannerSelectionUiSubmission.Loading, viewModel.uiState.value.submission)
        assertTrue(executor.requests.isEmpty())
        assertEquals(1, timeProvider.calls)
        runCurrent()
        val request = executor.requests.single()
        assertEquals(" underlying-test-1 ", request.underlyingId)
        assertEquals(TradeDirection.SHORT, request.direction)
        assertEquals(" broker-test-1 ", request.brokerId)
        assertEquals(issuerIds, request.enabledIssuerIds)
        assertEquals(MarketDataCalculationType.SPREAD, request.calculationType)
        assertEquals(9_876_543_210L, request.evaluationTimeEpochMillis)
        assertEquals(-98_765L, request.maxFxAgeMillis)
        assertEquals(123.456789, request.underlyingPrice, 0.0)
        assertEquals(118.765432, request.plannedEntryPrice, 0.0)
        assertEquals(4.250001, request.targetLeverage, 0.0)
        assertEquals(-2.3456789, request.maxRelativeLeverageDeviationPercent, 0.0)
        assertEquals(-4.5678912, request.maxBarrierDeviationPercentOfPlannedEntry, 0.0)
        assertEquals(" 123,456789 ", viewModel.uiState.value.currentUnderlyingPriceInput)
        assertEquals(" 118.765432 ", viewModel.uiState.value.plannedEntryPriceInput)
        assertEquals(" 4,250001 ", viewModel.uiState.value.targetLeverageInput)
        assertEquals(1, timeProvider.calls)
    }

    @Test
    fun emptyIssuerSetIsValidAndReachesExecutor() = runTest {
        val executor = RecordingExecutor(noCatalogResult())
        val viewModel = validViewModel(executor = executor)

        viewModel.onCalculateClicked()
        runCurrent()

        assertEquals(emptySet<String>(), executor.requests.single().enabledIssuerIds)
        assertTrue(viewModel.uiState.value.submission is TradePlannerSelectionUiSubmission.Completed)
    }

    @Test
    fun selectedPipelineResultIsMappedToCompletedSelected() = runTest {
        val executor = RecordingExecutor(selectedApplicationResult())
        val viewModel = validViewModel(executor = executor)

        viewModel.onCalculateClicked()
        runCurrent()

        val result = completedResult(viewModel)
        assertTrue(result is TradePlannerSelectionUiResult.Selected)
        result as TradePlannerSelectionUiResult.Selected
        assertEquals("SYNTHETIC-PRODUCT", result.primaryCandidate.productIsin)
        assertEquals(91.25, result.primaryCandidate.knockoutBarrier, 0.0)
    }

    @Test
    fun noSelectionAndInconsistentPipelineResultsUsePublishedMapper() = runTest {
        val noSelectionViewModel = validViewModel(
            executor = RecordingExecutor(noCatalogResult())
        )
        noSelectionViewModel.onCalculateClicked()
        runCurrent()
        assertEquals(
            TradePlannerSelectionUiResult.NoSelection(
                reason = TradePlannerSelectionUiNoSelectionReason.NO_CATALOG_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics()
            ),
            completedResult(noSelectionViewModel)
        )

        val inconsistentViewModel = validViewModel(
            executor = RecordingExecutor(inconsistentApplicationResult())
        )
        inconsistentViewModel.onCalculateClicked()
        runCurrent()
        assertEquals(
            TradePlannerSelectionUiResult.InconsistentData(
                TradePlannerSelectionUiMappingError
                    .CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT
            ),
            completedResult(inconsistentViewModel)
        )
    }

    @Test
    fun inputChangeDuringLoadingInvalidatesNonCooperativeOldResult() = runTest {
        val executor = ControlledExecutor()
        val viewModel = validViewModel(executor = executor)
        viewModel.onCalculateClicked()
        assertSame(TradePlannerSelectionUiSubmission.Loading, viewModel.uiState.value.submission)
        runCurrent()
        assertEquals(1, executor.calls.size)

        viewModel.onBrokerSelected("new-broker")
        assertEquals("new-broker", viewModel.uiState.value.selectedBrokerId)
        assertSame(TradePlannerSelectionUiSubmission.Idle, viewModel.uiState.value.submission)
        executor.complete(0, noCatalogResult())
        runCurrent()

        assertSame(TradePlannerSelectionUiSubmission.Idle, viewModel.uiState.value.submission)
    }

    @Test
    fun repeatedCalculatePublishesOnlyNewestNonCooperativeResult() = runTest {
        val executor = ControlledExecutor()
        val timeProvider = RecordingTimeProvider(101L, 202L)
        val viewModel = validViewModel(executor = executor, timeProvider = timeProvider)
        viewModel.onCalculateClicked()
        runCurrent()
        viewModel.onCalculateClicked()
        assertSame(TradePlannerSelectionUiSubmission.Loading, viewModel.uiState.value.submission)
        runCurrent()
        assertEquals(2, executor.calls.size)
        assertEquals(2, timeProvider.calls)

        executor.complete(0, noCatalogResult())
        runCurrent()
        assertSame(TradePlannerSelectionUiSubmission.Loading, viewModel.uiState.value.submission)

        executor.complete(1, noBrokerCandidatesResult())
        runCurrent()
        assertEquals(
            TradePlannerSelectionUiResult.NoSelection(
                reason = TradePlannerSelectionUiNoSelectionReason.NO_BROKER_TRADABLE_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics()
            ),
            completedResult(viewModel)
        )
        assertEquals(listOf(101L, 202L), executor.calls.map { it.request.evaluationTimeEpochMillis })
    }

    @Test
    fun constructorAndPublicContractsRemainPresentationOnly() {
        assertEquals(
            listOf(
                TradePlannerSelectionExecutor::class.java,
                TradePlannerSelectionExecutionSettings::class.java,
                TradePlannerSelectionEvaluationTimeProvider::class.java
            ),
            TradePlannerSelectionViewModel::class.java.constructors.single().parameterTypes.toList()
        )
        assertEquals(
            listOf(
                "UNDERLYING_REQUIRED",
                "BROKER_REQUIRED",
                "CURRENT_PRICE_REQUIRED",
                "CURRENT_PRICE_INVALID",
                "PLANNED_ENTRY_PRICE_REQUIRED",
                "PLANNED_ENTRY_PRICE_INVALID",
                "TARGET_LEVERAGE_REQUIRED",
                "TARGET_LEVERAGE_INVALID"
            ),
            TradePlannerSelectionUiInputError.entries.map { it.name }
        )
        assertEquals(
            setOf("Idle", "InvalidInput", "Loading", "Completed"),
            TradePlannerSelectionUiSubmission::class.java.declaredClasses
                .map { it.simpleName }.toSet()
        )
        val forbidden = listOf(
            "repository",
            "provider.fx",
            "calculator",
            "ranker",
            "selector",
            "android.content",
            "compose"
        )
        val dependencyNames = TradePlannerSelectionViewModel::class.java.declaredFields
            .map { it.type.name }
        assertFalse(
            dependencyNames.any { name -> forbidden.any { name.contains(it, ignoreCase = true) } }
        )
    }

    private fun viewModel(
        executor: TradePlannerSelectionExecutor = RecordingExecutor(noCatalogResult()),
        timeProvider: RecordingTimeProvider = RecordingTimeProvider(1_234L),
        settings: TradePlannerSelectionExecutionSettings = settings()
    ) = TradePlannerSelectionViewModel(executor, settings, timeProvider)

    private fun validViewModel(
        executor: TradePlannerSelectionExecutor = RecordingExecutor(noCatalogResult()),
        timeProvider: RecordingTimeProvider = RecordingTimeProvider(1_234L)
    ) = viewModel(executor, timeProvider).apply {
        onUnderlyingSelected("underlying")
        onBrokerSelected("broker")
    }

    private fun settings() = TradePlannerSelectionExecutionSettings(
        calculationType = MarketDataCalculationType.MID,
        maxFxAgeMillis = 5_000L,
        maxRelativeLeverageDeviationPercent = 5.0,
        maxBarrierDeviationPercentOfPlannedEntry = 10.0
    )

    private fun invalidErrors(
        viewModel: TradePlannerSelectionViewModel
    ) = (viewModel.uiState.value.submission as
        TradePlannerSelectionUiSubmission.InvalidInput).errors

    private fun completedResult(
        viewModel: TradePlannerSelectionViewModel
    ) = (viewModel.uiState.value.submission as
        TradePlannerSelectionUiSubmission.Completed).result

    private fun noCatalogResult() = discoveryStopped(
        KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates
    )

    private fun noBrokerCandidatesResult() = discoveryStopped(
        KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates
    )

    private fun discoveryStopped(
        result: KnockoutProductDiscoveryApplicationResult
    ) = KnockoutProductCandidateSelectionPipelineApplicationResult.CalculationPipelineStopped(
        KnockoutProductCandidateCalculationPipelineApplicationResult.DiscoveryStopped(result)
    )

    private fun inconsistentApplicationResult() =
        KnockoutProductCandidateSelectionPipelineApplicationResult.CalculationPipelineStopped(
            calculationSuccess()
        )

    private fun calculationSuccess(
        candidate: KnockoutProductCandidateWithCalculation? = null
    ) = KnockoutProductCandidateCalculationPipelineApplicationResult
        .SuccessfulCalculationCandidates(
            successfulCandidates = listOfNotNull(candidate),
            blockedDataQualityCandidates = emptyList(),
            calculationUnavailableCandidates = emptyList(),
            notFreshCandidates = emptyList(),
            sourceBlockedCandidates = emptyList(),
            failedCalculationCandidates = emptyList()
        )

    private fun selectedApplicationResult():
        KnockoutProductCandidateSelectionPipelineApplicationResult {
        val eur = currency("EUR")
        val snapshot = KnockoutProductSpecificationSnapshot(
            specification = KnockoutProductSpecification(
                productIsin = "SYNTHETIC-PRODUCT",
                productWkn = "SYN001",
                issuerId = "synthetic-issuer",
                underlyingId = "underlying",
                direction = TradeDirection.LONG,
                basePrice = 90.0,
                knockoutBarrier = 91.25,
                ratio = 0.1,
                underlyingCurrency = "EUR",
                productCurrency = "EUR"
            ),
            sourceId = "SYNTHETIC-SPECIFICATION-SOURCE",
            retrievedAtEpochMillis = 1_000L,
            sourceTimestampEpochMillis = 900L
        )
        val marketData = KnockoutProductCandidateWithMarketData(
            specificationSnapshot = snapshot,
            marketData = KnockoutProductMarketData(
                productIsin = "SYNTHETIC-PRODUCT",
                bid = 1.8,
                ask = 1.9,
                bidTimestampEpochMillis = 995L,
                askTimestampEpochMillis = 995L,
                currency = "EUR",
                sourceId = "SYNTHETIC-MARKET-SOURCE"
            )
        )
        val dataQuality = KnockoutProductCandidateWithDataQuality(
            candidateWithMarketData = marketData,
            dataQualityAssessment = DataQualityAssessment.passed()
        )
        val availability = KnockoutProductCandidateWithCalculationAvailability(
            candidateWithDataQuality = dataQuality,
            availabilityResult = MarketDataCalculationAvailabilityResult.StructurallyAvailable
        )
        val freshness = KnockoutProductCandidateWithFreshness(
            candidateWithCalculationAvailability = availability,
            freshnessResult = MarketDataFreshnessResult.Fresh
        )
        val source = KnockoutProductCandidateWithSourceEvaluation(
            candidateWithFreshness = freshness,
            sourceResult = MarketDataSourceResult.Allowed
        )
        val calculation = KnockoutProductCandidateWithCalculation(
            candidateWithSourceEvaluation = source,
            calculationOutcome = KnockoutProductCandidateCalculationOutcome.Success(
                MarketDataCalculationValue.MidPrice(1.85, "EUR")
            )
        )
        val targetInput = KnockoutProductCandidateTargetLeverageInput(
            candidateWithCalculation = calculation,
            currencyConversion = CurrencyConversion.SameCurrency(eur)
        )
        val targetPlan = KnockoutProductCandidateWithTargetLeveragePlan(
            input = targetInput,
            tradeCalculationResult = TradeCalculationResult(
                isValid = true,
                underlyingPrice = 100.0,
                targetLeverage = 5.0,
                knockoutPrice = 88.0,
                theoreticalValueInUnderlyingCurrency = 20.0,
                theoreticalProductValue = 2.0,
                underlyingExposureInProductCurrency = 10.0,
                calculatedTheoreticalLeverageAtEntry = 5.0,
                underlyingCurrency = eur,
                productCurrency = eur,
                distanceToKnockoutAbsolute = 12.0,
                distanceToKnockoutPercent = 12.0
            )
        )
        val existingEntry = KnockoutProductCandidateWithExistingEntryCalculation(
            candidateWithTargetLeveragePlan = targetPlan,
            existingEntryCalculationResult =
                ExistingKnockoutProductEntryCalculationResult.Success(
                    intrinsicValueInUnderlyingCurrency = 18.0,
                    theoreticalProductValue = 1.84,
                    knockoutDistanceAbsolute = 8.75,
                    knockoutDistancePercent = 8.75,
                    underlyingExposureInProductCurrency = 10.0,
                    calculatedLeverageAtEntry = 4.72,
                    underlyingCurrency = eur,
                    productCurrency = eur
                )
        )
        val deviation = KnockoutProductCandidateWithTargetDeviation(
            candidateWithExistingEntryCalculation = existingEntry,
            targetDeviationResult = ExistingKnockoutProductTargetDeviationResult.Success(
                leverageDifference = -0.28,
                absoluteLeverageDeviation = 0.28,
                relativeLeverageDeviationPercent = 5.6,
                barrierDifference = 3.25,
                absoluteBarrierDeviation = 3.25,
                barrierDeviationPercentOfPlannedEntry = 3.25
            )
        )
        val fit = KnockoutProductCandidateWithTargetFit(
            candidateWithTargetDeviation = deviation,
            targetFitResult = ExistingKnockoutProductTargetFitResult.Success(
                leverageWithinTolerance = true,
                barrierWithinTolerance = true,
                withinAllTargetTolerances = true
            )
        )
        val currencyResult = KnockoutProductCandidateCurrencyConversionApplicationResult
            .CandidatesWithCurrencyConversion(
                successfulCandidates = listOf(
                    KnockoutProductCandidateWithCurrencyConversion(
                        targetLeverageInput = targetInput,
                        evidence = KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
                    )
                ),
                failedCandidates = emptyList()
            )
        return KnockoutProductCandidateSelectionPipelineApplicationResult
            .PlannedEntrySelectionEvaluated(
                calculationPipelineResult = calculationSuccess(calculation),
                currencyConversionResult = currencyResult,
                plannedEntrySelectionResult =
                    KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                        .TargetSelectionEvaluated(
                            targetSelectionResult =
                                KnockoutProductCandidateTargetSelectionApplicationResult
                                    .SelectedCandidates(
                                        primaryCandidate = fit,
                                        alternativeCandidates = emptyList(),
                                        targetDeviationFailedCandidates = emptyList(),
                                        nonMatchingCandidates = emptyList(),
                                        targetFitFailedCandidates = emptyList()
                                    ),
                            invalidTargetLeveragePlanCandidates = emptyList(),
                            existingEntryFailedCandidates = emptyList()
                        )
            )
    }

    private fun currency(value: String): CurrencyCode = when (val result = CurrencyCode.create(value)) {
        is CurrencyCodeCreationResult.Success -> result.currencyCode
        is CurrencyCodeCreationResult.Failure ->
            error("Unexpected invalid synthetic currency: ${result.error}")
    }

    private class RecordingExecutor(
        private val result: KnockoutProductCandidateSelectionPipelineApplicationResult
    ) : TradePlannerSelectionExecutor {
        val requests = mutableListOf<KnockoutProductCandidateSelectionPipelineApplicationRequest>()

        override suspend fun execute(
            request: KnockoutProductCandidateSelectionPipelineApplicationRequest
        ): KnockoutProductCandidateSelectionPipelineApplicationResult {
            requests += request
            return result
        }
    }

    private class RecordingTimeProvider(
        vararg evaluationTimes: Long
    ) : TradePlannerSelectionEvaluationTimeProvider {
        private val values = ArrayDeque(evaluationTimes.toList())
        var calls = 0
            private set

        override fun evaluationTimeEpochMillis(): Long {
            calls++
            return values.removeFirst()
        }
    }

    private class ControlledExecutor : TradePlannerSelectionExecutor {
        val calls = mutableListOf<ControlledCall>()

        override suspend fun execute(
            request: KnockoutProductCandidateSelectionPipelineApplicationRequest
        ): KnockoutProductCandidateSelectionPipelineApplicationResult = suspendCoroutine {
                continuation ->
            calls += ControlledCall(request, continuation)
        }

        fun complete(
            index: Int,
            result: KnockoutProductCandidateSelectionPipelineApplicationResult
        ) {
            calls[index].continuation.resume(result)
        }
    }

    private data class ControlledCall(
        val request: KnockoutProductCandidateSelectionPipelineApplicationRequest,
        val continuation: Continuation<KnockoutProductCandidateSelectionPipelineApplicationResult>
    )
}
