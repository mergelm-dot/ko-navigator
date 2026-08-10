package de.konavigator.app.application.productdiscovery

/** Verschachtelter Ergebnisvertrag ohne Flattening vorhandener Diagnosegruppen. */
sealed interface KnockoutProductCandidateSelectionPipelineApplicationResult {
    data class CalculationPipelineStopped(
        val calculationPipelineResult:
            KnockoutProductCandidateCalculationPipelineApplicationResult
    ) : KnockoutProductCandidateSelectionPipelineApplicationResult

    data class CurrencyConversionStopped(
        val calculationPipelineResult:
            KnockoutProductCandidateCalculationPipelineApplicationResult
                .SuccessfulCalculationCandidates,
        val currencyConversionResult:
            KnockoutProductCandidateCurrencyConversionApplicationResult
    ) : KnockoutProductCandidateSelectionPipelineApplicationResult

    data class PlannedEntrySelectionEvaluated(
        val calculationPipelineResult:
            KnockoutProductCandidateCalculationPipelineApplicationResult
                .SuccessfulCalculationCandidates,
        val currencyConversionResult:
            KnockoutProductCandidateCurrencyConversionApplicationResult
                .CandidatesWithCurrencyConversion,
        val plannedEntrySelectionResult:
            KnockoutProductCandidatePlannedEntrySelectionApplicationResult
    ) : KnockoutProductCandidateSelectionPipelineApplicationResult
}
