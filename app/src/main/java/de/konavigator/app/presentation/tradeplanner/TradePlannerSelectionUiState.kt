package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.domain.model.TradeDirection

/** Unveränderlicher State des vollständigen providerneutralen Selection-Pfads. */
data class TradePlannerSelectionUiState(
    val selectedUnderlyingId: String? = null,
    val selectedBrokerId: String? = null,
    val enabledIssuerIds: Set<String> = emptySet(),
    val currentUnderlyingPriceInput: String = "100,00",
    val plannedEntryPriceInput: String = "95,00",
    val targetLeverageInput: String = "3",
    val direction: TradeDirection = TradeDirection.LONG,
    val submission: TradePlannerSelectionUiSubmission =
        TradePlannerSelectionUiSubmission.Idle
)

sealed interface TradePlannerSelectionUiSubmission {
    data object Idle : TradePlannerSelectionUiSubmission

    data class InvalidInput(
        val errors: List<TradePlannerSelectionUiInputError>
    ) : TradePlannerSelectionUiSubmission

    data object Loading : TradePlannerSelectionUiSubmission

    data class Completed(
        val result: TradePlannerSelectionUiResult
    ) : TradePlannerSelectionUiSubmission
}

/** Maschinenlesbare Presentation-Eingabefehler ohne Benutzertexte. */
enum class TradePlannerSelectionUiInputError {
    UNDERLYING_REQUIRED,
    BROKER_REQUIRED,
    CURRENT_PRICE_REQUIRED,
    CURRENT_PRICE_INVALID,
    PLANNED_ENTRY_PRICE_REQUIRED,
    PLANNED_ENTRY_PRICE_INVALID,
    TARGET_LEVERAGE_REQUIRED,
    TARGET_LEVERAGE_INVALID
}
