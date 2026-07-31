package de.konavigator.app.application.productdiscovery

/**
 * Reiner, zustandsloser und providerneutraler Application-Layer-Filter für bereits
 * brokerhandelbare Kandidaten.
 *
 * Der Filter prüft Broker-Verfügbarkeit nicht erneut. Er filtert ausschließlich durch exakte
 * Set-Mitgliedschaft von `candidate.specification.issuerId` in `request.enabledIssuerIds`.
 * Emittenten-IDs bleiben case- und whitespace-sensitiv; es erfolgen keine Normalisierung, kein
 * `trim()`, keine Änderung der Groß-/Kleinschreibung und keine Validierung. Der Filter aktiviert
 * keine Emittenten automatisch und leitet keine Standardauswahl ab.
 *
 * Request, Kandidatenliste und aktivierte Emittentenmenge werden nicht mutiert. Ursprüngliche
 * Kandidatenreihenfolge, Duplikate und unterschiedliche Produkte desselben Emittenten bleiben
 * erhalten. Es erfolgen keine Sortierung, Gruppierung, Deduplizierung oder Begrenzung der
 * Kandidatenzahl und keine Exceptions für erwartbare leere Zustände.
 *
 * Der Filter enthält keine Repository-, Provider-, DTO- oder Mappingdetails, keine Marktdaten-,
 * Berechnungs- oder Rankinglogik, keine Systemzeit oder Zeitumrechnung und keine Android-,
 * Compose- oder UI-Abhängigkeit.
 */
class KnockoutProductIssuerSelectionFilter {

    fun filter(
        request: KnockoutProductIssuerSelectionRequest
    ): KnockoutProductIssuerSelectionResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductIssuerSelectionResult
                .NoInputCandidates
        }

        val enabledIssuerCandidates =
            request.candidates.filter { candidate ->
                candidate.specification.issuerId in
                    request.enabledIssuerIds
            }

        return if (enabledIssuerCandidates.isEmpty()) {
            KnockoutProductIssuerSelectionResult
                .NoEnabledIssuerCandidates
        } else {
            KnockoutProductIssuerSelectionResult
                .EnabledIssuerCandidates(
                    candidates = enabledIssuerCandidates
                )
        }
    }
}
