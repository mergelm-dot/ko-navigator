package de.konavigator.app.domain.model

/**
 * Beschreibt eine konkret beobachtete KO-Produktspezifikation mit Herkunft und Zeitbezug.
 *
 * [specification] enthält die bereits gemappten Produktparameter. [sourceId] bezeichnet die
 * konkrete Datenquelle. [retrievedAtEpochMillis] ist der von unserer Infrastruktur erfasste
 * Abrufzeitpunkt. [sourceTimestampEpochMillis] ist ausschließlich ein vom Anbieter
 * bereitgestellter Zeitpunkt und darf `null` sein. [retrievedAtEpochMillis] darf niemals still
 * als [sourceTimestampEpochMillis] verwendet werden.
 *
 * Das Modell führt weder Freshness-Bewertung noch Validierung, Normalisierung oder Zeitumrechnung
 * durch. Es enthält keine Android-, Compose-, Provider-, DTO- oder Infrastrukturabhängigkeiten.
 */
data class KnockoutProductSpecificationSnapshot(
    val specification: KnockoutProductSpecification,
    val sourceId: String,
    val retrievedAtEpochMillis: Long,
    val sourceTimestampEpochMillis: Long?
)
