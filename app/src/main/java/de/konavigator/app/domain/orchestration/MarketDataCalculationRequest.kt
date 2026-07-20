package de.konavigator.app.domain.orchestration

import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification

/**
 * Unveränderlicher Auftrag für genau eine Marktdatenberechnung.
 *
 * Der Bewertungszeitpunkt wird ausdrücklich als UTC Epoch Milliseconds
 * übergeben. Der Auftrag liest keine Systemzeit, validiert und normalisiert
 * seine Werte nicht und verändert die enthaltenen Domainmodelle nicht.
 */
data class MarketDataCalculationRequest(
    val calculationType: MarketDataCalculationType,
    val specification: KnockoutProductSpecification,
    val marketData: KnockoutProductMarketData,
    val evaluationTimeEpochMillis: Long
)
