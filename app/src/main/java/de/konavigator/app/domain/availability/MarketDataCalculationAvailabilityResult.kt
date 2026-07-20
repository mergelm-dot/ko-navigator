package de.konavigator.app.domain.availability

sealed interface MarketDataCalculationAvailabilityResult {

    data object StructurallyAvailable :
        MarketDataCalculationAvailabilityResult

    data class StructurallyUnavailable(
        val errors: List<MarketDataCalculationAvailabilityError>
    ) : MarketDataCalculationAvailabilityResult
}
