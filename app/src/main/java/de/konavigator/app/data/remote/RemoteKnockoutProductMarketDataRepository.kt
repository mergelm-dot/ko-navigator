package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.mapper.KnockoutProductMarketDataMapper
import de.konavigator.app.data.mapper.KnockoutProductMarketDataMappingResult
import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.provider.KnockoutProductMarketDataProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import de.konavigator.app.domain.model.KnockoutProductMarketData

/**
 * Remote-Data-Adapter für den bestehenden Produktmarktdaten-Repository-Port.
 *
 * Technische Provider-Ergebnisse und DTO-Mapping-Ergebnisse werden ohne
 * Normalisierung, Defaults oder fachliche Validierung auf den
 * Application-Layer-Vertrag abgebildet.
 */
class RemoteKnockoutProductMarketDataRepository(
    private val provider: KnockoutProductMarketDataProvider
) : KnockoutProductMarketDataRepository {

    override suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductMarketData> =
        when (val providerResult = provider.findByProductIsin(productIsin)) {
            is ProviderResult.Success -> map(providerResult.value)
            ProviderResult.NotFound -> RepositoryResult.NotFound
            ProviderResult.DataAccessFailure -> RepositoryResult.DataAccessFailure
        }

    private fun map(
        dto: KnockoutProductMarketDataDto
    ): RepositoryResult<KnockoutProductMarketData> =
        when (val mappingResult = KnockoutProductMarketDataMapper.map(dto)) {
            is KnockoutProductMarketDataMappingResult.Success ->
                RepositoryResult.Success(mappingResult.marketData)

            is KnockoutProductMarketDataMappingResult.Failure ->
                RepositoryResult.InvalidData
        }
}
