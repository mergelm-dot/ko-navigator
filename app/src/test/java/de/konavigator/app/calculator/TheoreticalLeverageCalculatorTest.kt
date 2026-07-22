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

class TheoreticalLeverageCalculatorTest {

    @Test
    fun longLikeSameCurrencyExampleReturnsLeverageFive() {
        val result = success(calculate(100.0, 0.1, sameCurrency("EUR"), 2.0))

        assertHybridEquals(10.0, result.underlyingExposureInProductCurrency)
        assertHybridEquals(5.0, result.calculatedTheoreticalLeverageAtEntry)
        assertEquals(currencyCode("EUR"), result.productCurrency)
    }

    @Test
    fun shortLikeSameCurrencyExampleReturnsLeverageFive() {
        val result = success(calculate(100.0, 0.1, sameCurrency("USD"), 2.0))

        assertHybridEquals(10.0, result.underlyingExposureInProductCurrency)
        assertHybridEquals(5.0, result.calculatedTheoreticalLeverageAtEntry)
        assertEquals(currencyCode("USD"), result.productCurrency)
    }

    @Test
    fun usdToEurReturnsLeverageFive() {
        val result = success(
            calculate(
                plannedEntryPrice = 110.0,
                ratio = 0.1,
                currencyConversion = crossCurrency("USD", "EUR", 1.1),
                theoreticalProductValue = 2.0
            )
        )

        assertHybridEquals(10.0, result.underlyingExposureInProductCurrency)
        assertHybridEquals(5.0, result.calculatedTheoreticalLeverageAtEntry)
        assertEquals(currencyCode("EUR"), result.productCurrency)
    }

    @Test
    fun eurToUsdReturnsLeverageFive() {
        val result = success(
            calculate(
                plannedEntryPrice = 100.0,
                ratio = 0.1,
                currencyConversion = crossCurrency("EUR", "USD", 0.8),
                theoreticalProductValue = 2.5
            )
        )

        assertHybridEquals(12.5, result.underlyingExposureInProductCurrency)
        assertHybridEquals(5.0, result.calculatedTheoreticalLeverageAtEntry)
        assertEquals(currencyCode("USD"), result.productCurrency)
    }

    @Test
    fun ratiosZeroPointOneZeroPointZeroOneAndZeroPointZeroZeroOneCancel() {
        listOf(0.1, 0.01, 0.001).forEach { ratio ->
            val exposure = 100.0 * ratio
            val result = success(
                calculate(100.0, ratio, sameCurrency("EUR"), exposure / 5.0)
            )

            assertHybridEquals(exposure, result.underlyingExposureInProductCurrency)
            assertHybridEquals(5.0, result.calculatedTheoreticalLeverageAtEntry)
        }
    }

    @Test
    fun fxRatesZeroPointEightOneAndOnePointOneCancel() {
        listOf(0.8, 1.0, 1.1).forEach { rate ->
            val exposure = 100.0 * 0.1 / rate
            val result = success(
                calculate(
                    100.0,
                    0.1,
                    crossCurrency("USD", "EUR", rate),
                    exposure / 5.0
                )
            )

            assertHybridEquals(exposure, result.underlyingExposureInProductCurrency)
            assertHybridEquals(5.0, result.calculatedTheoreticalLeverageAtEntry)
        }
    }

    @Test
    fun invalidPlannedEntryPricesAreRejected() {
        invalidNumbers().forEach { plannedEntryPrice ->
            assertFailure(
                calculate(plannedEntryPrice, 0.1, sameCurrency("EUR"), 2.0),
                TheoreticalLeverageCalculationError.INVALID_PLANNED_ENTRY_PRICE
            )
        }
    }

    @Test
    fun invalidRatiosAreRejected() {
        invalidNumbers().forEach { ratio ->
            assertFailure(
                calculate(100.0, ratio, sameCurrency("EUR"), 2.0),
                TheoreticalLeverageCalculationError.INVALID_RATIO
            )
        }
    }

    @Test
    fun invalidTheoreticalProductValuesAreRejected() {
        invalidNumbers().forEach { theoreticalProductValue ->
            assertFailure(
                calculate(100.0, 0.1, sameCurrency("EUR"), theoreticalProductValue),
                TheoreticalLeverageCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
            )
        }
    }

    @Test
    fun nonFiniteExposureIsRejected() {
        assertFailure(
            calculate(Double.MAX_VALUE, 2.0, sameCurrency("EUR"), 1.0),
            TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE
        )
    }

    @Test
    fun nonFiniteCalculatedLeverageIsRejected() {
        assertFailure(
            calculate(100.0, 0.1, sameCurrency("EUR"), Double.MIN_VALUE),
            TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE
        )
    }

    @Test
    fun calculatedLeverageAtOrBelowOneIsRejected() {
        listOf(10.0, 20.0).forEach { theoreticalProductValue ->
            assertFailure(
                calculate(100.0, 0.1, sameCurrency("EUR"), theoreticalProductValue),
                TheoreticalLeverageCalculationError.INVALID_CALCULATED_LEVERAGE
            )
        }
    }

    @Test
    fun verySmallPositiveUnroundedProductValueReturnsLeverageFive() {
        val result = success(
            calculate(
                plannedEntryPrice = 0.000001,
                ratio = 0.001,
                currencyConversion = sameCurrency("EUR"),
                theoreticalProductValue = 0.0000000002
            )
        )

        assertHybridEquals(0.000000001, result.underlyingExposureInProductCurrency)
        assertHybridEquals(5.0, result.calculatedTheoreticalLeverageAtEntry)
    }

    @Test
    fun identicalInputsAreDeterministic() {
        val conversion = crossCurrency("USD", "EUR", 1.1)
        val first = calculate(110.0, 0.1, conversion, 2.0)
        val repeated = calculate(110.0, 0.1, conversion, 2.0)

        assertEquals(first, repeated)
    }

    @Test
    fun contractHasExactErrorsAndNoForbiddenDependencies() {
        assertEquals(
            listOf(
                "INVALID_PLANNED_ENTRY_PRICE",
                "INVALID_RATIO",
                "INVALID_EXCHANGE_RATE",
                "INVALID_THEORETICAL_PRODUCT_VALUE",
                "INVALID_CALCULATED_LEVERAGE"
            ),
            TheoreticalLeverageCalculationError.entries.map { it.name }
        )

        val calculateMethod = TheoreticalLeverageCalculator::class.java.declaredMethods
            .single { it.name == "calculate" && Modifier.isPublic(it.modifiers) }
        val publicTypeNames = buildSet {
            add(calculateMethod.returnType.name)
            calculateMethod.parameterTypes.forEach { add(it.name) }
            TheoreticalLeverageCalculationResult::class.java.declaredClasses
                .flatMap { it.declaredFields.asList() }
                .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
                .forEach { add(it.type.name) }
        }
        val forbidden = listOf(
            "android",
            "compose",
            "repository",
            "marketdata",
            "coroutine",
            "clock",
            "instant"
        )

        assertFalse(
            publicTypeNames.any { typeName ->
                forbidden.any { typeName.contains(it, ignoreCase = true) }
            }
        )
    }

    private fun calculate(
        plannedEntryPrice: Double,
        ratio: Double,
        currencyConversion: CurrencyConversion,
        theoreticalProductValue: Double
    ) = TheoreticalLeverageCalculator.calculate(
        plannedEntryPrice,
        ratio,
        currencyConversion,
        theoreticalProductValue
    )

    private fun success(
        result: TheoreticalLeverageCalculationResult
    ): TheoreticalLeverageCalculationResult.Success {
        assertTrue(result is TheoreticalLeverageCalculationResult.Success)
        return result as TheoreticalLeverageCalculationResult.Success
    }

    private fun assertFailure(
        result: TheoreticalLeverageCalculationResult,
        expected: TheoreticalLeverageCalculationError
    ) {
        assertEquals(TheoreticalLeverageCalculationResult.Failure(expected), result)
    }

    private fun assertHybridEquals(expected: Double, actual: Double) {
        val tolerance = max(ABSOLUTE_TOLERANCE, abs(expected) * RELATIVE_TOLERANCE)
        assertTrue(
            "Expected $expected, actual $actual, tolerance $tolerance",
            abs(actual - expected) <= tolerance
        )
    }

    private fun invalidNumbers() = listOf(
        0.0,
        -1.0,
        Double.NaN,
        Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY
    )

    private fun sameCurrency(currency: String) =
        CurrencyConversion.SameCurrency(currencyCode(currency))

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
