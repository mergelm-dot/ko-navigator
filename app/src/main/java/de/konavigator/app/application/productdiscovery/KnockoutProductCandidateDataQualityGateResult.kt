package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Ergebnisvertrag der strukturellen Data-Quality-Freigabe.
 *
 * Das Ergebnis ist keine Kauf- oder Verkaufsempfehlung und trifft weder eine Freshness-, Spread-,
 * Zielhebel-, Preis-, Ranking-, Hauptprodukt- oder Alternativenentscheidung noch enthält es
 * UI-Texte. Es besitzt keine Android-, Compose-, Netzwerk-, Provider- oder DTO-Abhängigkeit.
 */
sealed interface KnockoutProductCandidateDataQualityGateResult {

    /**
     * Enthält mindestens einen Kandidaten, der anhand seines bereits vorhandenen Status für die
     * nachfolgende Freshness-Prüfung strukturell freigegeben ist.
     *
     * [eligibleCandidates] enthält ausschließlich PASSED- oder WARNING-Kandidaten,
     * [blockedCandidates] ausschließlich BLOCKED-Kandidaten und darf leer sein. Beide Listen
     * erhalten ihre jeweilige relative Eingabereihenfolge, Duplikate und alle ursprünglichen
     * Kandidaten-, Domain-, Assessment- und Finding-Instanzen. Die Freigabe ist keine endgültige
     * Berechnungs- oder Rankingfreigabe.
     */
    data class StructurallyEligibleCandidates(
        val eligibleCandidates: List<KnockoutProductCandidateWithDataQuality>,
        val blockedCandidates: List<KnockoutProductCandidateWithDataQuality>
    ) : KnockoutProductCandidateDataQualityGateResult

    /**
     * Die nichtleere Eingabe enthielt ausschließlich BLOCKED-Kandidaten. Alle Kandidaten bleiben
     * mitsamt Assessment und Findings in ursprünglicher Reihenfolge erhalten. Dieser Zustand ist
     * weder ein technischer Fehler noch DataAccessFailure, InvalidData oder NotFound und trifft
     * keine Aussage zu Katalog, Broker, Emittentenauswahl oder Marktdatenladung.
     */
    data class NoStructurallyEligibleCandidates(
        val blockedCandidates: List<KnockoutProductCandidateWithDataQuality>
    ) : KnockoutProductCandidateDataQualityGateResult

    /**
     * Die Eingabeliste war leer. Es entstehen keine künstlichen Kandidaten oder Assessments;
     * dieser Zustand ist weder ein technischer Fehler noch DataAccessFailure, InvalidData oder
     * NotFound.
     */
    data object NoInputCandidates : KnockoutProductCandidateDataQualityGateResult
}
