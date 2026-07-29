package de.konavigator.app.data.remote.provider.hsbc

/**
 * Providerbezogener Zwischenvertrag für lokale, bereinigte Produktspezifikationsdaten.
 *
 * Der Record dient ausschließlich Forschung, Debugging und späteren kontrollierten lokalen
 * Importen. Er bildet weder eine originale HSBC-Webseitenantwort noch eine Vaadin-UIDL-Antwort
 * ab und enthält keine Cookies, Sitzungskennungen, Security Keys, Push-IDs, Request-Header oder
 * sonstigen Sicherheitswerte.
 *
 * Werte werden nicht validiert, normalisiert oder fachlich interpretiert. [directionLabel]
 * bleibt der unveränderte providerbezogene Text; numerische Werte werden weder begrenzt noch
 * korrigiert. `null`, `0.0` und negative Werte bleiben unterscheidbar. Zeichenketten werden nicht
 * getrimmt und ihre Groß-/Kleinschreibung bleibt unverändert.
 *
 * [sourceTimestampEpochMillis] ist ausschließlich ein bereits ausdrücklich aufgelöster
 * Anbieterzeitpunkt. Bei einem unbekannten oder nicht eindeutig auflösbaren Anbieterzeitpunkt
 * bleibt er `null`; es gibt keinen stillen Ersatz durch einen Abrufzeitpunkt oder die Systemzeit.
 * Der Record trifft keine Data-Quality-, Freshness- oder Berechnungsentscheidung.
 */
data class HsbcKnockoutProductSpecificationRecord(
    val productIsin: String?,
    val productWkn: String?,
    val issuerId: String?,
    val underlyingId: String?,
    val directionLabel: String?,
    val basePrice: Double?,
    val knockoutBarrier: Double?,
    val ratio: Double?,
    val underlyingCurrency: String?,
    val productCurrency: String?,
    val sourceTimestampEpochMillis: Long?
)
