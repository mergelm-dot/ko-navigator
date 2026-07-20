package de.konavigator.app.domain.calculator

sealed interface MarketDataCalculationResult {

    data class Success(
        val value: Double
    ) : MarketDataCalculationResult

    data class Failure(
        val error: MarketDataCalculationError
    ) : MarketDataCalculationResult
}
