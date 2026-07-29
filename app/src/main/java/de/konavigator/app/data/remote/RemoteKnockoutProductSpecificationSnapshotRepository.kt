package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.KnockoutProductSpecificationSnapshotRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.mapper.KnockoutProductSpecificationSnapshotMapper
import de.konavigator.app.data.mapper.KnockoutProductSpecificationSnapshotMappingResult
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Remote-Data-Adapter für den neuen Produktspezifikations-Snapshot-Repository-Port.
 *
 * Der Adapter verbindet den providerneutralen technischen Snapshot-Provider mit dem
 * Application-Layer-Port. Erfolgreiche Provider-Ergebnisse werden ausschließlich über den
 * bestehenden [KnockoutProductSpecificationSnapshotMapper] übersetzt. `NotFound` und
 * `DataAccessFailure` bleiben entsprechende Repository-Zustände; Mappingfehler werden zu
 * [RepositoryResult.InvalidData].
 *
 * Die Produkt-ISIN wird exakt und ohne Normalisierung an den Provider übergeben. Der Adapter
 * enthält keine Defaults, fachliche Validierung, Freshness-Bewertung, Systemzeit oder
 * Zeitumrechnung und kennt weder einen konkreten Emittenten noch ein Dateiformat.
 */
class RemoteKnockoutProductSpecificationSnapshotRepository(
    private val provider: KnockoutProductSpecificationSnapshotProvider
) : KnockoutProductSpecificationSnapshotRepository {

    override suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecificationSnapshot> =
        when (val providerResult = provider.findByProductIsin(productIsin)) {
            is ProviderResult.Success -> map(providerResult.value)
            ProviderResult.NotFound -> RepositoryResult.NotFound
            ProviderResult.DataAccessFailure -> RepositoryResult.DataAccessFailure
        }

    private fun map(
        dto: KnockoutProductSpecificationSnapshotDto
    ): RepositoryResult<KnockoutProductSpecificationSnapshot> =
        when (
            val mappingResult = KnockoutProductSpecificationSnapshotMapper.map(dto)
        ) {
            is KnockoutProductSpecificationSnapshotMappingResult.Success ->
                RepositoryResult.Success(mappingResult.snapshot)

            is KnockoutProductSpecificationSnapshotMappingResult.Failure ->
                RepositoryResult.InvalidData
        }
}
