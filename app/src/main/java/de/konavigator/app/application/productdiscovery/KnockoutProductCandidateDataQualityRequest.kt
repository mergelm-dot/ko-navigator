package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Application-Request zur strukturellen Data-Quality-Bewertung bereits mit
 * Marktdaten angereicherter KO-Produktkandidaten.
 *
 * Jeder Kandidat enthält den zuvor ermittelten Spezifikations-Snapshot und das zugehörige
 * Marktdatenobjekt. Die Liste wird exakt mit Reihenfolge und Duplikaten übernommen und darf leer
 * sein. Der Vertrag prüft weder Katalog, Broker-Verfügbarkeit, Emittentenauswahl noch
 * Repository-Verfügbarkeit erneut.
 *
 * Er enthält keine separate ISIN, Broker-ID, aktivierten Emittenten, Bewertungszeit,
 * Freshness- oder Spread-Schwellen, Zielhebel- oder Rankingangaben. Werte werden weder
 * normalisiert noch getrimmt oder in ihrer Groß-/Kleinschreibung verändert. Der Vertrag besitzt
 * keine Android-, Compose-, Provider-, DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateDataQualityRequest(
    val candidates: List<KnockoutProductCandidateWithMarketData>
)
