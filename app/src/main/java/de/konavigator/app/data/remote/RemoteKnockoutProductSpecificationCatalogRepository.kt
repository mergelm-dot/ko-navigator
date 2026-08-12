package de.konavigator.app.data.remote

import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogResult
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.data.mapper.KnockoutProductSpecificationSnapshotMapper
import de.konavigator.app.data.mapper.KnockoutProductSpecificationSnapshotMappingResult
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationCatalogProvider
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationCatalogProviderResult
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Remote-Data-Adapter zwischen dem technischen Katalogprovider und dem Application-Port.
 *
 * Erfolgreiche Provider-Kandidaten werden ausschließlich mit dem bestehenden
 * [KnockoutProductSpecificationSnapshotMapper] abgebildet. Ein einzelner Mappingfehler macht
 * das gesamte Katalogergebnis zu [KnockoutProductSpecificationCatalogResult.InvalidData], damit
 * keine unvollständige Kandidatenliste weitergegeben wird.
 */
class RemoteKnockoutProductSpecificationCatalogRepository(
    private val provider: KnockoutProductSpecificationCatalogProvider
) : KnockoutProductSpecificationCatalogRepository {

    override suspend fun findCandidates(
        query: KnockoutProductSpecificationCatalogQuery
    ): KnockoutProductSpecificationCatalogResult =
        when (
            val providerResult = provider.findCandidates(
                underlyingId = query.underlyingId,
                direction = query.direction
            )
        ) {
            is KnockoutProductSpecificationCatalogProviderResult.Success ->
                mapCandidates(providerResult.candidates)

            KnockoutProductSpecificationCatalogProviderResult.DataAccessFailure ->
                KnockoutProductSpecificationCatalogResult.DataAccessFailure

            KnockoutProductSpecificationCatalogProviderResult.InvalidData ->
                KnockoutProductSpecificationCatalogResult.InvalidData
        }

    private fun mapCandidates(
        candidates: List<KnockoutProductSpecificationSnapshotDto>
    ): KnockoutProductSpecificationCatalogResult {
        val snapshots = ArrayList<KnockoutProductSpecificationSnapshot>(candidates.size)

        for (candidate in candidates) {
            when (val mappingResult = KnockoutProductSpecificationSnapshotMapper.map(candidate)) {
                is KnockoutProductSpecificationSnapshotMappingResult.Success ->
                    snapshots += mappingResult.snapshot

                is KnockoutProductSpecificationSnapshotMappingResult.Failure ->
                    return KnockoutProductSpecificationCatalogResult.InvalidData
            }
        }

        return KnockoutProductSpecificationCatalogResult.Success(snapshots)
    }
}
