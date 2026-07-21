package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.domain.model.TradeDirection

/**
 * Unveränderlicher Presentation-Zustand der theoretischen Trade-Planung.
 *
 * Die drei Eingaben bleiben bis zum expliziten Berechnungsaufruf unverändert
 * in ihrer UI-Form erhalten. Der Vertrag enthält weder UI-Texte noch Android-
 * oder Compose-Typen.
 */
data class TradePlannerUiState(
    val currentUnderlyingPriceInput: String = "100,00",
    val plannedEntryPriceInput: String = "95,00",
    val targetLeverageInput: String = "3",
    val direction: TradeDirection = TradeDirection.LONG,
    val submission: TradePlannerUiSubmission =
        TradePlannerUiSubmission.Idle
)

/**
 * Trennt den Ruhezustand, UI-nahe Eingabefehler und ein abgeschlossenes
 * synchrones Ergebnis. Ein Loading-Zustand ist für diesen Pfad nicht nötig.
 */
sealed interface TradePlannerUiSubmission {

    data object Idle : TradePlannerUiSubmission

    data class InvalidInput(
        val errors: List<TradePlannerUiInputError>
    ) : TradePlannerUiSubmission

    data class Completed(
        val result: TradePlannerUiResult
    ) : TradePlannerUiSubmission
}

/** Maschinenlesbare Presentation-Fehler ohne Benutzertexte. */
enum class TradePlannerUiInputError {
    CURRENT_PRICE_REQUIRED,
    CURRENT_PRICE_INVALID,
    PLANNED_ENTRY_PRICE_REQUIRED,
    PLANNED_ENTRY_PRICE_INVALID,
    TARGET_LEVERAGE_REQUIRED,
    TARGET_LEVERAGE_INVALID
}
