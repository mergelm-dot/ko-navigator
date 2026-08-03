package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult

/**
 * Reines, synchrones, zustandsloses und providerneutrales Gate für bereits bewertete
 * Calculation-Availability-Kandidaten.
 *
 * Es verwendet ausschließlich das vorhandene Availability-Ergebnis: StructurallyAvailable wird
 * für die folgende Freshness-Stufe weitergegeben, StructurallyUnavailable wird separat für
 * nachvollziehbare Hinweise erhalten. Die Freigabe ist keine endgültige Berechnungsfreigabe.
 *
 * Das Gate ruft den Availability-Evaluator nicht erneut auf, implementiert keine
 * Availability-Regeln und verändert weder Request, Kandidaten, Domainobjekte, Assessments,
 * Findings, Availability-Ergebnisse noch Fehler. Jeder Eingabeeintrag wird genau einer
 * Ergebnisliste zugeordnet; Reihenfolge, Duplikate und Objektinstanzen bleiben erhalten. Es gibt
 * keinen Cache oder globalen Zustand sowie keine Repository-, Systemzeit-, Freshness-, Quellen-,
 * Spread-, Zielhebel-, Ranking-, UI-, Android- oder Compose-Logik.
 */
class KnockoutProductCandidateCalculationAvailabilityGate {

    fun filter(
        request: KnockoutProductCandidateCalculationAvailabilityGateRequest
    ): KnockoutProductCandidateCalculationAvailabilityGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateCalculationAvailabilityGateResult.NoInputCandidates
        }

        val availableCandidates = ArrayList<KnockoutProductCandidateWithCalculationAvailability>(
            request.candidates.size
        )
        val unavailableCandidates = ArrayList<KnockoutProductCandidateWithCalculationAvailability>()

        request.candidates.forEach { candidate ->
            when (candidate.availabilityResult) {
                MarketDataCalculationAvailabilityResult.StructurallyAvailable ->
                    availableCandidates += candidate

                is MarketDataCalculationAvailabilityResult.StructurallyUnavailable ->
                    unavailableCandidates += candidate
            }
        }

        return if (availableCandidates.isEmpty()) {
            KnockoutProductCandidateCalculationAvailabilityGateResult
                .NoCalculationAvailableCandidates(
                    unavailableCandidates = unavailableCandidates
                )
        } else {
            KnockoutProductCandidateCalculationAvailabilityGateResult.CalculationAvailableCandidates(
                availableCandidates = availableCandidates,
                unavailableCandidates = unavailableCandidates
            )
        }
    }
}
