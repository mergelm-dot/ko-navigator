package de.konavigator.app.composition

import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationAvailabilityApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationAvailabilityGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCalculationPipelineApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateCurrencyConversionApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateDataQualityApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateDataQualityGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateExistingEntryCalculationApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateExistingEntryCalculationGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateFreshnessApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateFreshnessGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateMarketDataApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidatePlannedEntrySelectionApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSelectionPipelineApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSourceEvaluationApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateSourceGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetDeviationApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetDeviationGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetFitApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetFitGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetFitRanker
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetFitSelector
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetLeverageApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetLeverageGate
import de.konavigator.app.application.productdiscovery.KnockoutProductCandidateTargetSelectionApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductDiscoveryApplicationService
import de.konavigator.app.application.productdiscovery.KnockoutProductIssuerSelectionFilter
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitCalculator
import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.domain.currency.CurrencyConversionPolicy
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionExecutor
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionViewModelFactory

/**
 * Android-freier Aufbau des vollständigen providerneutralen Selection-Pfads.
 * Externe Datenadapter, Policies und Ausführungsparameter bleiben explizit
 * injiziert; diese Composition enthält keine Produkt- oder Providerdaten.
 */
object TradePlannerSelectionComposition {

    fun createViewModelFactory(
        dependencies: TradePlannerSelectionCompositionDependencies
    ): TradePlannerSelectionViewModelFactory {
        val discoveryApplicationService = KnockoutProductDiscoveryApplicationService(
            catalogRepository = dependencies.specificationCatalogRepository,
            brokerAvailabilityRepository = dependencies.brokerAvailabilityRepository,
            issuerSelectionFilter = KnockoutProductIssuerSelectionFilter()
        )
        val calculationPipelineApplicationService =
            KnockoutProductCandidateCalculationPipelineApplicationService(
                discoveryApplicationService = discoveryApplicationService,
                marketDataApplicationService =
                    KnockoutProductCandidateMarketDataApplicationService(
                        marketDataRepository = dependencies.marketDataRepository
                    ),
                dataQualityApplicationService =
                    KnockoutProductCandidateDataQualityApplicationService(),
                dataQualityGate = KnockoutProductCandidateDataQualityGate(),
                calculationAvailabilityApplicationService =
                    KnockoutProductCandidateCalculationAvailabilityApplicationService(),
                calculationAvailabilityGate =
                    KnockoutProductCandidateCalculationAvailabilityGate(),
                freshnessApplicationService =
                    KnockoutProductCandidateFreshnessApplicationService(
                        freshnessPolicy = dependencies.freshnessPolicy
                    ),
                freshnessGate = KnockoutProductCandidateFreshnessGate(),
                sourceEvaluationApplicationService =
                    KnockoutProductCandidateSourceEvaluationApplicationService(
                        sourcePolicy = dependencies.sourcePolicy
                    ),
                sourceGate = KnockoutProductCandidateSourceGate(),
                calculationApplicationService =
                    KnockoutProductCandidateCalculationApplicationService(),
                calculationGate = KnockoutProductCandidateCalculationGate()
            )
        val targetSelectionApplicationService =
            KnockoutProductCandidateTargetSelectionApplicationService(
                targetDeviationApplicationService =
                    KnockoutProductCandidateTargetDeviationApplicationService(
                        existingKnockoutProductTargetDeviationCalculator =
                            ExistingKnockoutProductTargetDeviationCalculator
                    ),
                targetDeviationGate = KnockoutProductCandidateTargetDeviationGate(),
                targetFitApplicationService =
                    KnockoutProductCandidateTargetFitApplicationService(
                        existingKnockoutProductTargetFitCalculator =
                            ExistingKnockoutProductTargetFitCalculator
                    ),
                targetFitGate = KnockoutProductCandidateTargetFitGate(),
                targetFitRanker = KnockoutProductCandidateTargetFitRanker(),
                targetFitSelector = KnockoutProductCandidateTargetFitSelector()
            )
        val plannedEntrySelectionApplicationService =
            KnockoutProductCandidatePlannedEntrySelectionApplicationService(
                targetLeverageApplicationService =
                    KnockoutProductCandidateTargetLeverageApplicationService(
                        tradeCalculationEngine = TradeCalculationEngine
                    ),
                targetLeverageGate = KnockoutProductCandidateTargetLeverageGate(),
                existingEntryCalculationApplicationService =
                    KnockoutProductCandidateExistingEntryCalculationApplicationService(
                        existingKnockoutProductEntryCalculator =
                            ExistingKnockoutProductEntryCalculator
                    ),
                existingEntryCalculationGate =
                    KnockoutProductCandidateExistingEntryCalculationGate(),
                targetSelectionApplicationService = targetSelectionApplicationService
            )
        val selectionPipelineApplicationService =
            KnockoutProductCandidateSelectionPipelineApplicationService(
                calculationPipelineApplicationService = calculationPipelineApplicationService,
                currencyConversionApplicationService =
                    KnockoutProductCandidateCurrencyConversionApplicationService(
                        fxRateProvider = dependencies.fxRateProvider,
                        currencyConversionPolicy = CurrencyConversionPolicy()
                    ),
                plannedEntrySelectionApplicationService =
                    plannedEntrySelectionApplicationService
            )
        val selectionExecutor = TradePlannerSelectionExecutor { request ->
            selectionPipelineApplicationService.execute(request)
        }

        return TradePlannerSelectionViewModelFactory(
            selectionExecutor = selectionExecutor,
            executionSettings = dependencies.executionSettings,
            evaluationTimeProvider = dependencies.evaluationTimeProvider
        )
    }
}
