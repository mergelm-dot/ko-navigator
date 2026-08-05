package de.konavigator.app.application.productdiscovery

/**
 * Typisierter Ergebnisvertrag des Existing-Entry-Calculation-Gates. Eine
 * erfolgreiche Berechnung ist nur fuer die spaetere Eignungsbewertung
 * freigegeben; sie ist weder Ranking noch Produktauswahl oder Orderfreigabe.
 */
sealed interface KnockoutProductCandidateExistingEntryCalculationGateResult {
    data class SuccessfulExistingEntryCalculationCandidates(
        val successfulCandidates: List<KnockoutProductCandidateWithExistingEntryCalculation>,
        val failedCandidates: List<KnockoutProductCandidateWithExistingEntryCalculation>
    ) : KnockoutProductCandidateExistingEntryCalculationGateResult

    data class NoSuccessfulExistingEntryCalculationCandidates(
        val failedCandidates: List<KnockoutProductCandidateWithExistingEntryCalculation>
    ) : KnockoutProductCandidateExistingEntryCalculationGateResult

    data object NoInputCandidates : KnockoutProductCandidateExistingEntryCalculationGateResult
}
