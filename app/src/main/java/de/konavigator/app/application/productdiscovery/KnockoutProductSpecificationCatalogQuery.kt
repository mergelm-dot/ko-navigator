package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.model.TradeDirection

/**
 * Application-Auftrag zur Suche nach grundsätzlich passenden KO-Produktspezifikationen.
 *
 * [underlyingId] bezeichnet einen bereits eindeutig erkannten Basiswert und wird exakt
 * übernommen: ohne Normalisierung, `trim()`, Änderung der Groß-/Kleinschreibung oder
 * Validierung. Eine Mehrdeutigkeit der Basiswerterkennung wird vor diesem Vertrag behandelt.
 * [direction] bleibt als [TradeDirection.LONG] oder [TradeDirection.SHORT] typisiert.
 *
 * Der Auftrag benötigt keine Produkt-ISIN und enthält noch keine Broker-, Emittenten-,
 * Marktpreis-, Hebel- oder Rankingangabe. Er besitzt keine Android-, Compose-, Provider- oder
 * Infrastrukturabhängigkeit.
 */
data class KnockoutProductSpecificationCatalogQuery(
    val underlyingId: String,
    val direction: TradeDirection
)
