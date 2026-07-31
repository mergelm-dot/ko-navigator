package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.TradeDirection

/**
 * Providerneutraler Application-Auftrag für die ersten drei verpflichtenden Filterstufen der
 * KO-Produktfindung: Katalogsuche, Broker-Verfügbarkeit und aktivierte Emittenten.
 *
 * [underlyingId] bezeichnet einen bereits eindeutig erkannten Basiswert, [direction] bleibt als
 * [TradeDirection.LONG] oder [TradeDirection.SHORT] typisiert, und [brokerId] bezeichnet den
 * bereits eindeutig ausgewählten Broker. [enabledIssuerIds] enthält exakt die momentan
 * aktivierten Emittenten-IDs. Eine leere Menge bedeutet, dass kein Emittent aktiviert ist, und
 * lässt nicht automatisch alle Emittenten zu. Es erfolgen keine automatische Aktivierung und
 * keine automatische Standardauswahl; eine spätere UI- oder Profilvorbelegung liegt außerhalb
 * dieses Requests.
 *
 * [underlyingId], [direction], [brokerId] und [enabledIssuerIds] werden exakt übernommen, ohne
 * Normalisierung, `trim()`, Änderung der Groß-/Kleinschreibung oder Validierung. Der Aufrufer
 * benötigt keine Produkt-ISIN. Der Auftrag enthält keine Marktdaten-, Kurs-, Zielhebel-, Spread-,
 * Score- oder Rankingangaben und besitzt keine Android-, Compose-, Provider-, DTO- oder
 * Infrastrukturabhängigkeit.
 */
data class KnockoutProductDiscoveryApplicationRequest(
    val underlyingId: String,
    val direction: TradeDirection,
    val brokerId: String,
    val enabledIssuerIds: Set<String>
)
