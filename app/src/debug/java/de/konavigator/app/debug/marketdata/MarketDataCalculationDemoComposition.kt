package de.konavigator.app.debug.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.data.inmemory.InMemoryKnockoutProductMarketDataRepository
import de.konavigator.app.data.inmemory.InMemoryKnockoutProductSpecificationRepository
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory

/**
 * Erzeugt einen vollständig neuen, deterministischen Objektgraphen für die
 * Debug-Engine-Demo. Alle Daten sind lokale, neutrale Testdaten.
 */
object MarketDataCalculationDemoComposition {

    fun createFactory(): MarketDataCalculationViewModelFactory {
        val specification = KnockoutProductSpecification(
            productIsin = "DE000DEMO001",
            productWkn = null,
            issuerId = "demo-issuer",
            underlyingId = "demo-underlying",
            direction = TradeDirection.LONG,
            basePrice = 80.0,
            knockoutBarrier = 82.0,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        )
        val marketData = KnockoutProductMarketData(
            productIsin = "DE000DEMO001",
            bid = 1.8,
            ask = 2.0,
            bidTimestampEpochMillis = 1_700_000_000_000L,
            askTimestampEpochMillis = 1_700_000_000_000L,
            currency = "EUR",
            sourceId = "demo-source"
        )
        val specificationRepository =
            InMemoryKnockoutProductSpecificationRepository(listOf(specification))
        val marketDataRepository =
            InMemoryKnockoutProductMarketDataRepository(listOf(marketData))
        val freshnessPolicy = MarketDataFreshnessPolicy(
            MarketDataFreshnessThresholds(
                maxBidAgeMillis = 0L,
                maxAskAgeMillis = 0L,
                maxBidAskDifferenceMillis = 0L,
                allowedFutureSkewMillis = 0L
            )
        )
        val sourcePolicy = MarketDataSourcePolicy(
            MarketDataSourcePolicyConfig(
                listOf(
                    MarketDataSourceRule(
                        sourceId = "demo-source",
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
