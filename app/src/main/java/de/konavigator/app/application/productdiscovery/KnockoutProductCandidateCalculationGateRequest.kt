package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Auftrag für das Gate bereits vollständig berechneter
 * Kandidaten. Die Liste wird exakt übernommen, darf leer sein und behält
 * dieselbe Referenz, Reihenfolge und Duplikate.
 *
 * Der Auftrag berechnet keinen Wert erneut und enthält weder Calculation-Type,
 * Preise, Basiswert, Einstiegskurs, Zielhebel, Qualitäts- oder Rankingparameter,
 * Broker- oder Emittentenauswahl, separate Kennungslisten noch Zeitwerte. Er
 * normalisiert und mutiert keine Eingaben und besitzt keine Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateCalculationGateRequest(
    val candidates: List<KnockoutProductCandidateWithCalculation>
)
