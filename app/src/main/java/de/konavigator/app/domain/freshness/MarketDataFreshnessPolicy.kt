package de.konavigator.app.domain.freshness

import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.model.KnockoutProductMarketData

/**
 * Reine zeitliche Domainpolicy für genau einen Berechnungstyp je Aufruf.
 *
 * Die unveränderlichen Schwellen werden explizit im Konstruktor und der
 * Bewertungszeitpunkt explizit an [evaluate] übergeben; die Policy liest keine
 * Systemzeit. Sie setzt intern validierte und kompatible Marktdaten sowie eine
 * erfolgreiche strukturelle Availability voraus. Validatoren und der
 * AvailabilityEvaluator werden nicht aufgerufen, fehlende Preise oder
 * Zeitstempel nicht erneut fachlich geprüft. Ein vertragswidrig fehlender
 * relevanter Zeitstempel führt stattdessen zu einer technischen Vorbedingung.
 *
 * Die Policy berechnet keine Preise, bewertet weder Quelle, Handelszeit noch
 * Marktstatus und enthält keine UI-Texte oder Android-/Compose-Abhängigkeiten.
 * Sie ist nicht an Engine, UI oder Repository angebunden. Alle Grenzwerte sind
 * inklusiv; Zeitdistanzen werden ohne überlaufgefährdete Subtraktion oder
 * `abs()` verglichen. [MarketDataFreshnessResult.Fresh] ist weder eine
 * vollständige fachliche Berechnungsfreigabe noch eine Handelbarkeitsaussage.
 */
class MarketDataFreshnessPolicy(
    private val thresholds: MarketDataFreshnessThresholds
) {
    fun evaluate(
        calculationType: MarketDataCalculationType,
        marketData: KnockoutProductMarketData,
        evaluationTimeEpochMillis: Long
    ): MarketDataFreshnessResult {
        val errors = mutableListOf<MarketDataFreshnessError>()

        when (calculationType) {
            MarketDataCalculationType.PURCHASE_PRICE -> {
                val askTimestamp = requireNotNull(marketData.askTimestampEpochMillis) {
                    "Ask timestamp is required for PURCHASE_PRICE freshness evaluation"
                }
                appendAskErrors(askTimestamp, evaluationTimeEpochMillis, errors)
            }

            MarketDataCalculationType.SALE_PRICE -> {
                val bidTimestamp = requireNotNull(marketData.bidTimestampEpochMillis) {
                    "Bid timestamp is required for SALE_PRICE freshness evaluation"
                }
                appendBidErrors(bidTimestamp, evaluationTimeEpochMillis, errors)
            }

            MarketDataCalculationType.SPREAD,
            MarketDataCalculationType.MID -> {
                val bidTimestamp = requireNotNull(marketData.bidTimestampEpochMillis) {
                    "Bid timestamp is required for $calculationType freshness evaluation"
                }
                val askTimestamp = requireNotNull(marketData.askTimestampEpochMillis) {
                    "Ask timestamp is required for $calculationType freshness evaluation"
                }
                val bidIsTooFarInFuture = appendBidErrors(
                    bidTimestamp,
                    evaluationTimeEpochMillis,
                    errors
                )
                val askIsTooFarInFuture = appendAskErrors(
                    askTimestamp,
                    evaluationTimeEpochMillis,
                    errors
                )

                if (
                    !bidIsTooFarInFuture &&
                    !askIsTooFarInFuture &&
                    distanceExceeds(
                        earlier = minOf(bidTimestamp, askTimestamp),
                        later = maxOf(bidTimestamp, askTimestamp),
                        limit = thresholds.maxBidAskDifferenceMillis
                    )
                ) {
                    errors += MarketDataFreshnessError.BID_ASK_TIMESTAMPS_TOO_FAR_APART
                }
            }
        }

        return if (errors.isEmpty()) {
            MarketDataFreshnessResult.Fresh
        } else {
            MarketDataFreshnessResult.NotFresh(errors)
        }
    }

    private fun appendBidErrors(
        timestampEpochMillis: Long,
        evaluationTimeEpochMillis: Long,
        errors: MutableList<MarketDataFreshnessError>
    ): Boolean = when {
        timestampEpochMillis > evaluationTimeEpochMillis && distanceExceeds(
            earlier = evaluationTimeEpochMillis,
            later = timestampEpochMillis,
            limit = thresholds.allowedFutureSkewMillis
        ) -> {
            errors += MarketDataFreshnessError.BID_TIMESTAMP_IN_FUTURE
            true
        }

        timestampEpochMillis < evaluationTimeEpochMillis && distanceExceeds(
            earlier = timestampEpochMillis,
            later = evaluationTimeEpochMillis,
            limit = thresholds.maxBidAgeMillis
        ) -> {
            errors += MarketDataFreshnessError.STALE_BID
            false
        }

        else -> false
    }

    private fun appendAskErrors(
        timestampEpochMillis: Long,
        evaluationTimeEpochMillis: Long,
        errors: MutableList<MarketDataFreshnessError>
    ): Boolean = when {
        timestampEpochMillis > evaluationTimeEpochMillis && distanceExceeds(
            earlier = evaluationTimeEpochMillis,
            later = timestampEpochMillis,
            limit = thresholds.allowedFutureSkewMillis
        ) -> {
            errors += MarketDataFreshnessError.ASK_TIMESTAMP_IN_FUTURE
            true
        }

        timestampEpochMillis < evaluationTimeEpochMillis && distanceExceeds(
            earlier = timestampEpochMillis,
            later = evaluationTimeEpochMillis,
            limit = thresholds.maxAskAgeMillis
        ) -> {
            errors += MarketDataFreshnessError.STALE_ASK
            false
        }

        else -> false
    }

    private fun distanceExceeds(
        earlier: Long,
        later: Long,
        limit: Long
    ): Boolean {
        require(earlier <= later) { "earlier must not be after later" }
        require(limit >= 0) { "limit must be non-negative" }

        return if (earlier > Long.MAX_VALUE - limit) {
            false
        } else {
            later > earlier + limit
        }
    }
}
