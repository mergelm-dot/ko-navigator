package de.konavigator.app.domain.validation

import de.konavigator.app.domain.model.KnockoutProductMarketData

/**
 * Prüft die allgemeinen Version-1-Regeln von [KnockoutProductMarketData].
 *
 * Vollständige Quotes, Bid-only-Quotes, Ask-only-Quotes und vollständig leere
 * Quotes sind zulässig. Alle unabhängigen Fehler werden vollständig in stabiler
 * Reihenfolge gesammelt. Der Validator normalisiert keine Eingaben, erzeugt
 * keine UI-Texte und prüft weder Aktualität noch die Kompatibilität mit einer
 * Produktspezifikation. Er ist nicht an die Berechnungsengine angebunden.
 *
 * Eine leere Fehlerliste bedeutet nur, dass der Marktdatensatz intern nach den
 * Version-1-Regeln konsistent ist. Sie bedeutet nicht, dass Bid, Ask, Spread
 * oder andere Berechnungen verfügbar beziehungsweise freigegeben sind.
 */
object KnockoutProductMarketDataValidator {

    private val currencyCodePattern = Regex("[A-Z]{3}")

    fun validate(
        marketData: KnockoutProductMarketData
    ): List<KnockoutProductMarketDataValidationError> {
        val errors = mutableListOf<KnockoutProductMarketDataValidationError>()

        if (marketData.productIsin.isBlank()) {
            errors += KnockoutProductMarketDataValidationError.MISSING_PRODUCT_ISIN
        }

        if (marketData.sourceId.isBlank()) {
            errors += KnockoutProductMarketDataValidationError.MISSING_SOURCE_ID
        }

        if (!currencyCodePattern.matches(marketData.currency)) {
            errors += KnockoutProductMarketDataValidationError.INVALID_CURRENCY
        }

        val bidIsValid = marketData.bid?.let { it.isFinite() && it >= 0.0 } ?: true
        if (!bidIsValid) {
            errors += KnockoutProductMarketDataValidationError.INVALID_BID
        }

        when {
            marketData.bid != null && marketData.bidTimestampEpochMillis == null ->
                errors += KnockoutProductMarketDataValidationError.MISSING_BID_TIMESTAMP
            marketData.bid == null && marketData.bidTimestampEpochMillis != null ->
                errors += KnockoutProductMarketDataValidationError.ORPHAN_BID_TIMESTAMP
        }

        val askIsValid = marketData.ask?.let { it.isFinite() && it > 0.0 } ?: true
        if (!askIsValid) {
            errors += KnockoutProductMarketDataValidationError.INVALID_ASK
        }

        when {
            marketData.ask != null && marketData.askTimestampEpochMillis == null ->
                errors += KnockoutProductMarketDataValidationError.MISSING_ASK_TIMESTAMP
            marketData.ask == null && marketData.askTimestampEpochMillis != null ->
                errors += KnockoutProductMarketDataValidationError.ORPHAN_ASK_TIMESTAMP
        }

        if (
            marketData.bid != null &&
            marketData.ask != null &&
            bidIsValid &&
            askIsValid &&
            marketData.bid > marketData.ask
        ) {
            errors += KnockoutProductMarketDataValidationError.BID_ABOVE_ASK
        }

        return errors
    }
}
