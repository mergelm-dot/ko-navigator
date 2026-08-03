package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.dataquality.DataQualityStatus

/**
 * Reines, zustandsloses und providerneutrales Gate für bereits bewertete Kandidaten.
 *
 * Es verwendet ausschließlich den vorhandenen [DataQualityStatus]: PASSED und WARNING werden
 * für die folgende Freshness-Stufe weitergegeben, BLOCKED wird separat für nachvollziehbare
 * Hinweise erhalten. WARNING ist keine endgültige Berechnungsfreigabe.
 *
 * Der Gate ruft den Data-Quality-Validator nicht erneut auf, implementiert keine eigenen
 * Data-Quality-Regeln und verändert weder Request, Kandidaten, Domainobjekte, Assessments noch
 * Findings. Jeder Eingabeeintrag wird genau einer Ergebnisliste zugeordnet; relative Reihenfolge,
 * Duplikate und Objektinstanzen bleiben erhalten. Es gibt keinen Cache oder globalen Zustand sowie
 * keine Repository-, Systemzeit-, Freshness-, Spread-, Zielhebel-, Ranking-, UI-, Android- oder
 * Compose-Logik.
 */
class KnockoutProductCandidateDataQualityGate {

    fun filter(
        request: KnockoutProductCandidateDataQualityGateRequest
    ): KnockoutProductCandidateDataQualityGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateDataQualityGateResult.NoInputCandidates
        }

        val eligibleCandidates = ArrayList<KnockoutProductCandidateWithDataQuality>(
            request.candidates.size
        )
        val blockedCandidates = ArrayList<KnockoutProductCandidateWithDataQuality>()

        request.candidates.forEach { candidate ->
            when (candidate.dataQualityAssessment.status) {
                DataQualityStatus.PASSED,
                DataQualityStatus.WARNING -> eligibleCandidates += candidate

                DataQualityStatus.BLOCKED -> blockedCandidates += candidate
            }
        }

        return if (eligibleCandidates.isEmpty()) {
            KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates(
                blockedCandidates = blockedCandidates
            )
        } else {
            KnockoutProductCandidateDataQualityGateResult.StructurallyEligibleCandidates(
                eligibleCandidates = eligibleCandidates,
                blockedCandidates = blockedCandidates
            )
        }
    }
}
