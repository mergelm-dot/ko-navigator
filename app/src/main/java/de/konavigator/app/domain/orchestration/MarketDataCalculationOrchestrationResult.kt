package de.konavigator.app.domain.orchestration

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.source.MarketDataSourceError

sealed interface MarketDataCalculationOrchestrationResult {

    val dataQualityAssessment: DataQualityAssessment

    data class StructuralDataQualityBlocked(
        override val dataQualityAssessment: DataQualityAssessment
    ) : MarketDataCalculationOrchestrationResult {
        init {
            require(dataQualityAssessment.status == DataQualityStatus.BLOCKED) {
                "StructuralDataQualityBlocked requires a BLOCKED assessment"
            }
        }
    }

    data class StructurallyUnavailable(
        val errors: List<MarketDataCalculationAvailabilityError>,
        override val dataQualityAssessment: DataQualityAssessment
    ) : MarketDataCalculationOrchestrationResult {
        init {
            requireNonBlocking(dataQualityAssessment)
        }
    }

    data class NotFresh(
        val errors: List<MarketDataFreshnessError>,
        override val dataQualityAssessment: DataQualityAssessment
    ) : MarketDataCalculationOrchestrationResult {
        init {
            requireNonBlocking(dataQualityAssessment)
        }
    }

    data class SourceBlocked(
        val error: MarketDataSourceError,
        override val dataQualityAssessment: DataQualityAssessment
    ) : MarketDataCalculationOrchestrationResult {
        init {
            requireNonBlocking(dataQualityAssessment)
        }
    }

    data class CalculationFailure(
        val error: MarketDataCalculationError,
        override val dataQualityAssessment: DataQualityAssessment
    ) : MarketDataCalculationOrchestrationResult {
        init {
            requireNonBlocking(dataQualityAssessment)
        }
    }

    data class Success(
        val value: MarketDataCalculationValue,
        override val dataQualityAssessment: DataQualityAssessment
    ) : MarketDataCalculationOrchestrationResult {
        init {
            requireNonBlocking(dataQualityAssessment)
        }
    }
}

private fun requireNonBlocking(dataQualityAssessment: DataQualityAssessment) {
    require(dataQualityAssessment.status != DataQualityStatus.BLOCKED) {
        "Only StructuralDataQualityBlocked may carry a BLOCKED assessment"
    }
}
