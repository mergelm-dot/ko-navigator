package de.konavigator.app.domain.freshness

/**
 * Explizite, unveränderliche Grenzwerte für die zeitliche Marktdatenprüfung.
 *
 * Es bestehen keine Produktionsdefaults. Alle Werte müssen größer oder gleich
 * `0` sein; sowohl `0` als auch [Long.MAX_VALUE] sind zulässig. Eine spätere
 * externe Konfiguration muss an der Systemgrenze validiert werden, bevor sie
 * diesen Domain-Typ erzeugt.
 */
data class MarketDataFreshnessThresholds(
    val maxBidAgeMillis: Long,
    val maxAskAgeMillis: Long,
    val maxBidAskDifferenceMillis: Long,
    val allowedFutureSkewMillis: Long
) {
    init {
        require(maxBidAgeMillis >= 0) { "maxBidAgeMillis must be non-negative" }
        require(maxAskAgeMillis >= 0) { "maxAskAgeMillis must be non-negative" }
        require(maxBidAskDifferenceMillis >= 0) {
            "maxBidAskDifferenceMillis must be non-negative"
        }
        require(allowedFutureSkewMillis >= 0) {
            "allowedFutureSkewMillis must be non-negative"
        }
    }
}
