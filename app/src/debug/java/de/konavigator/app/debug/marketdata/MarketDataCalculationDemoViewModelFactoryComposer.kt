package de.konavigator.app.debug.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory

/**
 * Providerneutrale Abschlussverdrahtung ausschliesslich fuer lokale Debug- und
 * Demo-Compositions.
 *
 * Der Composer verbindet bereits erzeugte Produktspezifikations- und
 * Produktmarktdaten-Repositorys mit den bestehenden Policies, dem Orchestrator, dem
 * Application-Service und der ViewModel-Factory. Fuer die uebergebene Marktdatenquelle werden
 * exakt `PURCHASE_PRICE`, `SALE_PRICE`, `SPREAD` und `MID` unterstuetzt.
 *
 * Die Quellenkennung wird weder validiert noch normalisiert. Der Composer veraendert keine
 * Repository-Ergebnisse und fuehrt keine Repository-Abfragen aus. Er enthaelt keine Datei-,
 * Netzwerk-, Provider-, Mapper- oder Berechnungslogik, keine Systemzeit oder Zeitumrechnung
 * und trifft keine zusaetzliche Data-Quality- oder Freshness-Entscheidung. Dieser Baustein ist
 * kein Bestandteil des Release-Source-Sets.
 */
object MarketDataCalculationDemoViewModelFactoryComposer {

    fun create(
        specificationRepository: KnockoutProductSpecificationRepository,
        marketDataRepository: KnockoutProductMarketDataRepository,
        freshnessThresholds: MarketDataFreshnessThresholds,
        marketDataSourceId: String
    ): MarketDataCalculationViewModelFactory {
        val freshnessPolicy = MarketDataFreshnessPolicy(freshnessThresholds)
        val sourcePolicy = MarketDataSourcePolicy(
            MarketDataSourcePolicyConfig(
                rules = listOf(
                    MarketDataSourceRule(
                        sourceId = marketDataSourceId,
                        supportedCalculationTypes = setOf(
                            MarketDataCalculationType.PURCHASE_PRICE,
                            MarketDataCalculationType.SALE_PRICE,
                            MarketDataCalculationType.SPREAD,
                            MarketDataCalculationType.MID
                        )
                    )
                )
            )
        )
        val orchestrator = MarketDataCalculationOrchestrator(
            freshnessPolicy = freshnessPolicy,
            sourcePolicy = sourcePolicy
        )
        val applicationService = MarketDataCalculationApplicationService(
            specificationRepository = specificationRepository,
            marketDataRepository = marketDataRepository,
            orchestrator = orchestrator
        )

        return MarketDataCalculationViewModelFactory(applicationService)
    }
}
