package de.konavigator.app.application.productdiscovery

/**
 * Kompositorischer Ergebnisvertrag der fruehen Calculation-Pipeline. Alle
 * Ausschlussgruppen bleiben entsprechend ihrer verantwortlichen Stufe getrennt.
 */
sealed interface KnockoutProductCandidateCalculationPipelineApplicationResult {
    data class SuccessfulCalculationCandidates(
        val successfulCandidates: List<KnockoutProductCandidateWithCalculation>,
        val blockedDataQualityCandidates: List<KnockoutProductCandidateWithDataQuality>,
        val calculationUnavailableCandidates:
            List<KnockoutProductCandidateWithCalculationAvailability>,
        val notFreshCandidates: List<KnockoutProductCandidateWithFreshness>,
        val sourceBlockedCandidates: List<KnockoutProductCandidateWithSourceEvaluation>,
        val failedCalculationCandidates: List<KnockoutProductCandidateWithCalculation>
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult

    data class DiscoveryStopped(
        val discoveryResult: KnockoutProductDiscoveryApplicationResult
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult

    data class MarketDataStopped(
        val marketDataResult: KnockoutProductCandidateMarketDataResult
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult

    data class NoStructurallyEligibleCandidates(
        val blockedDataQualityCandidates: List<KnockoutProductCandidateWithDataQuality>
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult

    data class NoCalculationAvailableCandidates(
        val blockedDataQualityCandidates: List<KnockoutProductCandidateWithDataQuality>,
        val calculationUnavailableCandidates:
            List<KnockoutProductCandidateWithCalculationAvailability>
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult

    data class NoFreshCandidates(
        val blockedDataQualityCandidates: List<KnockoutProductCandidateWithDataQuality>,
        val calculationUnavailableCandidates:
            List<KnockoutProductCandidateWithCalculationAvailability>,
        val notFreshCandidates: List<KnockoutProductCandidateWithFreshness>
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult

    data class NoSourceAllowedCandidates(
        val blockedDataQualityCandidates: List<KnockoutProductCandidateWithDataQuality>,
        val calculationUnavailableCandidates:
            List<KnockoutProductCandidateWithCalculationAvailability>,
        val notFreshCandidates: List<KnockoutProductCandidateWithFreshness>,
        val sourceBlockedCandidates: List<KnockoutProductCandidateWithSourceEvaluation>
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult

    data class NoSuccessfulCalculationCandidates(
        val blockedDataQualityCandidates: List<KnockoutProductCandidateWithDataQuality>,
        val calculationUnavailableCandidates:
            List<KnockoutProductCandidateWithCalculationAvailability>,
        val notFreshCandidates: List<KnockoutProductCandidateWithFreshness>,
        val sourceBlockedCandidates: List<KnockoutProductCandidateWithSourceEvaluation>,
        val failedCalculationCandidates: List<KnockoutProductCandidateWithCalculation>
    ) : KnockoutProductCandidateCalculationPipelineApplicationResult
}
