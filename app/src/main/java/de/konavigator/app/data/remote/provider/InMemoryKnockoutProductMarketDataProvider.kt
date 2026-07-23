package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto

/**
 * Read-only Mock-Provider für lokale KO-Produktmarktdaten-DTOs.
 *
 * Die Suche verwendet die übergebene Produkt-ISIN exakt und ohne
 * Normalisierung. Beim Erzeugen wird ein defensiver Snapshot der Map gebildet.
 */
class InMemoryKnockoutProductMarketDataProvider(
    marketDataByProductIsin: Map<String, KnockoutProductMarketDataDto>
) : KnockoutProductMarketDataProvider {

    private val marketDataByProductIsin = marketDataByProductIsin.toMap()

    override suspend fun findByProductIsin(
        productIsin: String
    ): ProviderResult<KnockoutProductMarketDataDto> =
        marketDataByProductIsin[productIsin]
            ?.let { ProviderResult.Success(it) }
            ?: ProviderResult.NotFound
}
