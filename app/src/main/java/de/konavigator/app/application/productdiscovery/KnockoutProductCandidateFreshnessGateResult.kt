package de.konavigator.app.application.productdiscovery

/** Providerneutraler Ergebnisvertrag ohne Quellen-, Berechnungs-, Ranking- oder UI-Entscheidung. */
sealed interface KnockoutProductCandidateFreshnessGateResult {
    /** Mindestens ein Fresh-Kandidat ist ausschließlich für die folgende Quellenprüfung freigegeben. */
    data class FreshCandidates(
        val freshCandidates: List<KnockoutProductCandidateWithFreshness>,
        val notFreshCandidates: List<KnockoutProductCandidateWithFreshness>
    ) : KnockoutProductCandidateFreshnessGateResult

    /** Nichtleere Eingabe mit ausschließlich NotFresh-Kandidaten samt unveränderten Fehlern. */
    data class NoFreshCandidates(
        val notFreshCandidates: List<KnockoutProductCandidateWithFreshness>
    ) : KnockoutProductCandidateFreshnessGateResult

    /** Leere Eingabe ohne künstliche Kandidaten oder Freshness-Ergebnisse. */
    data object NoInputCandidates : KnockoutProductCandidateFreshnessGateResult
}
