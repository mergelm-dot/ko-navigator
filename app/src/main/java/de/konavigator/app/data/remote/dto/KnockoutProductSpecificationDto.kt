package de.konavigator.app.data.remote.dto

/**
 * Providerneutrale externe Darstellung einer KO-Produktspezifikation.
 *
 * Alle Felder sind nullable, damit fehlende externe Daten ohne stille Defaults
 * dargestellt werden können. Das DTO enthält weder Domainregeln noch
 * Normalisierung, Mappinglogik oder Infrastrukturabhängigkeiten.
 */
data class KnockoutProductSpecificationDto(
    val productIsin: String?,
    val productWkn: String?,
    val issuerId: String?,
    val underlyingId: String?,
    val direction: String?,
    val basePrice: Double?,
    val knockoutBarrier: Double?,
    val ratio: Double?,
    val underlyingCurrency: String?,
    val productCurrency: String?
)
