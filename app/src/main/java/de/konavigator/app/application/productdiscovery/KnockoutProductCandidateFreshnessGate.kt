package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.freshness.MarketDataFreshnessResult

/**
 * Reines, synchrones und zustandsloses Gate. Es wertet ausschließlich vorhandene Freshness-
 * Ergebnisse aus: Fresh wird für die Quellenprüfung weitergegeben, NotFresh mitsamt Fehlern
 * erhalten. Es ruft keine Policy auf, implementiert keine Freshness-Regel und besitzt keinen
 * Cache, globalen Zustand, Repository-, Zeit-, Quellen-, Ranking-, UI-, Android- oder Compose-Code.
 */
class KnockoutProductCandidateFreshnessGate {
    fun filter(
        request: KnockoutProductCandidateFreshnessGateRequest
    ): KnockoutProductCandidateFreshnessGateResult {
        if (request.candidates.isEmpty()) return KnockoutProductCandidateFreshnessGateResult.NoInputCandidates
        val fresh = ArrayList<KnockoutProductCandidateWithFreshness>(request.candidates.size)
        val notFresh = ArrayList<KnockoutProductCandidateWithFreshness>()
        request.candidates.forEach { candidate ->
            when (candidate.freshnessResult) {
                MarketDataFreshnessResult.Fresh -> fresh += candidate
                is MarketDataFreshnessResult.NotFresh -> notFresh += candidate
            }
        }
        return if (fresh.isEmpty()) {
            KnockoutProductCandidateFreshnessGateResult.NoFreshCandidates(notFresh)
        } else {
            KnockoutProductCandidateFreshnessGateResult.FreshCandidates(fresh, notFresh)
        }
    }
}
