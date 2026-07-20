package de.konavigator.app.domain.freshness

sealed interface MarketDataFreshnessResult {
    data object Fresh : MarketDataFreshnessResult

    data class NotFresh(
        val errors: List<MarketDataFreshnessError>
    ) : MarketDataFreshnessResult
}
