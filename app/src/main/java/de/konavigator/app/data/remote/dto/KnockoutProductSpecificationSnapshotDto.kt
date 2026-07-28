package de.konavigator.app.data.remote.dto

/**
 * Providerneutrale Transporthülle für eine KO-Produktspezifikation mit Herkunft und Zeitbezug.
 *
 * [specification] enthält ausschließlich die providerneutrale Produktausstattung.
 * [sourceId] bezeichnet die konkrete Datenquelle.
 * [retrievedAtEpochMillis] ist der von unserer Infrastruktur erfasste Abrufzeitpunkt.
 * [sourceTimestampEpochMillis] ist ausschließlich ein vom Anbieter gelieferter Zeitpunkt und
 * darf `null` sein. [retrievedAtEpochMillis] darf niemals still als
 * [sourceTimestampEpochMillis] eingesetzt werden.
 *
 * Der Vertrag enthält keine Domainvalidierung, Normalisierung oder Freshness-Entscheidung.
 */
data class KnockoutProductSpecificationSnapshotDto(
    val specification: KnockoutProductSpecificationDto,
    val sourceId: String,
    val retrievedAtEpochMillis: Long,
    val sourceTimestampEpochMillis: Long?
)
