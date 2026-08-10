package de.konavigator.app.domain.currency

/** Explizite Eingaben für die Freigabe einer Cross-Currency-Umrechnung. */
data class CurrencyConversionPolicyInput(
    val requestedUnderlyingCurrency: CurrencyCode,
    val requestedProductCurrency: CurrencyCode,
    val fxQuote: FxRateQuote,
    val evaluationTimeEpochMillis: Long,
    val maxFxAgeMillis: Long
)
