package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Application-Auftrag für die strukturelle Data-Quality-Freigabe bereits
 * bewerteter KO-Produktkandidaten.
 *
 * Die Kandidaten stammen aus der vorherigen Data-Quality-Bewertungsstufe; jedes Assessment ist
 * bereits vollständig erzeugt und wird vom Gate nicht erneut bewertet. Die Liste wird exakt mit
 * derselben Referenz, Reihenfolge und Duplikaten übernommen und darf leer sein.
 *
 * Der Vertrag enthält keine separate ISIN-Liste, Broker-ID, Emittentenauswahl, Evaluierungszeit,
 * Freshness- oder Spread-Schwellen sowie keine Zielhebel- oder Rankingwerte. Er normalisiert oder
 * trimmt keine Werte und besitzt keine Android-, Compose-, Provider-, DTO- oder
 * Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateDataQualityGateRequest(
    val candidates: List<KnockoutProductCandidateWithDataQuality>
)
