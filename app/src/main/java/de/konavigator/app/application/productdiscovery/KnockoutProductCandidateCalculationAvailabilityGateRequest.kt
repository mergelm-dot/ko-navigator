package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Application-Auftrag für das Calculation-Availability-Gate.
 *
 * Die Kandidaten stammen aus der vorherigen Availability-Bewertungsstufe und enthalten jeweils
 * bereits ein vollständiges Availability-Ergebnis, das vom Request nicht erneut bewertet wird.
 * Die Liste wird mit derselben Referenz, Reihenfolge und Duplikaten exakt übernommen; eine leere
 * Liste ist zulässig.
 *
 * Der Vertrag enthält keinen separaten MarketDataCalculationType, keine Produkt-ISIN-Liste,
 * Broker-ID, Emittentenauswahl, Evaluierungszeit, Freshness-Schwellen, Quellenrichtlinie sowie
 * keine Spread-, Zielhebel- oder Rankingparameter. Er normalisiert oder trimmt keine Werte und
 * besitzt keine Android-, Compose-, Provider-, DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateCalculationAvailabilityGateRequest(
    val candidates: List<KnockoutProductCandidateWithCalculationAvailability>
)
