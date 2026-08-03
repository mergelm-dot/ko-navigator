package de.konavigator.app.application.productdiscovery

/**
 * Reiner providerneutraler Ergebnisvertrag für die Quellenfreigabe. Er ist
 * keine Kauf- oder Verkaufsempfehlung und trifft keine Preis-, Spread-, Hebel-,
 * Qualitäts-, Ranking-, Hauptprodukt-, Alternativen- oder UI-Entscheidung.
 */
sealed interface KnockoutProductCandidateSourceGateResult {

    /** Mindestens ein Kandidat ist ausschließlich für die folgende Berechnungsstufe freigegeben. */
    data class SourceAllowedCandidates(
        val allowedCandidates: List<KnockoutProductCandidateWithSourceEvaluation>,
        val blockedCandidates: List<KnockoutProductCandidateWithSourceEvaluation>
    ) : KnockoutProductCandidateSourceGateResult

    /** Nichtleere Eingabe ohne freigegebene Quelle; Blockierungsgründe bleiben erhalten. */
    data class NoSourceAllowedCandidates(
        val blockedCandidates: List<KnockoutProductCandidateWithSourceEvaluation>
    ) : KnockoutProductCandidateSourceGateResult

    /** Leere Eingabe ohne künstliche Kandidaten oder Source-Ergebnisse. */
    data object NoInputCandidates : KnockoutProductCandidateSourceGateResult
}
