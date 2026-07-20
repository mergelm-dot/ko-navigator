package de.konavigator.app.application.marketdata

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.orchestration.MarketDataCalculationRequest

/**
 * Koordiniert einen Marktdatenberechnungsablauf sequenziell und Fail Fast.
 *
 * Zuerst wird die Produktspezifikation geladen. Nur nach deren erfolgreichem
 * Abruf werden die Marktdaten geladen, und nur nach zwei erfolgreichen
 * Repository-Zugriffen wird der fertig konfigurierte Orchestrator aufgerufen.
 *
 * Der Service enthält keine Domainregeln, liest keine Systemzeit und kennt
 * keine Netzwerk- oder Repository-Implementierungsdetails. Er besitzt keine
 * UI-, Android- oder Compose-Abhängigkeit. Sein Ergebnis ist weder eine
 * Anlageempfehlung noch eine allgemeine Zusage der Handelbarkeit.
 */
class MarketDataCalculationApplicationService(
    private val specificationRepository: KnockoutProductSpecificationRepository,
    private val marketDataRepository: KnockoutProductMarketDataRepository,
    private val orchestrator: MarketDataCalculationOrchestrator
) {
    suspend fun execute(
        request: MarketDataCalculationApplicationRequest
    ): MarketDataCalculationApplicationResult {
        val specification = when (
            val result = specificationRepository.findByProductIsin(request.productIsin)
        ) {
            is RepositoryResult.Success -> result.value
            RepositoryResult.NotFound -> {
                return MarketDataCalculationApplicationResult.DataUnavailable(
                    MarketDataCalculationApplicationError.PRODUCT_NOT_FOUND
                )
            }
            RepositoryResult.DataAccessFailure -> {
                return MarketDataCalculationApplicationResult.DataUnavailable(
                    MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE
                )
            }
        }

        val marketData = when (
            val result = marketDataRepository.findByProductIsin(request.productIsin)
        ) {
            is RepositoryResult.Success -> result.value
            RepositoryResult.NotFound -> {
                return MarketDataCalculationApplicationResult.DataUnavailable(
                    MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND
                )
            }
            RepositoryResult.DataAccessFailure -> {
                return MarketDataCalculationApplicationResult.DataUnavailable(
                    MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE
                )
            }
        }

        val domainRequest = MarketDataCalculationRequest(
            calculationType = request.calculationType,
            specification = specification,
            marketData = marketData,
            evaluationTimeEpochMillis = request.evaluationTimeEpochMillis
        )
        val result = orchestrator.calculate(domainRequest)

        return MarketDataCalculationApplicationResult.DomainEvaluated(
            domainResult = result
        )
    }
}
