package de.konavigator.app.application.productdiscovery

/**
 * Reiner, synchroner und zustandsloser Orchestrator der bestehenden
 * Target-Leverage-, Existing-Entry- und Target-Selection-Komponenten.
 */
class KnockoutProductCandidatePlannedEntrySelectionApplicationService(
    private val targetLeverageApplicationService:
        KnockoutProductCandidateTargetLeverageApplicationService,
    private val targetLeverageGate: KnockoutProductCandidateTargetLeverageGate,
    private val existingEntryCalculationApplicationService:
        KnockoutProductCandidateExistingEntryCalculationApplicationService,
    private val existingEntryCalculationGate:
        KnockoutProductCandidateExistingEntryCalculationGate,
    private val targetSelectionApplicationService:
        KnockoutProductCandidateTargetSelectionApplicationService
) {

    fun execute(
        request: KnockoutProductCandidatePlannedEntrySelectionApplicationRequest
    ): KnockoutProductCandidatePlannedEntrySelectionApplicationResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .NoInputCandidates
        }

        val targetLeverageResult = targetLeverageApplicationService.execute(
            KnockoutProductCandidateTargetLeverageRequest(
                candidates = request.candidates,
                underlyingPrice = request.underlyingPrice,
                plannedEntryPrice = request.plannedEntryPrice,
                targetLeverage = request.targetLeverage
            )
        )
        val targetLeverageCandidates = when (targetLeverageResult) {
            is KnockoutProductCandidateTargetLeverageResult
                .CandidatesWithTargetLeveragePlan -> targetLeverageResult.candidates

            KnockoutProductCandidateTargetLeverageResult.NoInputCandidates ->
                error("Non-empty input must produce target-leverage candidates")
        }

        val validTargetLeverageResult = when (val gateResult = targetLeverageGate.filter(
            KnockoutProductCandidateTargetLeverageGateRequest(targetLeverageCandidates)
        )) {
            is KnockoutProductCandidateTargetLeverageGateResult
                .ValidTargetLeveragePlanCandidates -> gateResult

            is KnockoutProductCandidateTargetLeverageGateResult
                .NoValidTargetLeveragePlanCandidates ->
                return KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                    .NoValidTargetLeveragePlanCandidates(gateResult.invalidCandidates)

            KnockoutProductCandidateTargetLeverageGateResult.NoInputCandidates ->
                error("Target-leverage candidates must be non-empty")
        }

        val existingEntryResult = existingEntryCalculationApplicationService.execute(
            KnockoutProductCandidateExistingEntryCalculationRequest(
                candidates = validTargetLeverageResult.validCandidates,
                plannedEntryPrice = request.plannedEntryPrice
            )
        )
        val existingEntryCandidates = when (existingEntryResult) {
            is KnockoutProductCandidateExistingEntryCalculationResult
                .CandidatesWithExistingEntryCalculation -> existingEntryResult.candidates

            KnockoutProductCandidateExistingEntryCalculationResult.NoInputCandidates ->
                error("Valid target-leverage candidates must be non-empty")
        }

        val successfulExistingEntryResult = when (val gateResult =
            existingEntryCalculationGate.filter(
                KnockoutProductCandidateExistingEntryCalculationGateRequest(
                    existingEntryCandidates
                )
            )) {
            is KnockoutProductCandidateExistingEntryCalculationGateResult
                .SuccessfulExistingEntryCalculationCandidates -> gateResult

            is KnockoutProductCandidateExistingEntryCalculationGateResult
                .NoSuccessfulExistingEntryCalculationCandidates ->
                return KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                    .NoSuccessfulExistingEntryCalculationCandidates(
                        invalidTargetLeveragePlanCandidates =
                            validTargetLeverageResult.invalidCandidates,
                        failedCandidates = gateResult.failedCandidates
                    )

            KnockoutProductCandidateExistingEntryCalculationGateResult.NoInputCandidates ->
                error("Existing-entry candidates must be non-empty")
        }

        val targetSelectionResult = targetSelectionApplicationService.execute(
            KnockoutProductCandidateTargetSelectionApplicationRequest(
                candidates = successfulExistingEntryResult.successfulCandidates,
                plannedEntryPrice = request.plannedEntryPrice,
                maxRelativeLeverageDeviationPercent =
                    request.maxRelativeLeverageDeviationPercent,
                maxBarrierDeviationPercentOfPlannedEntry =
                    request.maxBarrierDeviationPercentOfPlannedEntry
            )
        )

        return KnockoutProductCandidatePlannedEntrySelectionApplicationResult
            .TargetSelectionEvaluated(
                targetSelectionResult = targetSelectionResult,
                invalidTargetLeveragePlanCandidates =
                    validTargetLeverageResult.invalidCandidates,
                existingEntryFailedCandidates =
                    successfulExistingEntryResult.failedCandidates
            )
    }
}
