package de.konavigator.app.domain.model

/**
 * Beschreibt veränderliche Marktdaten eines konkreten KO-Produkts.
 *
 * Die Verbindung zur statischen Produktspezifikation erfolgt ausschließlich
 * über [productIsin]. Bid und Ask sind unabhängig nullable; fehlende Quotes
 * werden als `null` und nicht als `0.0` dargestellt. Beide Seiten besitzen
 * getrennte Zeitstempel in UTC Epoch Milliseconds, damit getrennte Updates
 * erhalten bleiben. Quote-Währung und Datenquelle werden ausdrücklich
 * mitgeführt; die Produktwährung wird nicht pauschal als EUR angenommen.
 *
 * Das Modell enthält weder Last noch Bid-/Ask-Stückzahlen, Spread, relativen
 * Spread, Mid-Preis, Quote-Alter oder eine Qualitätsbewertung. Es enthält
 * keine Validierungslogik, keine UI-Zustände und keine Android- oder
 * Compose-Abhängigkeiten. Spread, Mid-Preis und Quote-Alter werden in späteren
 * Schritten aus validierten Marktdaten berechnet.
 */
data class KnockoutProductMarketData(
    val productIsin: String,
    val bid: Double?,
    val ask: Double?,
    val bidTimestampEpochMillis: Long?,
    val askTimestampEpochMillis: Long?,
    val currency: String,
    val sourceId: String
)
