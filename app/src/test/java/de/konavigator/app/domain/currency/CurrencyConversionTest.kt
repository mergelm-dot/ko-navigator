package de.konavigator.app.domain.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConversionTest {

    @Test
    fun sameCurrencyUsesTheSameCurrencyOnBothSidesWithoutNumericRate() {
        val eur = currencyCode("EUR")
        val conversion = CurrencyConversion.SameCurrency(eur)

        assertEquals(eur, conversion.underlyingCurrency)
        assertEquals(eur, conversion.productCurrency)
        assertTrue(
            CurrencyConversion.SameCurrency::class.java.declaredFields.none { field ->
                field.type == java.lang.Double.TYPE || field.type == java.lang.Double::class.java
            }
        )
    }

    @Test
    fun crossCurrencyAcceptsDifferentCurrenciesAndPositiveFiniteRate() {
        val usd = currencyCode("USD")
        val eur = currencyCode("EUR")
        val conversion = crossCurrency(usd, eur, 1.1)

        assertEquals(usd, conversion.underlyingCurrency)
        assertEquals(eur, conversion.productCurrency)
        assertEquals(1.1, conversion.underlyingCurrencyPerProductCurrencyRate, 0.0)
    }

    @Test
    fun crossCurrencyRejectsZeroRate() {
        assertFailure(0.0, CurrencyConversionCreationError.INVALID_EXCHANGE_RATE)
    }

    @Test
    fun crossCurrencyRejectsNegativeRate() {
        assertFailure(-1.1, CurrencyConversionCreationError.INVALID_EXCHANGE_RATE)
    }

    @Test
    fun crossCurrencyRejectsNanAndInfiniteRates() {
        listOf(
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        ).forEach { rate ->
            assertFailure(rate, CurrencyConversionCreationError.INVALID_EXCHANGE_RATE)
        }
    }

    @Test
    fun crossCurrencyRejectsIdenticalCurrencies() {
        val eur = currencyCode("EUR")

        assertEquals(
            CurrencyConversionCreationResult.Failure(
                CurrencyConversionCreationError.IDENTICAL_CURRENCIES
            ),
            CurrencyConversion.CrossCurrency.create(eur, eur, 1.0)
        )
    }

    private fun assertFailure(
        rate: Double,
        expected: CurrencyConversionCreationError
    ) {
        assertEquals(
            CurrencyConversionCreationResult.Failure(expected),
            CurrencyConversion.CrossCurrency.create(
                underlyingCurrency = currencyCode("USD"),
                productCurrency = currencyCode("EUR"),
                underlyingCurrencyPerProductCurrencyRate = rate
            )
        )
    }

    private fun crossCurrency(
        underlyingCurrency: CurrencyCode,
        productCurrency: CurrencyCode,
        rate: Double
    ): CurrencyConversion.CrossCurrency =
        when (
            val result = CurrencyConversion.CrossCurrency.create(
                underlyingCurrency,
                productCurrency,
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
}
