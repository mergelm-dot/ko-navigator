package de.konavigator.app.application.productdiscovery

/**
 * Providerneutraler Application-Auftrag für das Source-Gate bereits vollständig
 * bewerteter Kandidaten. Die Liste wird exakt übernommen und darf leer sein;
 * dieselbe Referenz, Reihenfolge und Duplikate bleiben erhalten.
 *
 * Der Request bewertet keine Quelle erneut und enthält weder Berechnungstyp,
 * sourceId-Liste, Source-Policy oder -Konfiguration, Produkt-ISIN-Liste,
 * Broker- oder Emittentenauswahl, Zeit- oder Freshness-Werte noch Preis-,
 * Hebel-, Qualitäts- oder Rankingparameter. Er normalisiert keine Werte und
 * besitzt keine Android-, Compose-, Provider-, DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateSourceGateRequest(
    val candidates: List<KnockoutProductCandidateWithSourceEvaluation>
)
