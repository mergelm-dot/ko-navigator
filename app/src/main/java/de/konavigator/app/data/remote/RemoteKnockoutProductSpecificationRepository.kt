package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.mapper.KnockoutProductSpecificationMapper
import de.konavigator.app.data.mapper.KnockoutProductSpecificationMappingResult
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import de.konavigator.app.domain.model.KnockoutProductSpecification

/**
 * Remote-Data-Adapter für den bestehenden Produktspezifikations-Repository-Port.
 *
 * Technische Provider-Ergebnisse und DTO-Mapping-Ergebnisse werden ohne
 * Normalisierung, Defaults oder fachliche Validierung auf den
 * Application-Layer-Vertrag abgebildet.
 */
class RemoteKnockoutProductSpecificationRepository(
    private val provider: KnockoutProductSpecificationProvider
) : KnockoutProductSpecificationRepository {

    override suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecification> =
        when (val providerResult = provider.findByProductIsin(productIsin)) {
            is ProviderResult.Success -> map(providerResult.value)
            ProviderResult.NotFound -> RepositoryResult.NotFound
            ProviderResult.DataAccessFailure -> RepositoryResult.DataAccessFailure
        }

    private fun map(
        dto: KnockoutProductSpecificationDto
    ): RepositoryResult<KnockoutProductSpecification> =
        when (val mappingResult = KnockoutProductSpecificationMapper.map(dto)) {
            is KnockoutProductSpecificationMappingResult.Success ->
                RepositoryResult.Success(mappingResult.specification)

            is KnockoutProductSpecificationMappingResult.Failure ->
                RepositoryResult.InvalidData
        }
}
