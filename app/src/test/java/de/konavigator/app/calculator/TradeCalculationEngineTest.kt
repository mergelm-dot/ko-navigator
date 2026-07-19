package de.konavigator.app.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeCalculationEngineTest {

    @Test
    fun zeroPlannedEntryPriceIsInvalid() {
        val result = calculateTrade(plannedEntryPrice = 0.0)

        assertInvalid(result, TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE)
    }

    @Test
    fun negativePlannedEntryPriceIsInvalid() {
        val result = calculateTrade(plannedEntryPrice = -100.0)

        assertInvalid(result, TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE)
    }

    @Test
    fun nanPlannedEntryPriceIsInvalid() {
        val result = calculateTrade(plannedEntryPrice = Double.NaN)

        assertInvalid(result, TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE)
    }

    @Test
    fun positiveInfinitePlannedEntryPriceIsInvalid() {
        val result = calculateTrade(plannedEntryPrice = Double.POSITIVE_INFINITY)

        assertInvalid(result, TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE)
    }

    @Test
    fun negativeInfinitePlannedEntryPriceIsInvalid() {
        val result = calculateTrade(plannedEntryPrice = Double.NEGATIVE_INFINITY)

        assertInvalid(result, TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE)
    }

    @Test
    fun targetLeverageOfOneIsInvalid() {
        val result = calculateTrade(leverage = 1.0)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun targetLeverageBelowOneIsInvalid() {
        val result = calculateTrade(leverage = 0.5)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun zeroTargetLeverageIsInvalid() {
        val result = calculateTrade(leverage = 0.0)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun negativeTargetLeverageIsInvalid() {
        val result = calculateTrade(leverage = -5.0)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun nanTargetLeverageIsInvalid() {
        val result = calculateTrade(leverage = Double.NaN)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun positiveInfiniteTargetLeverageIsInvalid() {
        val result = calculateTrade(leverage = Double.POSITIVE_INFINITY)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun negativeInfiniteTargetLeverageIsInvalid() {
        val result = calculateTrade(leverage = Double.NEGATIVE_INFINITY)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun validLongTradeCalculatesTheoreticalBarrierAndDistances() {
        val result = calculateTrade(
            plannedEntryPrice = 100.0,
            leverage = 5.0,
            isLong = true
        )

        assertTrue(result.isValid)
        assertNull(result.error)
        assertEquals(80.0, result.knockoutPrice, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutAbsolute, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutPercent, TOLERANCE)
    }

    @Test
    fun validShortTradeCalculatesTheoreticalBarrierAndDistances() {
        val result = calculateTrade(
            plannedEntryPrice = 100.0,
            leverage = 5.0,
            isLong = false
        )

        assertTrue(result.isValid)
        assertNull(result.error)
        assertEquals(120.0, result.knockoutPrice, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutAbsolute, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutPercent, TOLERANCE)
    }

    @Test
    fun overflowingDerivedShortKnockoutPriceIsInvalid() {
        val result = calculateTrade(
            plannedEntryPrice = Double.MAX_VALUE,
            leverage = 2.0,
            isLong = false
        )

        assertInvalid(result, TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE)
    }

    private fun calculateTrade(
        plannedEntryPrice: Double = 100.0,
        leverage: Double = 5.0,
        isLong: Boolean = true
    ): TradeCalculationResult {
        return TradeCalculationEngine.calculateTrade(
            TradeCalculationInput(
                underlyingPrice = 110.0,
                plannedEntryPrice = plannedEntryPrice,
                leverage = leverage,
                isLong = isLong
            )
        )
    }

    private fun assertInvalid(
        result: TradeCalculationResult,
        expectedError: TradeCalculationError
    ) {
        assertFalse(result.isValid)
        assertEquals(expectedError, result.error)
        assertEquals(0.0, result.knockoutPrice, TOLERANCE)
        assertEquals(0.0, result.distanceToKnockoutAbsolute, TOLERANCE)
        assertEquals(0.0, result.distanceToKnockoutPercent, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
