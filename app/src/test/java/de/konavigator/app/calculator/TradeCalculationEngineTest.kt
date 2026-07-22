package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val result = calculateTrade(targetLeverage = 1.0)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun targetLeverageBelowOneIsInvalid() {
        val result = calculateTrade(targetLeverage = 0.5)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun zeroTargetLeverageIsInvalid() {
        val result = calculateTrade(targetLeverage = 0.0)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun negativeTargetLeverageIsInvalid() {
        val result = calculateTrade(targetLeverage = -5.0)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun nanTargetLeverageIsInvalid() {
        val result = calculateTrade(targetLeverage = Double.NaN)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun positiveInfiniteTargetLeverageIsInvalid() {
        val result = calculateTrade(targetLeverage = Double.POSITIVE_INFINITY)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun negativeInfiniteTargetLeverageIsInvalid() {
        val result = calculateTrade(targetLeverage = Double.NEGATIVE_INFINITY)

        assertInvalid(result, TradeCalculationError.INVALID_TARGET_LEVERAGE)
    }

    @Test
    fun validLongTradeCalculatesTheoreticalBarrierAndDistances() {
        val result = calculateTrade(
            plannedEntryPrice = 100.0,
            targetLeverage = 5.0,
            isLong = true
        )

        assertTrue(result.isValid)
        assertNull(result.error)
        assertNotNull(result.theoreticalValueInUnderlyingCurrency)
        assertNotNull(result.theoreticalProductValue)
        assertNotNull(result.knockoutPrice)
        assertNotNull(result.distanceToKnockoutAbsolute)
        assertNotNull(result.distanceToKnockoutPercent)
        assertEquals(0.2, result.theoreticalValueInUnderlyingCurrency!!, TOLERANCE)
        assertEquals(0.2, result.theoreticalProductValue!!, TOLERANCE)
        assertEquals(currencyCode("EUR"), result.underlyingCurrency)
        assertEquals(currencyCode("EUR"), result.productCurrency)
        assertEquals(80.0, result.knockoutPrice!!, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutAbsolute!!, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutPercent!!, TOLERANCE)
    }

    @Test
    fun validShortTradeCalculatesTheoreticalBarrierAndDistances() {
        val result = calculateTrade(
            plannedEntryPrice = 100.0,
            targetLeverage = 5.0,
            isLong = false
        )

        assertTrue(result.isValid)
        assertNull(result.error)
        assertNotNull(result.theoreticalValueInUnderlyingCurrency)
        assertNotNull(result.theoreticalProductValue)
        assertNotNull(result.knockoutPrice)
        assertNotNull(result.distanceToKnockoutAbsolute)
        assertNotNull(result.distanceToKnockoutPercent)
        assertEquals(0.2, result.theoreticalValueInUnderlyingCurrency!!, TOLERANCE)
        assertEquals(0.2, result.theoreticalProductValue!!, TOLERANCE)
        assertEquals(120.0, result.knockoutPrice!!, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutAbsolute!!, TOLERANCE)
        assertEquals(20.0, result.distanceToKnockoutPercent!!, TOLERANCE)
    }

    @Test
    fun overflowingDerivedShortKnockoutPriceIsInvalid() {
        val result = calculateTrade(
            plannedEntryPrice = Double.MAX_VALUE,
            targetLeverage = 2.0,
            isLong = false
        )

        assertInvalid(result, TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE)
    }

    @Test
    fun zeroRatioIsInvalid() {
        assertInvalid(calculateTrade(ratio = 0.0), TradeCalculationError.INVALID_RATIO)
    }

    @Test
    fun negativeRatioIsInvalid() {
        assertInvalid(calculateTrade(ratio = -0.01), TradeCalculationError.INVALID_RATIO)
    }

    @Test
    fun nanRatioIsInvalid() {
        assertInvalid(calculateTrade(ratio = Double.NaN), TradeCalculationError.INVALID_RATIO)
    }

    @Test
    fun infiniteRatiosAreInvalid() {
        listOf(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { ratio ->
            assertInvalid(calculateTrade(ratio = ratio), TradeCalculationError.INVALID_RATIO)
        }
    }

    @Test
    fun nonFiniteTheoreticalUnderlyingValueIsInvalid() {
        assertInvalid(
            calculateTrade(
                plannedEntryPrice = Double.MAX_VALUE,
                targetLeverage = 2.0,
                ratio = 4.0
            ),
            TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
        )
    }

    @Test
    fun nonFiniteTheoreticalProductValueAfterFxIsInvalid() {
        assertInvalid(
            calculateTrade(
                ratio = 0.05,
                currencyConversion = crossCurrency("USD", "EUR", Double.MIN_VALUE)
            ),
            TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
        )
    }

    @Test
    fun inputAndResultContractsHaveNoLegacyExchangeRateCertificatePriceOrDefaults() {
        assertEquals(
            setOf(
                "underlyingPrice",
                "plannedEntryPrice",
                "targetLeverage",
                "isLong",
                "ratio",
                "currencyConversion"
            ),
            instanceFieldNames(TradeCalculationInput::class.java)
        )
        assertEquals(
            setOf(
                "isValid",
                "underlyingPrice",
                "knockoutPrice",
                "theoreticalValueInUnderlyingCurrency",
                "theoreticalProductValue",
                "underlyingCurrency",
                "productCurrency",
                "distanceToKnockoutAbsolute",
                "distanceToKnockoutPercent",
                "error"
            ),
            instanceFieldNames(TradeCalculationResult::class.java)
        )
        assertEquals(6, TradeCalculationInput::class.java.constructors.single().parameterCount)
    }

    private fun calculateTrade(
        plannedEntryPrice: Double = 100.0,
        targetLeverage: Double = 5.0,
        isLong: Boolean = true,
        ratio: Double = 0.01,
        currencyConversion: CurrencyConversion =
            CurrencyConversion.SameCurrency(currencyCode("EUR"))
    ): TradeCalculationResult {
        return TradeCalculationEngine.calculateTrade(
            TradeCalculationInput(
                underlyingPrice = 110.0,
                plannedEntryPrice = plannedEntryPrice,
                targetLeverage = targetLeverage,
                isLong = isLong,
                ratio = ratio,
                currencyConversion = currencyConversion
            )
        )
    }

    private fun assertInvalid(
        result: TradeCalculationResult,
        expectedError: TradeCalculationError
    ) {
        assertFalse(result.isValid)
        assertEquals(expectedError, result.error)
        assertNull(result.underlyingPrice)
        assertNull(result.knockoutPrice)
        assertNull(result.theoreticalValueInUnderlyingCurrency)
        assertNull(result.theoreticalProductValue)
        assertNull(result.underlyingCurrency)
        assertNull(result.productCurrency)
        assertNull(result.distanceToKnockoutAbsolute)
        assertNull(result.distanceToKnockoutPercent)
    }

    private fun crossCurrency(
        underlyingCurrency: String,
        productCurrency: String,
        rate: Double
    ): CurrencyConversion.CrossCurrency =
        when (
            val result = CurrencyConversion.CrossCurrency.create(
                currencyCode(underlyingCurrency),
                currencyCode(productCurrency),
                rate
            )
        ) {
            is CurrencyConversionCreationResult.Success -> result.conversion
            is CurrencyConversionCreationResult.Failure ->
                error("Unexpected invalid test conversion: ${result.error}")
        }

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid test currency: ${result.error}")
        }

    private fun instanceFieldNames(type: Class<*>) =
        type.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
            .map { it.name }
            .toSet()

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
