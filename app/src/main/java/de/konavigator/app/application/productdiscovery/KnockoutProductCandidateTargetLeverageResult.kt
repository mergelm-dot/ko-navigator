package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.TradeCalculationResult

/** Verbindet einen unveränderten Kandidaten-Input mit dem unveränderten Engine-Ergebnis. */
data class KnockoutProductCandidateWithTargetLeveragePlan(
    val input: KnockoutProductCandidateTargetLeverageInput,
    val tradeCalculationResult: TradeCalculationResult
)

/**
 * Providerneutraler Ergebnisvertrag für theoretische Zielhebel-Pläne. Er
 * bewertet weder reale Barrieren- oder Preisübereinstimmungen noch Ranking,
 * Hauptprodukt, Alternativen oder Orderentscheidungen.
 */
sealed interface KnockoutProductCandidateTargetLeverageResult {
    data class CandidatesWithTargetLeveragePlan(
        val candidates: List<KnockoutProductCandidateWithTargetLeveragePlan>
    ) : KnockoutProductCandidateTargetLeverageResult

    data object NoInputCandidates : KnockoutProductCandidateTargetLeverageResult
}
