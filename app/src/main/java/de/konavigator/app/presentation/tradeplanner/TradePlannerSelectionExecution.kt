package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationRequest
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationResult
import de.konavigator.app.domain.availability.MarketDataCalculationType

/** Explizit injizierte, unveränderte Ausführungsparameter des Selection-Pfads. */
data class TradePlannerSelectionExecutionSettings(
    val calculationType: MarketDataCalculationType,
    val maxFxAgeMillis: Long,
    val maxRelativeLeverageDeviationPercent: Double,
    val maxBarrierDeviationPercentOfPlannedEntry: Double
)

/** Liefert genau den Bewertungszeitpunkt eines tatsächlich gestarteten Pipeline-Laufs. */
fun interface TradePlannerSelectionEvaluationTimeProvider {
    fun evaluationTimeEpochMillis(): Long
}

/** Kleine Presentation-Grenze zum suspendierenden 21f-Application-Service. */
fun interface TradePlannerSelectionExecutor {
    suspend fun execute(
        request: KnockoutProductCandidateSelectionPipelineApplicationRequest
    ): KnockoutProductCandidateSelectionPipelineApplicationResult
}
