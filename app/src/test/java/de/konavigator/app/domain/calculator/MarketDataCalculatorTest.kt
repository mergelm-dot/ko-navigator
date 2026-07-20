package de.konavigator.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculatorTest {

    @Test
    fun absoluteSpreadRejectsNegativeBid() = assertAbsoluteFailure(-1.0, 2.0, INVALID_BID)

    @Test
    fun absoluteSpreadRejectsNanBid() = assertAbsoluteFailure(Double.NaN, 2.0, INVALID_BID)

    @Test
    fun absoluteSpreadRejectsPositiveInfiniteBid() =
        assertAbsoluteFailure(Double.POSITIVE_INFINITY, 2.0, INVALID_BID)

    @Test
    fun absoluteSpreadRejectsNegativeInfiniteBid() =
        assertAbsoluteFailure(Double.NEGATIVE_INFINITY, 2.0, INVALID_BID)

    @Test
    fun absoluteSpreadRejectsZeroAsk() = assertAbsoluteFailure(0.0, 0.0, INVALID_ASK)

    @Test
    fun absoluteSpreadRejectsNegativeAsk() = assertAbsoluteFailure(0.0, -1.0, INVALID_ASK)

    @Test
    fun absoluteSpreadRejectsNanAsk() = assertAbsoluteFailure(0.0, Double.NaN, INVALID_ASK)

    @Test
    fun absoluteSpreadRejectsPositiveInfiniteAsk() =
        assertAbsoluteFailure(0.0, Double.POSITIVE_INFINITY, INVALID_ASK)

    @Test
    fun absoluteSpreadRejectsNegativeInfiniteAsk() =
        assertAbsoluteFailure(0.0, Double.NEGATIVE_INFINITY, INVALID_ASK)

    @Test
    fun absoluteSpreadRejectsBidAboveAsk() = assertAbsoluteFailure(2.01, 2.0, BID_ABOVE_ASK)

    @Test
    fun relativeSpreadRejectsNegativeBid() = assertRelativeFailure(-1.0, 2.0, INVALID_BID)

    @Test
    fun relativeSpreadRejectsNanBid() = assertRelativeFailure(Double.NaN, 2.0, INVALID_BID)

    @Test
    fun relativeSpreadRejectsPositiveInfiniteBid() =
        assertRelativeFailure(Double.POSITIVE_INFINITY, 2.0, INVALID_BID)

    @Test
    fun relativeSpreadRejectsNegativeInfiniteBid() =
        assertRelativeFailure(Double.NEGATIVE_INFINITY, 2.0, INVALID_BID)

    @Test
    fun relativeSpreadRejectsZeroAsk() = assertRelativeFailure(0.0, 0.0, INVALID_ASK)

    @Test
    fun relativeSpreadRejectsNegativeAsk() = assertRelativeFailure(0.0, -1.0, INVALID_ASK)

    @Test
    fun relativeSpreadRejectsNanAsk() = assertRelativeFailure(0.0, Double.NaN, INVALID_ASK)

    @Test
    fun relativeSpreadRejectsPositiveInfiniteAsk() =
        assertRelativeFailure(0.0, Double.POSITIVE_INFINITY, INVALID_ASK)

    @Test
    fun relativeSpreadRejectsNegativeInfiniteAsk() =
        assertRelativeFailure(0.0, Double.NEGATIVE_INFINITY, INVALID_ASK)

    @Test
    fun relativeSpreadRejectsBidAboveAsk() = assertRelativeFailure(2.01, 2.0, BID_ABOVE_ASK)

    @Test
    fun midPriceRejectsNegativeBid() = assertMidFailure(-1.0, 2.0, INVALID_BID)

    @Test
    fun midPriceRejectsNanBid() = assertMidFailure(Double.NaN, 2.0, INVALID_BID)

    @Test
    fun midPriceRejectsPositiveInfiniteBid() =
        assertMidFailure(Double.POSITIVE_INFINITY, 2.0, INVALID_BID)

    @Test
    fun midPriceRejectsNegativeInfiniteBid() =
        assertMidFailure(Double.NEGATIVE_INFINITY, 2.0, INVALID_BID)

    @Test
    fun midPriceRejectsZeroAsk() = assertMidFailure(0.0, 0.0, INVALID_ASK)

    @Test
    fun midPriceRejectsNegativeAsk() = assertMidFailure(0.0, -1.0, INVALID_ASK)

    @Test
    fun midPriceRejectsNanAsk() = assertMidFailure(0.0, Double.NaN, INVALID_ASK)

    @Test
    fun midPriceRejectsPositiveInfiniteAsk() =
        assertMidFailure(0.0, Double.POSITIVE_INFINITY, INVALID_ASK)

    @Test
    fun midPriceRejectsNegativeInfiniteAsk() =
        assertMidFailure(0.0, Double.NEGATIVE_INFINITY, INVALID_ASK)

    @Test
    fun midPriceRejectsBidAboveAsk() = assertMidFailure(2.01, 2.0, BID_ABOVE_ASK)

    @Test
    fun invalidBidHasPriorityOverInvalidAsk() {
        calculations().forEach { calculate ->
            assertFailure(calculate(Double.NaN, 0.0), INVALID_BID)
        }
    }

    @Test
    fun validBidAndInvalidAskReturnInvalidAsk() {
        calculations().forEach { calculate ->
            assertFailure(calculate(0.0, Double.NaN), INVALID_ASK)
        }
    }

    @Test
    fun bidAboveAskIsCheckedOnlyAfterBothNumbersAreValid() {
        calculations().forEach { calculate ->
            assertFailure(calculate(Double.POSITIVE_INFINITY, 1.0), INVALID_BID)
            assertFailure(calculate(2.0, Double.NEGATIVE_INFINITY), INVALID_ASK)
            assertFailure(calculate(2.0, 1.0), BID_ABOVE_ASK)
        }
    }

    @Test
    fun invalidInputsDoNotThrowExceptions() {
        calculations().forEach { calculate ->
            assertTrue(runCatching { calculate(Double.NaN, Double.NaN) }.isSuccess)
        }
    }

    @Test
    fun invalidInputsNeverReturnSuccess() {
        calculations().forEach { calculate ->
            assertTrue(calculate(-1.0, 0.0) is MarketDataCalculationResult.Failure)
        }
    }

    @Test
    fun absoluteSpreadCalculatesStandardCase() {
        assertSuccessValue(calculateAbsolute(1.95, 2.0), 0.05)
    }

    @Test
    fun absoluteSpreadAllowsZeroBid() {
        assertSuccessValue(calculateAbsolute(0.0, 2.0), 2.0)
    }

    @Test
    fun absoluteSpreadReturnsZeroForEqualPrices() {
        assertSuccessValue(calculateAbsolute(2.0, 2.0), 0.0)
    }

    @Test
    fun absoluteSpreadPreservesVerySmallPositiveDifference() {
        val bid = 1.0
        val ask = Math.nextUp(bid)
        val value = successValue(calculateAbsolute(bid, ask))

        assertEquals(ask - bid, value, 0.0)
        assertTrue(value > 0.0)
    }

    @Test
    fun absoluteSpreadSupportsVerySmallPositiveAsk() {
        assertSuccessValue(calculateAbsolute(0.0, Double.MIN_VALUE), Double.MIN_VALUE, 0.0)
    }

    @Test
    fun absoluteSpreadSupportsVeryLargeValidValues() {
        val value = successValue(calculateAbsolute(Double.MAX_VALUE / 2.0, Double.MAX_VALUE))

        assertTrue(value.isFinite())
        assertEquals(Double.MAX_VALUE / 2.0, value, LARGE_TOLERANCE)
    }

    @Test
    fun absoluteSpreadResultIsFinite() {
        assertTrue(successValue(calculateAbsolute(1.95, 2.0)).isFinite())
    }

    @Test
    fun absoluteSpreadResultIsNotNegative() {
        assertTrue(successValue(calculateAbsolute(1.95, 2.0)) >= 0.0)
    }

    @Test
    fun absoluteSpreadDoesNotHideCrossedQuoteWithAbsoluteValue() {
        assertFailure(calculateAbsolute(2.0, 1.0), BID_ABOVE_ASK)
    }

    @Test
    fun relativeSpreadCalculatesStandardCase() {
        assertSuccessValue(calculateRelative(1.95, 2.0), 2.5)
    }

    @Test
    fun relativeSpreadReturnsOneHundredPercentForZeroBid() {
        assertSuccessValue(calculateRelative(0.0, 2.0), 100.0)
    }

    @Test
    fun relativeSpreadReturnsZeroForEqualPrices() {
        assertSuccessValue(calculateRelative(2.0, 2.0), 0.0)
    }

    @Test
    fun relativeSpreadPreservesVerySmallPositiveDifference() {
        val bid = 1.0
        val ask = Math.nextUp(bid)
        val value = successValue(calculateRelative(bid, ask))

        assertTrue(value > 0.0)
        assertTrue(value < 100.0)
    }

    @Test
    fun relativeSpreadSupportsVerySmallPositiveAsk() {
        assertSuccessValue(calculateRelative(0.0, Double.MIN_VALUE), 100.0)
    }

    @Test
    fun relativeSpreadSupportsVeryLargeValidValues() {
        assertSuccessValue(calculateRelative(Double.MAX_VALUE / 2.0, Double.MAX_VALUE), 50.0)
    }

    @Test
    fun relativeSpreadResultIsFinite() {
        assertTrue(successValue(calculateRelative(1.95, 2.0)).isFinite())
    }

    @Test
    fun relativeSpreadResultStaysWithinZeroAndOneHundred() {
        listOf(0.0 to 2.0, 1.0 to 2.0, 2.0 to 2.0).forEach { (bid, ask) ->
            val value = successValue(calculateRelative(bid, ask))
            assertTrue(value in 0.0..100.0)
        }
    }

    @Test
    fun relativeSpreadIsExplicitlyBasedOnAsk() {
        val value = successValue(calculateRelative(1.0, 4.0))

        assertEquals(75.0, value, TOLERANCE)
        assertTrue(value != 300.0)
    }

    @Test
    fun midPriceCalculatesStandardCase() {
        assertSuccessValue(calculateMid(1.95, 2.0), 1.975)
    }

    @Test
    fun midPriceCalculatesHalfOfAskForZeroBid() {
        assertSuccessValue(calculateMid(0.0, 2.0), 1.0)
    }

    @Test
    fun midPriceReturnsSamePriceForEqualPrices() {
        assertSuccessValue(calculateMid(2.0, 2.0), 2.0)
    }

    @Test
    fun midPriceSupportsVerySmallValues() {
        assertSuccessValue(calculateMid(0.0, Double.MIN_VALUE * 2.0), Double.MIN_VALUE, 0.0)
    }

    @Test
    fun midPricePreservesVerySmallDifference() {
        val bid = 1.0
        val ask = Math.nextUp(Math.nextUp(bid))
        val value = successValue(calculateMid(bid, ask))

        assertTrue(value > bid)
        assertTrue(value < ask)
    }

    @Test
    fun midPriceSupportsLargeFiniteValues() {
        val bid = Double.MAX_VALUE / 2.0
        val ask = Double.MAX_VALUE
        val value = successValue(calculateMid(bid, ask))

        assertTrue(value.isFinite())
        assertTrue(value in bid..ask)
    }

    @Test
    fun midPriceForTwoMaximumValuesRemainsFinite() {
        assertSuccessValue(calculateMid(Double.MAX_VALUE, Double.MAX_VALUE), Double.MAX_VALUE, 0.0)
    }

    @Test
    fun midPriceResultLiesBetweenBidAndAsk() {
        val bid = 1.95
        val ask = 2.0
        val value = successValue(calculateMid(bid, ask))

        assertTrue(value in bid..ask)
    }

    @Test
    fun midPriceReturnsNoValueForCrossedQuote() {
        assertFailure(calculateMid(2.0, 1.0), BID_ABOVE_ASK)
    }

    @Test
    fun validCalculationReturnsSuccess() {
        assertTrue(calculateAbsolute(1.95, 2.0) is MarketDataCalculationResult.Success)
    }

    @Test
    fun invalidCalculationReturnsFailure() {
        assertTrue(calculateAbsolute(-1.0, 2.0) is MarketDataCalculationResult.Failure)
    }

    @Test
    fun successValueRemainsUnrounded() {
        val value = successValue(calculateAbsolute(1.666666, 2.0))

        assertEquals(0.333334, value, TOLERANCE)
        assertTrue(value > 0.33 + TOLERANCE)
    }

    @Test
    fun failureContainsExactlyExpectedErrorCode() {
        val result = calculateAbsolute(-1.0, 2.0) as MarketDataCalculationResult.Failure

        assertEquals(INVALID_BID, result.error)
    }

    @Test
    fun resultPayloadsAreNonNullableTypes() {
        assertEquals(
            Double::class.javaPrimitiveType,
            MarketDataCalculationResult.Success::class.java.getDeclaredField("value").type
        )
        assertEquals(
            MarketDataCalculationError::class.java,
            MarketDataCalculationResult.Failure::class.java.getDeclaredField("error").type
        )
    }

    @Test
    fun resultTypesExposeNoMessageProperty() {
        val methodNames =
            MarketDataCalculationResult.Success::class.java.declaredMethods.map { it.name } +
                MarketDataCalculationResult.Failure::class.java.declaredMethods.map { it.name }

        assertTrue(methodNames.none { it.contains("message", ignoreCase = true) })
    }

    private fun assertAbsoluteFailure(
        bid: Double,
        ask: Double,
        expectedError: MarketDataCalculationError
    ) = assertFailure(calculateAbsolute(bid, ask), expectedError)

    private fun assertRelativeFailure(
        bid: Double,
        ask: Double,
        expectedError: MarketDataCalculationError
    ) = assertFailure(calculateRelative(bid, ask), expectedError)

    private fun assertMidFailure(
        bid: Double,
        ask: Double,
        expectedError: MarketDataCalculationError
    ) = assertFailure(calculateMid(bid, ask), expectedError)

    private fun assertFailure(
        result: MarketDataCalculationResult,
        expectedError: MarketDataCalculationError
    ) {
        assertTrue(result is MarketDataCalculationResult.Failure)
        assertEquals(expectedError, (result as MarketDataCalculationResult.Failure).error)
    }

    private fun assertSuccessValue(
        result: MarketDataCalculationResult,
        expectedValue: Double,
        tolerance: Double = TOLERANCE
    ) {
        assertEquals(expectedValue, successValue(result), tolerance)
    }

    private fun successValue(result: MarketDataCalculationResult): Double {
        assertTrue(result is MarketDataCalculationResult.Success)
        return (result as MarketDataCalculationResult.Success).value
    }

    private fun calculateAbsolute(bid: Double, ask: Double): MarketDataCalculationResult {
        return MarketDataCalculator.calculateAbsoluteSpread(bid = bid, ask = ask)
    }

    private fun calculateRelative(bid: Double, ask: Double): MarketDataCalculationResult {
        return MarketDataCalculator.calculateRelativeSpreadToAskPercent(bid = bid, ask = ask)
    }

    private fun calculateMid(bid: Double, ask: Double): MarketDataCalculationResult {
        return MarketDataCalculator.calculateMidPrice(bid = bid, ask = ask)
    }

    private fun calculations(): List<(Double, Double) -> MarketDataCalculationResult> {
        return listOf(::calculateAbsolute, ::calculateRelative, ::calculateMid)
    }

    private companion object {
        val INVALID_BID = MarketDataCalculationError.INVALID_BID
        val INVALID_ASK = MarketDataCalculationError.INVALID_ASK
        val BID_ABOVE_ASK = MarketDataCalculationError.BID_ABOVE_ASK

        const val TOLERANCE = 1e-9
        const val LARGE_TOLERANCE = 1e292
    }
}
