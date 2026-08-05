package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Application-Auftrag fuer die theoretische Berechnung
 * bestehender Produkte am geplanten Einstieg. Die Kandidaten wurden durch die
 * vorgelagerten Gates freigegeben; dieser Auftrag wiederholt keine dieser
 * Pruefungen und gibt keine Kauf-, Verkaufs- oder Orderempfehlung.
 */
data class KnockoutProductCandidateExistingEntryCalculationRequest(
    val candidates: List<KnockoutProductCandidateWithTargetLeveragePlan>,
    val plannedEntryPrice: Double
)
