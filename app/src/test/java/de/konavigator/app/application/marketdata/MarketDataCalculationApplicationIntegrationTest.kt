package de.konavigator.app.application.marketdata

import de.konavigator.app.data.inmemory.InMemoryKnockoutProductMarketDataRepository
import de.konavigator.app.data.inmemory.InMemoryKnockoutProductSpecificationRepository
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculationApplicationIntegrationTest {

    @Test
    fun purchasePriceRunsThroughCompleteApplicationFlow() {
        val result = execute(createService(), request(MarketDataCalculationType.PURCHASE_PRICE))
        val value = successValue(result) as MarketDataCalculationValue.PurchasePrice

        assertEquals(2.0, value.value, 0.0)
        assertEquals("EUR", value.currency)
    }

    @Test
    fun salePriceRunsThroughCompleteApplicationFlow() {
        val result = execute(createService(), request(MarketDataCalculationType.SALE_PRICE))
        val value = successValue(result) as MarketDataCalculationValue.SalePrice

        assertEquals(1.8, value.value, 0.0)
        assertEquals("EUR", value.currency)
    }

    @Test
    fun spreadRunsThroughCompleteApplicationFlowWithoutAdditionalRounding() {
        val bid = 1.0
        val ask = 3.0
        val service = createService(
            marketDataEntries = listOf(marketData(bid = bid, ask = ask))
        )
        val value = successValue(
            execute(service, request(MarketDataCalculationType.SPREAD))
        ) as MarketDataCalculationValue.Spread

        assertEquals(ask - bid, value.absoluteSpread, 0.0)
        assertEquals((ask - bid) / ask * 100.0, value.relativeSpreadToAskPercent, 0.0)
        assertNotEquals(66.67, value.relativeSpreadToAskPercent, 0.0)
        assertEquals("EUR", value.currency)
    }

    @Test
    fun midRunsThroughCompleteApplicationFlowWithoutAdditionalRounding() {
        val bid = 0.1
        val ask = 0.2
        val service = createService(
            marketDataEntries = listOf(marketData(bid = bid, ask = ask))
        )
        val value = successValue(
            execute(service, request(MarketDataCalculationType.MID))
        ) as MarketDataCalculationValue.MidPrice

        assertEquals(bid + (ask - bid) / 2.0, value.value, 0.0)
        assertNotEquals(0.15, value.value, 0.0)
        assertEquals("EUR", value.currency)
    }

    @Test
    fun missingSpecificationReturnsProductNotFound() {
        val service = createService(
            specifications = emptyList(),
            marketDataEntries = listOf(marketData())
        )

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.PRODUCT_NOT_FOUND
            ),
            execute(service, request())
        )
    }

    @Test
    fun missingMarketDataReturnsMarketDataNotFound() {
        val service = createService(
            specifications = listOf(specification()),
            marketDataEntries = emptyList()
        )

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND
            ),
            execute(service, request())
        )
    }

    @Test
    fun staleQuoteRemainsUnchangedDomainResult() {
        val staleTimestamp = NOW - 11L
        val service = createService(
            marketDataEntries = listOf(
                marketData(
                    bidTimestampEpochMillis = staleTimestamp,
                    askTimestampEpochMillis = staleTimestamp
                )
            ),
            freshnessThresholds = thresholds(
                maxBidAgeMillis = 10L,
                maxAskAgeMillis = 10L
            )
        )
        val result = domainResult(
            execute(service, request(MarketDataCalculationType.PURCHASE_PRICE))
        )

        assertEquals(
            MarketDataCalculationOrchestrationResult.NotFresh(
                listOf(MarketDataFreshnessError.STALE_ASK)
            ),
            result
        )
    }

    @Test
    fun differentlyCasedRequestIsinIsNotNormalized() {
        val result = execute(
            createService(),
            request(productIsin = PRODUCT_ISIN.lowercase())
        )

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.PRODUCT_NOT_FOUND
            ),
            result
        )
    }

    private fun execute(
        service: MarketDataCalculationApplicationService,
        request: MarketDataCalculationApplicationRequest
    ): MarketDataCalculationApplicationResult = runSuspend {
        service.execute(request)
    }

    private fun successValue(
        result: MarketDataCalculationApplicationResult
    ): MarketDataCalculationValue {
        val domainResult = domainResult(result)
        assertTrue(domainResult is MarketDataCalculationOrchestrationResult.Success)
        return (domainResult as MarketDataCalculationOrchestrationResult.Success).value
    }

    private fun domainResult(
        result: MarketDataCalculationApplicationResult
    ): MarketDataCalculationOrchestrationResult {
        assertTrue(result is MarketDataCalculationApplicationResult.DomainEvaluated)
        return (result as MarketDataCalculationApplicationResult.DomainEvaluated).domainResult
    }

    private fun createService(
        specifications: List<KnockoutProductSpecification> = listOf(specification()),
        marketDataEntries: List<KnockoutProductMarketData> = listOf(marketData()),
        freshnessThresholds: MarketDataFreshnessThresholds = thresholds(),
        sourceRules: List<MarketDataSourceRule> = listOf(
            MarketDataSourceRule(
                sourceId = SOURCE_ID,
                supportedCalculationTypes = MarketDataCalculationType.entries.toSet()
            )
        )
    ): MarketDataCalculationApplicationService {
        val specificationRepository = InMemoryKnockoutProductSpecificationRepository(
            specifications
        )
        val marketDataRepository = InMemoryKnockoutProductMarketDataRepository(
            marketDataEntries
        )
        val freshnessPolicy = MarketDataFreshnessPolicy(freshnessThresholds)
        val sourcePolicy = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(sourceRules))
        val orchestrator = MarketDataCalculationOrchestrator(
            freshnessPolicy = freshnessPolicy,
            sourcePolicy = sourcePolicy
        )

        return MarketDataCalculationApplicationService(
            specificationRepository = specificationRepository,
            marketDataRepository = marketDataRepository,
            orchestrator = orchestrator
        )
    }

    private fun request(
        calculationType: MarketDataCalculationType = MarketDataCalculationType.PURCHASE_PRICE,
        productIsin: String = PRODUCT_ISIN,
        evaluationTimeEpochMillis: Long = NOW
    ) = MarketDataCalculationApplicationRequest(
        productIsin = productIsin,
        calculationType = calculationType,
        evaluationTimeEpochMillis = evaluationTimeEpochMillis
    )

    private fun specification(
        productIsin: String = PRODUCT_ISIN
    ) = KnockoutProductSpecification(
        productIsin = productIsin,
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

    private fun marketData(
        productIsin: String = PRODUCT_ISIN,
        bid: Double? = 1.8,
        ask: Double? = 2.0,
        bidTimestampEpochMillis: Long? = NOW,
        askTimestampEpochMillis: Long? = NOW
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestampEpochMillis,
        askTimestampEpochMillis = askTimestampEpochMillis,
        currency = "EUR",
        sourceId = SOURCE_ID
    )

    private fun thresholds(
        maxBidAgeMillis: Long = 100L,
        maxAskAgeMillis: Long = 100L,
        maxBidAskDifferenceMillis: Long = 100L,
        allowedFutureSkewMillis: Long = 0L
    ) = MarketDataFreshnessThresholds(
        maxBidAgeMillis = maxBidAgeMillis,
        maxAskAgeMillis = maxAskAgeMillis,
        maxBidAskDifferenceMillis = maxBidAskDifferenceMillis,
        allowedFutureSkewMillis = allowedFutureSkewMillis
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
        const val NOW = 1_000L
    }
}
