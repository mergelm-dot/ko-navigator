package de.konavigator.app.application.productdiscovery

/**
 * Reiner providerneutraler Ergebnisvertrag für die Berechnungsfreigabe. Er ist
 * keine Kauf- oder Verkaufsempfehlung und enthält keine Zielhebel-, Qualitäts-,
 * Ranking-, Hauptprodukt-, Alternativen- oder UI-Entscheidung.
 */
sealed interface KnockoutProductCandidateCalculationGateResult {
    /** Mindestens ein erfolgreich berechneter Kandidat ist für spätere Bewertungen freigegeben. */
    data class SuccessfulCalculationCandidates(
        val successfulCandidates: List<KnockoutProductCandidateWithCalculation>,
        val failedCandidates: List<KnockoutProductCandidateWithCalculation>
    ) : KnockoutProductCandidateCalculationGateResult

    /** Nichtleere Eingabe ohne erfolgreiche Berechnung; Calculator-Fehler bleiben erhalten. */
    data class NoSuccessfulCalculationCandidates(
        val failedCandidates: List<KnockoutProductCandidateWithCalculation>
    ) : KnockoutProductCandidateCalculationGateResult

    /** Leere Eingabe ohne künstliche Kandidaten oder CalculationOutcomes. */
    data object NoInputCandidates : KnockoutProductCandidateCalculationGateResult
}
