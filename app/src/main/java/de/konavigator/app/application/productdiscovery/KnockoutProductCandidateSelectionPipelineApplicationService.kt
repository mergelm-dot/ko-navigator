package de.konavigator.app.application.productdiscovery

/**
 * Orchestriert ausschließlich Calculation-Pipeline, Currency-Conversion und
 * Planned-Entry-Selection. Die vorhandenen Ergebnisverträge bleiben erhalten.
 */
class KnockoutProductCandidateSelectionPipelineApplicationService(
    private val calculationPipelineApplicationService:
        KnockoutProductCandidateCalculationPipelineApplicationService,
    private val currencyConversionApplicationService:
        KnockoutProductCandidateCurrencyConversionApplicationService,
    private val plannedEntrySelectionApplicationService:
        KnockoutProductCandidatePlannedEntrySelectionApplicationService
) {

    suspend fun execute(
        request: KnockoutProductCandidateSelectionPipelineApplicationRequest
    ): KnockoutProductCandidateSelectionPipelineApplicationResult {
        val calculationPipelineResult = calculationPipelineApplicationService.execute(
            KnockoutProductCandidateCalculationPipelineApplicationRequest(
                underlyingId = request.underlyingId,
                direction = request.direction,
                brokerId = request.brokerId,
                enabledIssuerIds = request.enabledIssuerIds,
                calculationType = request.calculationType,
                evaluationTimeEpochMillis = request.evaluationTimeEpochMillis
            )
        )
        val successfulCalculationResult = when (calculationPipelineResult) {
            is KnockoutProductCandidateCalculationPipelineApplicationResult
                .SuccessfulCalculationCandidates -> calculationPipelineResult

            else -> return KnockoutProductCandidateSelectionPipelineApplicationResult
                .CalculationPipelineStopped(calculationPipelineResult)
        }

        val currencyConversionResult = currencyConversionApplicationService.execute(
            KnockoutProductCandidateCurrencyConversionApplicationRequest(
                candidates = successfulCalculationResult.successfulCandidates,
                evaluationTimeEpochMillis = request.evaluationTimeEpochMillis,
                maxFxAgeMillis = request.maxFxAgeMillis
            )
        )
        val successfulCurrencyResult = when (currencyConversionResult) {
            is KnockoutProductCandidateCurrencyConversionApplicationResult
                .CandidatesWithCurrencyConversion -> currencyConversionResult

            else -> return KnockoutProductCandidateSelectionPipelineApplicationResult
                .CurrencyConversionStopped(
                    calculationPipelineResult = successfulCalculationResult,
                    currencyConversionResult = currencyConversionResult
                )
        }

        val plannedEntrySelectionResult = plannedEntrySelectionApplicationService.execute(
            KnockoutProductCandidatePlannedEntrySelectionApplicationRequest(
                candidates = successfulCurrencyResult.successfulCandidates.map {
                    candidate -> candidate.targetLeverageInput
                },
                underlyingPrice = request.underlyingPrice,
                plannedEntryPrice = request.plannedEntryPrice,
                targetLeverage = request.targetLeverage,
                maxRelativeLeverageDeviationPercent =
                    request.maxRelativeLeverageDeviationPercent,
                maxBarrierDeviationPercentOfPlannedEntry =
                    request.maxBarrierDeviationPercentOfPlannedEntry
            )
        )

        return KnockoutProductCandidateSelectionPipelineApplicationResult
            .PlannedEntrySelectionEvaluated(
                calculationPipelineResult = successfulCalculationResult,
                currencyConversionResult = successfulCurrencyResult,
                plannedEntrySelectionResult = plannedEntrySelectionResult
            )
    }
}
