package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Auftrag für das Gate bereits erzeugter theoretischer
 * Zielhebel-Pläne. Die Kandidatenliste wird exakt übernommen, darf leer sein
 * und behält Referenz, Reihenfolge und Duplikate. Der Request berechnet und
 * validiert nichts erneut und ist keine Kauf-, Verkaufs- oder Orderempfehlung.
 */
data class KnockoutProductCandidateTargetLeverageGateRequest(
    val candidates: List<KnockoutProductCandidateWithTargetLeveragePlan>
)
