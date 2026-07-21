package de.konavigator.app.presentation.marketdata

import de.konavigator.app.domain.availability.MarketDataCalculationType

/**
 * Unveränderlicher Presentation-Zustand der Marktdatenberechnung.
 *
 * Die Eingaben bleiben exakt in ihrer UI-Form erhalten. Der Bewertungszeitpunkt
 * wird ausdrücklich eingegeben; es gibt keinen Systemzeit-Default. Die
 * Button-Aktivierung wird ausschließlich aus Pflichtfeldern, Long-Parsebarkeit
 * und Ladezustand abgeleitet und stellt keine Domainvalidierung dar.
 */
data class MarketDataCalculationUiState(
    val productIsinInput: String = "",
    val selectedCalculationType: MarketDataCalculationType =
        MarketDataCalculationType.PURCHASE_PRICE,
    val evaluationTimeEpochMillisInput: String = "",
    val submission: MarketDataCalculationUiSubmission =
        MarketDataCalculationUiSubmission.Idle
) {
    val isCalculateEnabled: Boolean
        get() =
            productIsinInput.isNotEmpty() &&
                evaluationTimeEpochMillisInput.toLongOrNull() != null &&
                submission !is MarketDataCalculationUiSubmission.Loading
}

/**
 * Typisierte Trennung von Ruhezustand, laufender Auswertung, UI-Eingabefehlern
 * und abgeschlossenem Ergebnis. Der Vertrag enthält weder UI-Texte noch
 * Throwables oder einen zusätzlich gespeicherten Ladeindikator.
 */
sealed interface MarketDataCalculationUiSubmission {

    data object Idle : MarketDataCalculationUiSubmission

    data object Loading : MarketDataCalculationUiSubmission

    data class InvalidInput(
        val errors: List<MarketDataCalculationUiInputError>
    ) : MarketDataCalculationUiSubmission

    data class Completed(
        val result: MarketDataCalculationUiResult
    ) : MarketDataCalculationUiSubmission
}

enum class MarketDataCalculationUiInputError {
    PRODUCT_ISIN_REQUIRED,
    EVALUATION_TIME_REQUIRED,
    EVALUATION_TIME_INVALID
}
