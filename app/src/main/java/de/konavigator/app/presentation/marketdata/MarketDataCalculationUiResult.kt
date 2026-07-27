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

    val dataQuality: MarketDataCalculationUiDataQuality?

    data class PurchasePrice(
        val value: Double,
        val currency: String,
        override val dataQuality: MarketDataCalculationUiDataQuality
    ) : MarketDataCalculationUiResult

    data class SalePrice(
        val value: Double,
        val currency: String,
        override val dataQuality: MarketDataCalculationUiDataQuality
    ) : MarketDataCalculationUiResult

    data class Spread(
        val absoluteSpread: Double,
        val relativeSpreadToAskPercent: Double,
        val currency: String,
        override val dataQuality: MarketDataCalculationUiDataQuality
    ) : MarketDataCalculationUiResult

    data class MidPrice(
        val value: Double,
        val currency: String,
        override val dataQuality: MarketDataCalculationUiDataQuality
    ) : MarketDataCalculationUiResult

    data class Failure(
        val error: MarketDataCalculationUiError,
        override val dataQuality: MarketDataCalculationUiDataQuality?
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
                error = MarketDataCalculationUiError.PRODUCT_NOT_FOUND,
                dataQuality = null
            )

        MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND ->
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND,
                dataQuality = null
            )

        MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE ->
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.DATA_ACCESS_FAILURE,
                dataQuality = null
            )

        MarketDataCalculationApplicationError.INVALID_PRODUCT_SPECIFICATION ->
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.INVALID_SPECIFICATION,
                dataQuality = null
            )

        MarketDataCalculationApplicationError.INVALID_PRODUCT_MARKET_DATA ->
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.INVALID_MARKET_DATA,
                dataQuality = null
            )
    }

    is MarketDataCalculationApplicationResult.DomainEvaluated ->
        domainResult.toUiResult()
}

private fun MarketDataCalculationOrchestrationResult.toUiResult():
    MarketDataCalculationUiResult {
    val dataQuality = dataQualityAssessment.toUiDataQuality()
    return when (this) {
    is MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked ->
        MarketDataCalculationUiResult.Failure(
            error = MarketDataCalculationUiError.INVALID_MARKET_DATA,
            dataQuality = dataQuality
        )

    is MarketDataCalculationOrchestrationResult.StructurallyUnavailable ->
        MarketDataCalculationUiResult.Failure(
            error = MarketDataCalculationUiError.REQUIRED_QUOTE_UNAVAILABLE,
            dataQuality = dataQuality
        )

    is MarketDataCalculationOrchestrationResult.NotFresh ->
        MarketDataCalculationUiResult.Failure(
            error = MarketDataCalculationUiError.MARKET_DATA_NOT_FRESH,
            dataQuality = dataQuality
        )

    is MarketDataCalculationOrchestrationResult.SourceBlocked ->
        MarketDataCalculationUiResult.Failure(
            error = MarketDataCalculationUiError.SOURCE_UNAVAILABLE,
            dataQuality = dataQuality
        )

    is MarketDataCalculationOrchestrationResult.CalculationFailure ->
        MarketDataCalculationUiResult.Failure(
            error = MarketDataCalculationUiError.CALCULATION_FAILED,
            dataQuality = dataQuality
        )

    is MarketDataCalculationOrchestrationResult.Success ->
        value.toUiResult(dataQuality)
    }
}

private fun MarketDataCalculationValue.toUiResult(
    dataQuality: MarketDataCalculationUiDataQuality
): MarketDataCalculationUiResult =
    when (this) {
        is MarketDataCalculationValue.PurchasePrice ->
            MarketDataCalculationUiResult.PurchasePrice(
                value = value,
                currency = currency,
                dataQuality = dataQuality
            )

        is MarketDataCalculationValue.SalePrice ->
            MarketDataCalculationUiResult.SalePrice(
                value = value,
                currency = currency,
                dataQuality = dataQuality
            )

        is MarketDataCalculationValue.Spread ->
            MarketDataCalculationUiResult.Spread(
                absoluteSpread = absoluteSpread,
                relativeSpreadToAskPercent = relativeSpreadToAskPercent,
                currency = currency,
                dataQuality = dataQuality
            )

        is MarketDataCalculationValue.MidPrice ->
            MarketDataCalculationUiResult.MidPrice(
                value = value,
                currency = currency,
                dataQuality = dataQuality
            )
    }
