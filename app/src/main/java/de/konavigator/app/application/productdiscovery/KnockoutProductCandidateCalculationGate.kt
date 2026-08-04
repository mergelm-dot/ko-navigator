package de.konavigator.app.application.productdiscovery

/**
 * Reines, synchrones, zustandsloses und providerneutrales Application-Gate.
 * Es verwendet ausschließlich vorhandene CalculationOutcomes: Success wird
 * für spätere Zielhebel- und Produktbewertungen freigegeben, Failure samt
 * Calculator-Fehler erhalten. Dies ist keine finale Auswahl oder Orderfreigabe.
 *
 * Das Gate ruft weder Calculator, Calculation-Service noch Orchestrator auf,
 * berechnet nichts erneut und mutiert keine Eingaben. Es besitzt keinen Cache,
 * globalen Zustand, Repository-, Zeit-, Bewertungs-, Ranking-, Android-,
 * Compose- oder UI-Code.
 */
class KnockoutProductCandidateCalculationGate {

    fun filter(
        request: KnockoutProductCandidateCalculationGateRequest
    ): KnockoutProductCandidateCalculationGateResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateCalculationGateResult.NoInputCandidates
        }

        val successfulCandidates =
            ArrayList<KnockoutProductCandidateWithCalculation>(request.candidates.size)
        val failedCandidates = ArrayList<KnockoutProductCandidateWithCalculation>()

        request.candidates.forEach { candidate ->
            when (candidate.calculationOutcome) {
                is KnockoutProductCandidateCalculationOutcome.Success -> successfulCandidates += candidate
                is KnockoutProductCandidateCalculationOutcome.Failure -> failedCandidates += candidate
            }
        }

        return if (successfulCandidates.isEmpty()) {
            KnockoutProductCandidateCalculationGateResult.NoSuccessfulCalculationCandidates(
                failedCandidates = failedCandidates
            )
        } else {
            KnockoutProductCandidateCalculationGateResult.SuccessfulCalculationCandidates(
                successfulCandidates = successfulCandidates,
                failedCandidates = failedCandidates
            )
        }
    }
}
