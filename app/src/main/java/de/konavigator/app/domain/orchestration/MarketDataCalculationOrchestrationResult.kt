package de.konavigator.app.domain.orchestration

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.source.MarketDataSourceError
import de.konavigator.app.domain.validation.KnockoutProductCompatibilityError
import de.konavigator.app.domain.validation.KnockoutProductMarketDataValidationError
import de.konavigator.app.domain.validation.KnockoutProductValidationError

sealed interface MarketDataCalculationOrchestrationResult {

    data class InvalidSpecification(
        val errors: List<KnockoutProductValidationError>
    ) : MarketDataCalculationOrchestrationResult

    data class InvalidMarketData(
        val errors: List<KnockoutProductMarketDataValidationError>
    ) : MarketDataCalculationOrchestrationResult

    data class Incompatible(
        val errors: List<KnockoutProductCompatibilityError>
    ) : MarketDataCalculationOrchestrationResult

    data class StructurallyUnavailable(
        val errors: List<MarketDataCalculationAvailabilityError>
    ) : MarketDataCalculationOrchestrationResult

    data class NotFresh(
        val errors: List<MarketDataFreshnessError>
    ) : MarketDataCalculationOrchestrationResult

    data class SourceBlocked(
        val error: MarketDataSourceError
    ) : MarketDataCalculationOrchestrationResult

    data class CalculationFailure(
        val error: MarketDataCalculationError
    ) : MarketDataCalculationOrchestrationResult

    data class Success(
        val value: MarketDataCalculationValue
    ) : MarketDataCalculationOrchestrationResult
}
