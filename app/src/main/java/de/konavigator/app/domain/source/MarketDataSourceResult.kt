package de.konavigator.app.domain.source

sealed interface MarketDataSourceResult {
    data object Allowed : MarketDataSourceResult

    data class Blocked(
        val error: MarketDataSourceError
    ) : MarketDataSourceResult
}
