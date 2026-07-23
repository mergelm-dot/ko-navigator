package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto

interface KnockoutProductSpecificationProvider {

    suspend fun findByProductIsin(
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationDto>
}
