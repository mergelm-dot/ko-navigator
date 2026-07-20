package de.konavigator.app.domain.source

/**
 * Explizite Regelkonfiguration für [MarketDataSourcePolicy].
 *
 * Es bestehen keine Default-Regeln. Eine leere Konfiguration ist zulässig und
 * blockiert alle Quellen. Exakt doppelte `sourceId`-Schlüssel sind unzulässig;
 * Regeln werden weder normalisiert noch stillschweigend zusammengeführt. Eine
 * spätere externe Konfiguration muss an der Application-Grenze validiert und
 * auf diesen Domain-Typ gemappt werden.
 */
data class MarketDataSourcePolicyConfig(
    val rules: List<MarketDataSourceRule>
) {
    init {
        require(rules.map { it.sourceId }.distinct().size == rules.size) {
            "rules must contain unique sourceId values"
        }
    }
}
