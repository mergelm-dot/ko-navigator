package de.konavigator.app.domain.orchestration

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityEvaluator
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.calculator.MarketDataCalculationResult
import de.konavigator.app.domain.calculator.MarketDataCalculator
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourceResult
import de.konavigator.app.domain.validation.KnockoutProductMarketDataCompatibilityValidator
import de.konavigator.app.domain.validation.KnockoutProductMarketDataValidator
import de.konavigator.app.domain.validation.KnockoutProductSpecificationValidator

/**
 * Reine Domain-Orchestrierung für genau einen Marktdaten-CalculationType.
 *
 * Die feste Fail-Fast-Reihenfolge lautet Spezifikationsvalidierung,
 * Marktdatenvalidierung, Kompatibilität, strukturelle Availability, Freshness,
 * Quellenfreigabe und erst danach Berechnung beziehungsweise Quote-Auswahl.
 * Bestehende Komponenten bleiben dabei die alleinige Quelle ihrer Regeln und
 * ihre Fehlercodes werden unverändert weitergegeben.
 *
 * Nur [freshnessPolicy] und [sourcePolicy] sind konfigurierbare Abhängigkeiten.
 * Die Orchestrierung liest keine Systemzeit, greift nicht auf Netzwerk oder
 * Repositories zu und kennt weder UI-Texte noch Android- oder Compose-Typen.
 * Purchase und Sale übernehmen freigegebene Quotes ohne Arithmetik. Spread
 * verwendet beide bestehenden Spread-Funktionen des [MarketDataCalculator],
 * Mid dessen bestehende Mid-Funktion.
 *
 * Ein erfolgreiches Ergebnis ist eine fachlich geprüfte Berechnungshilfe. Es
 * ist weder eine Kauf- oder Verkaufsempfehlung noch eine allgemeine Zusage der
 * Handelbarkeit des Produkts.
 */
class MarketDataCalculationOrchestrator(
    private val freshnessPolicy: MarketDataFreshnessPolicy,
    private val sourcePolicy: MarketDataSourcePolicy
) {
    fun calculate(
        request: MarketDataCalculationRequest
    ): MarketDataCalculationOrchestrationResult {
        val specificationErrors = KnockoutProductSpecificationValidator.validate(
            request.specification
        )
        if (specificationErrors.isNotEmpty()) {
            return MarketDataCalculationOrchestrationResult.InvalidSpecification(
                errors = specificationErrors
            )
        }

        val marketDataErrors = KnockoutProductMarketDataValidator.validate(
            request.marketData
        )
        if (marketDataErrors.isNotEmpty()) {
            return MarketDataCalculationOrchestrationResult.InvalidMarketData(
                errors = marketDataErrors
            )
        }

        val compatibilityErrors = KnockoutProductMarketDataCompatibilityValidator.validate(
            specification = request.specification,
            marketData = request.marketData
        )
        if (compatibilityErrors.isNotEmpty()) {
            return MarketDataCalculationOrchestrationResult.Incompatible(
                errors = compatibilityErrors
            )
        }

        when (
            val availability = MarketDataCalculationAvailabilityEvaluator.evaluate(
                calculationType = request.calculationType,
                marketData = request.marketData
            )
        ) {
            MarketDataCalculationAvailabilityResult.StructurallyAvailable -> Unit
            is MarketDataCalculationAvailabilityResult.StructurallyUnavailable -> {
                return MarketDataCalculationOrchestrationResult.StructurallyUnavailable(
                    errors = availability.errors
                )
            }
        }

        when (
            val freshness = freshnessPolicy.evaluate(
                calculationType = request.calculationType,
                marketData = request.marketData,
                evaluationTimeEpochMillis = request.evaluationTimeEpochMillis
            )
        ) {
            MarketDataFreshnessResult.Fresh -> Unit
            is MarketDataFreshnessResult.NotFresh -> {
                return MarketDataCalculationOrchestrationResult.NotFresh(
                    errors = freshness.errors
                )
            }
        }

        when (
            val source = sourcePolicy.evaluate(
                calculationType = request.calculationType,
                sourceId = request.marketData.sourceId
            )
        ) {
            MarketDataSourceResult.Allowed -> Unit
            is MarketDataSourceResult.Blocked -> {
                return MarketDataCalculationOrchestrationResult.SourceBlocked(
                    error = source.error
                )
            }
        }

        return calculateAvailableMarketData(request)
    }

    private fun calculateAvailableMarketData(
        request: MarketDataCalculationRequest
    ): MarketDataCalculationOrchestrationResult {
        val marketData = request.marketData

        val value = when (request.calculationType) {
            MarketDataCalculationType.PURCHASE_PRICE -> {
                val ask = requireNotNull(marketData.ask) {
                    "Ask is required after successful PURCHASE_PRICE availability"
                }
                MarketDataCalculationValue.PurchasePrice(
                    value = ask,
                    currency = marketData.currency
                )
            }

            MarketDataCalculationType.SALE_PRICE -> {
                val bid = requireNotNull(marketData.bid) {
                    "Bid is required after successful SALE_PRICE availability"
                }
                MarketDataCalculationValue.SalePrice(
                    value = bid,
                    currency = marketData.currency
                )
            }

            MarketDataCalculationType.SPREAD -> {
                val bid = requireNotNull(marketData.bid) {
                    "Bid is required after successful SPREAD availability"
                }
                val ask = requireNotNull(marketData.ask) {
                    "Ask is required after successful SPREAD availability"
                }
                val absoluteSpread = when (
                    val result = MarketDataCalculator.calculateAbsoluteSpread(
                        bid = bid,
                        ask = ask
                    )
                ) {
                    is MarketDataCalculationResult.Success -> result.value
                    is MarketDataCalculationResult.Failure -> {
                        return MarketDataCalculationOrchestrationResult.CalculationFailure(
                            error = result.error
                        )
                    }
                }
                val relativeSpread = when (
                    val result = MarketDataCalculator.calculateRelativeSpreadToAskPercent(
                        bid = bid,
                        ask = ask
                    )
                ) {
                    is MarketDataCalculationResult.Success -> result.value
                    is MarketDataCalculationResult.Failure -> {
                        return MarketDataCalculationOrchestrationResult.CalculationFailure(
                            error = result.error
                        )
                    }
                }
                MarketDataCalculationValue.Spread(
                    absoluteSpread = absoluteSpread,
                    relativeSpreadToAskPercent = relativeSpread,
                    currency = marketData.currency
                )
            }

            MarketDataCalculationType.MID -> {
                val bid = requireNotNull(marketData.bid) {
                    "Bid is required after successful MID availability"
                }
                val ask = requireNotNull(marketData.ask) {
                    "Ask is required after successful MID availability"
                }
                when (
                    val result = MarketDataCalculator.calculateMidPrice(
                        bid = bid,
                        ask = ask
                    )
                ) {
                    is MarketDataCalculationResult.Success -> {
                        MarketDataCalculationValue.MidPrice(
                            value = result.value,
                            currency = marketData.currency
                        )
                    }

                    is MarketDataCalculationResult.Failure -> {
                        return MarketDataCalculationOrchestrationResult.CalculationFailure(
                            error = result.error
                        )
                    }
                }
            }
        }

        return MarketDataCalculationOrchestrationResult.Success(value = value)
    }
}
