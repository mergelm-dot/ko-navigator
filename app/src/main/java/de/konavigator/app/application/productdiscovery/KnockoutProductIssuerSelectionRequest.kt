package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Providerneutraler Application-Auftrag zur Filterung bereits brokerhandelbarer
 * KO-Produktkandidaten anhand der aktivierten Emittenten.
 *
 * [candidates] enthält die aus der vorherigen Broker-Verfügbarkeitsstufe verbliebenen Snapshots;
 * der Vertrag prüft deren Broker-Handelbarkeit nicht erneut. [enabledIssuerIds] enthält exakt die
 * momentan aktivierten Emittenten-IDs. Kandidatenliste und IDs werden ohne Normalisierung,
 * Validierung, `trim()` oder Änderung der Groß-/Kleinschreibung übernommen.
 *
 * Eine leere Kandidatenliste ist als passiver Vertragszustand zulässig. Eine leere
 * [enabledIssuerIds]-Menge bedeutet, dass kein Emittent aktiviert ist, und lässt nicht
 * automatisch alle Emittenten zu. Der Vertrag aktiviert keine Emittenten automatisch; die
 * spätere Vorbelegung aller brokerverfügbaren Emittenten liegt außerhalb dieses Filters.
 *
 * Der Auftrag benötigt keine Broker-ID, leitet aktivierte Emittenten nicht aus Kandidaten ab,
 * sucht keine Produkte und benötigt keine Produkt-ISIN-Liste. Er enthält keine Marktdaten-,
 * Zielhebel-, Spread-, Score- oder Rankingangaben und besitzt keine Android-, Compose-,
 * Provider-, DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductIssuerSelectionRequest(
    val candidates: List<KnockoutProductSpecificationSnapshot>,
    val enabledIssuerIds: Set<String>
)
