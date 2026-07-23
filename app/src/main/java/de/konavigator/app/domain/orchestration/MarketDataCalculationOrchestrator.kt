package de.konavigator.app.domain.orchestration

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityEvaluator
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.calculator.MarketDataCalculationResult
import de.konavigator.app.domain.calculator.MarketDataCalculator
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.dataquality.KnockoutProductDataQualityValidator
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourceResult

/**
 * Reine Domain-Orchestrierung für genau einen Marktdaten-CalculationType.
 *
 * Die feste Fail-Fast-Reihenfolge lautet strukturelle Datenqualität,
 * Availability, Freshness, Quellenfreigabe und erst danach Berechnung
 * beziehungsweise Quote-Auswahl. Bestehende Komponenten bleiben dabei die
 * alleinige Quelle ihrer Regeln und ihre Fehlercodes werden unverändert
 * weitergegeben.
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
        val dataQualityAssessment = KnockoutProductDataQualityValidator.assess(
            specification = request.specification,
            marketData = request.marketData
        )
        when (dataQualityAssessment.status) {
            DataQualityStatus.BLOCKED -> {
                return MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked(
                    dataQualityAssessment = dataQualityAssessment
                )
            }

            DataQualityStatus.PASSED,
            DataQualityStatus.WARNING -> Unit
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
                    errors = availability.errors,
                    dataQualityAssessment = dataQualityAssessment
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
                    errors = freshness.errors,
                    dataQualityAssessment = dataQualityAssessment
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
                    error = source.error,
                    dataQualityAssessment = dataQualityAssessment
                )
            }
        }

        return calculateAvailableMarketData(
            request = request,
            dataQualityAssessment = dataQualityAssessment
        )
    }

    private fun calculateAvailableMarketData(
        request: MarketDataCalculationRequest,
        dataQualityAssessment: DataQualityAssessment
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
                            error = result.error,
                            dataQualityAssessment = dataQualityAssessment
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
                            error = result.error,
                            dataQualityAssessment = dataQualityAssessment
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
                            error = result.error,
                            dataQualityAssessment = dataQualityAssessment
                        )
                    }
                }
            }
        }

        return MarketDataCalculationOrchestrationResult.Success(
            value = value,
            dataQualityAssessment = dataQualityAssessment
        )
    }
}
