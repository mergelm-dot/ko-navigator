package de.konavigator.app.presentation.tradeplanner

import androidx.annotation.StringRes
import de.konavigator.app.R

/** Zentrale, ausschließlich technische Zuordnung der Selection-Ergebnisse zu UI-Texten. */
object TradePlannerSelectionUiText {

    @StringRes
    fun inputErrorResource(error: TradePlannerSelectionUiInputError): Int = when (error) {
        TradePlannerSelectionUiInputError.UNDERLYING_REQUIRED ->
            R.string.trade_planner_selection_error_underlying_required

        TradePlannerSelectionUiInputError.BROKER_REQUIRED ->
            R.string.trade_planner_selection_error_broker_required

        TradePlannerSelectionUiInputError.CURRENT_PRICE_REQUIRED ->
            R.string.trade_planner_selection_error_current_price_required

        TradePlannerSelectionUiInputError.CURRENT_PRICE_INVALID ->
            R.string.trade_planner_selection_error_current_price_invalid

        TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_REQUIRED ->
            R.string.trade_planner_selection_error_planned_entry_price_required

        TradePlannerSelectionUiInputError.PLANNED_ENTRY_PRICE_INVALID ->
            R.string.trade_planner_selection_error_planned_entry_price_invalid

        TradePlannerSelectionUiInputError.TARGET_LEVERAGE_REQUIRED ->
            R.string.trade_planner_selection_error_target_leverage_required

        TradePlannerSelectionUiInputError.TARGET_LEVERAGE_INVALID ->
            R.string.trade_planner_selection_error_target_leverage_invalid
    }

    @StringRes
    fun noSelectionReasonResource(reason: TradePlannerSelectionUiNoSelectionReason): Int = when (reason) {
        TradePlannerSelectionUiNoSelectionReason.NO_CATALOG_CANDIDATES ->
            R.string.trade_planner_selection_no_catalog_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_BROKER_TRADABLE_CANDIDATES ->
            R.string.trade_planner_selection_no_broker_tradable_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_ENABLED_ISSUER_CANDIDATES ->
            R.string.trade_planner_selection_no_enabled_issuer_candidates

        TradePlannerSelectionUiNoSelectionReason.CATALOG_DATA_ACCESS_FAILURE ->
            R.string.trade_planner_selection_catalog_data_access_failure

        TradePlannerSelectionUiNoSelectionReason.CATALOG_INVALID_DATA ->
            R.string.trade_planner_selection_catalog_invalid_data

        TradePlannerSelectionUiNoSelectionReason.BROKER_AVAILABILITY_DATA_ACCESS_FAILURE ->
            R.string.trade_planner_selection_broker_availability_data_access_failure

        TradePlannerSelectionUiNoSelectionReason.BROKER_AVAILABILITY_INVALID_DATA ->
            R.string.trade_planner_selection_broker_availability_invalid_data

        TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_NOT_FOUND ->
            R.string.trade_planner_selection_market_data_not_found

        TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_DATA_ACCESS_FAILURE ->
            R.string.trade_planner_selection_market_data_data_access_failure

        TradePlannerSelectionUiNoSelectionReason.MARKET_DATA_INVALID_DATA ->
            R.string.trade_planner_selection_market_data_invalid_data

        TradePlannerSelectionUiNoSelectionReason.NO_STRUCTURALLY_ELIGIBLE_CANDIDATES ->
            R.string.trade_planner_selection_no_structurally_eligible_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_CALCULATION_AVAILABLE_CANDIDATES ->
            R.string.trade_planner_selection_no_calculation_available_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_FRESH_CANDIDATES ->
            R.string.trade_planner_selection_no_fresh_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_SOURCE_ALLOWED_CANDIDATES ->
            R.string.trade_planner_selection_no_source_allowed_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_SUCCESSFUL_CALCULATION_CANDIDATES ->
            R.string.trade_planner_selection_no_successful_calculation_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_CURRENCY_CONVERTIBLE_CANDIDATES ->
            R.string.trade_planner_selection_no_currency_convertible_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_VALID_TARGET_LEVERAGE_PLAN_CANDIDATES ->
            R.string.trade_planner_selection_no_valid_target_leverage_plan_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_SUCCESSFUL_EXISTING_ENTRY_CALCULATION_CANDIDATES ->
            R.string.trade_planner_selection_no_successful_existing_entry_calculation_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_SUCCESSFUL_TARGET_DEVIATION_CANDIDATES ->
            R.string.trade_planner_selection_no_successful_target_deviation_candidates

        TradePlannerSelectionUiNoSelectionReason.NO_CANDIDATES_WITHIN_TARGET_TOLERANCES ->
            R.string.trade_planner_selection_no_candidates_within_target_tolerances

        TradePlannerSelectionUiNoSelectionReason.EMPTY_MARKET_DATA_PIPELINE_INPUT,
        TradePlannerSelectionUiNoSelectionReason.EMPTY_CURRENCY_CONVERSION_PIPELINE_INPUT,
        TradePlannerSelectionUiNoSelectionReason.EMPTY_PLANNED_ENTRY_SELECTION_PIPELINE_INPUT,
        TradePlannerSelectionUiNoSelectionReason.EMPTY_TARGET_SELECTION_PIPELINE_INPUT ->
            R.string.trade_planner_selection_empty_pipeline_input
    }

    @StringRes
    fun mappingErrorResource(error: TradePlannerSelectionUiMappingError): Int = when (error) {
        TradePlannerSelectionUiMappingError.CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT ->
            R.string.trade_planner_selection_inconsistent_calculation_pipeline

        TradePlannerSelectionUiMappingError.CURRENCY_CONVERSION_STOPPED_WITH_SUCCESS_RESULT ->
            R.string.trade_planner_selection_inconsistent_currency_conversion

        TradePlannerSelectionUiMappingError.SELECTED_CANDIDATE_EXISTING_ENTRY_NOT_SUCCESSFUL ->
            R.string.trade_planner_selection_inconsistent_existing_entry

        TradePlannerSelectionUiMappingError.SELECTED_CANDIDATE_TARGET_DEVIATION_NOT_SUCCESSFUL ->
            R.string.trade_planner_selection_inconsistent_target_deviation

        TradePlannerSelectionUiMappingError.SELECTED_CANDIDATE_TARGET_FIT_NOT_SUCCESSFUL ->
            R.string.trade_planner_selection_inconsistent_target_fit

        TradePlannerSelectionUiMappingError.SELECTED_CANDIDATE_CURRENCY_EVIDENCE_NOT_FOUND ->
            R.string.trade_planner_selection_inconsistent_currency_evidence_missing

        TradePlannerSelectionUiMappingError.SELECTED_CANDIDATE_CURRENCY_EVIDENCE_AMBIGUOUS ->
            R.string.trade_planner_selection_inconsistent_currency_evidence_ambiguous
    }
}
