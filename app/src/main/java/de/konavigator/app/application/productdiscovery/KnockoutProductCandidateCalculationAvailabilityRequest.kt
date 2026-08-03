package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationType

/**
 * Providerneutraler Application-Auftrag zur Bewertung der Calculation Availability bereits
 * strukturell bewerteter KO-Produktkandidaten.
 *
 * Die Kandidaten stammen aus den eligibleCandidates des vorherigen Data-Quality-Gates und werden
 * hier nicht selbst bewertet. Die Kandidatenliste wird mit derselben Referenz, Reihenfolge und
 * Duplikaten exakt übernommen; eine leere Liste ist zulässig. Auch [calculationType] wird
 * unverändert übernommen. Unterstützt werden PURCHASE_PRICE, SALE_PRICE, SPREAD und MID.
 *
 * Der Vertrag enthält keine separate Produkt-ISIN-Liste, Broker-ID, Emittentenauswahl,
 * Evaluierungszeit, Freshness-Schwelle, Quellenrichtlinie, Spread-Grenze, Zielhebel- oder
 * Rankingwerte. Er normalisiert oder trimmt keine Werte und besitzt keine Android-, Compose-,
 * Provider-, DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateCalculationAvailabilityRequest(
    val candidates: List<KnockoutProductCandidateWithDataQuality>,
    val calculationType: MarketDataCalculationType
)
