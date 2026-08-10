package de.konavigator.app.application.productdiscovery

/**
 * Reiner, synchroner und zustandsloser Orchestrator der bestehenden
 * Target-Deviation-, Target-Fit-, Ranking- und Selection-Komponenten.
 */
class KnockoutProductCandidateTargetSelectionApplicationService(
    private val targetDeviationApplicationService:
        KnockoutProductCandidateTargetDeviationApplicationService,
    private val targetDeviationGate: KnockoutProductCandidateTargetDeviationGate,
    private val targetFitApplicationService:
        KnockoutProductCandidateTargetFitApplicationService,
    private val targetFitGate: KnockoutProductCandidateTargetFitGate,
    private val targetFitRanker: KnockoutProductCandidateTargetFitRanker,
    private val targetFitSelector: KnockoutProductCandidateTargetFitSelector
) {

    fun execute(
        request: KnockoutProductCandidateTargetSelectionApplicationRequest
    ): KnockoutProductCandidateTargetSelectionApplicationResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateTargetSelectionApplicationResult.NoInputCandidates
        }

        val targetDeviationResult = targetDeviationApplicationService.execute(
            KnockoutProductCandidateTargetDeviationRequest(
                candidates = request.candidates,
                plannedEntryPrice = request.plannedEntryPrice
            )
        )
        val targetDeviationCandidates = when (targetDeviationResult) {
            is KnockoutProductCandidateTargetDeviationResult.CandidatesWithTargetDeviation ->
                targetDeviationResult.candidates

            KnockoutProductCandidateTargetDeviationResult.NoInputCandidates ->
                error("Non-empty input must produce target-deviation candidates")
        }

        val successfulTargetDeviationResult = when (val targetDeviationGateResult =
            targetDeviationGate.filter(
            KnockoutProductCandidateTargetDeviationGateRequest(targetDeviationCandidates)
        )
        ) {
            is KnockoutProductCandidateTargetDeviationGateResult
                .SuccessfulTargetDeviationCandidates -> targetDeviationGateResult

            is KnockoutProductCandidateTargetDeviationGateResult.NoSuccessfulTargetDeviationCandidates ->
                return KnockoutProductCandidateTargetSelectionApplicationResult
                    .NoSuccessfulTargetDeviationCandidates(
                        targetDeviationGateResult.failedCandidates
                    )

            KnockoutProductCandidateTargetDeviationGateResult.NoInputCandidates ->
                error("Target-deviation candidates must be non-empty")
        }

        val targetFitResult = targetFitApplicationService.execute(
            KnockoutProductCandidateTargetFitRequest(
                candidates = successfulTargetDeviationResult.successfulCandidates,
                maxRelativeLeverageDeviationPercent =
                    request.maxRelativeLeverageDeviationPercent,
                maxBarrierDeviationPercentOfPlannedEntry =
                    request.maxBarrierDeviationPercentOfPlannedEntry
            )
        )
        val targetFitCandidates = when (targetFitResult) {
            is KnockoutProductCandidateTargetFitResult.CandidatesWithTargetFit ->
                targetFitResult.candidates

            KnockoutProductCandidateTargetFitResult.NoInputCandidates ->
                error("Successful target-deviation candidates must be non-empty")
        }

        val candidatesWithinTargetTolerancesResult = when (val targetFitGateResult =
            targetFitGate.filter(
            KnockoutProductCandidateTargetFitGateRequest(targetFitCandidates)
        )
        ) {
            is KnockoutProductCandidateTargetFitGateResult
                .CandidatesWithinTargetTolerances -> targetFitGateResult

            is KnockoutProductCandidateTargetFitGateResult
                .NoCandidatesWithinTargetTolerances ->
                return KnockoutProductCandidateTargetSelectionApplicationResult
                    .NoCandidatesWithinTargetTolerances(
                        targetDeviationFailedCandidates =
                            successfulTargetDeviationResult.failedCandidates,
                        nonMatchingCandidates = targetFitGateResult.nonMatchingCandidates,
                        targetFitFailedCandidates = targetFitGateResult.failedCandidates
                    )

            KnockoutProductCandidateTargetFitGateResult.NoInputCandidates ->
                error("Target-fit candidates must be non-empty")
        }

        val rankingResult = targetFitRanker.rank(
            KnockoutProductCandidateTargetFitRankingRequest(
                candidatesWithinTargetTolerancesResult.matchingCandidates
            )
        )
        val rankedCandidates = when (rankingResult) {
            is KnockoutProductCandidateTargetFitRankingResult.RankedCandidates ->
                rankingResult.candidates

            KnockoutProductCandidateTargetFitRankingResult.NoInputCandidates ->
                error("Matching candidates must be non-empty")
        }

        val selectionResult = targetFitSelector.select(
            KnockoutProductCandidateTargetFitSelectionRequest(rankedCandidates)
        )
        return when (selectionResult) {
            is KnockoutProductCandidateTargetFitSelectionResult.SelectedCandidates ->
                KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates(
                    primaryCandidate = selectionResult.primaryCandidate,
                    alternativeCandidates = selectionResult.alternativeCandidates,
                    targetDeviationFailedCandidates =
                        successfulTargetDeviationResult.failedCandidates,
                    nonMatchingCandidates =
                        candidatesWithinTargetTolerancesResult.nonMatchingCandidates,
                    targetFitFailedCandidates =
                        candidatesWithinTargetTolerancesResult.failedCandidates
                )

            KnockoutProductCandidateTargetFitSelectionResult.NoInputCandidates ->
                error("Ranked matching candidates must be non-empty")
        }
    }
}
