package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationPipelineApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionEvidence
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateMarketDataResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidatePlannedEntrySelectionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetSelectionApplicationResult
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithCurrencyConversion
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateWithTargetFit
import de.konavigator.app.application.productdiscovery.KnockoutProductDiscoveryApplicationResult
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitResult

/** Reine Mapping-Grenze zwischen der Auswahlpipeline und stabilen Presentation-Daten. */
object TradePlannerSelectionUiMapper {

    fun map(
        result: KnockoutProductCandidateSelectionPipelineApplicationResult
    ): TradePlannerSelectionUiResult = when (result) {
        is KnockoutProductCandidateSelectionPipelineApplicationResult.CalculationPipelineStopped ->
            mapCalculationPipelineStopped(result.calculationPipelineResult)

        is KnockoutProductCandidateSelectionPipelineApplicationResult.CurrencyConversionStopped ->
            mapCurrencyConversionStopped(result)

        is KnockoutProductCandidateSelectionPipelineApplicationResult
            .PlannedEntrySelectionEvaluated -> mapPlannedEntrySelection(result)
    }

    private fun mapCalculationPipelineStopped(
        result: KnockoutProductCandidateCalculationPipelineApplicationResult
    ): TradePlannerSelectionUiResult = when (result) {
        is KnockoutProductCandidateCalculationPipelineApplicationResult
            .SuccessfulCalculationCandidates -> inconsistent(
                TradePlannerSelectionUiMappingError
                    .CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT
            )

        is KnockoutProductCandidateCalculationPipelineApplicationResult.DiscoveryStopped ->
            mapDiscoveryStopped(result.discoveryResult)

        is KnockoutProductCandidateCalculationPipelineApplicationResult.MarketDataStopped ->
            mapMarketDataStopped(result.marketDataResult)

        is KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoStructurallyEligibleCandidates -> noSelection(
                reason = TradePlannerSelectionUiNoSelectionReason
                    .NO_STRUCTURALLY_ELIGIBLE_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics(
                    dataQualityBlockedCount = result.blockedDataQualityCandidates.size
                )
            )

        is KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoCalculationAvailableCandidates -> noSelection(
                reason = TradePlannerSelectionUiNoSelectionReason
                    .NO_CALCULATION_AVAILABLE_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics(
                    dataQualityBlockedCount = result.blockedDataQualityCandidates.size,
                    calculationUnavailableCount = result.calculationUnavailableCandidates.size
                )
            )

        is KnockoutProductCandidateCalculationPipelineApplicationResult.NoFreshCandidates ->
            noSelection(
                reason = TradePlannerSelectionUiNoSelectionReason.NO_FRESH_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics(
                    dataQualityBlockedCount = result.blockedDataQualityCandidates.size,
                    calculationUnavailableCount = result.calculationUnavailableCandidates.size,
                    notFreshCount = result.notFreshCandidates.size
                )
            )

        is KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoSourceAllowedCandidates -> noSelection(
                reason = TradePlannerSelectionUiNoSelectionReason.NO_SOURCE_ALLOWED_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics(
                    dataQualityBlockedCount = result.blockedDataQualityCandidates.size,
                    calculationUnavailableCount = result.calculationUnavailableCandidates.size,
                    notFreshCount = result.notFreshCandidates.size,
                    sourceBlockedCount = result.sourceBlockedCandidates.size
                )
            )

        is KnockoutProductCandidateCalculationPipelineApplicationResult
            .NoSuccessfulCalculationCandidates -> noSelection(
                reason = TradePlannerSelectionUiNoSelectionReason
                    .NO_SUCCESSFUL_CALCULATION_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics(
                    dataQualityBlockedCount = result.blockedDataQualityCandidates.size,
                    calculationUnavailableCount = result.calculationUnavailableCandidates.size,
                    notFreshCount = result.notFreshCandidates.size,
                    sourceBlockedCount = result.sourceBlockedCandidates.size,
                    calculationFailedCount = result.failedCalculationCandidates.size
                )
            )
    }

    private fun mapDiscoveryStopped(
        result: KnockoutProductDiscoveryApplicationResult
    ): TradePlannerSelectionUiResult = when (result) {
        is KnockoutProductDiscoveryApplicationResult.BrokerTradableCandidates -> inconsistent(
            TradePlannerSelectionUiMappingError.CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT
        )

        KnockoutProductDiscoveryApplicationResult.NoCatalogCandidates -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.NO_CATALOG_CANDIDATES
        )

        KnockoutProductDiscoveryApplicationResult.NoBrokerTradableCandidates -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.NO_BROKER_TRADABLE_CANDIDATES
        )

        KnockoutProductDiscoveryApplicationResult.NoEnabledIssuerCandidates -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.NO_ENABLED_ISSUER_CANDIDATES
        )

        KnockoutProductDiscoveryApplicationResult.CatalogDataAccessFailure -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.CATALOG_DATA_ACCESS_FAILURE
        )

        KnockoutProductDiscoveryApplicationResult.CatalogInvalidData -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.CATALOG_INVALID_DATA
        )

        KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityDataAccessFailure ->
            noSelection(
                TradePlannerSelectionUiNoSelectionReason
                    .BROKER_AVAILABILITY_DATA_ACCESS_FAILURE
            )

        KnockoutProductDiscoveryApplicationResult.BrokerAvailabilityInvalidData -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.BROKER_AVAILABILITY_INVALID_DATA
        )
    }

    private fun mapMarketDataStopped(
        result: KnockoutProductCandidateMarketDataResult
    ): TradePlannerSelectionUiResult = when (result) {
        is KnockoutProductCandidateMarketDataResult.CandidatesWithMarketData -> inconsistent(
            TradePlannerSelectionUiMappingError.CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT
        )

        KnockoutProductCandidateMarketDataResult.NoInputCandidates -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.EMPTY_MARKET_DATA_PIPELINE_INPUT
        )

        is KnockoutProductCandidateMarketDataResult.MarketDataNotFound -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_NOT_FOUND
        )

        is KnockoutProductCandidateMarketDataResult.MarketDataDataAccessFailure -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_DATA_ACCESS_FAILURE
        )

        is KnockoutProductCandidateMarketDataResult.MarketDataInvalidData -> noSelection(
            TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_INVALID_DATA
        )
    }

    private fun mapCurrencyConversionStopped(
        result: KnockoutProductCandidateSelectionPipelineApplicationResult
            .CurrencyConversionStopped
    ): TradePlannerSelectionUiResult {
        val diagnostics = result.calculationPipelineResult.toDiagnostics()
        return when (val currencyResult = result.currencyConversionResult) {
            is KnockoutProductCandidateCurrencyConversionApplicationResult
                .CandidatesWithCurrencyConversion -> inconsistent(
                    TradePlannerSelectionUiMappingError
                        .CURRENCY_CONVERSION_STOPPED_WITH_SUCCESS_RESULT
                )

            is KnockoutProductCandidateCurrencyConversionApplicationResult
                .NoCurrencyConvertibleCandidates -> noSelection(
                    reason = TradePlannerSelectionUiNoSelectionReason
                        .NO_CURRENCY_CONVERTIBLE_CANDIDATES,
                    diagnostics = diagnostics.copy(
                        currencyConversionFailedCount = currencyResult.failedCandidates.size
                    )
                )

            KnockoutProductCandidateCurrencyConversionApplicationResult.NoInputCandidates ->
                noSelection(
                    reason = TradePlannerSelectionUiNoSelectionReason
                        .EMPTY_CURRENCY_CONVERSION_PIPELINE_INPUT,
                    diagnostics = diagnostics
                )
        }
    }

    private fun mapPlannedEntrySelection(
        result: KnockoutProductCandidateSelectionPipelineApplicationResult
            .PlannedEntrySelectionEvaluated
    ): TradePlannerSelectionUiResult {
        val diagnostics = result.calculationPipelineResult.toDiagnostics(
            currencyConversionFailedCount = result.currencyConversionResult.failedCandidates.size
        )
        return when (val plannedResult = result.plannedEntrySelectionResult) {
            is KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .TargetSelectionEvaluated -> mapTargetSelection(
                    result = plannedResult,
                    currencyCandidates = result.currencyConversionResult.successfulCandidates,
                    diagnostics = diagnostics.copy(
                        invalidTargetLeveragePlanCount =
                            plannedResult.invalidTargetLeveragePlanCandidates.size,
                        existingEntryFailedCount =
                            plannedResult.existingEntryFailedCandidates.size
                    )
                )

            is KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .NoValidTargetLeveragePlanCandidates -> noSelection(
                    reason = TradePlannerSelectionUiNoSelectionReason
                        .NO_VALID_TARGET_LEVERAGE_PLAN_CANDIDATES,
                    diagnostics = diagnostics.copy(
                        invalidTargetLeveragePlanCount = plannedResult.invalidCandidates.size
                    )
                )

            is KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .NoSuccessfulExistingEntryCalculationCandidates -> noSelection(
                    reason = TradePlannerSelectionUiNoSelectionReason
                        .NO_SUCCESSFUL_EXISTING_ENTRY_CALCULATION_CANDIDATES,
                    diagnostics = diagnostics.copy(
                        invalidTargetLeveragePlanCount =
                            plannedResult.invalidTargetLeveragePlanCandidates.size,
                        existingEntryFailedCount = plannedResult.failedCandidates.size
                    )
                )

            KnockoutProductCandidatePlannedEntrySelectionApplicationResult.NoInputCandidates ->
                noSelection(
                    reason = TradePlannerSelectionUiNoSelectionReason
                        .EMPTY_PLANNED_ENTRY_SELECTION_PIPELINE_INPUT,
                    diagnostics = diagnostics
                )
        }
    }

    private fun mapTargetSelection(
        result: KnockoutProductCandidatePlannedEntrySelectionApplicationResult
            .TargetSelectionEvaluated,
        currencyCandidates: List<KnockoutProductCandidateWithCurrencyConversion>,
        diagnostics: TradePlannerSelectionUiDiagnostics
    ): TradePlannerSelectionUiResult = when (val targetResult = result.targetSelectionResult) {
        is KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates ->
            mapSelectedCandidates(targetResult, currencyCandidates, diagnostics)

        is KnockoutProductCandidateTargetSelectionApplicationResult
            .NoSuccessfulTargetDeviationCandidates -> noSelection(
                reason = TradePlannerSelectionUiNoSelectionReason
                    .NO_SUCCESSFUL_TARGET_DEVIATION_CANDIDATES,
                diagnostics = diagnostics.copy(
                    targetDeviationFailedCount = targetResult.failedCandidates.size
                )
            )

        is KnockoutProductCandidateTargetSelectionApplicationResult
            .NoCandidatesWithinTargetTolerances -> noSelection(
                reason = TradePlannerSelectionUiNoSelectionReason
                    .NO_CANDIDATES_WITHIN_TARGET_TOLERANCES,
                diagnostics = diagnostics.copy(
                    targetDeviationFailedCount =
                        targetResult.targetDeviationFailedCandidates.size,
                    nonMatchingTargetFitCount = targetResult.nonMatchingCandidates.size,
                    targetFitFailedCount = targetResult.targetFitFailedCandidates.size
                )
            )

        KnockoutProductCandidateTargetSelectionApplicationResult.NoInputCandidates -> noSelection(
            reason = TradePlannerSelectionUiNoSelectionReason.EMPTY_TARGET_SELECTION_PIPELINE_INPUT,
            diagnostics = diagnostics
        )
    }

    private fun mapSelectedCandidates(
        result: KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates,
        currencyCandidates: List<KnockoutProductCandidateWithCurrencyConversion>,
        diagnostics: TradePlannerSelectionUiDiagnostics
    ): TradePlannerSelectionUiResult {
        val selectedDiagnostics = diagnostics.copy(
            targetDeviationFailedCount = result.targetDeviationFailedCandidates.size,
            nonMatchingTargetFitCount = result.nonMatchingCandidates.size,
            targetFitFailedCount = result.targetFitFailedCandidates.size
        )
        val primary = mapCandidate(result.primaryCandidate, currencyCandidates)
        if (primary is CandidateMapping.Failure) {
            return inconsistent(primary.error)
        }

        val alternatives = mutableListOf<TradePlannerSelectedProductUiModel>()
        for (candidate in result.alternativeCandidates) {
            when (val mapped = mapCandidate(candidate, currencyCandidates)) {
                is CandidateMapping.Success -> alternatives += mapped.model
                is CandidateMapping.Failure -> return inconsistent(mapped.error)
            }
        }

        return TradePlannerSelectionUiResult.Selected(
            primaryCandidate = (primary as CandidateMapping.Success).model,
            alternativeCandidates = alternatives,
            diagnostics = selectedDiagnostics
        )
    }

    private fun mapCandidate(
        candidate: KnockoutProductCandidateWithTargetFit,
        currencyCandidates: List<KnockoutProductCandidateWithCurrencyConversion>
    ): CandidateMapping {
        val targetDeviationCandidate = candidate.candidateWithTargetDeviation
        val existingEntryCandidate =
            targetDeviationCandidate.candidateWithExistingEntryCalculation
        val existingEntryResult = existingEntryCandidate.existingEntryCalculationResult
        if (existingEntryResult !is ExistingKnockoutProductEntryCalculationResult.Success) {
            return CandidateMapping.Failure(
                TradePlannerSelectionUiMappingError
                    .SELECTED_CANDIDATE_EXISTING_ENTRY_NOT_SUCCESSFUL
            )
        }

        val targetDeviationResult = targetDeviationCandidate.targetDeviationResult
        if (targetDeviationResult !is ExistingKnockoutProductTargetDeviationResult.Success) {
            return CandidateMapping.Failure(
                TradePlannerSelectionUiMappingError
                    .SELECTED_CANDIDATE_TARGET_DEVIATION_NOT_SUCCESSFUL
            )
        }

        val targetFitResult = candidate.targetFitResult
        if (targetFitResult !is ExistingKnockoutProductTargetFitResult.Success) {
            return CandidateMapping.Failure(
                TradePlannerSelectionUiMappingError.SELECTED_CANDIDATE_TARGET_FIT_NOT_SUCCESSFUL
            )
        }

        val targetLeverageInput = existingEntryCandidate.candidateWithTargetLeveragePlan.input
        val evidenceMatches = currencyCandidates.filter {
            it.targetLeverageInput === targetLeverageInput
        }
        if (evidenceMatches.isEmpty()) {
            return CandidateMapping.Failure(
                TradePlannerSelectionUiMappingError
                    .SELECTED_CANDIDATE_CURRENCY_EVIDENCE_NOT_FOUND
            )
        }
        if (evidenceMatches.size > 1) {
            return CandidateMapping.Failure(
                TradePlannerSelectionUiMappingError
                    .SELECTED_CANDIDATE_CURRENCY_EVIDENCE_AMBIGUOUS
            )
        }

        val specification = targetLeverageInput.candidateWithCalculation
            .candidateWithSourceEvaluation.candidateWithFreshness
            .candidateWithCalculationAvailability.candidateWithDataQuality
            .candidateWithMarketData.specificationSnapshot.specification
        return CandidateMapping.Success(
            TradePlannerSelectedProductUiModel(
                productIsin = specification.productIsin,
                productWkn = specification.productWkn,
                issuerId = specification.issuerId,
                productCurrency = existingEntryResult.productCurrency.value,
                calculatedProductPriceAtPlannedEntry =
                    existingEntryResult.theoreticalProductValue,
                calculatedLeverageAtPlannedEntry = existingEntryResult.calculatedLeverageAtEntry,
                knockoutBarrier = specification.knockoutBarrier,
                knockoutDistanceAbsolute = existingEntryResult.knockoutDistanceAbsolute,
                knockoutDistancePercent = existingEntryResult.knockoutDistancePercent,
                relativeLeverageDeviationPercent =
                    targetDeviationResult.relativeLeverageDeviationPercent,
                barrierDeviationPercentOfPlannedEntry =
                    targetDeviationResult.barrierDeviationPercentOfPlannedEntry,
                leverageWithinTolerance = targetFitResult.leverageWithinTolerance,
                barrierWithinTolerance = targetFitResult.barrierWithinTolerance,
                withinAllTargetTolerances = targetFitResult.withinAllTargetTolerances,
                currencyEvidence = evidenceMatches.single().evidence.toUiEvidence()
            )
        )
    }

    private fun KnockoutProductCandidateCalculationPipelineApplicationResult
        .SuccessfulCalculationCandidates.toDiagnostics(
            currencyConversionFailedCount: Int = 0
        ) = TradePlannerSelectionUiDiagnostics(
            dataQualityBlockedCount = blockedDataQualityCandidates.size,
            calculationUnavailableCount = calculationUnavailableCandidates.size,
            notFreshCount = notFreshCandidates.size,
            sourceBlockedCount = sourceBlockedCandidates.size,
            calculationFailedCount = failedCalculationCandidates.size,
            currencyConversionFailedCount = currencyConversionFailedCount
        )

    private fun KnockoutProductCandidateCurrencyConversionEvidence.toUiEvidence():
        TradePlannerSelectionCurrencyEvidence = when (this) {
        KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency ->
            TradePlannerSelectionCurrencyEvidence.SameCurrency

        is KnockoutProductCandidateCurrencyConversionEvidence.CrossCurrency ->
            TradePlannerSelectionCurrencyEvidence.CrossCurrency(
                sourceId = sourceId,
                observedAtEpochMillis = observedAtEpochMillis
            )
    }

    private fun noSelection(
        reason: TradePlannerSelectionUiNoSelectionReason,
        diagnostics: TradePlannerSelectionUiDiagnostics = TradePlannerSelectionUiDiagnostics()
    ) = TradePlannerSelectionUiResult.NoSelection(reason, diagnostics)

    private fun inconsistent(
        error: TradePlannerSelectionUiMappingError
    ) = TradePlannerSelectionUiResult.InconsistentData(error)

    private sealed interface CandidateMapping {
        data class Success(
            val model: TradePlannerSelectedProductUiModel
        ) : CandidateMapping

        data class Failure(
            val error: TradePlannerSelectionUiMappingError
        ) : CandidateMapping
    }
}
