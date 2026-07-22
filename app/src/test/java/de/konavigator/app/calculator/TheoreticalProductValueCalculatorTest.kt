package de.konavigator.app.calculator

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
import java.lang.reflect.Modifier
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoreticalProductValueCalculatorTest {

    @Test
    fun sameCurrencyUsdWithDistanceTwentyAndRatioZeroPointOneReturnsTwoUsd() {
        val conversion = CurrencyConversion.SameCurrency(currencyCode("USD"))
        val result = assertSuccess(calculate(20.0, 0.1, conversion))

        assertHybridEquals(2.0, result.theoreticalValueInUnderlyingCurrency)
        assertHybridEquals(2.0, result.theoreticalProductValue)
        assertEquals(currencyCode("USD"), result.underlyingCurrency)
        assertEquals(currencyCode("USD"), result.productCurrency)
        assertEquals(conversion, result.currencyConversion)
    }

    @Test
    fun usdToEurDividesTwoPointTwoUsdByRateOnePointOne() {
        val conversion = crossCurrency("USD", "EUR", 1.1)
        val result = assertSuccess(calculate(22.0, 0.1, conversion))

        assertHybridEquals(2.2, result.theoreticalValueInUnderlyingCurrency)
        assertHybridEquals(2.0, result.theoreticalProductValue)
        assertEquals(currencyCode("USD"), result.underlyingCurrency)
        assertEquals(currencyCode("EUR"), result.productCurrency)
    }

    @Test
    fun eurToUsdDividesFourEurByRateZeroPointEight() {
        val conversion = crossCurrency("EUR", "USD", 0.8)
        val result = assertSuccess(calculate(20.0, 0.2, conversion))

        assertHybridEquals(4.0, result.theoreticalValueInUnderlyingCurrency)
        assertHybridEquals(5.0, result.theoreticalProductValue)
        assertEquals(currencyCode("USD"), result.productCurrency)
    }

    @Test
    fun ratiosZeroPointZeroOneAndZeroPointZeroZeroOneAreAppliedExactlyOnce() {
        val conversion = CurrencyConversion.SameCurrency(currencyCode("USD"))
        val ratioZeroPointZeroOne = assertSuccess(calculate(20.0, 0.01, conversion))
        val ratioZeroPointZeroZeroOne = assertSuccess(calculate(20.0, 0.001, conversion))

        assertHybridEquals(0.2, ratioZeroPointZeroOne.theoreticalProductValue)
        assertHybridEquals(0.02, ratioZeroPointZeroZeroOne.theoreticalProductValue)
        assertEquals(0.01, ratioZeroPointZeroOne.ratio, 0.0)
        assertEquals(0.001, ratioZeroPointZeroZeroOne.ratio, 0.0)
    }

    @Test
    fun invalidDirectInputsAndDerivedValuesReturnStructuredErrors() {
        val conversion = CurrencyConversion.SameCurrency(currencyCode("EUR"))
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { distance ->
                assertFailure(
                    calculate(distance, 0.1, conversion),
                    TheoreticalProductValueCalculationError.INVALID_KNOCKOUT_DISTANCE
                )
            }
        listOf(0.0, -0.1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { ratio ->
                assertFailure(
                    calculate(20.0, ratio, conversion),
                    TheoreticalProductValueCalculationError.INVALID_RATIO
                )
            }
        assertFailure(
            calculate(Double.MAX_VALUE, 2.0, conversion),
            TheoreticalProductValueCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
        )
        assertFailure(
            calculate(Double.MIN_VALUE, 0.5, conversion),
            TheoreticalProductValueCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
        )
    }

    @Test
    fun verySmallPositiveValueRemainsUnroundedDeterministicAndDependencyFree() {
        val conversion = CurrencyConversion.SameCurrency(currencyCode("EUR"))
        val first = calculate(2.0, 0.0001, conversion)
        val repeated = calculate(2.0, 0.0001, conversion)
        val result = assertSuccess(first)

        assertHybridEquals(0.0002, result.theoreticalProductValue)
        assertTrue(result.theoreticalProductValue > 0.0)
        assertTrue(abs(result.theoreticalProductValue - 0.0) > TOLERANCE_ABSOLUTE)
        assertEquals(first, repeated)
        assertEquals(CurrencyConversion.SameCurrency(currencyCode("EUR")), conversion)

        val calculateMethod = TheoreticalProductValueCalculator::class.java.declaredMethods
            .single { method -> method.name == "calculate" && Modifier.isPublic(method.modifiers) }
        assertFalse(
            calculateMethod.parameterTypes.any { type ->
                type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java
            }
        )
        val publicTypeNames = buildSet {
            add(calculateMethod.returnType.name)
            calculateMethod.parameterTypes.forEach { add(it.name) }
            TheoreticalProductValueCalculationResult::class.java.declaredClasses
                .flatMap { it.declaredFields.asList() }
                .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
                .forEach { add(it.type.name) }
        }
        val forbiddenFragments = listOf(
            "android",
            "compose",
            "repository",
            "marketdata",
            "coroutine",
            "clock",
            "instant"
        )
        assertTrue(
            publicTypeNames.none { typeName ->
                forbiddenFragments.any { fragment ->
                    typeName.contains(fragment, ignoreCase = true)
                }
            }
        )
    }

    private fun calculate(
        knockoutDistanceAbsolute: Double,
        ratio: Double,
        currencyConversion: CurrencyConversion
    ) = TheoreticalProductValueCalculator.calculate(
        knockoutDistanceAbsolute,
        ratio,
        currencyConversion
    )

    private fun assertSuccess(
        result: TheoreticalProductValueCalculationResult
    ): TheoreticalProductValueCalculationResult.Success {
        assertTrue(result is TheoreticalProductValueCalculationResult.Success)
        return result as TheoreticalProductValueCalculationResult.Success
    }

    private fun assertFailure(
        result: TheoreticalProductValueCalculationResult,
        expected: TheoreticalProductValueCalculationError
    ) {
        assertEquals(TheoreticalProductValueCalculationResult.Failure(expected), result)
    }

    private fun assertHybridEquals(expected: Double, actual: Double) {
        val tolerance = max(TOLERANCE_ABSOLUTE, abs(expected) * TOLERANCE_RELATIVE)
        assertTrue(
            "Expected $expected, actual $actual, tolerance $tolerance",
            abs(actual - expected) <= tolerance
        )
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
        const val TOLERANCE_ABSOLUTE = 1e-12
        const val TOLERANCE_RELATIVE = 1e-12
    }
}
