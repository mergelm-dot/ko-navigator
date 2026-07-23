package de.konavigator.app.data.remote.dto

/**
 * Providerneutrale externe Darstellung der Marktdaten eines KO-Produkts.
 *
 * Alle Felder sind nullable, damit fehlende externe Daten ohne stille Defaults
 * dargestellt werden können. Das DTO enthält weder Domainregeln noch
 * Normalisierung, Mappinglogik oder Infrastrukturabhängigkeiten.
 */
data class KnockoutProductMarketDataDto(
    val productIsin: String?,
    val bid: Double?,
    val ask: Double?,
    val bidTimestampEpochMillis: Long?,
    val askTimestampEpochMillis: Long?,
    val currency: String?,
    val sourceId: String?
)
