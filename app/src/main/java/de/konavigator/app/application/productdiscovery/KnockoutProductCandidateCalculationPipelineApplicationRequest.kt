package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.model.TradeDirection

/** Providerneutraler Auftrag fuer die fruehe Produkt- und Marktdatenpipeline. */
data class KnockoutProductCandidateCalculationPipelineApplicationRequest(
    val underlyingId: String,
    val direction: TradeDirection,
    val brokerId: String,
    val enabledIssuerIds: Set<String>,
    val calculationType: MarketDataCalculationType,
    val evaluationTimeEpochMillis: Long
)
