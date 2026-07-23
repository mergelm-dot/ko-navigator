package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto

/**
 * Read-only Mock-Provider für lokale KO-Produktspezifikations-DTOs.
 *
 * Die Suche verwendet die übergebene Produkt-ISIN exakt und ohne
 * Normalisierung. Beim Erzeugen wird ein defensiver Snapshot der Map gebildet.
 */
class InMemoryKnockoutProductSpecificationProvider(
    specificationsByProductIsin: Map<String, KnockoutProductSpecificationDto>
) : KnockoutProductSpecificationProvider {

    private val specificationsByProductIsin = specificationsByProductIsin.toMap()

    override suspend fun findByProductIsin(
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationDto> =
        specificationsByProductIsin[productIsin]
            ?.let { ProviderResult.Success(it) }
            ?: ProviderResult.NotFound
}
