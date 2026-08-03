package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.source.MarketDataSourceResult

/**
 * Reines, synchrones, zustandsloses und providerneutrales Application-Gate.
 * Es verwendet ausschließlich das vorhandene sourceResult: Allowed wird für
 * die nachfolgende Berechnungsstufe freigegeben, Blocked samt Fehler erhalten.
 * Dies ist keine endgültige Produkt-, Ranking- oder Orderfreigabe.
 *
 * Das Gate ruft keine Policy auf, implementiert keine Quellenregel und mutiert
 * weder Eingaben noch vorhandene Ergebnisse. Es sortiert, gruppiert oder
 * dedupliziert nicht und enthält keinen Cache, Repository-, Zeit-, Neubewertungs-,
 * Preis-, Hebel-, Qualitäts-, Ranking-, Android-, Compose- oder UI-Code.
 */
class KnockoutProductCandidateSourceGate {

    fun filter(
        request: KnockoutProductCandidateSourceGateRequest
    ): KnockoutProductCandidateSourceGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateSourceGateResult.NoInputCandidates
        }

        val allowedCandidates =
            ArrayList<KnockoutProductCandidateWithSourceEvaluation>(request.candidates.size)
        val blockedCandidates = ArrayList<KnockoutProductCandidateWithSourceEvaluation>()

        request.candidates.forEach { candidate ->
            when (candidate.sourceResult) {
                MarketDataSourceResult.Allowed -> allowedCandidates += candidate
                is MarketDataSourceResult.Blocked -> blockedCandidates += candidate
            }
        }

        return if (allowedCandidates.isEmpty()) {
            KnockoutProductCandidateSourceGateResult.NoSourceAllowedCandidates(
                blockedCandidates = blockedCandidates
            )
        } else {
            KnockoutProductCandidateSourceGateResult.SourceAllowedCandidates(
                allowedCandidates = allowedCandidates,
                blockedCandidates = blockedCandidates
            )
        }
    }
}
