package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Application-Auftrag fuer das Existing-Entry-Calculation-
 * Gate. Alle CalculationResults liegen bereits vollstaendig vor; dieser
 * Request validiert und berechnet nichts erneut und gibt keine Kauf-,
 * Verkaufs- oder Orderempfehlung.
 */
data class KnockoutProductCandidateExistingEntryCalculationGateRequest(
    val candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>
)
