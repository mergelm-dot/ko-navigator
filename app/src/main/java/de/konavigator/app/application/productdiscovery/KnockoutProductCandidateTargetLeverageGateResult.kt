package de.konavigator.app.application.productdiscovery

/**
 * Reiner providerneutraler Ergebnisvertrag für theoretische Zielhebel-Pläne.
 * Er bestätigt nur die Freigabe für den späteren Barrierenvergleich, nicht
 * reale Barrierenübereinstimmung, Zielhebel-Treffer, Ranking, Auswahl oder
 * Orderfreigabe.
 */
sealed interface KnockoutProductCandidateTargetLeverageGateResult {
    data class ValidTargetLeveragePlanCandidates(
        val validCandidates: List<KnockoutProductCandidateWithTargetLeveragePlan>,
        val invalidCandidates: List<KnockoutProductCandidateWithTargetLeveragePlan>
    ) : KnockoutProductCandidateTargetLeverageGateResult

    data class NoValidTargetLeveragePlanCandidates(
        val invalidCandidates: List<KnockoutProductCandidateWithTargetLeveragePlan>
    ) : KnockoutProductCandidateTargetLeverageGateResult

    data object NoInputCandidates : KnockoutProductCandidateTargetLeverageGateResult
}
