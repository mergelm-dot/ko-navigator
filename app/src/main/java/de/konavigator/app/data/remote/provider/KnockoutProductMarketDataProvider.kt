package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto

interface KnockoutProductMarketDataProvider {

    suspend fun findByProductIsin(
        productIsin: String
    ): ProviderResult<KnockoutProductMarketDataDto>
}
