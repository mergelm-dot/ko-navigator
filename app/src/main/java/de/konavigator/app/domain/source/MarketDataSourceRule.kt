package de.konavigator.app.domain.source

import de.konavigator.app.domain.availability.MarketDataCalculationType

/**
 * Konfiguriert einen exakten Quellenbezeichner und seine ausdrücklich
 * unterstützten Berechnungstypen.
 *
 * Es bestehen keine Defaults. [sourceId] wird weder normalisiert noch
 * inhaltlich interpretiert. Ein leeres [supportedCalculationTypes]-Set ist
 * zulässig und gibt keinen Berechnungstyp frei. Eine spätere externe
 * Konfiguration muss an der Application-Grenze validiert und auf diesen
 * Domain-Typ gemappt werden.
 */
data class MarketDataSourceRule(
    val sourceId: String,
    val supportedCalculationTypes: Set<MarketDataCalculationType>
) {
    init {
        require(sourceId.isNotBlank()) { "sourceId must not be blank" }
    }
}
