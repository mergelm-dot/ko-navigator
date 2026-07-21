package de.konavigator.app.presentation.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationError
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue

/**
 * Typisierte Presentation-Ergebnisse mit rohen, ungerundeten Rechenwerten.
 *
 * Eingabefehler und Ladezustand liegen getrennt in der Submission. Ergebnisse
 * enthalten weder Domainobjekte noch UI-Texte, formatierte Zahlen oder
 * Throwables. Anzeigeformatierung bleibt Aufgabe eines späteren Screens.
 */
sealed interface MarketDataCalculationUiResult {

    data class PurchasePrice(
        val value: Double,
        val currency: String
    ) : MarketDataCalculationUiResult

    data class SalePrice(
        val value: Double,
        val currency: String
    ) : MarketDataCalculationUiResult

    data class Spread(
        val absoluteSpread: Double,
        val relativeSpreadToAskPercent: Double,
        val currency: String
    ) : MarketDataCalculationUiResult

    data class MidPrice(
        val value: Double,
        val currency: String
    ) : MarketDataCalculationUiResult

    data class Failure(
        val error: MarketDataCalculationUiError
    ) : MarketDataCalculationUiResult
}

enum class MarketDataCalculationUiError {
    PRODUCT_NOT_FOUND,
    MARKET_DATA_NOT_FOUND,
    DATA_ACCESS_FAILURE,
    INVALID_SPECIFICATION,
    INVALID_MARKET_DATA,
    INCOMPATIBLE_PRODUCT_DATA,
    REQUIRED_QUOTE_UNAVAILABLE,
    MARKET_DATA_NOT_FRESH,
    SOURCE_UNAVAILABLE,
    CALCULATION_FAILED,
    UNEXPECTED_FAILURE
}

internal fun MarketDataCalculationApplicationResult.toUiResult():
    MarketDataCalculationUiResult = when (this) {
    is MarketDataCalculationApplicationResult.DataUnavailable -> when (error) {
        MarketDataCalculationApplicationError.PRODUCT_NOT_FOUND ->
            MarketDataCalculationUiResult.Failure(
                MarketDataCalculationUiError.PRODUCT_NOT_FOUND
            )

        MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND ->
            MarketDataCalculationUiResult.Failure(
                MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND
            )

        MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE ->
            MarketDataCalculationUiResult.Failure(
                MarketDataCalculationUiError.DATA_ACCESS_FAILURE
            )
    }

    is MarketDataCalculationApplicationResult.DomainEvaluated ->
        domainResult.toUiResult()
}

private fun MarketDataCalculationOrchestrationResult.toUiResult():
    MarketDataCalculationUiResult = when (this) {
    is MarketDataCalculationOrchestrationResult.InvalidSpecification ->
        MarketDataCalculationUiResult.Failure(
            MarketDataCalculationUiError.INVALID_SPECIFICATION
        )

    is MarketDataCalculationOrchestrationResult.InvalidMarketData ->
        MarketDataCalculationUiResult.Failure(
            MarketDataCalculationUiError.INVALID_MARKET_DATA
        )

    is MarketDataCalculationOrchestrationResult.Incompatible ->
        MarketDataCalculationUiResult.Failure(
            MarketDataCalculationUiError.INCOMPATIBLE_PRODUCT_DATA
        )

    is MarketDataCalculationOrchestrationResult.StructurallyUnavailable ->
        MarketDataCalculationUiResult.Failure(
            MarketDataCalculationUiError.REQUIRED_QUOTE_UNAVAILABLE
        )

    is MarketDataCalculationOrchestrationResult.NotFresh ->
        MarketDataCalculationUiResult.Failure(
            MarketDataCalculationUiError.MARKET_DATA_NOT_FRESH
        )

    is MarketDataCalculationOrchestrationResult.SourceBlocked ->
        MarketDataCalculationUiResult.Failure(
            MarketDataCalculationUiError.SOURCE_UNAVAILABLE
        )

    is MarketDataCalculationOrchestrationResult.CalculationFailure ->
        MarketDataCalculationUiResult.Failure(
            MarketDataCalculationUiError.CALCULATION_FAILED
        )

    is MarketDataCalculationOrchestrationResult.Success -> value.toUiResult()
}

private fun MarketDataCalculationValue.toUiResult(): MarketDataCalculationUiResult =
    when (this) {
        is MarketDataCalculationValue.PurchasePrice ->
            MarketDataCalculationUiResult.PurchasePrice(
                value = value,
                currency = currency
            )

        is MarketDataCalculationValue.SalePrice ->
            MarketDataCalculationUiResult.SalePrice(
                value = value,
                currency = currency
            )

        is MarketDataCalculationValue.Spread ->
            MarketDataCalculationUiResult.Spread(
                absoluteSpread = absoluteSpread,
                relativeSpreadToAskPercent = relativeSpreadToAskPercent,
                currency = currency
            )

        is MarketDataCalculationValue.MidPrice ->
            MarketDataCalculationUiResult.MidPrice(
                value = value,
                currency = currency
            )
    }
