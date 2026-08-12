package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationCatalogProvider
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationCatalogProviderResult
import de.konavigator.app.domain.model.TradeDirection

/**
 * Katalogprovider für kontrollierte, bereits verarbeitete lokale HSBC-Forschungsdaten.
 *
 * Die Eingabeliste wird defensiv kopiert. Eine Suche filtert ausschließlich anhand der exakten
 * Basiswert-ID und der bereits providerneutral gemappten Handelsrichtung. Reihenfolge und
 * Duplikate bleiben erhalten. Der Provider enthält keine Parsing-, Mapping-, Netzwerk-,
 * Freshness-, Data-Quality-, Broker-, Berechnungs- oder Rankinglogik.
 */
class HsbcResearchKnockoutProductSpecificationCatalogProvider(
    snapshots: List<KnockoutProductSpecificationSnapshotDto>
) : KnockoutProductSpecificationCatalogProvider {

    private val snapshots = snapshots.toList()

    override suspend fun findCandidates(
        underlyingId: String,
        direction: TradeDirection
    ): KnockoutProductSpecificationCatalogProviderResult =
        KnockoutProductSpecificationCatalogProviderResult.Success(
            candidates = snapshots.filter { snapshot ->
                snapshot.specification.underlyingId == underlyingId &&
                    snapshot.specification.direction == direction.name
            }
        )
}
