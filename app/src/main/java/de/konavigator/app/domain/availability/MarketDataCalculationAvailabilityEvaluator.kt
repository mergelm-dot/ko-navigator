package de.konavigator.app.domain.availability

import de.konavigator.app.domain.model.KnockoutProductMarketData

/**
 * Prüft die strukturelle Verfügbarkeit genau einer Marktdatenberechnung.
 *
 * Vor jedem Aufruf müssen Produktspezifikation und Marktdaten intern erfolgreich
 * validiert sowie durch den `KnockoutProductMarketDataCompatibilityValidator`
 * als kompatibel bestätigt worden sein. Der Evaluator ruft diese Validatoren
 * nicht selbst auf und wiederholt weder Preis- noch Zeitstempelprüfungen.
 *
 * [MarketDataCalculationType.PURCHASE_PRICE] benötigt ausschließlich Ask,
 * [MarketDataCalculationType.SALE_PRICE] ausschließlich einen positiven Bid.
 * [MarketDataCalculationType.SPREAD] und [MarketDataCalculationType.MID]
 * benötigen beide Quote-Seiten und erlauben Bid `0.0`. Mid bleibt ein nicht
 * handelbarer Referenzwert.
 *
 * Die Komponente führt keine Berechnung aus, liest keine Systemzeit und bewertet
 * weder Aktualität noch Quellenqualität. Sie erzeugt keine UI-Texte und besitzt
 * keine Android-, Compose-, Engine-, UI- oder Repository-Abhängigkeit. Ein
 * [MarketDataCalculationAvailabilityResult.StructurallyAvailable] ist keine
 * vollständige fachliche Berechnungsfreigabe.
 */
object MarketDataCalculationAvailabilityEvaluator {

    fun evaluate(
        calculationType: MarketDataCalculationType,
        marketData: KnockoutProductMarketData
    ): MarketDataCalculationAvailabilityResult {
        val errors = mutableListOf<MarketDataCalculationAvailabilityError>()

        when (calculationType) {
            MarketDataCalculationType.PURCHASE_PRICE -> {
                if (marketData.ask == null) {
                    errors += MarketDataCalculationAvailabilityError.MISSING_ASK
                }
            }

            MarketDataCalculationType.SALE_PRICE -> {
                when (marketData.bid) {
                    null -> errors += MarketDataCalculationAvailabilityError.MISSING_BID
                    0.0 -> errors +=
                        MarketDataCalculationAvailabilityError.BID_NOT_POSITIVE_FOR_SALE
                }
            }

            MarketDataCalculationType.SPREAD,
            MarketDataCalculationType.MID -> {
                if (marketData.bid == null) {
                    errors += MarketDataCalculationAvailabilityError.MISSING_BID
                }
                if (marketData.ask == null) {
                    errors += MarketDataCalculationAvailabilityError.MISSING_ASK
                }
            }
        }

        return if (errors.isEmpty()) {
            MarketDataCalculationAvailabilityResult.StructurallyAvailable
        } else {
            MarketDataCalculationAvailabilityResult.StructurallyUnavailable(errors)
        }
    }
}
