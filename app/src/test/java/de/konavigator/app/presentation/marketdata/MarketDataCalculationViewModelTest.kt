package de.konavigator.app.presentation.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationError
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationResult
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceError
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import java.lang.reflect.Modifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarketDataCalculationViewModelTest {

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
    fun scenario01InitialStateContainsEmptyInputsPurchasePriceAndIdle() {
        val state = viewModel().uiState.value

        assertEquals("", state.productIsinInput)
        assertEquals(MarketDataCalculationType.PURCHASE_PRICE, state.selectedCalculationType)
        assertEquals("", state.evaluationTimeEpochMillisInput)
        assertEquals(MarketDataCalculationUiSubmission.Idle, state.submission)
        assertFalse(state.isCalculateEnabled)
    }

    @Test
    fun scenario02ProductIsinInputIsTakenOverExactly() {
        val viewModel = viewModel()

        viewModel.onProductIsinChanged(PRODUCT_ISIN)

        assertEquals(PRODUCT_ISIN, viewModel.uiState.value.productIsinInput)
        assertEquals(MarketDataCalculationUiSubmission.Idle, viewModel.uiState.value.submission)
    }

    @Test
    fun scenario03ProductIsinCaseAndWhitespaceRemainUnchanged() {
        val viewModel = viewModel()
        val input = "  de000Test001  "

        viewModel.onProductIsinChanged(input)

        assertEquals(input, viewModel.uiState.value.productIsinInput)
    }

    @Test
    fun scenario04CalculationTypeIsTakenOverUnchanged() {
        val viewModel = viewModel()

        viewModel.onCalculationTypeChanged(MarketDataCalculationType.MID)

        assertEquals(
            MarketDataCalculationType.MID,
            viewModel.uiState.value.selectedCalculationType
        )
        assertEquals(MarketDataCalculationUiSubmission.Idle, viewModel.uiState.value.submission)
    }

    @Test
    fun scenario05TimestampInputIsTakenOverExactly() {
        val viewModel = viewModel()
        val input = " 1000 "

        viewModel.onEvaluationTimeChanged(input)

        assertEquals(input, viewModel.uiState.value.evaluationTimeEpochMillisInput)
    }

    @Test
    fun scenario06EmptyIsinPreventsServicePathAndReturnsRequiredError() =
        runTest(mainDispatcher) {
            val specificationRepository = RecordingSpecificationRepository()
            val viewModel = viewModel(specificationRepository = specificationRepository)
            viewModel.onEvaluationTimeChanged(NOW.toString())

            viewModel.onCalculateClicked()
            advanceUntilIdle()

            assertEquals(
                listOf(MarketDataCalculationUiInputError.PRODUCT_ISIN_REQUIRED),
                invalidInputErrors(viewModel)
            )
            assertEquals(0, specificationRepository.callCount)
        }

    @Test
    fun scenario07EmptyTimestampPreventsServicePathAndReturnsRequiredError() =
        runTest(mainDispatcher) {
            val specificationRepository = RecordingSpecificationRepository()
            val viewModel = viewModel(specificationRepository = specificationRepository)
            viewModel.onProductIsinChanged(PRODUCT_ISIN)

            viewModel.onCalculateClicked()
            advanceUntilIdle()

            assertEquals(
                listOf(MarketDataCalculationUiInputError.EVALUATION_TIME_REQUIRED),
                invalidInputErrors(viewModel)
            )
            assertEquals(0, specificationRepository.callCount)
        }

    @Test
    fun scenario08UnparseableTimestampReturnsInvalidError() = runTest(mainDispatcher) {
        val specificationRepository = RecordingSpecificationRepository()
        val viewModel = viewModel(specificationRepository = specificationRepository)
        viewModel.onProductIsinChanged(PRODUCT_ISIN)
        viewModel.onEvaluationTimeChanged("not-a-long")

        viewModel.onCalculateClicked()
        advanceUntilIdle()

        assertEquals(
            listOf(MarketDataCalculationUiInputError.EVALUATION_TIME_INVALID),
            invalidInputErrors(viewModel)
        )
        assertEquals(0, specificationRepository.callCount)
    }

    @Test
    fun scenario09WhitespaceAroundOtherwiseValidTimestampRemainsInvalid() =
        runTest(mainDispatcher) {
            val viewModel = viewModel()
            viewModel.onProductIsinChanged(PRODUCT_ISIN)
            viewModel.onEvaluationTimeChanged(" 1000 ")

            viewModel.onCalculateClicked()

            assertEquals(
                listOf(MarketDataCalculationUiInputError.EVALUATION_TIME_INVALID),
                invalidInputErrors(viewModel)
            )
            assertFalse(viewModel.uiState.value.isCalculateEnabled)
        }

    @Test
    fun scenario10NegativeMinimumAndMaximumLongTimestampsAreForwardedUnchanged() =
        runTest(mainDispatcher) {
            listOf(-100L, Long.MIN_VALUE, Long.MAX_VALUE).forEach { timestamp ->
                val specificationRepository = RecordingSpecificationRepository()
                val marketDataRepository = RecordingMarketDataRepository(
                    result = RepositoryResult.Success(
                        marketData(
                            bidTimestampEpochMillis = timestamp,
                            askTimestampEpochMillis = timestamp
                        )
                    )
                )
                val viewModel = viewModel(
                    specificationRepository = specificationRepository,
                    marketDataRepository = marketDataRepository,
                    thresholds = strictThresholds()
                )
                fillValidInputs(viewModel, timestamp = timestamp)

                viewModel.onCalculateClicked()
                advanceUntilIdle()

                assertTrue(completedResult(viewModel) is MarketDataCalculationUiResult.PurchasePrice)
                assertEquals(PRODUCT_ISIN, specificationRepository.receivedProductIsins.single())
            }
        }

    @Test
    fun scenario11ValidRequestContainsExactIsinCalculationTypeAndTimestamp() =
        runTest(mainDispatcher) {
            val timestamp = -77L
            val specificationRepository = RecordingSpecificationRepository()
            val marketDataRepository = RecordingMarketDataRepository(
                result = RepositoryResult.Success(
                    marketData(
                        bidTimestampEpochMillis = timestamp,
                        askTimestampEpochMillis = timestamp
                    )
                )
            )
            val viewModel = viewModel(
                specificationRepository = specificationRepository,
                marketDataRepository = marketDataRepository,
                thresholds = strictThresholds()
            )
            fillValidInputs(
                viewModel = viewModel,
                productIsin = PRODUCT_ISIN,
                calculationType = MarketDataCalculationType.SALE_PRICE,
                timestamp = timestamp
            )

            viewModel.onCalculateClicked()
            advanceUntilIdle()

            assertEquals(listOf(PRODUCT_ISIN), specificationRepository.receivedProductIsins)
            assertEquals(listOf(PRODUCT_ISIN), marketDataRepository.receivedProductIsins)
            assertTrue(completedResult(viewModel) is MarketDataCalculationUiResult.SalePrice)
        }

    @Test
    fun scenario12OneClickStartsServicePathExactlyOnce() = runTest(mainDispatcher) {
        val specificationRepository = RecordingSpecificationRepository()
        val marketDataRepository = RecordingMarketDataRepository()
        val viewModel = viewModel(specificationRepository, marketDataRepository)
        fillValidInputs(viewModel)

        viewModel.onCalculateClicked()
        advanceUntilIdle()

        assertEquals(1, specificationRepository.callCount)
        assertEquals(1, marketDataRepository.callCount)
    }

    @Test
    fun scenario13SubmissionIsLoadingBeforeCompletionAndCompletedAfterwards() =
        runTest(mainDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val specificationRepository = RecordingSpecificationRepository(gate = gate)
            val viewModel = viewModel(specificationRepository = specificationRepository)
            fillValidInputs(viewModel)

            viewModel.onCalculateClicked()
            assertEquals(
                MarketDataCalculationUiSubmission.Loading,
                viewModel.uiState.value.submission
            )
            runCurrent()
            assertEquals(
                MarketDataCalculationUiSubmission.Loading,
                viewModel.uiState.value.submission
            )

            gate.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.submission is MarketDataCalculationUiSubmission.Completed)
        }

    @Test
    fun scenario14SecondClickDuringLoadingStartsNoAdditionalRequest() =
        runTest(mainDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val specificationRepository = RecordingSpecificationRepository(gate = gate)
            val viewModel = viewModel(specificationRepository = specificationRepository)
            fillValidInputs(viewModel)

            viewModel.onCalculateClicked()
            runCurrent()
            viewModel.onCalculateClicked()
            runCurrent()

            assertEquals(1, specificationRepository.callCount)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, specificationRepository.callCount)
        }

    @Test
    fun scenario15InputChangeDuringRequestPreventsStaleResult() = runTest(mainDispatcher) {
        val gate = CompletableDeferred<Unit>()
        val specificationRepository = RecordingSpecificationRepository(gate = gate)
        val viewModel = viewModel(specificationRepository = specificationRepository)
        fillValidInputs(viewModel)
        viewModel.onCalculateClicked()
        runCurrent()

        viewModel.onProductIsinChanged("NEW-ISIN")
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("NEW-ISIN", viewModel.uiState.value.productIsinInput)
        assertEquals(MarketDataCalculationUiSubmission.Idle, viewModel.uiState.value.submission)
        assertFalse(
            completedResultOrNull(viewModel) is MarketDataCalculationUiResult.Failure &&
                (completedResultOrNull(viewModel) as MarketDataCalculationUiResult.Failure).error ==
                MarketDataCalculationUiError.UNEXPECTED_FAILURE
        )
    }

    @Test
    fun scenario16AllFiveApplicationErrorsMapToUiErrors() {
        val mappings = listOf(
            MarketDataCalculationApplicationError.PRODUCT_NOT_FOUND to
                MarketDataCalculationUiError.PRODUCT_NOT_FOUND,
            MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND to
                MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND,
            MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE to
                MarketDataCalculationUiError.DATA_ACCESS_FAILURE,
            MarketDataCalculationApplicationError.INVALID_PRODUCT_SPECIFICATION to
                MarketDataCalculationUiError.INVALID_MARKET_DATA,
            MarketDataCalculationApplicationError.INVALID_PRODUCT_MARKET_DATA to
                MarketDataCalculationUiError.INVALID_MARKET_DATA
        )

        mappings.forEach { (applicationError, uiError) ->
            val result = MarketDataCalculationApplicationResult.DataUnavailable(
                applicationError
            ).toUiResult()

            assertEquals(MarketDataCalculationUiResult.Failure(uiError), result)
        }
    }

    @Test
    fun scenario17AllFiveDomainFailureTypesMapToUiErrors() {
        val mappings = listOf(
            MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked(
                DataQualityAssessment.blocked(
                    listOf(
                        DataQualityFinding(
                            category = DataQualityCategory.INVALID_NUMERIC_VALUE,
                            severity = DataQualitySeverity.BLOCKING,
                            code = DataQualityFindingCode.MARKET_DATA_INVALID_BID,
                            component = DataQualityComponent.PRODUCT_MARKET_DATA
                        )
                    )
                )
            ) to MarketDataCalculationUiError.INVALID_MARKET_DATA,
            MarketDataCalculationOrchestrationResult.StructurallyUnavailable(
                errors = listOf(MarketDataCalculationAvailabilityError.MISSING_ASK),
                dataQualityAssessment = DataQualityAssessment.passed()
            ) to MarketDataCalculationUiError.REQUIRED_QUOTE_UNAVAILABLE,
            MarketDataCalculationOrchestrationResult.NotFresh(
                errors = listOf(MarketDataFreshnessError.STALE_ASK),
                dataQualityAssessment = DataQualityAssessment.passed()
            ) to MarketDataCalculationUiError.MARKET_DATA_NOT_FRESH,
            MarketDataCalculationOrchestrationResult.SourceBlocked(
                error = MarketDataSourceError.SOURCE_NOT_CONFIGURED,
                dataQualityAssessment = DataQualityAssessment.passed()
            ) to MarketDataCalculationUiError.SOURCE_UNAVAILABLE,
            MarketDataCalculationOrchestrationResult.CalculationFailure(
                error = MarketDataCalculationError.INVALID_ASK,
                dataQualityAssessment = DataQualityAssessment.passed()
            ) to MarketDataCalculationUiError.CALCULATION_FAILED
        )

        mappings.forEach { (domainResult, uiError) ->
            val result = MarketDataCalculationApplicationResult.DomainEvaluated(
                domainResult
            ).toUiResult()

            assertEquals(MarketDataCalculationUiResult.Failure(uiError), result)
        }
    }

    @Test
    fun scenario18PurchasePriceAndCurrencyRemainUnchanged() {
        val value = 2.123456789
        val result = successResult(MarketDataCalculationValue.PurchasePrice(value, "USD"))

        assertEquals(MarketDataCalculationUiResult.PurchasePrice(value, "USD"), result)
    }

    @Test
    fun scenario19SalePriceAndCurrencyRemainUnchanged() {
        val value = 1.987654321
        val result = successResult(MarketDataCalculationValue.SalePrice(value, "CHF"))

        assertEquals(MarketDataCalculationUiResult.SalePrice(value, "CHF"), result)
    }

    @Test
    fun scenario20BothSpreadValuesAndCurrencyRemainUnrounded() {
        val absolute = 0.123456789
        val relative = 6.666666666666667
        val result = successResult(
            MarketDataCalculationValue.Spread(absolute, relative, "EUR")
        )

        assertEquals(
            MarketDataCalculationUiResult.Spread(absolute, relative, "EUR"),
            result
        )
    }

    @Test
    fun scenario21MidPriceAndCurrencyRemainUnrounded() {
        val value = 0.15000000000000002
        val result = successResult(MarketDataCalculationValue.MidPrice(value, "JPY"))

        assertEquals(MarketDataCalculationUiResult.MidPrice(value, "JPY"), result)
    }

    @Test
    fun scenario22UnexpectedExceptionMapsWithoutThrowableOrMessagePayload() =
        runTest(mainDispatcher) {
            val specificationRepository = RecordingSpecificationRepository(
                failure = IllegalStateException("sensitive detail")
            )
            val viewModel = viewModel(specificationRepository = specificationRepository)
            fillValidInputs(viewModel)

            viewModel.onCalculateClicked()
            advanceUntilIdle()

            assertEquals(
                MarketDataCalculationUiResult.Failure(
                    MarketDataCalculationUiError.UNEXPECTED_FAILURE
                ),
                completedResult(viewModel)
            )
            assertEquals(
                listOf("error"),
                instanceFields(MarketDataCalculationUiResult.Failure::class.java)
                    .map { it.name }
            )
            assertTrue(
                instanceFields(MarketDataCalculationUiResult.Failure::class.java)
                    .none { Throwable::class.java.isAssignableFrom(it.type) }
            )
        }

    @Test
    fun scenario23PublicApiHasOnlyApprovedPresentationDependenciesAndExactContract() {
        assertEquals(
            listOf(
                "productIsinInput",
                "selectedCalculationType",
                "evaluationTimeEpochMillisInput",
                "submission"
            ),
            instanceFields(MarketDataCalculationUiState::class.java).map { it.name }
        )
        assertEquals(
            setOf("Idle", "Loading", "InvalidInput", "Completed"),
            MarketDataCalculationUiSubmission::class.java.declaredClasses
                .map { it.simpleName }
                .toSet()
        )
        assertEquals(
            listOf(
                "PRODUCT_ISIN_REQUIRED",
                "EVALUATION_TIME_REQUIRED",
                "EVALUATION_TIME_INVALID"
            ),
            MarketDataCalculationUiInputError.entries.map { it.name }
        )
        assertEquals(
            setOf("PurchasePrice", "SalePrice", "Spread", "MidPrice", "Failure"),
            MarketDataCalculationUiResult::class.java.declaredClasses
                .map { it.simpleName }
                .toSet()
        )
        assertEquals(11, MarketDataCalculationUiError.entries.size)
        assertEquals(
            listOf(MarketDataCalculationApplicationService::class.java),
            MarketDataCalculationViewModel::class.java.constructors
                .single()
                .parameterTypes
                .toList()
        )
        assertEquals(
            setOf(
                "onProductIsinChanged",
                "onCalculationTypeChanged",
                "onEvaluationTimeChanged",
                "onCalculateClicked"
            ),
            MarketDataCalculationViewModel::class.java.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) && it.name.startsWith("on") }
                .map { it.name }
                .toSet()
        )
        assertEquals(
            23,
            MarketDataCalculationViewModelTest::class.java.declaredMethods
                .count { it.getAnnotation(Test::class.java) != null }
        )

        val forbiddenFragments = listOf(
            "android.content.Context",
            "resource",
            "repository",
            "policy",
            "orchestrator",
            "compose",
            "message",
            "text"
        )
        assertTrue(
            publicViewModelApiTypeNames().none { typeName ->
                forbiddenFragments.any { typeName.contains(it, ignoreCase = true) }
            }
        )
        assertTrue(
            presentationResultFields().none {
                Throwable::class.java.isAssignableFrom(it.type)
            }
        )
    }

    private fun fillValidInputs(
        viewModel: MarketDataCalculationViewModel,
        productIsin: String = PRODUCT_ISIN,
        calculationType: MarketDataCalculationType =
            MarketDataCalculationType.PURCHASE_PRICE,
        timestamp: Long = NOW
    ) {
        viewModel.onProductIsinChanged(productIsin)
        viewModel.onCalculationTypeChanged(calculationType)
        viewModel.onEvaluationTimeChanged(timestamp.toString())
    }

    private fun invalidInputErrors(
        viewModel: MarketDataCalculationViewModel
    ): List<MarketDataCalculationUiInputError> =
        (viewModel.uiState.value.submission as
            MarketDataCalculationUiSubmission.InvalidInput).errors

    private fun completedResult(
        viewModel: MarketDataCalculationViewModel
    ): MarketDataCalculationUiResult =
        (viewModel.uiState.value.submission as
            MarketDataCalculationUiSubmission.Completed).result

    private fun completedResultOrNull(
        viewModel: MarketDataCalculationViewModel
    ): MarketDataCalculationUiResult? =
        (viewModel.uiState.value.submission as?
            MarketDataCalculationUiSubmission.Completed)?.result

    private fun successResult(
        value: MarketDataCalculationValue
    ): MarketDataCalculationUiResult =
        MarketDataCalculationApplicationResult.DomainEvaluated(
            MarketDataCalculationOrchestrationResult.Success(
                value = value,
                dataQualityAssessment = DataQualityAssessment.passed()
            )
        ).toUiResult()

    private fun viewModel(
        specificationRepository: RecordingSpecificationRepository =
            RecordingSpecificationRepository(),
        marketDataRepository: RecordingMarketDataRepository =
            RecordingMarketDataRepository(),
        thresholds: MarketDataFreshnessThresholds = thresholds()
    ): MarketDataCalculationViewModel {
        val orchestrator = MarketDataCalculationOrchestrator(
            freshnessPolicy = MarketDataFreshnessPolicy(thresholds),
            sourcePolicy = MarketDataSourcePolicy(
                MarketDataSourcePolicyConfig(
                    listOf(
                        MarketDataSourceRule(
                            sourceId = SOURCE_ID,
                            supportedCalculationTypes =
                                MarketDataCalculationType.entries.toSet()
                        )
                    )
                )
            )
        )
        val service = MarketDataCalculationApplicationService(
            specificationRepository = specificationRepository,
            marketDataRepository = marketDataRepository,
            orchestrator = orchestrator
        )
        return MarketDataCalculationViewModel(service)
    }

    private fun thresholds() = MarketDataFreshnessThresholds(
        maxBidAgeMillis = Long.MAX_VALUE,
        maxAskAgeMillis = Long.MAX_VALUE,
        maxBidAskDifferenceMillis = Long.MAX_VALUE,
        allowedFutureSkewMillis = Long.MAX_VALUE
    )

    private fun strictThresholds() = MarketDataFreshnessThresholds(
        maxBidAgeMillis = 0L,
        maxAskAgeMillis = 0L,
        maxBidAskDifferenceMillis = 0L,
        allowedFutureSkewMillis = 0L
    )

    private fun instanceFields(type: Class<*>) = type.declaredFields
        .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }

    private fun publicViewModelApiTypeNames(): Set<String> = buildSet {
        MarketDataCalculationViewModel::class.java.constructors.forEach { constructor ->
            constructor.parameterTypes.forEach { add(it.name) }
        }
        MarketDataCalculationViewModel::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { add(it.name) }
                method.genericReturnType.typeName.let(::add)
                method.genericParameterTypes.forEach { add(it.typeName) }
            }
    }

    private fun presentationResultFields() =
        (MarketDataCalculationUiResult::class.java.declaredClasses +
            MarketDataCalculationUiSubmission::class.java.declaredClasses)
            .flatMap(::instanceFields)

    private class RecordingSpecificationRepository(
        private val result: RepositoryResult<KnockoutProductSpecification> =
            RepositoryResult.Success(specification()),
        private val gate: CompletableDeferred<Unit>? = null,
        private val failure: Exception? = null
    ) : KnockoutProductSpecificationRepository {

        var callCount: Int = 0
            private set

        val receivedProductIsins = mutableListOf<String>()

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductSpecification> {
            callCount++
            receivedProductIsins += productIsin
            gate?.await()
            failure?.let { throw it }
            return result
        }
    }

    private class RecordingMarketDataRepository(
        private val result: RepositoryResult<KnockoutProductMarketData> =
            RepositoryResult.Success(marketData())
    ) : KnockoutProductMarketDataRepository {

        var callCount: Int = 0
            private set

        val receivedProductIsins = mutableListOf<String>()

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductMarketData> {
            callCount++
            receivedProductIsins += productIsin
            return result
        }
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val SOURCE_ID = "source-a"
        const val NOW = 1_000L

        fun specification() = KnockoutProductSpecification(
            productIsin = PRODUCT_ISIN,
            productWkn = "ABC123",
            issuerId = "issuer-a",
            underlyingId = "underlying-a",
            direction = TradeDirection.LONG,
            basePrice = 80.0,
            knockoutBarrier = 82.0,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        )

        fun marketData(
            bidTimestampEpochMillis: Long = NOW,
            askTimestampEpochMillis: Long = NOW
        ) = KnockoutProductMarketData(
            productIsin = PRODUCT_ISIN,
            bid = 1.8,
            ask = 2.0,
            bidTimestampEpochMillis = bidTimestampEpochMillis,
            askTimestampEpochMillis = askTimestampEpochMillis,
            currency = "EUR",
            sourceId = SOURCE_ID
        )
    }
}
