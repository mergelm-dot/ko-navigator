package de.konavigator.app.domain.currency

/**
 * Providerneutrale, gerichtete FX-Quote.
 *
 * Die Rate bezeichnet Einheiten der Basiswertwährung je einer Einheit
 * Produktwährung. Sie wird weder invertiert noch implizit normalisiert.
 */
data class FxRateQuote(
    val underlyingCurrency: CurrencyCode,
    val productCurrency: CurrencyCode,
    val underlyingCurrencyPerProductCurrencyRate: Double,
    val sourceId: String,
    val observedAtEpochMillis: Long
)
