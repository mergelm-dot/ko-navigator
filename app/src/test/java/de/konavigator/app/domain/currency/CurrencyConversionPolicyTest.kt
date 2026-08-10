package de.konavigator.app.domain.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConversionPolicyTest {
    private val usd = currencyCode("USD")
    private val eur = currencyCode("EUR")
    private val policy = CurrencyConversionPolicy()

    @Test
    fun validCrossCurrencyQuoteIsApprovedWithExactRateAndMetadata() {
        val result = approved(evaluate())

        assertEquals(usd, result.conversion.underlyingCurrency)
        assertEquals(eur, result.conversion.productCurrency)
        assertEquals(
            1.1,
            result.conversion.underlyingCurrencyPerProductCurrencyRate,
            0.0
        )
        assertEquals("SYNTHETIC_FX", result.sourceId)
        assertEquals(900L, result.observedAtEpochMillis)
    }

    @Test
    fun mismatchingCurrencyPairIsBlocked() {
        assertBlocked(
            CurrencyConversionPolicyError.CURRENCY_PAIR_MISMATCH,
            evaluate(quote = quote(productCurrency = currencyCode("GBP")))
        )
    }

    @Test
    fun reversedCurrencyPairIsNotInverted() {
        val reversedQuote = quote(
            underlyingCurrency = eur,
            productCurrency = usd,
            rate = 0.91
        )

        assertBlocked(
            CurrencyConversionPolicyError.CURRENCY_PAIR_MISMATCH,
            evaluate(quote = reversedQuote)
        )
        assertEquals(0.91, reversedQuote.underlyingCurrencyPerProductCurrencyRate, 0.0)
    }

    @Test
    fun blankSourceIsBlockedWithoutNormalizationOrDefault() {
        listOf("", " ", "\t").forEach { sourceId ->
            assertBlocked(
                CurrencyConversionPolicyError.INVALID_SOURCE,
                evaluate(quote = quote(sourceId = sourceId))
            )
        }
    }

    @Test
    fun negativeObservedAtIsBlocked() {
        assertBlocked(
            CurrencyConversionPolicyError.INVALID_OBSERVED_AT,
            evaluate(quote = quote(observedAtEpochMillis = -1L))
        )
    }

    @Test
    fun quoteFromFutureIsBlocked() {
        assertBlocked(
            CurrencyConversionPolicyError.QUOTE_FROM_FUTURE,
            evaluate(quote = quote(observedAtEpochMillis = 1_001L))
        )
    }

    @Test
    fun quoteExactlyAtMaximumAgeIsApproved() {
        val result = evaluate(
            quote = quote(observedAtEpochMillis = 900L),
            evaluationTimeEpochMillis = 1_000L,
            maxFxAgeMillis = 100L
        )

        assertTrue(result is CurrencyConversionPolicyResult.Approved)
    }

    @Test
    fun quoteOneMillisecondOlderThanMaximumIsBlocked() {
        assertBlocked(
            CurrencyConversionPolicyError.FX_QUOTE_TOO_OLD,
            evaluate(
                quote = quote(observedAtEpochMillis = 899L),
                evaluationTimeEpochMillis = 1_000L,
                maxFxAgeMillis = 100L
            )
        )
    }

    @Test
    fun negativeMaximumAgeIsBlocked() {
        assertBlocked(
            CurrencyConversionPolicyError.INVALID_MAX_FX_AGE,
            evaluate(maxFxAgeMillis = -1L)
        )
    }

    @Test
    fun invalidRatesAreBlockedByExistingCurrencyConversionCreation() {
        listOf(
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            0.0,
            -1.1
        ).forEach { rate ->
            assertBlocked(
                CurrencyConversionPolicyError.INVALID_EXCHANGE_RATE,
                evaluate(quote = quote(rate = rate))
            )
        }
    }

    @Test
    fun identicalRequestedCurrenciesAreNotApprovedAsCrossCurrency() {
        assertBlocked(
            CurrencyConversionPolicyError.IDENTICAL_CURRENCIES,
            evaluate(
                requestedUnderlyingCurrency = eur,
                requestedProductCurrency = eur,
                quote = quote(underlyingCurrency = eur, productCurrency = eur)
            )
        )
    }

    @Test
    fun inputAndQuoteRemainUnchanged() {
        val quote = quote(sourceId = " exact source ", rate = 1.2345)
        val input = input(quote = quote)

        val result = approved(policy.evaluate(input))

        assertSame(quote, input.fxQuote)
        assertEquals(" exact source ", quote.sourceId)
        assertEquals(1.2345, quote.underlyingCurrencyPerProductCurrencyRate, 0.0)
        assertEquals(usd, input.requestedUnderlyingCurrency)
        assertEquals(eur, input.requestedProductCurrency)
        assertEquals(1_000L, input.evaluationTimeEpochMillis)
        assertEquals(100L, input.maxFxAgeMillis)
        assertEquals(" exact source ", result.sourceId)
    }

    private fun evaluate(
        requestedUnderlyingCurrency: CurrencyCode = usd,
        requestedProductCurrency: CurrencyCode = eur,
        quote: FxRateQuote = quote(),
        evaluationTimeEpochMillis: Long = 1_000L,
        maxFxAgeMillis: Long = 100L
    ): CurrencyConversionPolicyResult = policy.evaluate(
        input(
            requestedUnderlyingCurrency = requestedUnderlyingCurrency,
            requestedProductCurrency = requestedProductCurrency,
            quote = quote,
            evaluationTimeEpochMillis = evaluationTimeEpochMillis,
            maxFxAgeMillis = maxFxAgeMillis
        )
    )

    private fun input(
        requestedUnderlyingCurrency: CurrencyCode = usd,
        requestedProductCurrency: CurrencyCode = eur,
        quote: FxRateQuote = quote(),
        evaluationTimeEpochMillis: Long = 1_000L,
        maxFxAgeMillis: Long = 100L
    ) = CurrencyConversionPolicyInput(
        requestedUnderlyingCurrency = requestedUnderlyingCurrency,
        requestedProductCurrency = requestedProductCurrency,
        fxQuote = quote,
        evaluationTimeEpochMillis = evaluationTimeEpochMillis,
        maxFxAgeMillis = maxFxAgeMillis
    )

    private fun quote(
        underlyingCurrency: CurrencyCode = usd,
        productCurrency: CurrencyCode = eur,
        rate: Double = 1.1,
        sourceId: String = "SYNTHETIC_FX",
        observedAtEpochMillis: Long = 900L
    ) = FxRateQuote(
        underlyingCurrency = underlyingCurrency,
        productCurrency = productCurrency,
        underlyingCurrencyPerProductCurrencyRate = rate,
        sourceId = sourceId,
        observedAtEpochMillis = observedAtEpochMillis
    )

    private fun approved(
        result: CurrencyConversionPolicyResult
    ): CurrencyConversionPolicyResult.Approved {
        assertTrue(result is CurrencyConversionPolicyResult.Approved)
        return result as CurrencyConversionPolicyResult.Approved
    }

    private fun assertBlocked(
        expectedError: CurrencyConversionPolicyError,
        result: CurrencyConversionPolicyResult
    ) {
        assertEquals(CurrencyConversionPolicyResult.Blocked(expectedError), result)
    }

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid test currency: ${result.error}")
        }
}
