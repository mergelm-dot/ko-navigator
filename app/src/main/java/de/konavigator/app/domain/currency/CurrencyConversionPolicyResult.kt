package de.konavigator.app.domain.currency

sealed interface CurrencyConversionPolicyResult {
    data class Approved(
        val conversion: CurrencyConversion.CrossCurrency,
        val sourceId: String,
        val observedAtEpochMillis: Long
    ) : CurrencyConversionPolicyResult

    data class Blocked(
        val error: CurrencyConversionPolicyError
    ) : CurrencyConversionPolicyResult
}

enum class CurrencyConversionPolicyError {
    IDENTICAL_CURRENCIES,
    CURRENCY_PAIR_MISMATCH,
    INVALID_SOURCE,
    INVALID_OBSERVED_AT,
    QUOTE_FROM_FUTURE,
    INVALID_MAX_FX_AGE,
    FX_QUOTE_TOO_OLD,
    INVALID_EXCHANGE_RATE
}
