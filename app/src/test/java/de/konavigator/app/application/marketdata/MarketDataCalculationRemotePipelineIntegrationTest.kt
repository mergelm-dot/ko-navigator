package de.konavigator.app.application.marketdata

import de.konavigator.app.data.remote.RemoteKnockoutProductMarketDataRepository
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationRepository
import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductMarketDataProvider
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationProvider
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculationRemotePipelineIntegrationTest {

    @Test
    fun validDtosProducePassedDomainSuccess() {
        val result = domainResult(execute(service()))

        assertTrue(result is MarketDataCalculationOrchestrationResult.Success)
        assertEquals(
            DataQualityStatus.PASSED,
            (result as MarketDataCalculationOrchestrationResult.Success)
                .dataQualityAssessment.status
        )
    }

    @Test
    fun mappableInvalidSpecificationProducesStructuralDataQualityBlocked() {
        val result = domainResult(
            execute(
                service(
                    specificationDtos = mapOf(
                        PRODUCT_ISIN to specificationDto(basePrice = 0.0)
                    )
                )
            )
        )

        assertTrue(
            result is MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked
        )
    }

    @Test
    fun missingRequiredSpecificationDtoFieldProducesInvalidSpecification() {
        val result = execute(
            service(
                specificationDtos = mapOf(
                    PRODUCT_ISIN to specificationDto(issuerId = null)
                )
            )
        )

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.INVALID_PRODUCT_SPECIFICATION
            ),
            result
        )
    }

    @Test
    fun missingRequiredMarketDataDtoFieldProducesInvalidMarketData() {
        val result = execute(
            service(
                marketDataDtos = mapOf(
                    PRODUCT_ISIN to marketDataDto(sourceId = null)
                )
            )
        )

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.INVALID_PRODUCT_MARKET_DATA
            ),
            result
        )
    }

    @Test
    fun unknownProductIsinProducesProductNotFound() {
        val result = execute(service(), request(productIsin = "DE000UNKNOWN1"))

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.PRODUCT_NOT_FOUND
            ),
            result
        )
    }

    @Test
    fun existingSpecificationWithoutMarketDataProducesMarketDataNotFound() {
        val result = execute(service(marketDataDtos = emptyMap()))

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND
            ),
            result
        )
    }

    private fun service(
        specificationDtos: Map<String, KnockoutProductSpecificationDto> = mapOf(
            PRODUCT_ISIN to specificationDto()
        ),
        marketDataDtos: Map<String, KnockoutProductMarketDataDto> = mapOf(
            PRODUCT_ISIN to marketDataDto()
        )
    ): MarketDataCalculationApplicationService {
        val specificationRepository = RemoteKnockoutProductSpecificationRepository(
            InMemoryKnockoutProductSpecificationProvider(specificationDtos)
        )
        val marketDataRepository = RemoteKnockoutProductMarketDataRepository(
            InMemoryKnockoutProductMarketDataProvider(marketDataDtos)
        )
        val freshnessPolicy = MarketDataFreshnessPolicy(
            MarketDataFreshnessThresholds(
                maxBidAgeMillis = 100L,
                maxAskAgeMillis = 100L,
                maxBidAskDifferenceMillis = 100L,
                allowedFutureSkewMillis = 0L
            )
        )
        val sourcePolicy = MarketDataSourcePolicy(
            MarketDataSourcePolicyConfig(
                listOf(
                    MarketDataSourceRule(
                        sourceId = SOURCE_ID,
                        supportedCalculationTypes = setOf(
                            MarketDataCalculationType.PURCHASE_PRICE
                        )
                    )
                )
            )
        )

        return MarketDataCalculationApplicationService(
            specificationRepository = specificationRepository,
            marketDataRepository = marketDataRepository,
            orchestrator = MarketDataCalculationOrchestrator(
                freshnessPolicy = freshnessPolicy,
                sourcePolicy = sourcePolicy
            )
        )
    }

    private fun execute(
        service: MarketDataCalculationApplicationService,
        request: MarketDataCalculationApplicationRequest = request()
    ): MarketDataCalculationApplicationResult = runSuspend {
        service.execute(request)
    }

    private fun domainResult(
        result: MarketDataCalculationApplicationResult
    ): MarketDataCalculationOrchestrationResult {
        assertTrue(result is MarketDataCalculationApplicationResult.DomainEvaluated)
        return (result as MarketDataCalculationApplicationResult.DomainEvaluated).domainResult
    }

    private fun request(
        productIsin: String = PRODUCT_ISIN
    ) = MarketDataCalculationApplicationRequest(
        productIsin = productIsin,
        calculationType = MarketDataCalculationType.PURCHASE_PRICE,
        evaluationTimeEpochMillis = EVALUATION_TIME
    )

    private fun specificationDto(
        issuerId: String? = "issuer-a",
        basePrice: Double? = 80.0
    ) = KnockoutProductSpecificationDto(
        productIsin = PRODUCT_ISIN,
        productWkn = "ABC123",
        issuerId = issuerId,
        underlyingId = "underlying-a",
        direction = "LONG",
        basePrice = basePrice,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )

    private fun marketDataDto(
        sourceId: String? = SOURCE_ID
    ) = KnockoutProductMarketDataDto(
        productIsin = PRODUCT_ISIN,
        bid = 1.8,
        ask = 2.0,
        bidTimestampEpochMillis = EVALUATION_TIME,
        askTimestampEpochMillis = EVALUATION_TIME,
        currency = "EUR",
        sourceId = sourceId
    )

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

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val SOURCE_ID = "source-a"
        const val EVALUATION_TIME = 1_000L
    }
}
