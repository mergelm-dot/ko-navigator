package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationType

/**
 * Providerneutraler Auftrag zur Berechnung bereits source-freigegebener
 * Kandidaten. Die Liste und der Berechnungstyp werden exakt übernommen;
 * dieselbe Referenz, Reihenfolge und Duplikate bleiben erhalten. Die Liste
 * darf leer sein.
 *
 * Der Auftrag enthält keine Zeit-, Freshness- oder Source-Policy-Werte, keine
 * Broker-, Emittenten- oder separaten Kennungslisten sowie keinen Basiswert,
 * Einstiegskurs, Zielhebel, Qualitäts- oder Rankingparameter. Er normalisiert
 * und mutiert keine Eingaben.
 */
data class KnockoutProductCandidateCalculationRequest(
    val candidates: List<KnockoutProductCandidateWithSourceEvaluation>,
    val calculationType: MarketDataCalculationType
)
