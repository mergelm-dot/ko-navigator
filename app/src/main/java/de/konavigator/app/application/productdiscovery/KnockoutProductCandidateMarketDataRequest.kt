package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Providerneutraler Application-Auftrag zur Marktdatenanreicherung bereits broker- und
 * emittentengefilterter KO-Produktkandidaten.
 *
 * [candidates] enthält ausschließlich Snapshots aus der vorherigen Discovery-Stufe. Dieser
 * Vertrag prüft weder Broker-Verfügbarkeit noch Emittentenauswahl erneut. Die Kandidatenliste
 * wird exakt übernommen; Reihenfolge und Duplikate werden nicht verändert. Eine leere Liste ist
 * als passiver Vertragszustand zulässig.
 *
 * Produkt-ISINs werden später ausschließlich aus `candidate.specification.productIsin` gelesen.
 * Es gibt keine separate Produkt-ISIN-Liste, Broker-ID oder aktivierte Emittenten-IDs im Request.
 * Der Vertrag normalisiert, trimmt und validiert nicht und verändert keine Groß-/Kleinschreibung.
 * Er enthält keine Data-Quality-, Freshness-, Kurs-, Zielhebel-, Spread-, Score- oder
 * Rankingangaben und besitzt keine Android-, Compose-, Provider-, DTO- oder
 * Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateMarketDataRequest(
    val candidates: List<KnockoutProductSpecificationSnapshot>
)
