package de.konavigator.app.application.productdiscovery

/**
 * Typisierter Ergebnisvertrag der reinen Target-Deviation-Partitionierung.
 * Ein Success bestaetigt nur eine erfolgreiche mathematische Berechnung, nicht
 * Zieltreffer, Eignung, Ranking, Produktauswahl oder Orderfreigabe.
 */
sealed interface KnockoutProductCandidateTargetDeviationGateResult {
    data class SuccessfulTargetDeviationCandidates(
        val successfulCandidates: List<KnockoutProductCandidateWithTargetDeviation>,
        val failedCandidates: List<KnockoutProductCandidateWithTargetDeviation>
    ) : KnockoutProductCandidateTargetDeviationGateResult

    data class NoSuccessfulTargetDeviationCandidates(
        val failedCandidates: List<KnockoutProductCandidateWithTargetDeviation>
    ) : KnockoutProductCandidateTargetDeviationGateResult

    data object NoInputCandidates : KnockoutProductCandidateTargetDeviationGateResult
}
