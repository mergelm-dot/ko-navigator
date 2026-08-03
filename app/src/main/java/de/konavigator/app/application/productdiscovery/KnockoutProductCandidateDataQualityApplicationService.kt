package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.dataquality.KnockoutProductDataQualityValidator

/**
 * Bewertet marktdatenangereicherte KO-Produktkandidaten strukturell und providerneutral.
 *
 * Der Service ist rein und zustandslos. Er delegiert jeden Listeneintrag separat an
 * [KnockoutProductDataQualityValidator] als einzige Regelquelle und ergänzt keine eigenen
 * Data-Quality-Regeln. Spezifikation und Marktdaten werden exakt und ohne Normalisierung,
 * Korrektur, `trim()` oder Änderung der Groß-/Kleinschreibung übergeben.
 *
 * Eingaben und erzeugte Assessments werden nicht verändert. Reihenfolge, Duplikate und gleiche
 * ISINs bleiben erhalten; PASSED, WARNING und BLOCKED werden weder gefiltert noch sortiert,
 * gruppiert, dedupliziert oder begrenzt. Der Service verwendet keinen Cache und keinen globalen
 * Zustand.
 *
 * Er greift auf keine Repositories, Systemzeit oder Zeitumrechnung zu und enthält keine
 * Freshness-, Spread-, Zielhebel-, Score-, Ranking-, Hauptprodukt- oder Alternativenlogik sowie
 * keine Android-, Compose- oder UI-Abhängigkeit.
 */
class KnockoutProductCandidateDataQualityApplicationService {

    fun execute(
        request: KnockoutProductCandidateDataQualityRequest
    ): KnockoutProductCandidateDataQualityResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateDataQualityResult.NoInputCandidates
        }

        val candidatesWithDataQuality = request.candidates.map { candidate ->
            val dataQualityAssessment = KnockoutProductDataQualityValidator.assess(
                specification = candidate.specificationSnapshot.specification,
                marketData = candidate.marketData
            )
            KnockoutProductCandidateWithDataQuality(
                candidateWithMarketData = candidate,
                dataQualityAssessment = dataQualityAssessment
            )
        }

        return KnockoutProductCandidateDataQualityResult.CandidatesWithDataQuality(
            candidates = candidatesWithDataQuality
        )
    }
}
