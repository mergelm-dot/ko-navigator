package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.source.MarketDataSourceResult

/**
 * Verbindet genau einen unveränderten Freshness-Kandidaten mit dem unveränderten
 * Ergebnis der vorhandenen Quellenpolicy. Domainobjekte, vorherige Assessments,
 * Findings und Fehler werden weder kopiert noch korrigiert oder normalisiert.
 */
data class KnockoutProductCandidateWithSourceEvaluation(
    val candidateWithFreshness: KnockoutProductCandidateWithFreshness,
    val sourceResult: MarketDataSourceResult
)

/**
 * Providerneutraler Ergebnisvertrag ohne Quellenfilterung oder spätere
 * Preis-, Hebel-, Qualitäts-, Ranking-, Hauptprodukt-, Alternativen- oder
 * UI-Entscheidung. Er ist keine Kauf- oder Verkaufsempfehlung.
 */
sealed interface KnockoutProductCandidateSourceEvaluationResult {

    /** Nichtleere Liste mit genau einem Policy-Ergebnis je Eingabekandidat. */
    data class CandidatesWithSourceEvaluation(
        val candidates: List<KnockoutProductCandidateWithSourceEvaluation>
    ) : KnockoutProductCandidateSourceEvaluationResult

    /** Leere Eingabe ohne Policy-Aufruf, künstlichen Kandidaten oder Ergebnis. */
    data object NoInputCandidates : KnockoutProductCandidateSourceEvaluationResult
}
