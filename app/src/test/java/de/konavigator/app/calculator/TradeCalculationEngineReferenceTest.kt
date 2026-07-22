package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeCalculationEngineReferenceTest {

    @Test
    fun longStandardReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 110.0,
            plannedEntryPrice = 100.0,
            targetLeverage = 4.0,
            isLong = true,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedKnockoutPrice = 75.0,
            expectedTheoreticalUnderlyingValue = 0.25,
            expectedTheoreticalProductValue = 0.25,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 25.0,
            expectedDistancePercent = 25.0
        )
    }

    @Test
    fun longTargetLeverageThreeReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 100.0,
            plannedEntryPrice = 96.0,
            targetLeverage = 3.0,
            isLong = true,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 96.0,
            expectedKnockoutPrice = 64.0,
            expectedTheoreticalUnderlyingValue = 0.32,
            expectedTheoreticalProductValue = 0.32,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 32.0,
            expectedDistancePercent = 33.333333333333336
        )
    }

    @Test
    fun longUsdToEurReferenceAppliesFxByDivisionExactlyOnce() {
        val result = calculate(
            underlyingPrice = 120.0,
            plannedEntryPrice = 110.0,
            targetLeverage = 5.0,
            isLong = true,
            ratio = 0.1,
            currencyConversion = crossCurrency("USD", "EUR", 1.1)
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 110.0,
            expectedKnockoutPrice = 88.0,
            expectedTheoreticalUnderlyingValue = 2.20,
            expectedTheoreticalProductValue = 2.0,
            expectedUnderlyingCurrency = "USD",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 22.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun longVerySmallPositiveProductValueRemainsPositiveAndUnrounded() {
        val result = calculate(
            underlyingPrice = 10.0,
            plannedEntryPrice = 10.0,
            targetLeverage = 5.0,
            isLong = true,
            ratio = 0.0001
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 10.0,
            expectedKnockoutPrice = 8.0,
            expectedTheoreticalUnderlyingValue = 0.0002,
            expectedTheoreticalProductValue = 0.0002,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 2.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun shortStandardReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 90.0,
            plannedEntryPrice = 100.0,
            targetLeverage = 4.0,
            isLong = false,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedKnockoutPrice = 125.0,
            expectedTheoreticalUnderlyingValue = 0.25,
            expectedTheoreticalProductValue = 0.25,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 25.0,
            expectedDistancePercent = 25.0
        )
    }

    @Test
    fun shortTargetLeverageThreeReferenceCalculatesExpectedValues() {
        val result = calculate(
            underlyingPrice = 100.0,
            plannedEntryPrice = 90.0,
            targetLeverage = 3.0,
            isLong = false,
            ratio = 0.01
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 90.0,
            expectedKnockoutPrice = 120.0,
            expectedTheoreticalUnderlyingValue = 0.30,
            expectedTheoreticalProductValue = 0.30,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 30.0,
            expectedDistancePercent = 33.333333333333336
        )
    }

    @Test
    fun shortUsdToEurReferenceAppliesFxByDivisionExactlyOnce() {
        val result = calculate(
            underlyingPrice = 100.0,
            plannedEntryPrice = 110.0,
            targetLeverage = 5.0,
            isLong = false,
            ratio = 0.1,
            currencyConversion = crossCurrency("USD", "EUR", 1.1)
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 110.0,
            expectedKnockoutPrice = 132.0,
            expectedTheoreticalUnderlyingValue = 2.20,
            expectedTheoreticalProductValue = 2.0,
            expectedUnderlyingCurrency = "USD",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 22.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun shortVerySmallPositiveProductValueRemainsPositiveAndUnrounded() {
        val result = calculate(
            underlyingPrice = 10.0,
            plannedEntryPrice = 10.0,
            targetLeverage = 5.0,
            isLong = false,
            ratio = 0.0001
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 10.0,
            expectedKnockoutPrice = 12.0,
            expectedTheoreticalUnderlyingValue = 0.0002,
            expectedTheoreticalProductValue = 0.0002,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 2.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun eurToUsdReferenceAppliesFxByDivisionExactlyOnce() {
        val result = calculate(
            plannedEntryPrice = 100.0,
            targetLeverage = 5.0,
            ratio = 0.2,
            currencyConversion = crossCurrency("EUR", "USD", 0.8)
        )

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedKnockoutPrice = 80.0,
            expectedTheoreticalUnderlyingValue = 4.0,
            expectedTheoreticalProductValue = 5.0,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "USD",
            expectedDistanceAbsolute = 20.0,
            expectedDistancePercent = 20.0
        )
    }

    @Test
    fun zeroRatioIsRejectedWithStructuredError() {
        val result = calculate(
            plannedEntryPrice = 100.0,
            targetLeverage = 5.0,
            ratio = 0.0
        )

        assertInvalidReference(result, TradeCalculationError.INVALID_RATIO)
    }

    @Test
    fun extremeFiniteLongLeverageRejectsZeroProductValue() {
        val result = calculate(
            plannedEntryPrice = 100.0,
            targetLeverage = Double.MAX_VALUE,
            isLong = true
        )

        assertInvalidReference(
            result,
            TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
        )
    }

    @Test
    fun extremeFiniteShortLeverageRejectsZeroProductValue() {
        val result = calculate(
            plannedEntryPrice = 100.0,
            targetLeverage = Double.MAX_VALUE,
            isLong = false
        )

        assertInvalidReference(
            result,
            TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
        )
    }

    @Test
    fun nextUpFromOneCharacterizesFiniteLongAndShortResults() {
        val targetLeverage = Math.nextUp(1.0)
        val longResult = calculate(targetLeverage = targetLeverage, isLong = true)
        val shortResult = calculate(targetLeverage = targetLeverage, isLong = false)

        assertSuccessfulReference(
            result = longResult,
            expectedPlannedEntryPrice = 100.0,
            expectedKnockoutPrice = 2.220446049250313E-14,
            expectedTheoreticalUnderlyingValue = 0.9999999999999998,
            expectedTheoreticalProductValue = 0.9999999999999998,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 99.99999999999997,
            expectedDistancePercent = 99.99999999999997
        )
        assertSuccessfulReference(
            result = shortResult,
            expectedPlannedEntryPrice = 100.0,
            expectedKnockoutPrice = 199.99999999999997,
            expectedTheoreticalUnderlyingValue = 0.9999999999999998,
            expectedTheoreticalProductValue = 0.9999999999999998,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 99.99999999999997,
            expectedDistancePercent = 99.99999999999997
        )
        assertTrue(longResult.knockoutPrice!! > 0.0)
        assertTrue(shortResult.knockoutPrice!! < 200.0)
        assertTrue(allCalculatedValuesAreFinite(longResult))
        assertTrue(allCalculatedValuesAreFinite(shortResult))
    }

    @Test
    fun isDeterministicAndCurrentlyIndependentOfCurrentUnderlyingPrice() {
        val firstInput = input(
            underlyingPrice = 100.0,
            plannedEntryPrice = 95.0,
            targetLeverage = 5.0,
            isLong = false,
            ratio = 0.1
        )
        val changedCurrentPriceInput = firstInput.copy(underlyingPrice = 1_000.0)

        val first = TradeCalculationEngine.calculateTrade(firstInput)
        val repeated = TradeCalculationEngine.calculateTrade(firstInput)
        val changedCurrentPrice =
            TradeCalculationEngine.calculateTrade(changedCurrentPriceInput)

        assertEquals(first, repeated)
        // The current underlying price is used by the relation evaluator before
        // the engine. It currently has no effect on the engine mathematics.
        assertEquals(first, changedCurrentPrice)
    }

    @Test
    fun ratioZeroPointZeroZeroOneIsAppliedExactlyOnce() {
        val result = calculate(ratio = 0.001)

        assertSuccessfulReference(
            result = result,
            expectedPlannedEntryPrice = 100.0,
            expectedKnockoutPrice = 80.0,
            expectedTheoreticalUnderlyingValue = 0.02,
            expectedTheoreticalProductValue = 0.02,
            expectedUnderlyingCurrency = "EUR",
            expectedProductCurrency = "EUR",
            expectedDistanceAbsolute = 20.0,
            expectedDistancePercent = 20.0
        )
    }

    private fun calculate(
        underlyingPrice: Double = 110.0,
        plannedEntryPrice: Double = 100.0,
        targetLeverage: Double = 5.0,
        isLong: Boolean = true,
        ratio: Double = 0.01,
        currencyConversion: CurrencyConversion =
            CurrencyConversion.SameCurrency(currencyCode("EUR"))
    ): TradeCalculationResult = TradeCalculationEngine.calculateTrade(
        input(
            underlyingPrice = underlyingPrice,
            plannedEntryPrice = plannedEntryPrice,
            targetLeverage = targetLeverage,
            isLong = isLong,
            ratio = ratio,
            currencyConversion = currencyConversion
        )
    )

    private fun input(
        underlyingPrice: Double,
        plannedEntryPrice: Double,
        targetLeverage: Double,
        isLong: Boolean,
        ratio: Double,
        currencyConversion: CurrencyConversion =
            CurrencyConversion.SameCurrency(currencyCode("EUR"))
    ) = TradeCalculationInput(
        underlyingPrice = underlyingPrice,
        plannedEntryPrice = plannedEntryPrice,
        targetLeverage = targetLeverage,
        isLong = isLong,
        ratio = ratio,
        currencyConversion = currencyConversion
    )

    private fun assertSuccessfulReference(
        result: TradeCalculationResult,
        expectedPlannedEntryPrice: Double,
        expectedKnockoutPrice: Double,
        expectedTheoreticalUnderlyingValue: Double,
        expectedTheoreticalProductValue: Double,
        expectedUnderlyingCurrency: String,
        expectedProductCurrency: String,
        expectedDistanceAbsolute: Double,
        expectedDistancePercent: Double
    ) {
        assertTrue(result.isValid)
        assertNull(result.error)
        assertEquals(expectedPlannedEntryPrice, result.underlyingPrice!!, 0.0)
        assertNotNull(result.knockoutPrice)
        assertNotNull(result.targetLeverage)
        assertNotNull(result.theoreticalValueInUnderlyingCurrency)
        assertNotNull(result.theoreticalProductValue)
        assertNotNull(result.underlyingExposureInProductCurrency)
        assertNotNull(result.calculatedTheoreticalLeverageAtEntry)
        assertNotNull(result.distanceToKnockoutAbsolute)
        assertNotNull(result.distanceToKnockoutPercent)
        assertHybridEquals(expectedKnockoutPrice, result.knockoutPrice!!)
        assertHybridEquals(
            expectedTheoreticalUnderlyingValue,
            result.theoreticalValueInUnderlyingCurrency!!
        )
        assertHybridEquals(expectedTheoreticalProductValue, result.theoreticalProductValue!!)
        val expectedTargetLeverage =
            expectedPlannedEntryPrice / expectedDistanceAbsolute
        assertHybridEquals(expectedTargetLeverage, result.targetLeverage!!)
        assertHybridEquals(
            expectedTargetLeverage,
            result.calculatedTheoreticalLeverageAtEntry!!
        )
        assertHybridEquals(
            result.theoreticalProductValue!! *
                result.calculatedTheoreticalLeverageAtEntry!!,
            result.underlyingExposureInProductCurrency!!
        )
        assertEquals(currencyCode(expectedUnderlyingCurrency), result.underlyingCurrency)
        assertEquals(currencyCode(expectedProductCurrency), result.productCurrency)
        assertHybridEquals(expectedDistanceAbsolute, result.distanceToKnockoutAbsolute!!)
        assertHybridEquals(expectedDistancePercent, result.distanceToKnockoutPercent!!)
    }

    private fun assertHybridEquals(expected: Double, actual: Double) {
        val tolerance = max(
            ABSOLUTE_TOLERANCE,
            RELATIVE_TOLERANCE * abs(expected)
        )
        assertTrue(
            "Expected $expected, actual $actual, tolerance $tolerance",
            abs(actual - expected) <= tolerance
        )
    }

    private fun allCalculatedValuesAreFinite(result: TradeCalculationResult) =
        result.knockoutPrice!!.isFinite() &&
            result.targetLeverage!!.isFinite() &&
            result.theoreticalValueInUnderlyingCurrency!!.isFinite() &&
            result.theoreticalProductValue!!.isFinite() &&
            result.underlyingExposureInProductCurrency!!.isFinite() &&
            result.calculatedTheoreticalLeverageAtEntry!!.isFinite() &&
            result.distanceToKnockoutAbsolute!!.isFinite() &&
            result.distanceToKnockoutPercent!!.isFinite()

    private fun assertInvalidReference(
        result: TradeCalculationResult,
        expectedError: TradeCalculationError
    ) {
        assertTrue(!result.isValid)
        assertEquals(expectedError, result.error)
        assertNull(result.underlyingPrice)
        assertNull(result.targetLeverage)
        assertNull(result.knockoutPrice)
        assertNull(result.theoreticalValueInUnderlyingCurrency)
        assertNull(result.theoreticalProductValue)
        assertNull(result.underlyingExposureInProductCurrency)
        assertNull(result.calculatedTheoreticalLeverageAtEntry)
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

    private companion object {
        const val ABSOLUTE_TOLERANCE = 1e-12
        const val RELATIVE_TOLERANCE = 1e-12
    }
}
