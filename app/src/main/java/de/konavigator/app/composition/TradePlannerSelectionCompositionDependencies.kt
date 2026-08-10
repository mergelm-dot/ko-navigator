package de.konavigator.app.composition

import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionEvaluationTimeProvider
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionExecutionSettings

/**
 * Alle externen Adapter und explizit konfigurierten Policies des parallelen
 * Selection-Objektgraphen. Die Composition erzeugt weder Datenadapter noch
 * Freshness- oder Source-Regeln selbst.
 */
data class TradePlannerSelectionCompositionDependencies(
    val specificationCatalogRepository: KnockoutProductSpecificationCatalogRepository,
    val brokerAvailabilityRepository: KnockoutProductBrokerAvailabilityRepository,
    val marketDataRepository: KnockoutProductMarketDataRepository,
    val fxRateProvider: FxRateProvider,
    val freshnessPolicy: MarketDataFreshnessPolicy,
    val sourcePolicy: MarketDataSourcePolicy,
    val executionSettings: TradePlannerSelectionExecutionSettings,
    val evaluationTimeProvider: TradePlannerSelectionEvaluationTimeProvider
)
