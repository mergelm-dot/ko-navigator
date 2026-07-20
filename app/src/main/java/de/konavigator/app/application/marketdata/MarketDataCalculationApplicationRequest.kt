package de.konavigator.app.application.marketdata

import de.konavigator.app.domain.availability.MarketDataCalculationType

/**
 * Application-Auftrag für genau ein Produkt und einen CalculationType.
 *
 * Die Produkt-ISIN wird exakt weitergereicht. Der Bewertungszeitpunkt wird
 * explizit als UTC Epoch Milliseconds übergeben; der Request liest keine
 * Systemzeit. Policy-Konfiguration wird nicht aus UI-Eingaben übernommen.
 */
data class MarketDataCalculationApplicationRequest(
    val productIsin: String,
    val calculationType: MarketDataCalculationType,
    val evaluationTimeEpochMillis: Long
)
