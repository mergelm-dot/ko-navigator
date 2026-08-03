package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.freshness.MarketDataFreshnessResult

/** Verbindet einen unveränderten Availability-Kandidaten mit seinem Policy-Freshness-Ergebnis. */
data class KnockoutProductCandidateWithFreshness(
    val candidateWithCalculationAvailability: KnockoutProductCandidateWithCalculationAvailability,
    val freshnessResult: MarketDataFreshnessResult
)

/**
 * Providerneutraler Freshness-Ergebnisvertrag ohne Freshness-Filterung oder weitere Freigabe.
 * Fresh und NotFresh bleiben beide enthalten; das Ergebnis besitzt keine Quellen-, Preis-, Hebel-,
 * Ranking-, UI-, Android-, Compose-, Netzwerk-, Provider- oder DTO-Abhängigkeit.
 */
sealed interface KnockoutProductCandidateFreshnessResult {

    /** Genau ein unveränderter Kandidat und ein Policy-Ergebnis pro nichtleerem Eingabeeintrag. */
    data class CandidatesWithFreshness(
        val candidates: List<KnockoutProductCandidateWithFreshness>
    ) : KnockoutProductCandidateFreshnessResult

    /** Leere Eingabe ohne Policy-Aufruf, künstlichen Kandidaten oder Freshness-Ergebnis. */
    data object NoInputCandidates : KnockoutProductCandidateFreshnessResult
}
