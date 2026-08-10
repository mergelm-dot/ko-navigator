package de.konavigator.app.domain.currency

/** Reine Freigabegrenze für eine explizite Cross-Currency-Quote. */
class CurrencyConversionPolicy {

    fun evaluate(
        input: CurrencyConversionPolicyInput
    ): CurrencyConversionPolicyResult {
        if (input.requestedUnderlyingCurrency == input.requestedProductCurrency) {
            return blocked(CurrencyConversionPolicyError.IDENTICAL_CURRENCIES)
        }

        val quote = input.fxQuote
        if (
            quote.underlyingCurrency != input.requestedUnderlyingCurrency ||
            quote.productCurrency != input.requestedProductCurrency
        ) {
            return blocked(CurrencyConversionPolicyError.CURRENCY_PAIR_MISMATCH)
        }

        if (quote.sourceId.isBlank()) {
            return blocked(CurrencyConversionPolicyError.INVALID_SOURCE)
        }

        if (quote.observedAtEpochMillis < 0L) {
            return blocked(CurrencyConversionPolicyError.INVALID_OBSERVED_AT)
        }

        if (quote.observedAtEpochMillis > input.evaluationTimeEpochMillis) {
            return blocked(CurrencyConversionPolicyError.QUOTE_FROM_FUTURE)
        }

        if (input.maxFxAgeMillis < 0L) {
            return blocked(CurrencyConversionPolicyError.INVALID_MAX_FX_AGE)
        }

        val fxAgeMillis = input.evaluationTimeEpochMillis - quote.observedAtEpochMillis
        if (fxAgeMillis > input.maxFxAgeMillis) {
            return blocked(CurrencyConversionPolicyError.FX_QUOTE_TOO_OLD)
        }

        return when (
            val conversionResult = CurrencyConversion.CrossCurrency.create(
                underlyingCurrency = quote.underlyingCurrency,
                productCurrency = quote.productCurrency,
                underlyingCurrencyPerProductCurrencyRate =
                    quote.underlyingCurrencyPerProductCurrencyRate
            )
        ) {
            is CurrencyConversionCreationResult.Success ->
                CurrencyConversionPolicyResult.Approved(
                    conversion = conversionResult.conversion,
                    sourceId = quote.sourceId,
                    observedAtEpochMillis = quote.observedAtEpochMillis
                )

            is CurrencyConversionCreationResult.Failure ->
                blocked(CurrencyConversionPolicyError.INVALID_EXCHANGE_RATE)
        }
    }

    private fun blocked(
        error: CurrencyConversionPolicyError
    ) = CurrencyConversionPolicyResult.Blocked(error)
}
