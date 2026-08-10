package de.konavigator.app.presentation.tradeplanner

/** Rohe Presentation-Daten eines technisch ausgewählten bestehenden KO-Produkts. */
data class TradePlannerSelectedProductUiModel(
    val productIsin: String,
    val productWkn: String?,
    val issuerId: String,
    val productCurrency: String,
    val calculatedProductPriceAtPlannedEntry: Double,
    val calculatedLeverageAtPlannedEntry: Double,
    val knockoutBarrier: Double,
    val knockoutDistanceAbsolute: Double,
    val knockoutDistancePercent: Double,
    val relativeLeverageDeviationPercent: Double,
    val barrierDeviationPercentOfPlannedEntry: Double,
    val leverageWithinTolerance: Boolean,
    val barrierWithinTolerance: Boolean,
    val withinAllTargetTolerances: Boolean,
    val currencyEvidence: TradePlannerSelectionCurrencyEvidence
)

/** Maschinenlesbarer Währungsnachweis ohne Benutzertext oder Formatierung. */
sealed interface TradePlannerSelectionCurrencyEvidence {
    data object SameCurrency : TradePlannerSelectionCurrencyEvidence

    data class CrossCurrency(
        val sourceId: String,
        val observedAtEpochMillis: Long
    ) : TradePlannerSelectionCurrencyEvidence
}

/** Ausschließlich aus vorhandenen Diagnosegruppengrößen abgeleitete Zähler. */
data class TradePlannerSelectionUiDiagnostics(
    val dataQualityBlockedCount: Int = 0,
    val calculationUnavailableCount: Int = 0,
    val notFreshCount: Int = 0,
    val sourceBlockedCount: Int = 0,
    val calculationFailedCount: Int = 0,
    val currencyConversionFailedCount: Int = 0,
    val invalidTargetLeveragePlanCount: Int = 0,
    val existingEntryFailedCount: Int = 0,
    val targetDeviationFailedCount: Int = 0,
    val nonMatchingTargetFitCount: Int = 0,
    val targetFitFailedCount: Int = 0
)

sealed interface TradePlannerSelectionUiResult {
    data class Selected(
        val primaryCandidate: TradePlannerSelectedProductUiModel,
        val alternativeCandidates: List<TradePlannerSelectedProductUiModel>,
        val diagnostics: TradePlannerSelectionUiDiagnostics
    ) : TradePlannerSelectionUiResult

    data class NoSelection(
        val reason: TradePlannerSelectionUiNoSelectionReason,
        val diagnostics: TradePlannerSelectionUiDiagnostics
    ) : TradePlannerSelectionUiResult

    data class InconsistentData(
        val error: TradePlannerSelectionUiMappingError
    ) : TradePlannerSelectionUiResult
}

enum class TradePlannerSelectionUiNoSelectionReason {
    NO_CATALOG_CANDIDATES,
    NO_BROKER_TRADABLE_CANDIDATES,
    NO_ENABLED_ISSUER_CANDIDATES,
    CATALOG_DATA_ACCESS_FAILURE,
    CATALOG_INVALID_DATA,
    BROKER_AVAILABILITY_DATA_ACCESS_FAILURE,
    BROKER_AVAILABILITY_INVALID_DATA,
    MARKET_DATA_NOT_FOUND,
    MARKET_DATA_DATA_ACCESS_FAILURE,
    MARKET_DATA_INVALID_DATA,
    NO_STRUCTURALLY_ELIGIBLE_CANDIDATES,
    NO_CALCULATION_AVAILABLE_CANDIDATES,
    NO_FRESH_CANDIDATES,
    NO_SOURCE_ALLOWED_CANDIDATES,
    NO_SUCCESSFUL_CALCULATION_CANDIDATES,
    NO_CURRENCY_CONVERTIBLE_CANDIDATES,
    NO_VALID_TARGET_LEVERAGE_PLAN_CANDIDATES,
    NO_SUCCESSFUL_EXISTING_ENTRY_CALCULATION_CANDIDATES,
    NO_SUCCESSFUL_TARGET_DEVIATION_CANDIDATES,
    NO_CANDIDATES_WITHIN_TARGET_TOLERANCES,
    EMPTY_MARKET_DATA_PIPELINE_INPUT,
    EMPTY_CURRENCY_CONVERSION_PIPELINE_INPUT,
    EMPTY_PLANNED_ENTRY_SELECTION_PIPELINE_INPUT,
    EMPTY_TARGET_SELECTION_PIPELINE_INPUT
}

enum class TradePlannerSelectionUiMappingError {
    CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT,
    CURRENCY_CONVERSION_STOPPED_WITH_SUCCESS_RESULT,
    SELECTED_CANDIDATE_EXISTING_ENTRY_NOT_SUCCESSFUL,
    SELECTED_CANDIDATE_TARGET_DEVIATION_NOT_SUCCESSFUL,
    SELECTED_CANDIDATE_TARGET_FIT_NOT_SUCCESSFUL,
    SELECTED_CANDIDATE_CURRENCY_EVIDENCE_NOT_FOUND,
    SELECTED_CANDIDATE_CURRENCY_EVIDENCE_AMBIGUOUS
}
