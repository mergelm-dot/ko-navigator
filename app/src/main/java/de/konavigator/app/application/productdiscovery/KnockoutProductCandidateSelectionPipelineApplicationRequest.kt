package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.model.TradeDirection

/** Providerneutraler Gesamtauftrag von der Produktsuche bis zur technischen Auswahl. */
data class KnockoutProductCandidateSelectionPipelineApplicationRequest(
    val underlyingId: String,
    val direction: TradeDirection,
    val brokerId: String,
    val enabledIssuerIds: Set<String>,
    val calculationType: MarketDataCalculationType,
    val evaluationTimeEpochMillis: Long,
    val maxFxAgeMillis: Long,
    val underlyingPrice: Double,
    val plannedEntryPrice: Double,
    val targetLeverage: Double,
    val maxRelativeLeverageDeviationPercent: Double,
    val maxBarrierDeviationPercentOfPlannedEntry: Double
)
