package de.konavigator.app.application.productdiscovery

/**
 * Kompositorischer Ergebnisvertrag der Planned-Entry-Selection. Das
 * Target-Selection-Ergebnis bleibt als bestehender Vertrag unveraendert.
 */
sealed interface KnockoutProductCandidatePlannedEntrySelectionApplicationResult {
    data class TargetSelectionEvaluated(
        val targetSelectionResult: KnockoutProductCandidateTargetSelectionApplicationResult,
        val invalidTargetLeveragePlanCandidates:
            List<KnockoutProductCandidateWithTargetLeveragePlan>,
        val existingEntryFailedCandidates:
            List<KnockoutProductCandidateWithExistingEntryCalculation>
    ) : KnockoutProductCandidatePlannedEntrySelectionApplicationResult

    data class NoValidTargetLeveragePlanCandidates(
        val invalidCandidates: List<KnockoutProductCandidateWithTargetLeveragePlan>
    ) : KnockoutProductCandidatePlannedEntrySelectionApplicationResult

    data class NoSuccessfulExistingEntryCalculationCandidates(
        val invalidTargetLeveragePlanCandidates:
            List<KnockoutProductCandidateWithTargetLeveragePlan>,
        val failedCandidates: List<KnockoutProductCandidateWithExistingEntryCalculation>
    ) : KnockoutProductCandidatePlannedEntrySelectionApplicationResult

    data object NoInputCandidates :
        KnockoutProductCandidatePlannedEntrySelectionApplicationResult
}
