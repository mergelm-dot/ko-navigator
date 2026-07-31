package de.konavigator.app.application.productdiscovery

/**
 * Application-Auftrag zur verpflichtenden Prüfung der Handelbarkeit konkreter KO-Produkte bei
 * genau einem bereits ausgewählten Broker.
 *
 * [brokerId] bezeichnet den bereits eindeutig ausgewählten Broker und wird exakt übernommen:
 * ohne Normalisierung, `trim()`, Änderung der Groß-/Kleinschreibung oder Validierung. Die
 * fachliche Eindeutigkeit der Broker-Auswahl wird vor diesem Vertrag sichergestellt.
 *
 * [productIsins] enthält die zu prüfenden Produkt-ISINs. Die Werte werden exakt übernommen; der
 * Query-Vertrag verändert weder Reihenfolge noch Duplikate der Eingabeliste. Eine leere Liste ist
 * als passiver Vertragszustand zulässig. ISINs werden nicht aus WKN oder anderen Kennungen
 * abgeleitet, und unbekannte oder doppelte Werte werden nicht automatisch entfernt.
 *
 * Die Query enthält keine Produktspezifikationen, Produktmarktdaten oder Emittenten-, Zielhebel-,
 * Kurs-, Spread-, Score- und Rankingangaben. Sie besitzt keine Android-, Compose-, Provider-,
 * DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductBrokerAvailabilityQuery(
    val brokerId: String,
    val productIsins: List<String>
)
