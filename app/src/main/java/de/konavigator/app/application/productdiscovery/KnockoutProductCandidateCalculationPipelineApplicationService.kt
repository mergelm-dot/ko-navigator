package de.konavigator.app.application.productdiscovery

/**
 * Suspendierender, providerneutraler Orchestrator der vorhandenen fruehen
 * Produkt-, Marktdaten-, Bewertungs- und Calculation-Stufen.
 */
class KnockoutProductCandidateCalculationPipelineApplicationService(
    private val discoveryApplicationService: KnockoutProductDiscoveryApplicationService,
    private val marketDataApplicationService:
        KnockoutProductCandidateMarketDataApplicationService,
    private val dataQualityApplicationService:
        KnockoutProductCandidateDataQualityApplicationService,
    private val dataQualityGate: KnockoutProductCandidateDataQualityGate,
    private val calculationAvailabilityApplicationService:
        KnockoutProductCandidateCalculationAvailabilityApplicationService,
    private val calculationAvailabilityGate:
        KnockoutProductCandidateCalculationAvailabilityGate,
    private val freshnessApplicationService:
        KnockoutProductCandidateFreshnessApplicationService,
    private val freshnessGate: KnockoutProductCandidateFreshnessGate,
    private val sourceEvaluationApplicationService:
        KnockoutProductCandidateSourceEvaluationApplicationService,
    private val sourceGate: KnockoutProductCandidateSourceGate,
    private val calculationApplicationService:
        KnockoutProductCandidateCalculationApplicationService,
    private val calculationGate: KnockoutProductCandidateCalculationGate
) {

    suspend fun execute(
        request: KnockoutProductCandidateCalculationPipelineApplicationRequest
    ): KnockoutProductCandidateCalculationPipelineApplicationResult {
        val discoveryResult = discoveryApplicationService.execute(
            KnockoutProductDiscoveryApplicationRequest(
                underlyingId = request.underlyingId,
                direction = request.direction,
                brokerId = request.brokerId,
                enabledIssuerIds = request.enabledIssuerIds
            )
        )
        val discoveryCandidates = when (discoveryResult) {
            is KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates ->
                discoveryResult.candidates

            else -> return KnockoutProductCandidateCalculationPipelineApplicationResult
                .DiscoveryStopped(discoveryResult)
        }

        val marketDataResult = marketDataApplicationService.execute(
            KnockoutProductCandidateMarketDataRequest(discoveryCandidates)
        )
        val marketDataCandidates = when (marketDataResult) {
            is KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData ->
                marketDataResult.candidates

            else -> return KnockoutProductCandidateCalculationPipelineApplicationResult
                .MarketDataStopped(marketDataResult)
        }

        val dataQualityResult = dataQualityApplicationService.execute(
            KnockoutProductCandidateDataQualityRequest(marketDataCandidates)
        )
        val dataQualityCandidates = when (dataQualityResult) {
            is KnockoutProductCandidateDataQualityResult.CandidatesWithDataQuality ->
                dataQualityResult.candidates

            KnockoutProductCandidateDataQualityResult.NoInputCandidates ->
                error("Market-data candidates must be non-empty")
        }

        val dataQualityGateResult = when (val gateResult = dataQualityGate.filter(
            KnockoutProductCandidateDataQualityGateRequest(dataQualityCandidates)
        )) {
            is KnockoutProductCandidateDataQualityGateResult.StructurallyEligibleCandidates ->
                gateResult

            is KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates ->
                return KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoStructurallyEligibleCandidates(gateResult.blockedCandidates)

            KnockoutProductCandidateDataQualityGateResult.NoInputCandidates ->
                error("Data-quality candidates must be non-empty")
        }

        val availabilityResult = calculationAvailabilityApplicationService.execute(
            KnockoutProductCandidateCalculationAvailabilityRequest(
                candidates = dataQualityGateResult.eligibleCandidates,
                calculationType = request.calculationType
            )
        )
        val availabilityCandidates = when (availabilityResult) {
            is KnockoutProductCandidateCalculationAvailabilityResult
                .CandidatesWithCalculationAvailability -> availabilityResult.candidates

            KnockoutProductCandidateCalculationAvailabilityResult.NoInputCandidates ->
                error("Structurally eligible candidates must be non-empty")
        }

        val availabilityGateResult = when (val gateResult =
            calculationAvailabilityGate.filter(
                KnockoutProductCandidateCalculationAvailabilityGateRequest(
                    availabilityCandidates
                )
            )) {
            is KnockoutProductCandidateCalculationAvailabilityGateResult
                .CalculationAvailableCandidates -> gateResult

            is KnockoutProductCandidateCalculationAvailabilityGateResult
                .NoCalculationAvailableCandidates ->
                return KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoCalculationAvailableCandidates(
                        blockedDataQualityCandidates =
                            dataQualityGateResult.blockedCandidates,
                        calculationUnavailableCandidates = gateResult.unavailableCandidates
                    )

            KnockoutProductCandidateCalculationAvailabilityGateResult.NoInputCandidates ->
                error("Availability candidates must be non-empty")
        }

        val freshnessResult = freshnessApplicationService.execute(
            KnockoutProductCandidateFreshnessRequest(
                candidates = availabilityGateResult.availableCandidates,
                calculationType = request.calculationType,
                evaluationTimeEpochMillis = request.evaluationTimeEpochMillis
            )
        )
        val freshnessCandidates = when (freshnessResult) {
            is KnockoutProductCandidateFreshnessResult.CandidatesWithFreshness ->
                freshnessResult.candidates

            KnockoutProductCandidateFreshnessResult.NoInputCandidates ->
                error("Calculation-available candidates must be non-empty")
        }

        val freshnessGateResult = when (val gateResult = freshnessGate.filter(
            KnockoutProductCandidateFreshnessGateRequest(freshnessCandidates)
        )) {
            is KnockoutProductCandidateFreshnessGateResult.FreshCandidates -> gateResult

            is KnockoutProductCandidateFreshnessGateResult.NoFreshCandidates ->
                return KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoFreshCandidates(
                        blockedDataQualityCandidates =
                            dataQualityGateResult.blockedCandidates,
                        calculationUnavailableCandidates =
                            availabilityGateResult.unavailableCandidates,
                        notFreshCandidates = gateResult.notFreshCandidates
                    )

            KnockoutProductCandidateFreshnessGateResult.NoInputCandidates ->
                error("Freshness candidates must be non-empty")
        }

        val sourceResult = sourceEvaluationApplicationService.execute(
            KnockoutProductCandidateSourceEvaluationRequest(
                candidates = freshnessGateResult.freshCandidates,
                calculationType = request.calculationType
            )
        )
        val sourceCandidates = when (sourceResult) {
            is KnockoutProductCandidateSourceEvaluationResult.CandidatesWithSourceEvaluation ->
                sourceResult.candidates

            KnockoutProductCandidateSourceEvaluationResult.NoInputCandidates ->
                error("Fresh candidates must be non-empty")
        }

        val sourceGateResult = when (val gateResult = sourceGate.filter(
            KnockoutProductCandidateSourceGateRequest(sourceCandidates)
        )) {
            is KnockoutProductCandidateSourceGateResult.SourceAllowedCandidates -> gateResult

            is KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates ->
                return KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoSourceAllowedCandidates(
                        blockedDataQualityCandidates =
                            dataQualityGateResult.blockedCandidates,
                        calculationUnavailableCandidates =
                            availabilityGateResult.unavailableCandidates,
                        notFreshCandidates = freshnessGateResult.notFreshCandidates,
                        sourceBlockedCandidates = gateResult.blockedCandidates
                    )

            KnockoutProductCandidateSourceGateResult.NoInputCandidates ->
                error("Source-evaluated candidates must be non-empty")
        }

        val calculationResult = calculationApplicationService.execute(
            KnockoutProductCandidateCalculationRequest(
                candidates = sourceGateResult.allowedCandidates,
                calculationType = request.calculationType
            )
        )
        val calculationCandidates = when (calculationResult) {
            is KnockoutProductCandidateCalculationResult.CandidatesWithCalculation ->
                calculationResult.candidates

            KnockoutProductCandidateCalculationResult.NoInputCandidates ->
                error("Source-allowed candidates must be non-empty")
        }

        return when (val gateResult = calculationGate.filter(
            KnockoutProductCandidateCalculationGateRequest(calculationCandidates)
        )) {
            is KnockoutProductCandidateCalculationGateResult.SuccessfulCalculationCandidates ->
                KnockoutProductCandidateCalculationPipelineApplicationResult
                    .SuccessfulCalculationCandidates(
                        successfulCandidates = gateResult.successfulCandidates,
                        blockedDataQualityCandidates =
                            dataQualityGateResult.blockedCandidates,
                        calculationUnavailableCandidates =
                            availabilityGateResult.unavailableCandidates,
                        notFreshCandidates = freshnessGateResult.notFreshCandidates,
                        sourceBlockedCandidates = sourceGateResult.blockedCandidates,
                        failedCalculationCandidates = gateResult.failedCandidates
                    )

            is KnockoutProductCandidateCalculationGateResult
                .NoSuccessfulCalculationCandidates ->
                KnockoutProductCandidateCalculationPipelineApplicationResult
                    .NoSuccessfulCalculationCandidates(
                        blockedDataQualityCandidates =
                            dataQualityGateResult.blockedCandidates,
                        calculationUnavailableCandidates =
                            availabilityGateResult.unavailableCandidates,
                        notFreshCandidates = freshnessGateResult.notFreshCandidates,
                        sourceBlockedCandidates = sourceGateResult.blockedCandidates,
                        failedCalculationCandidates = gateResult.failedCandidates
                    )

            KnockoutProductCandidateCalculationGateResult.NoInputCandidates ->
                error("Calculated candidates must be non-empty")
        }
    }
}
