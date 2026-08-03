package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult

/**
 * Verbindet exakt einen ursprünglichen, bereits Data-Quality-bewerteten Kandidaten mit dem
 * Ergebnis des vorhandenen Availability-Evaluators.
 *
 * Kandidat, Marktdatenkandidat, Snapshot, Spezifikation, Marktdaten, Data-Quality-Assessment und
 * Findings bleiben dieselben unveränderten Objektinstanzen. Das Availability-Ergebnis wird ohne
 * Kopie oder Korrektur transportiert.
 */
data class KnockoutProductCandidateWithCalculationAvailability(
    val candidateWithDataQuality: KnockoutProductCandidateWithDataQuality,
    val availabilityResult: MarketDataCalculationAvailabilityResult
)

/**
 * Providerneutraler Ergebnisvertrag der Calculation-Availability-Bewertung.
 *
 * Das Ergebnis ist keine Kauf- oder Verkaufsempfehlung und enthält keine Freshness-, Quellen-,
 * Preis-, Hebel-, Ranking-, Hauptprodukt- oder Alternativenentscheidung sowie keine UI-Texte. Es
 * besitzt keine Android-, Compose-, Netzwerk-, Provider- oder DTO-Abhängigkeit.
 */
sealed interface KnockoutProductCandidateCalculationAvailabilityResult {

    /**
     * Genau ein Availability-Ergebnis pro Eingabekandidat in unveränderter Reihenfolge.
     *
     * Dieser Zustand wird nur mit einer nichtleeren Liste erzeugt. Duplikate, gleiche ISINs und
     * unterschiedliche Produkte desselben Emittenten bleiben getrennte Einträge.
     * StructurallyAvailable und StructurallyUnavailable werden gleichermaßen samt vollständiger,
     * geordneter Fehlerliste erhalten; es erfolgt noch keine statusabhängige Filterung,
     * Freshness- oder Berechnungsfreigabe.
     */
    data class CandidatesWithCalculationAvailability(
        val candidates: List<KnockoutProductCandidateWithCalculationAvailability>
    ) : KnockoutProductCandidateCalculationAvailabilityResult

    /**
     * Die Eingabeliste war leer. Der Evaluator wird nicht aufgerufen und es entstehen weder
     * künstliche Kandidaten noch Availability-Ergebnisse. Dieser Zustand ist weder ein technischer
     * Fehler noch Repository-NotFound, DataAccessFailure oder InvalidData.
     */
    data object NoInputCandidates : KnockoutProductCandidateCalculationAvailabilityResult
}
