package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.TradeDirection

/**
 * Providerneutraler Application-Auftrag für die ersten beiden verpflichtenden Stufen der
 * KO-Produktfindung.
 *
 * [underlyingId] bezeichnet einen bereits eindeutig erkannten Basiswert, [direction] bleibt als
 * [TradeDirection.LONG] oder [TradeDirection.SHORT] typisiert, und [brokerId] bezeichnet den
 * bereits eindeutig ausgewählten Broker. Alle drei Werte werden exakt übernommen, ohne
 * Normalisierung, `trim()`, Änderung der Groß-/Kleinschreibung oder Validierung.
 *
 * Der Aufrufer benötigt keine Produkt-ISIN. Die ISIN-Liste wird später ausschließlich aus
 * erfolgreichen Katalogkandidaten gebildet. Der Auftrag enthält keine Emittenten-, Marktdaten-,
 * Kurs-, Zielhebel-, Spread-, Score- oder Rankingangaben und besitzt keine Android-, Compose-,
 * Provider-, DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductDiscoveryApplicationRequest(
    val underlyingId: String,
    val direction: TradeDirection,
    val brokerId: String
)
