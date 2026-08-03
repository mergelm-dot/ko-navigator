package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.dataquality.DataQualityAssessment

/**
 * Verbindet exakt einen ursprünglichen marktdatenangereicherten Kandidaten mit seiner
 * strukturellen Data-Quality-Bewertung.
 *
 * Kandidat, Spezifikations-Snapshot, Spezifikation und Marktdaten werden als dieselben
 * Objektinstanzen unverändert transportiert. Auch das Assessment wird ohne Kopie oder Änderung
 * übernommen. Es bestätigt weder Freshness noch Berechnungs- oder Rankingfreigabe.
 */
data class KnockoutProductCandidateWithDataQuality(
    val candidateWithMarketData: KnockoutProductCandidateWithMarketData,
    val dataQualityAssessment: DataQualityAssessment
)

/**
 * Providerneutrales Ergebnis der strukturellen Data-Quality-Bewertung.
 *
 * Das Ergebnis ist keine Kauf- oder Verkaufsempfehlung und enthält keine Freshness-,
 * Berechnungs-, Spread-, Zielhebel-, Ranking-, Hauptprodukt- oder Alternativenentscheidung sowie
 * keine UI-Texte. Es besitzt keine Android-, Compose-, Netzwerk-, Provider- oder
 * DTO-Abhängigkeit.
 */
sealed interface KnockoutProductCandidateDataQualityResult {

    /**
     * Genau eine Bewertung je Eingabeeintrag in unveränderter Reihenfolge.
     *
     * Dieser Zustand wird nur mit einer nichtleeren Liste erzeugt. Duplikate, mehrfach
     * enthaltene identische Instanzen, gleiche ISINs und gleiche Emittenten bleiben getrennte
     * Einträge. Snapshot-, Spezifikations- und Marktdateninstanzen bleiben erhalten. Die
     * Findings werden einschließlich ihrer Reihenfolge unverändert transportiert.
     *
     * PASSED-, WARNING- und BLOCKED-Assessments werden gleichermaßen beibehalten; es erfolgt
     * keine statusabhängige Filterung, Sortierung, Gruppierung, Deduplizierung oder Begrenzung.
     */
    data class CandidatesWithDataQuality(
        val candidates: List<KnockoutProductCandidateWithDataQuality>
    ) : KnockoutProductCandidateDataQualityResult

    /**
     * Die übergebene Kandidatenliste war leer. Es wird kein künstliches Assessment erzeugt.
     * Dieser Zustand ist weder ein technischer Fehler noch `DataAccessFailure`, `InvalidData`
     * oder `NotFound`.
     */
    data object NoInputCandidates : KnockoutProductCandidateDataQualityResult
}
