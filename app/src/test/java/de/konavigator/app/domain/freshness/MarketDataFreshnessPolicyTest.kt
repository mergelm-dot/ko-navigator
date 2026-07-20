package de.konavigator.app.domain.freshness

import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.model.KnockoutProductMarketData
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MarketDataFreshnessPolicyTest {

    @Test
    fun thresholdsContainExactlyFourFields() {
        assertEquals(
            setOf(
                "maxBidAgeMillis",
                "maxAskAgeMillis",
                "maxBidAskDifferenceMillis",
                "allowedFutureSkewMillis"
            ),
            MarketDataFreshnessThresholds::class.java.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .map { it.name }
                .toSet()
        )
    }

    @Test
    fun thresholdsHaveNoDefaultValues() {
        val constructors = MarketDataFreshnessThresholds::class.java.constructors

        assertEquals(1, constructors.size)
        assertEquals(List(4) { java.lang.Long.TYPE }, constructors.single().parameterTypes.toList())
    }

    @Test
    fun negativeMaxBidAgeIsRejected() {
        assertThresholdFailure { thresholds(maxBidAgeMillis = -1) }
    }

    @Test
    fun negativeMaxAskAgeIsRejected() {
        assertThresholdFailure { thresholds(maxAskAgeMillis = -1) }
    }

    @Test
    fun negativeMaxBidAskDifferenceIsRejected() {
        assertThresholdFailure { thresholds(maxBidAskDifferenceMillis = -1) }
    }

    @Test
    fun negativeAllowedFutureSkewIsRejected() {
        assertThresholdFailure { thresholds(allowedFutureSkewMillis = -1) }
    }

    @Test
    fun zeroThresholdsAreAccepted() {
        assertEquals(thresholds(0, 0, 0, 0), thresholds(0, 0, 0, 0))
    }

    @Test
    fun maximumThresholdsAreAccepted() {
        val maximum = thresholds(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)

        assertEquals(Long.MAX_VALUE, maximum.maxBidAgeMillis)
        assertEquals(Long.MAX_VALUE, maximum.maxAskAgeMillis)
        assertEquals(Long.MAX_VALUE, maximum.maxBidAskDifferenceMillis)
        assertEquals(Long.MAX_VALUE, maximum.allowedFutureSkewMillis)
    }

    @Test
    fun thresholdValuesAreStoredUnchanged() {
        assertEquals(
            MarketDataFreshnessThresholds(11, 12, 13, 14),
            thresholds(11, 12, 13, 14)
        )
    }

    @Test
    fun freshnessErrorEnumContainsExactlyFiveCodesInStableOrder() {
        assertEquals(
            listOf(BID_FUTURE, STALE_BID, ASK_FUTURE, STALE_ASK, TOO_FAR_APART),
            MarketDataFreshnessError.entries
        )
    }

    @Test
    fun freshnessResultContainsExactlyFreshAndNotFresh() {
        assertEquals(
            setOf("Fresh", "NotFresh"),
            MarketDataFreshnessResult::class.java.declaredClasses.map { it.simpleName }.toSet()
        )
    }

    @Test
    fun freshContainsNoValues() {
        val fields = MarketDataFreshnessResult.Fresh::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }

        assertTrue(fields.isEmpty())
    }

    @Test
    fun notFreshContainsOnlyErrorList() {
        val fields = MarketDataFreshnessResult.NotFresh::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }

        assertEquals(listOf("errors"), fields.map { it.name })
        assertEquals(List::class.java, fields.single().type)
    }

    @Test
    fun freshnessTypesContainNoMessageOrUiTextFields() {
        val names = listOf(
            MarketDataFreshnessThresholds::class.java,
            MarketDataFreshnessResult.Fresh::class.java,
            MarketDataFreshnessResult.NotFresh::class.java
        ).flatMap { type -> type.declaredFields.map { it.name.lowercase() } }

        assertTrue(names.none { it.contains("message") || it.contains("text") })
    }

    @Test
    fun policyExposesExactlyOnePublicEvaluateFunction() {
        val methods = MarketDataFreshnessPolicy::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }

        assertEquals(listOf("evaluate"), methods.map { it.name })
    }

    @Test
    fun evaluationTimeIsExplicitAndControlsFreshness() {
        val policy = policy(maxBidAgeMillis = 0)
        val quote = marketData(bidTimestamp = 1_000)

        assertFresh(policy.evaluate(SALE_PRICE, quote, 1_000))
        assertErrors(policy.evaluate(SALE_PRICE, quote, 1_001), STALE_BID)
    }

    @Test
    fun publicApiContainsNoAndroidOrComposeTypes() {
        val typeNames = MarketDataFreshnessPolicy::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .flatMap { it.parameterTypes.toList() + it.returnType }
            .map { it.name }

        assertTrue(typeNames.none { it.startsWith("android.") || it.contains("compose") })
    }

    @Test
    fun purchaseAskAtEvaluationTimeIsFresh() {
        assertFresh(evaluate(PURCHASE_PRICE, askTimestamp = 1_000, evaluationTime = 1_000))
    }

    @Test
    fun purchaseAskAtMaximumAgeIsFresh() {
        assertFresh(evaluate(PURCHASE_PRICE, askTimestamp = 900, evaluationTime = 1_000))
    }

    @Test
    fun purchaseAskOlderThanMaximumAgeIsStale() {
        assertErrors(
            evaluate(PURCHASE_PRICE, askTimestamp = 899, evaluationTime = 1_000),
            STALE_ASK
        )
    }

    @Test
    fun purchaseAskAtAllowedFutureSkewIsFresh() {
        assertFresh(evaluate(PURCHASE_PRICE, askTimestamp = 1_010, evaluationTime = 1_000))
    }

    @Test
    fun purchaseAskBeyondAllowedFutureSkewIsFuture() {
        assertErrors(
            evaluate(PURCHASE_PRICE, askTimestamp = 1_011, evaluationTime = 1_000),
            ASK_FUTURE
        )
    }

    @Test
    fun purchaseIgnoresStaleBid() {
        assertFresh(evaluate(PURCHASE_PRICE, bidTimestamp = 0, askTimestamp = 1_000))
    }

    @Test
    fun purchaseIgnoresFutureBid() {
        assertFresh(evaluate(PURCHASE_PRICE, bidTimestamp = 2_000, askTimestamp = 1_000))
    }

    @Test
    fun purchaseDoesNotCheckBidAskTimestampDifference() {
        assertFresh(evaluate(PURCHASE_PRICE, bidTimestamp = 0, askTimestamp = 1_000))
    }

    @Test
    fun saleBidAtEvaluationTimeIsFresh() {
        assertFresh(evaluate(SALE_PRICE, bidTimestamp = 1_000, evaluationTime = 1_000))
    }

    @Test
    fun saleBidAtMaximumAgeIsFresh() {
        assertFresh(evaluate(SALE_PRICE, bidTimestamp = 900, evaluationTime = 1_000))
    }

    @Test
    fun saleBidOlderThanMaximumAgeIsStale() {
        assertErrors(evaluate(SALE_PRICE, bidTimestamp = 899), STALE_BID)
    }

    @Test
    fun saleBidAtAllowedFutureSkewIsFresh() {
        assertFresh(evaluate(SALE_PRICE, bidTimestamp = 1_010))
    }

    @Test
    fun saleBidBeyondAllowedFutureSkewIsFuture() {
        assertErrors(evaluate(SALE_PRICE, bidTimestamp = 1_011), BID_FUTURE)
    }

    @Test
    fun saleIgnoresStaleAsk() {
        assertFresh(evaluate(SALE_PRICE, bidTimestamp = 1_000, askTimestamp = 0))
    }

    @Test
    fun saleIgnoresFutureAsk() {
        assertFresh(evaluate(SALE_PRICE, bidTimestamp = 1_000, askTimestamp = 2_000))
    }

    @Test
    fun saleDoesNotCheckBidAskTimestampDifference() {
        assertFresh(evaluate(SALE_PRICE, bidTimestamp = 1_000, askTimestamp = 0))
    }

    @Test
    fun spreadWithFreshCloseTimestampsIsFresh() {
        assertFresh(evaluate(SPREAD, bidTimestamp = 990, askTimestamp = 995))
    }

    @Test
    fun spreadReportsStaleBid() {
        assertErrors(evaluate(SPREAD, bidTimestamp = 899, askTimestamp = 900), STALE_BID)
    }

    @Test
    fun spreadReportsStaleAsk() {
        assertErrors(evaluate(SPREAD, bidTimestamp = 900, askTimestamp = 899), STALE_ASK)
    }

    @Test
    fun spreadReportsBothStaleSidesInStableOrder() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 899, askTimestamp = 898),
            STALE_BID,
            STALE_ASK
        )
    }

    @Test
    fun spreadReportsFutureBid() {
        assertErrors(evaluate(SPREAD, bidTimestamp = 1_011, askTimestamp = 1_000), BID_FUTURE)
    }

    @Test
    fun spreadReportsFutureAsk() {
        assertErrors(evaluate(SPREAD, bidTimestamp = 1_000, askTimestamp = 1_011), ASK_FUTURE)
    }

    @Test
    fun spreadReportsFutureBidAndStaleAsk() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 1_011, askTimestamp = 899),
            BID_FUTURE,
            STALE_ASK
        )
    }

    @Test
    fun spreadReportsStaleBidAndFutureAsk() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 899, askTimestamp = 1_011),
            STALE_BID,
            ASK_FUTURE
        )
    }

    @Test
    fun spreadAtMaximumTimestampDifferenceIsFresh() {
        assertFresh(evaluate(SPREAD, bidTimestamp = 980, askTimestamp = 1_000))
    }

    @Test
    fun spreadBeyondMaximumTimestampDifferenceIsNotFresh() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 979, askTimestamp = 1_000),
            TOO_FAR_APART
        )
    }

    @Test
    fun spreadWithEqualTimestampsIsFresh() {
        assertFresh(evaluate(SPREAD, bidTimestamp = 1_000, askTimestamp = 1_000))
    }

    @Test
    fun spreadFreshnessDoesNotDependOnZeroBidPrice() {
        val quote = marketData(bid = 0.0, bidTimestamp = 990, askTimestamp = 995)

        assertFresh(policy().evaluate(SPREAD, quote, 1_000))
    }

    @Test
    fun spreadReportsStaleBidAndTimestampDifference() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 899, askTimestamp = 1_000),
            STALE_BID,
            TOO_FAR_APART
        )
    }

    @Test
    fun spreadReportsBothStaleSidesAndTimestampDifference() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 800, askTimestamp = 899),
            STALE_BID,
            STALE_ASK,
            TOO_FAR_APART
        )
    }

    @Test
    fun spreadFutureBidSuppressesTimestampDifferenceError() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 1_011, askTimestamp = 900),
            BID_FUTURE
        )
    }

    @Test
    fun spreadFutureAskSuppressesTimestampDifferenceError() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 900, askTimestamp = 1_011),
            ASK_FUTURE
        )
    }

    @Test
    fun spreadBothFutureSidesSuppressTimestampDifferenceError() {
        assertErrors(
            evaluate(SPREAD, bidTimestamp = 1_011, askTimestamp = 1_050),
            BID_FUTURE,
            ASK_FUTURE
        )
    }

    @Test
    fun spreadErrorsFollowEnumOrder() {
        assertEquals(
            listOf(STALE_BID, STALE_ASK, TOO_FAR_APART),
            errorsOf(evaluate(SPREAD, bidTimestamp = 800, askTimestamp = 899))
        )
    }

    @Test
    fun midUsesTheSameRulesAsSpread() {
        val quote = marketData(bidTimestamp = 800, askTimestamp = 899)

        assertEquals(
            policy().evaluate(SPREAD, quote, 1_000),
            policy().evaluate(MID, quote, 1_000)
        )
    }

    @Test
    fun midWithFreshCompleteQuoteIsFresh() {
        assertFresh(evaluate(MID, bidTimestamp = 990, askTimestamp = 995))
    }

    @Test
    fun midReportsStaleBidAndAsk() {
        assertErrors(evaluate(MID, bidTimestamp = 899, askTimestamp = 898), STALE_BID, STALE_ASK)
    }

    @Test
    fun midBeyondMaximumTimestampDifferenceIsNotFresh() {
        assertErrors(evaluate(MID, bidTimestamp = 979, askTimestamp = 1_000), TOO_FAR_APART)
    }

    @Test
    fun midFutureTimestampSuppressesTimestampDifferenceError() {
        assertErrors(evaluate(MID, bidTimestamp = 900, askTimestamp = 1_011), ASK_FUTURE)
    }

    @Test
    fun midFreshnessDoesNotDependOnZeroBidPrice() {
        val quote = marketData(bid = 0.0, bidTimestamp = 990, askTimestamp = 995)

        assertFresh(policy().evaluate(MID, quote, 1_000))
    }

    @Test
    fun negativeEpochTimestampsAreEvaluatedNormally() {
        assertFresh(evaluate(SPREAD, bidTimestamp = -110, askTimestamp = -105, evaluationTime = -100))
    }

    @Test
    fun minimumTimestampsAtMinimumEvaluationTimeAreFresh() {
        assertFresh(
            evaluate(
                SPREAD,
                bidTimestamp = Long.MIN_VALUE,
                askTimestamp = Long.MIN_VALUE,
                evaluationTime = Long.MIN_VALUE
            )
        )
    }

    @Test
    fun maximumTimestampsAtMaximumEvaluationTimeAreFresh() {
        assertFresh(
            evaluate(
                SPREAD,
                bidTimestamp = Long.MAX_VALUE,
                askTimestamp = Long.MAX_VALUE,
                evaluationTime = Long.MAX_VALUE
            )
        )
    }

    @Test
    fun minimumBidAgainstMaximumEvaluationTimeIsStaleWithoutOverflow() {
        val result = policy(maxBidAgeMillis = Long.MAX_VALUE).evaluate(
            SALE_PRICE,
            marketData(bidTimestamp = Long.MIN_VALUE),
            Long.MAX_VALUE
        )

        assertErrors(result, STALE_BID)
    }

    @Test
    fun maximumBidAgainstMinimumEvaluationTimeIsFutureWithoutOverflow() {
        val result = policy(allowedFutureSkewMillis = Long.MAX_VALUE).evaluate(
            SALE_PRICE,
            marketData(bidTimestamp = Long.MAX_VALUE),
            Long.MIN_VALUE
        )

        assertErrors(result, BID_FUTURE)
    }

    @Test
    fun minimumAndMaximumQuoteTimestampsProduceStaleAndDifferenceErrors() {
        val result = policy(
            maxBidAgeMillis = Long.MAX_VALUE,
            maxAskAgeMillis = Long.MAX_VALUE,
            maxBidAskDifferenceMillis = Long.MAX_VALUE
        ).evaluate(
            SPREAD,
            marketData(bidTimestamp = Long.MIN_VALUE, askTimestamp = Long.MAX_VALUE),
            Long.MAX_VALUE
        )

        assertErrors(result, STALE_BID, TOO_FAR_APART)
    }

    @Test
    fun maximumThresholdsAllowOrdinaryDistances() {
        val maximumPolicy = policy(
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        )

        assertFresh(maximumPolicy.evaluate(SPREAD, marketData(0, 1), 1))
    }

    @Test
    fun distanceExactlyLongMaxAtLongMaxLimitIsAllowed() {
        val maximumPolicy = policy(
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        )

        assertFresh(maximumPolicy.evaluate(SPREAD, marketData(0, Long.MAX_VALUE), Long.MAX_VALUE))
    }

    @Test
    fun distanceGreaterThanLongMaxIsDetected() {
        val maximumPolicy = policy(
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        )

        assertErrors(
            maximumPolicy.evaluate(
                SPREAD,
                marketData(Long.MIN_VALUE, Long.MAX_VALUE),
                Long.MAX_VALUE
            ),
            STALE_BID,
            TOO_FAR_APART
        )
    }

    @Test
    fun extremeValuesDoNotCauseArithmeticExceptions() {
        val result = runCatching {
            policy(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE).evaluate(
                MID,
                marketData(Long.MIN_VALUE, Long.MAX_VALUE),
                0
            )
        }

        assertTrue(result.isSuccess)
    }

    @Test
    fun timestampDifferenceAcrossZeroIsNotHiddenByAbsoluteOverflow() {
        val result = policy(maxBidAskDifferenceMillis = Long.MAX_VALUE).evaluate(
            SPREAD,
            marketData(Long.MIN_VALUE, Long.MAX_VALUE),
            Long.MAX_VALUE
        )

        assertTrue(TOO_FAR_APART in errorsOf(result))
    }

    @Test
    fun ageAcrossEntireLongRangeIsNotMadeNegativeByOverflow() {
        val result = policy(maxBidAgeMillis = Long.MAX_VALUE).evaluate(
            SALE_PRICE,
            marketData(bidTimestamp = Long.MIN_VALUE),
            Long.MAX_VALUE
        )

        assertEquals(listOf(STALE_BID), errorsOf(result))
    }

    @Test
    fun evaluationBeforeUnixEpochWorks() {
        assertFresh(evaluate(SPREAD, bidTimestamp = -110, askTimestamp = -105, evaluationTime = -100))
    }

    @Test
    fun policyDoesNotCallValidatorsAndEnforcesTimestampPrecondition() {
        val error = capturePreconditionFailure {
            policy().evaluate(PURCHASE_PRICE, marketData(askTimestamp = null), 1_000)
        }

        assertTrue(error.message.orEmpty().contains("Ask timestamp"))
    }

    @Test
    fun policyDoesNotCallAvailabilityEvaluatorAndEnforcesTimestampPrecondition() {
        val error = capturePreconditionFailure {
            policy().evaluate(SALE_PRICE, marketData(bidTimestamp = null), 1_000)
        }

        assertTrue(error.message.orEmpty().contains("Bid timestamp"))
    }

    @Test
    fun pricesDoNotAffectFreshness() {
        val first = policy().evaluate(SPREAD, marketData(bid = 1.0, ask = 2.0), 1_000)
        val second = policy().evaluate(SPREAD, marketData(bid = 10.0, ask = 20.0), 1_000)

        assertEquals(first, second)
    }

    @Test
    fun sourceDoesNotAffectFreshness() {
        val first = policy().evaluate(SPREAD, marketData(sourceId = "source-a"), 1_000)
        val second = policy().evaluate(SPREAD, marketData(sourceId = "source-b"), 1_000)

        assertEquals(first, second)
    }

    @Test
    fun tradingContextDoesNotAffectFreshness() {
        val first = policy().evaluate(SPREAD, marketData(productIsin = "A", currency = "EUR"), 1_000)
        val second = policy().evaluate(SPREAD, marketData(productIsin = "B", currency = "USD"), 1_000)

        assertEquals(first, second)
    }

    @Test
    fun evaluationDoesNotModifyMarketData() {
        val quote = marketData()
        val original = quote.copy()

        policy().evaluate(SPREAD, quote, 1_000)

        assertEquals(original, quote)
    }

    @Test
    fun evaluationDoesNotModifyThresholds() {
        val configuredThresholds = thresholds()
        val original = configuredThresholds.copy()

        MarketDataFreshnessPolicy(configuredThresholds).evaluate(SPREAD, marketData(), 1_000)

        assertEquals(original, configuredThresholds)
    }

    @Test
    fun policyHasNoEngineUiRepositoryValidatorAvailabilityOrCalculatorApiDependency() {
        val method = MarketDataFreshnessPolicy::class.java.declaredMethods
            .single { Modifier.isPublic(it.modifiers) }
        val apiNames = (method.parameterTypes.toList() + method.returnType).map { it.name }

        assertTrue(apiNames.none { it.contains("Validator") || it.contains("AvailabilityEvaluator") })
        assertTrue(apiNames.none { it.contains("Calculator") && !it.contains("CalculationType") })
        assertTrue(apiNames.none { it.contains("Engine") || it.contains("Ui") || it.contains("Repository") })
    }

    private fun evaluate(
        type: MarketDataCalculationType,
        bidTimestamp: Long? = 990,
        askTimestamp: Long? = 995,
        evaluationTime: Long = 1_000
    ): MarketDataFreshnessResult = policy().evaluate(
        type,
        marketData(bidTimestamp = bidTimestamp, askTimestamp = askTimestamp),
        evaluationTime
    )

    private fun policy(
        maxBidAgeMillis: Long = 100,
        maxAskAgeMillis: Long = 100,
        maxBidAskDifferenceMillis: Long = 20,
        allowedFutureSkewMillis: Long = 10
    ) = MarketDataFreshnessPolicy(
        thresholds(
            maxBidAgeMillis,
            maxAskAgeMillis,
            maxBidAskDifferenceMillis,
            allowedFutureSkewMillis
        )
    )

    private fun thresholds(
        maxBidAgeMillis: Long = 100,
        maxAskAgeMillis: Long = 100,
        maxBidAskDifferenceMillis: Long = 20,
        allowedFutureSkewMillis: Long = 10
    ) = MarketDataFreshnessThresholds(
        maxBidAgeMillis,
        maxAskAgeMillis,
        maxBidAskDifferenceMillis,
        allowedFutureSkewMillis
    )

    private fun marketData(
        bidTimestamp: Long? = 990,
        askTimestamp: Long? = 995,
        productIsin: String = "DE000TEST001",
        bid: Double? = 1.95,
        ask: Double? = 2.0,
        currency: String = "EUR",
        sourceId: String = "source-1"
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestamp,
        askTimestampEpochMillis = askTimestamp,
        currency = currency,
        sourceId = sourceId
    )

    private fun assertFresh(result: MarketDataFreshnessResult) {
        assertSame(MarketDataFreshnessResult.Fresh, result)
    }

    private fun assertErrors(
        result: MarketDataFreshnessResult,
        vararg expected: MarketDataFreshnessError
    ) {
        assertEquals(expected.toList(), errorsOf(result))
    }

    private fun errorsOf(result: MarketDataFreshnessResult): List<MarketDataFreshnessError> {
        assertTrue(result is MarketDataFreshnessResult.NotFresh)
        return (result as MarketDataFreshnessResult.NotFresh).errors
    }

    private fun assertThresholdFailure(block: () -> Unit) {
        capturePreconditionFailure(block)
    }

    private fun capturePreconditionFailure(block: () -> Unit): IllegalArgumentException {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (error: IllegalArgumentException) {
            return error
        }
        throw AssertionError("unreachable")
    }

    private companion object {
        val PURCHASE_PRICE = MarketDataCalculationType.PURCHASE_PRICE
        val SALE_PRICE = MarketDataCalculationType.SALE_PRICE
        val SPREAD = MarketDataCalculationType.SPREAD
        val MID = MarketDataCalculationType.MID

        val BID_FUTURE = MarketDataFreshnessError.BID_TIMESTAMP_IN_FUTURE
        val STALE_BID = MarketDataFreshnessError.STALE_BID
        val ASK_FUTURE = MarketDataFreshnessError.ASK_TIMESTAMP_IN_FUTURE
        val STALE_ASK = MarketDataFreshnessError.STALE_ASK
        val TOO_FAR_APART = MarketDataFreshnessError.BID_ASK_TIMESTAMPS_TOO_FAR_APART
    }
}
