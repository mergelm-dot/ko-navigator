package de.konavigator.app.application.marketdata

import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult

/**
 * Trennt technische Datenverfügbarkeit von der fachlichen Domainauswertung.
 *
 * Ein Domainresult wird unverändert eingebettet. Der Vertrag enthält keine
 * UI-Texte oder Infrastrukturdetails.
 */
sealed interface MarketDataCalculationApplicationResult {

    data class DataUnavailable(
        val error: MarketDataCalculationApplicationError
    ) : MarketDataCalculationApplicationResult

    data class DomainEvaluated(
        val domainResult: MarketDataCalculationOrchestrationResult
    ) : MarketDataCalculationApplicationResult
}
