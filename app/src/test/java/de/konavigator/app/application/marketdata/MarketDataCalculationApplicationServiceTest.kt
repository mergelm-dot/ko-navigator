package de.konavigator.app.application.marketdata

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.orchestration.MarketDataCalculationRequest
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceError
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculationApplicationServiceTest {

    @Test
    fun requestContainsExactlyThreeFields() {
        assertEquals(
            listOf("productIsin", "calculationType", "evaluationTimeEpochMillis"),
            instanceFields(MarketDataCalculationApplicationRequest::class.java).map { it.name }
        )
    }

    @Test
    fun requestHasNoDefaultValues() {
        assertEquals(1, MarketDataCalculationApplicationRequest::class.java.constructors.size)
        assertEquals(
            3,
            MarketDataCalculationApplicationRequest::class.java.constructors.single().parameterCount
        )
    }

    @Test
    fun applicationErrorContainsExactlyThreeCodes() {
        assertEquals(
            listOf("PRODUCT_NOT_FOUND", "MARKET_DATA_NOT_FOUND", "DATA_ACCESS_FAILURE"),
            MarketDataCalculationApplicationError.entries.map { it.name }
        )
    }

    @Test
    fun applicationResultContainsExactlyTwoSubtypes() {
        assertEquals(
            setOf("DataUnavailable", "DomainEvaluated"),
            resultTypes().map { it.simpleName }.toSet()
        )
    }

    @Test
    fun dataUnavailableContainsExactlyErrorField() {
        assertEquals(
            listOf("error"),
            instanceFields(MarketDataCalculationApplicationResult.DataUnavailable::class.java)
                .map { it.name }
        )
    }

    @Test
    fun domainEvaluatedContainsExactlyDomainResultField() {
        assertEquals(
            listOf("domainResult"),
            instanceFields(MarketDataCalculationApplicationResult.DomainEvaluated::class.java)
                .map { it.name }
        )
    }

    @Test
    fun applicationTypesContainNoMessages() {
        assertTrue(applicationFieldNames().none { it.contains("message") || it.contains("text") })
    }

    @Test
    fun applicationResultContainsNoThrowableFields() {
        assertTrue(
            resultTypes().flatMap(::instanceFields).none {
                Throwable::class.java.isAssignableFrom(it.type)
            }
        )
    }

    @Test
    fun applicationResultContainsNoErrorListsOrBooleans() {
        assertTrue(
            resultTypes().flatMap(::instanceFields).none {
                Collection::class.java.isAssignableFrom(it.type) || it.type == Boolean::class.java
            }
        )
    }

    @Test
    fun serviceHasExactlyThreeConstructorDependencies() {
        assertEquals(
            listOf(
                KnockoutProductSpecificationRepository::class.java,
                KnockoutProductMarketDataRepository::class.java,
                MarketDataCalculationOrchestrator::class.java
            ),
            MarketDataCalculationApplicationService::class.java.constructors
                .single()
                .parameterTypes
                .toList()
        )
    }

    @Test
    fun serviceHasExactlyOnePublicFunction() {
        assertEquals(1, serviceMethods().size)
    }

    @Test
    fun serviceFunctionIsNamedExecute() {
        assertEquals("execute", serviceMethods().single().name)
    }

    @Test
    fun executeIsSuspendFunction() {
        assertEquals(Continuation::class.java, serviceMethods().single().parameterTypes.last())
    }

    @Test
    fun publicApiContainsNoAndroidOrComposeTypes() {
        assertNoApiTypeName("android", "compose")
    }

    @Test
    fun specificationRepositoryReceivesExactIsin() {
        val fixture = fixture()

        execute(fixture, request(productIsin = PRODUCT_ISIN))

        assertEquals(listOf(PRODUCT_ISIN), fixture.specificationRepository.receivedProductIsins)
    }

    @Test
    fun leadingWhitespaceRemainsForSpecificationRepository() {
        val fixture = fixture()
        val productIsin = "  $PRODUCT_ISIN"

        execute(fixture, request(productIsin = productIsin))

        assertEquals(productIsin, fixture.specificationRepository.receivedProductIsins.single())
    }

    @Test
    fun trailingWhitespaceRemainsForSpecificationRepository() {
        val fixture = fixture()
        val productIsin = "$PRODUCT_ISIN  "

        execute(fixture, request(productIsin = productIsin))

        assertEquals(productIsin, fixture.specificationRepository.receivedProductIsins.single())
    }

    @Test
    fun characterCaseRemainsForSpecificationRepository() {
        val fixture = fixture()
        val productIsin = "de000Test001"

        execute(fixture, request(productIsin = productIsin))

        assertEquals(productIsin, fixture.specificationRepository.receivedProductIsins.single())
    }

    @Test
    fun specificationLookupDoesNotNormalizeIsin() {
        val fixture = fixture()
        val productIsin = "  de000Test001  "

        execute(fixture, request(productIsin = productIsin))

        assertEquals(productIsin, fixture.specificationRepository.receivedProductIsins.single())
    }

    @Test
    fun emptyIsinIsPassedToSpecificationRepository() {
        val fixture = fixture()

        execute(fixture, request(productIsin = ""))

        assertEquals("", fixture.specificationRepository.receivedProductIsins.single())
    }

    @Test
    fun specificationNotFoundReturnsProductNotFound() {
        val result = execute(fixture(specificationResult = RepositoryResult.NotFound))

        assertEquals(
            MarketDataCalculationApplicationError.PRODUCT_NOT_FOUND,
            dataUnavailable(result).error
        )
    }

    @Test
    fun specificationDataAccessFailureReturnsDataAccessFailure() {
        val result = execute(fixture(specificationResult = RepositoryResult.DataAccessFailure))

        assertEquals(
            MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE,
            dataUnavailable(result).error
        )
    }

    @Test
    fun marketDataRepositoryIsNotCalledAfterSpecificationNotFound() {
        val fixture = fixture(specificationResult = RepositoryResult.NotFound)

        execute(fixture)

        assertEquals(0, fixture.marketDataRepository.callCount)
    }

    @Test
    fun marketDataRepositoryIsNotCalledAfterSpecificationDataAccessFailure() {
        val fixture = fixture(specificationResult = RepositoryResult.DataAccessFailure)

        execute(fixture)

        assertEquals(0, fixture.marketDataRepository.callCount)
    }

    @Test
    fun specificationFailureDoesNotProduceDomainResult() {
        val result = execute(fixture(specificationResult = RepositoryResult.NotFound))

        assertFalse(result is MarketDataCalculationApplicationResult.DomainEvaluated)
    }

    @Test
    fun marketDataRepositoryIsCalledOnlyAfterSuccessfulSpecification() {
        val failed = fixture(specificationResult = RepositoryResult.NotFound)
        val successful = fixture()

        execute(failed)
        execute(successful)

        assertEquals(0, failed.marketDataRepository.callCount)
        assertEquals(1, successful.marketDataRepository.callCount)
    }

    @Test
    fun marketDataRepositoryReceivesExactIsin() {
        val fixture = fixture()

        execute(fixture, request(productIsin = PRODUCT_ISIN))

        assertEquals(listOf(PRODUCT_ISIN), fixture.marketDataRepository.receivedProductIsins)
    }

    @Test
    fun whitespaceRemainsForMarketDataRepository() {
        val fixture = fixture()
        val productIsin = "  $PRODUCT_ISIN  "

        execute(fixture, request(productIsin = productIsin))

        assertEquals(productIsin, fixture.marketDataRepository.receivedProductIsins.single())
    }

    @Test
    fun characterCaseRemainsForMarketDataRepository() {
        val fixture = fixture()
        val productIsin = "de000Test001"

        execute(fixture, request(productIsin = productIsin))

        assertEquals(productIsin, fixture.marketDataRepository.receivedProductIsins.single())
    }

    @Test
    fun marketDataNotFoundReturnsMarketDataNotFound() {
        val result = execute(fixture(marketDataResult = RepositoryResult.NotFound))

        assertEquals(
            MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND,
            dataUnavailable(result).error
        )
    }

    @Test
    fun marketDataAccessFailureReturnsDataAccessFailure() {
        val result = execute(fixture(marketDataResult = RepositoryResult.DataAccessFailure))

        assertEquals(
            MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE,
            dataUnavailable(result).error
        )
    }

    @Test
    fun marketDataNotFoundDoesNotProduceDomainResult() {
        val result = execute(fixture(marketDataResult = RepositoryResult.NotFound))

        assertFalse(result is MarketDataCalculationApplicationResult.DomainEvaluated)
    }

    @Test
    fun marketDataAccessFailureDoesNotProduceDomainResult() {
        val result = execute(fixture(marketDataResult = RepositoryResult.DataAccessFailure))

        assertFalse(result is MarketDataCalculationApplicationResult.DomainEvaluated)
    }

    @Test
    fun calculationTypeIsForwardedUnchanged() {
        val result = domainResult(execute(fixture(), request(calculationType = MID)))

        assertTrue(
            (result as MarketDataCalculationOrchestrationResult.Success).value is
                MarketDataCalculationValue.MidPrice
        )
    }

    @Test
    fun specificationIsForwardedUnchanged() {
        val specification = specification(basePrice = 77.0)
        val quote = marketData()
        val orchestrator = allowedOrchestrator()
        val expected = orchestrator.calculate(domainRequest(PURCHASE_PRICE, specification, quote))
        val actual = domainResult(
            execute(
                fixture(
                    specificationResult = RepositoryResult.Success(specification),
                    marketDataResult = RepositoryResult.Success(quote),
                    orchestrator = orchestrator
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun marketDataIsForwardedUnchanged() {
        val specification = specification()
        val quote = marketData(ask = 2.123456789)
        val orchestrator = allowedOrchestrator()
        val expected = orchestrator.calculate(domainRequest(PURCHASE_PRICE, specification, quote))
        val actual = domainResult(
            execute(
                fixture(
                    specificationResult = RepositoryResult.Success(specification),
                    marketDataResult = RepositoryResult.Success(quote),
                    orchestrator = orchestrator
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun evaluationTimeIsForwardedUnchanged() {
        val time = 123_456L
        val quote = marketData(bidTimestampEpochMillis = time, askTimestampEpochMillis = time)
        val result = domainResult(
            execute(
                fixture(
                    marketDataResult = RepositoryResult.Success(quote),
                    orchestrator = strictTimeOrchestrator()
                ),
                request(evaluationTimeEpochMillis = time)
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.Success)
    }

    @Test
    fun negativeEvaluationTimeIsForwardedUnchanged() {
        assertExtremeEvaluationTime(-100L)
    }

    @Test
    fun minimumEvaluationTimeIsForwardedUnchanged() {
        assertExtremeEvaluationTime(Long.MIN_VALUE)
    }

    @Test
    fun maximumEvaluationTimeIsForwardedUnchanged() {
        assertExtremeEvaluationTime(Long.MAX_VALUE)
    }

    @Test
    fun applicationRequestIsNotMutated() {
        val request = request(productIsin = "  de000Test001  ", evaluationTimeEpochMillis = -10L)
        val copy = request.copy()

        execute(fixture(), request)

        assertEquals(copy, request)
    }

    @Test
    fun domainModelsAreNotMutated() {
        val specification = specification()
        val quote = marketData()
        val specificationCopy = specification.copy()
        val quoteCopy = quote.copy()

        execute(
            fixture(
                specificationResult = RepositoryResult.Success(specification),
                marketDataResult = RepositoryResult.Success(quote)
            )
        )

        assertEquals(specificationCopy, specification)
        assertEquals(quoteCopy, quote)
    }

    @Test
    fun successfulRepositoriesProduceExactlyOneDomainEvaluation() {
        val fixture = fixture()
        val result = execute(fixture)

        assertEquals(1, fixture.specificationRepository.callCount)
        assertEquals(1, fixture.marketDataRepository.callCount)
        assertTrue(result is MarketDataCalculationApplicationResult.DomainEvaluated)
    }

    @Test
    fun domainResultCanBeEmbeddedByIdentity() {
        val domainResult = MarketDataCalculationOrchestrationResult.CalculationFailure(
            error = MarketDataCalculationError.INVALID_BID,
            dataQualityAssessment = DataQualityAssessment.passed()
        )
        val result = MarketDataCalculationApplicationResult.DomainEvaluated(domainResult)

        assertSame(domainResult, result.domainResult)
    }

    @Test
    fun successRemainsUnchanged() {
        val result = domainResult(execute(fixture()))

        assertEquals(
            MarketDataCalculationOrchestrationResult.Success(
                value = MarketDataCalculationValue.PurchasePrice(2.0, "EUR"),
                dataQualityAssessment = DataQualityAssessment.passed()
            ),
            result
        )
    }

    @Test
    fun structuralSpecificationBlockRemainsUnchanged() {
        val invalid = specification(productIsin = "")
        val result = domainResult(
            execute(fixture(specificationResult = RepositoryResult.Success(invalid)))
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked)
        assertEquals(
            DataQualityFindingCode.SPECIFICATION_MISSING_PRODUCT_ISIN,
            result.dataQualityAssessment.findings.single().code
        )
    }

    @Test
    fun structuralMarketDataBlockRemainsUnchanged() {
        val invalid = marketData(currency = "eur")
        val result = domainResult(
            execute(fixture(marketDataResult = RepositoryResult.Success(invalid)))
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked)
        assertEquals(
            DataQualityFindingCode.MARKET_DATA_INVALID_CURRENCY,
            result.dataQualityAssessment.findings.single().code
        )
    }

    @Test
    fun structuralCompatibilityBlockRemainsUnchanged() {
        val incompatible = marketData(productIsin = "OTHER")
        val result = domainResult(
            execute(fixture(marketDataResult = RepositoryResult.Success(incompatible)))
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked)
        assertEquals(
            DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH,
            result.dataQualityAssessment.findings.single().code
        )
    }

    @Test
    fun structurallyUnavailableRemainsUnchanged() {
        val incomplete = marketData(ask = null, askTimestampEpochMillis = null)
        val result = domainResult(
            execute(fixture(marketDataResult = RepositoryResult.Success(incomplete)))
        )

        assertEquals(
            MarketDataCalculationOrchestrationResult.StructurallyUnavailable(
                errors = listOf(MarketDataCalculationAvailabilityError.MISSING_ASK),
                dataQualityAssessment = DataQualityAssessment.passed()
            ),
            result
        )
    }

    @Test
    fun notFreshRemainsUnchanged() {
        val stale = marketData(bidTimestampEpochMillis = 0L, askTimestampEpochMillis = 0L)
        val result = domainResult(
            execute(
                fixture(
                    marketDataResult = RepositoryResult.Success(stale),
                    orchestrator = staleOrchestrator()
                ),
                request(evaluationTimeEpochMillis = 100L)
            )
        )

        assertEquals(
            MarketDataCalculationOrchestrationResult.NotFresh(
                errors = listOf(MarketDataFreshnessError.STALE_ASK),
                dataQualityAssessment = DataQualityAssessment.passed()
            ),
            result
        )
    }

    @Test
    fun sourceBlockedRemainsUnchanged() {
        val result = domainResult(execute(fixture(orchestrator = blockedSourceOrchestrator())))

        assertEquals(
            MarketDataCalculationOrchestrationResult.SourceBlocked(
                error = MarketDataSourceError.SOURCE_NOT_CONFIGURED,
                dataQualityAssessment = DataQualityAssessment.passed()
            ),
            result
        )
    }

    @Test
    fun calculationFailureRemainsUnchanged() {
        val domainResult = MarketDataCalculationOrchestrationResult.CalculationFailure(
            error = MarketDataCalculationError.BID_ABOVE_ASK,
            dataQualityAssessment = DataQualityAssessment.passed()
        )
        val result = MarketDataCalculationApplicationResult.DomainEvaluated(domainResult)

        assertSame(domainResult, result.domainResult)
    }

    @Test
    fun domainErrorsAreNotMappedToApplicationErrors() {
        val invalid = specification(productIsin = "")
        val result = execute(fixture(specificationResult = RepositoryResult.Success(invalid)))

        assertTrue(result is MarketDataCalculationApplicationResult.DomainEvaluated)
        assertFalse(result is MarketDataCalculationApplicationResult.DataUnavailable)
    }

    @Test
    fun specificationRepositoryIsCalledFirst() {
        val fixture = fixture()

        execute(fixture)

        assertEquals("specification", fixture.events.first())
    }

    @Test
    fun marketDataRepositoryIsCalledAfterSpecificationRepository() {
        val fixture = fixture()

        execute(fixture)

        assertEquals(listOf("specification", "marketData"), fixture.events)
    }

    @Test
    fun domainEvaluationOccursAfterBothRepositoryCalls() {
        val fixture = fixture()

        val result = execute(fixture)

        assertEquals(listOf("specification", "marketData"), fixture.events)
        assertTrue(result is MarketDataCalculationApplicationResult.DomainEvaluated)
    }

    @Test
    fun repositoryOrderRemainsStable() {
        repeat(3) {
            val fixture = fixture()

            execute(fixture)

            assertEquals(listOf("specification", "marketData"), fixture.events)
        }
    }

    @Test
    fun repositoryCallsAreSequential() {
        val fixture = fixture()

        execute(fixture)

        assertEquals(1, fixture.specificationRepository.callCount)
        assertEquals(1, fixture.marketDataRepository.callCount)
        assertEquals(listOf("specification", "marketData"), fixture.events)
    }

    @Test
    fun firstTechnicalFailureWins() {
        val fixture = fixture(
            specificationResult = RepositoryResult.DataAccessFailure,
            marketDataResult = RepositoryResult.NotFound
        )

        val result = execute(fixture)

        assertEquals(
            MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE,
            dataUnavailable(result).error
        )
        assertEquals(0, fixture.marketDataRepository.callCount)
    }

    @Test
    fun technicalErrorsAreNotAggregated() {
        val result = dataUnavailable(
            execute(fixture(specificationResult = RepositoryResult.DataAccessFailure))
        )

        assertEquals(listOf("error"), instanceFields(result.javaClass).map { it.name })
    }

    @Test
    fun domainResultRequiresBothSuccessfulRepositoryCalls() {
        val specificationFailure = execute(
            fixture(specificationResult = RepositoryResult.NotFound)
        )
        val marketDataFailure = execute(
            fixture(marketDataResult = RepositoryResult.NotFound)
        )
        val success = execute(fixture())

        assertFalse(specificationFailure is MarketDataCalculationApplicationResult.DomainEvaluated)
        assertFalse(marketDataFailure is MarketDataCalculationApplicationResult.DomainEvaluated)
        assertTrue(success is MarketDataCalculationApplicationResult.DomainEvaluated)
    }

    @Test
    fun servicePerformsNoOwnProductIsinValidation() {
        val result = execute(fixture(), request(productIsin = ""))

        assertTrue(domainResult(result) is MarketDataCalculationOrchestrationResult.Success)
    }

    @Test
    fun servicePerformsNoOwnPriceCalculation() {
        val ask = 2.123456789
        val result = domainResult(
            execute(fixture(marketDataResult = RepositoryResult.Success(marketData(ask = ask))))
        ) as MarketDataCalculationOrchestrationResult.Success

        assertEquals(
            ask,
            (result.value as MarketDataCalculationValue.PurchasePrice).value,
            0.0
        )
    }

    @Test
    fun servicePerformsNoOwnFreshnessMapping() {
        val stale = marketData(bidTimestampEpochMillis = 0L, askTimestampEpochMillis = 0L)
        val result = execute(
            fixture(
                marketDataResult = RepositoryResult.Success(stale),
                orchestrator = staleOrchestrator()
            ),
            request(evaluationTimeEpochMillis = 100L)
        )

        assertTrue(
            domainResult(result) is MarketDataCalculationOrchestrationResult.NotFresh
        )
    }

    @Test
    fun servicePerformsNoOwnSourcePolicyMapping() {
        val result = execute(fixture(orchestrator = blockedSourceOrchestrator()))

        assertTrue(
            domainResult(result) is MarketDataCalculationOrchestrationResult.SourceBlocked
        )
    }

    @Test
    fun serviceReadsNoSystemTime() {
        assertExtremeEvaluationTime(Long.MIN_VALUE)
    }

    @Test
    fun applicationApiContainsNoNetworkTypes() {
        assertNoApiTypeName("network", "retrofit", "okhttp")
    }

    @Test
    fun applicationApiContainsNoDatabaseTypes() {
        assertNoApiTypeName("database", "room", "sqlite")
    }

    @Test
    fun applicationApiContainsNoDtoOrMapperTypes() {
        assertNoApiTypeName("dto", "mapper")
    }

    @Test
    fun applicationTypesContainNoUiTexts() {
        assertTrue(applicationFieldNames().none { it.contains("message") || it.contains("text") })
        assertNoApiTypeName("presentation", ".ui.", "UiState", "UiEvent")
    }

    @Test
    fun applicationApiContainsNoAndroidOrComposeDependencies() {
        assertNoApiTypeName("android", "compose")
    }

    @Test
    fun applicationApiContainsNoLoggingDependency() {
        assertNoApiTypeName("logger", "logging", "timber")
    }

    @Test
    fun productionServiceDependsOnlyOnRepositoryPorts() {
        val constructorTypes = MarketDataCalculationApplicationService::class.java
            .constructors
            .single()
            .parameterTypes

        assertTrue(constructorTypes[0].isInterface)
        assertTrue(constructorTypes[1].isInterface)
        assertEquals(MarketDataCalculationOrchestrator::class.java, constructorTypes[2])
    }

    @Test
    fun applicationApiContainsNoTradeCalculationEngineDependency() {
        assertNoApiTypeName("TradeCalculationEngine")
    }

    @Test
    fun applicationApiContainsNoKoCalculatorDependency() {
        assertNoApiTypeName("KoCalculator")
    }

    @Test
    fun serviceReceivesConfiguredOrchestratorInsteadOfPolicies() {
        val constructorTypes = MarketDataCalculationApplicationService::class.java
            .constructors
            .single()
            .parameterTypes
            .toList()

        assertTrue(constructorTypes.contains(MarketDataCalculationOrchestrator::class.java))
        assertFalse(constructorTypes.contains(MarketDataFreshnessPolicy::class.java))
        assertFalse(constructorTypes.contains(MarketDataSourcePolicy::class.java))
    }

    @Test
    fun serviceDoesNotMutateInput() {
        val request = request(productIsin = " de000Test001 ", evaluationTimeEpochMillis = -1L)
        val copy = request.copy()

        execute(fixture(), request)

        assertEquals(copy, request)
    }

    @Test
    fun identicalInputsAndDependenciesProduceIdenticalResults() {
        val first = execute(fixture(), request())
        val second = execute(fixture(), request())

        assertEquals(first, second)
    }

    private fun assertExtremeEvaluationTime(evaluationTimeEpochMillis: Long) {
        val quote = marketData(
            bidTimestampEpochMillis = evaluationTimeEpochMillis,
            askTimestampEpochMillis = evaluationTimeEpochMillis
        )
        val result = domainResult(
            execute(
                fixture(
                    marketDataResult = RepositoryResult.Success(quote),
                    orchestrator = strictTimeOrchestrator()
                ),
                request(evaluationTimeEpochMillis = evaluationTimeEpochMillis)
            )
        )

        assertTrue(result is MarketDataCalculationOrchestrationResult.Success)
    }

    private fun execute(
        fixture: Fixture,
        request: MarketDataCalculationApplicationRequest = request()
    ): MarketDataCalculationApplicationResult = runSuspend {
        fixture.service.execute(request)
    }

    private fun dataUnavailable(
        result: MarketDataCalculationApplicationResult
    ) = result as MarketDataCalculationApplicationResult.DataUnavailable

    private fun domainResult(
        result: MarketDataCalculationApplicationResult
    ) = (result as MarketDataCalculationApplicationResult.DomainEvaluated).domainResult

    private fun fixture(
        specificationResult: RepositoryResult<KnockoutProductSpecification> =
            RepositoryResult.Success(specification()),
        marketDataResult: RepositoryResult<KnockoutProductMarketData> =
            RepositoryResult.Success(marketData()),
        orchestrator: MarketDataCalculationOrchestrator = allowedOrchestrator()
    ): Fixture {
        val events = mutableListOf<String>()
        val specificationRepository = RecordingSpecificationRepository(
            result = specificationResult,
            events = events
        )
        val marketDataRepository = RecordingMarketDataRepository(
            result = marketDataResult,
            events = events
        )
        return Fixture(
            specificationRepository = specificationRepository,
            marketDataRepository = marketDataRepository,
            service = MarketDataCalculationApplicationService(
                specificationRepository = specificationRepository,
                marketDataRepository = marketDataRepository,
                orchestrator = orchestrator
            ),
            events = events
        )
    }

    private fun request(
        productIsin: String = PRODUCT_ISIN,
        calculationType: MarketDataCalculationType = PURCHASE_PRICE,
        evaluationTimeEpochMillis: Long = NOW
    ) = MarketDataCalculationApplicationRequest(
        productIsin = productIsin,
        calculationType = calculationType,
        evaluationTimeEpochMillis = evaluationTimeEpochMillis
    )

    private fun domainRequest(
        calculationType: MarketDataCalculationType,
        specification: KnockoutProductSpecification,
        marketData: KnockoutProductMarketData,
        evaluationTimeEpochMillis: Long = NOW
    ) = MarketDataCalculationRequest(
        calculationType = calculationType,
        specification = specification,
        marketData = marketData,
        evaluationTimeEpochMillis = evaluationTimeEpochMillis
    )

    private fun specification(
        productIsin: String = PRODUCT_ISIN,
        basePrice: Double = 80.0
    ) = KnockoutProductSpecification(
        productIsin = productIsin,
        productWkn = "ABC123",
        issuerId = "issuer-a",
        underlyingId = "underlying-a",
        direction = TradeDirection.LONG,
        basePrice = basePrice,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )

    private fun marketData(
        productIsin: String = PRODUCT_ISIN,
        bid: Double? = 1.8,
        ask: Double? = 2.0,
        bidTimestampEpochMillis: Long? = NOW,
        askTimestampEpochMillis: Long? = NOW,
        currency: String = "EUR",
        sourceId: String = SOURCE_ID
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestampEpochMillis,
        askTimestampEpochMillis = askTimestampEpochMillis,
        currency = currency,
        sourceId = sourceId
    )

    private fun allowedOrchestrator() = orchestrator()

    private fun strictTimeOrchestrator() = orchestrator(
        thresholds = thresholds(
            maxBidAgeMillis = 0L,
            maxAskAgeMillis = 0L,
            maxBidAskDifferenceMillis = 0L,
            allowedFutureSkewMillis = 0L
        )
    )

    private fun staleOrchestrator() = orchestrator(
        thresholds = thresholds(maxBidAgeMillis = 10L, maxAskAgeMillis = 10L)
    )

    private fun blockedSourceOrchestrator() = orchestrator(sourceRules = emptyList())

    private fun orchestrator(
        thresholds: MarketDataFreshnessThresholds = thresholds(),
        sourceRules: List<MarketDataSourceRule> = listOf(
            MarketDataSourceRule(
                sourceId = SOURCE_ID,
                supportedCalculationTypes = MarketDataCalculationType.entries.toSet()
            )
        )
    ) = MarketDataCalculationOrchestrator(
        freshnessPolicy = MarketDataFreshnessPolicy(thresholds),
        sourcePolicy = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(sourceRules))
    )

    private fun thresholds(
        maxBidAgeMillis: Long = Long.MAX_VALUE,
        maxAskAgeMillis: Long = Long.MAX_VALUE,
        maxBidAskDifferenceMillis: Long = Long.MAX_VALUE,
        allowedFutureSkewMillis: Long = Long.MAX_VALUE
    ) = MarketDataFreshnessThresholds(
        maxBidAgeMillis = maxBidAgeMillis,
        maxAskAgeMillis = maxAskAgeMillis,
        maxBidAskDifferenceMillis = maxBidAskDifferenceMillis,
        allowedFutureSkewMillis = allowedFutureSkewMillis
    )

    private fun serviceMethods(): List<Method> =
        MarketDataCalculationApplicationService::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }

    private fun resultTypes(): List<Class<*>> =
        MarketDataCalculationApplicationResult::class.java.declaredClasses.toList()

    private fun applicationFieldNames(): List<String> =
        (resultTypes() + MarketDataCalculationApplicationRequest::class.java)
            .flatMap(::instanceFields)
            .map { it.name.lowercase() }

    private fun instanceFields(type: Class<*>) = type.declaredFields
        .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }

    private fun assertNoApiTypeName(vararg forbiddenFragments: String) {
        assertTrue(
            applicationApiTypeNames().none { typeName ->
                forbiddenFragments.any { typeName.contains(it, ignoreCase = true) }
            }
        )
    }

    private fun applicationApiTypeNames(): Set<String> = buildSet {
        val applicationTypes = listOf(
            MarketDataCalculationApplicationRequest::class.java,
            MarketDataCalculationApplicationError::class.java,
            MarketDataCalculationApplicationResult::class.java,
            MarketDataCalculationApplicationService::class.java
        ) + resultTypes()

        applicationTypes.forEach { type ->
            add(type.name)
            instanceFields(type).forEach { add(it.type.name) }
            type.constructors.forEach { constructor ->
                constructor.parameterTypes.forEach { add(it.name) }
            }
            type.declaredMethods.filter { Modifier.isPublic(it.modifiers) }.forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { add(it.name) }
                method.genericParameterTypes.forEach { add(it.typeName) }
            }
        }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<T>) {
                    completed = result
                }
            }
        )

        return (completed ?: error("Suspend application call did not complete synchronously"))
            .getOrThrow()
    }

    private data class Fixture(
        val specificationRepository: RecordingSpecificationRepository,
        val marketDataRepository: RecordingMarketDataRepository,
        val service: MarketDataCalculationApplicationService,
        val events: List<String>
    )

    private class RecordingSpecificationRepository(
        private val result: RepositoryResult<KnockoutProductSpecification>,
        private val events: MutableList<String>
    ) : KnockoutProductSpecificationRepository {

        var callCount: Int = 0
            private set

        val receivedProductIsins = mutableListOf<String>()

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductSpecification> {
            callCount++
            receivedProductIsins += productIsin
            events += "specification"
            return result
        }
    }

    private class RecordingMarketDataRepository(
        private val result: RepositoryResult<KnockoutProductMarketData>,
        private val events: MutableList<String>
    ) : KnockoutProductMarketDataRepository {

        var callCount: Int = 0
            private set

        val receivedProductIsins = mutableListOf<String>()

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductMarketData> {
            callCount++
            receivedProductIsins += productIsin
            events += "marketData"
            return result
        }
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val SOURCE_ID = "source-a"
        const val NOW = 1_000L

        val PURCHASE_PRICE = MarketDataCalculationType.PURCHASE_PRICE
        val MID = MarketDataCalculationType.MID
    }
}
