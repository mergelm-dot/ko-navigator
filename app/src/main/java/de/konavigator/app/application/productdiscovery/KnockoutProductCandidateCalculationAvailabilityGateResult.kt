package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Ergebnisvertrag des Calculation-Availability-Gates.
 *
 * Das Ergebnis ist keine Kauf- oder Verkaufsempfehlung und trifft keine Freshness-, Quellen-,
 * Preis-, Hebel-, Ranking-, Hauptprodukt- oder Alternativenentscheidung. Es enthält keine
 * UI-Texte und besitzt keine Android-, Compose-, Netzwerk-, Provider- oder DTO-Abhängigkeit.
 */
sealed interface KnockoutProductCandidateCalculationAvailabilityGateResult {

    /**
     * Enthält mindestens einen nur für die nachfolgende Freshness-Stufe freigegebenen Kandidaten.
     *
     * [availableCandidates] enthält ausschließlich StructurallyAvailable-Kandidaten,
     * [unavailableCandidates] ausschließlich StructurallyUnavailable-Kandidaten und darf leer
     * sein. Beide Listen bewahren ihre jeweilige relative Eingabereihenfolge, Duplikate,
     * Objektinstanzen, Data-Quality-Assessments, Findings, Availability-Ergebnisse und Fehler.
     * Die Freigabe ist keine endgültige Berechnungs- oder Rankingfreigabe.
     */
    data class CalculationAvailableCandidates(
        val availableCandidates: List<KnockoutProductCandidateWithCalculationAvailability>,
        val unavailableCandidates: List<KnockoutProductCandidateWithCalculationAvailability>
    ) : KnockoutProductCandidateCalculationAvailabilityGateResult

    /**
     * Die nichtleere Eingabe enthielt ausschließlich StructurallyUnavailable-Kandidaten. Alle
     * Kandidaten bleiben mitsamt ihrer vollständigen Availability-Fehler in ursprünglicher
     * Reihenfolge erhalten. Dieser Zustand ist weder ein technischer Fehler noch Repository-
     * NotFound, DataAccessFailure oder InvalidData und trifft keine Aussage zu Katalog, Broker,
     * Emittentenauswahl, Marktdatenladung oder Data-Quality-Prüfung.
     */
    data class NoCalculationAvailableCandidates(
        val unavailableCandidates: List<KnockoutProductCandidateWithCalculationAvailability>
    ) : KnockoutProductCandidateCalculationAvailabilityGateResult

    /**
     * Die Eingabeliste war leer. Es entstehen keine künstlichen Kandidaten oder
     * Availability-Ergebnisse; dieser Zustand ist weder ein technischer Fehler noch NotFound,
     * DataAccessFailure oder InvalidData.
     */
    data object NoInputCandidates : KnockoutProductCandidateCalculationAvailabilityGateResult
}
