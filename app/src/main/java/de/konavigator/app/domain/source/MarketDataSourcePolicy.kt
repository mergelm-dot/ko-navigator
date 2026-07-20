package de.konavigator.app.domain.source

import de.konavigator.app.domain.availability.MarketDataCalculationType

/**
 * Reine konfigurationsbasierte Domainpolicy für genau einen Berechnungstyp und
 * einen `sourceId` je Aufruf.
 *
 * Quellenbezeichner werden exakt und case-sensitiv verglichen, nicht
 * normalisiert oder interpretiert. Nicht konfigurierte Quellen werden
 * blockiert; konfigurierte Quellen erlauben ausschließlich explizit enthaltene
 * Berechnungstypen. Es gibt keine automatische Capability-Ableitung.
 *
 * Die Policy erstellt beim Erzeugen einen defensiven Snapshot aller Regeln und
 * Capability-Sets. Sie prüft weder Freshness, Availability, Preise noch
 * Zeitstempel und enthält keine Netzwerk- oder Provider-Mapping-Logik. Sie
 * erzeugt keine UI-Texte, besitzt keine Android-/Compose-Abhängigkeit und ist
 * nicht an Engine, UI oder Repository angebunden. Ein
 * [MarketDataSourceResult.Allowed] ist keine vollständige
 * Berechnungsfreigabe.
 */
class MarketDataSourcePolicy(
    config: MarketDataSourcePolicyConfig
) {
    private val supportedCalculationTypesBySourceId:
        Map<String, Set<MarketDataCalculationType>> = config.rules.associate { rule ->
            rule.sourceId to rule.supportedCalculationTypes.toSet()
        }

    fun evaluate(
        calculationType: MarketDataCalculationType,
        sourceId: String
    ): MarketDataSourceResult {
        val supportedCalculationTypes = supportedCalculationTypesBySourceId[sourceId]
            ?: return MarketDataSourceResult.Blocked(
                MarketDataSourceError.SOURCE_NOT_CONFIGURED
            )

        return if (calculationType in supportedCalculationTypes) {
            MarketDataSourceResult.Allowed
        } else {
            MarketDataSourceResult.Blocked(
                MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED
            )
        }
    }
}
