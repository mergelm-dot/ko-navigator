package de.konavigator.app.application.repository

import de.konavigator.app.domain.model.KnockoutProductSpecification

/**
 * Serverneutraler Repository-Port für KO-Produktspezifikationen.
 *
 * Produktspezifikation und Marktdaten bleiben getrennt. Die Produkt-ISIN wird
 * exakt und ohne Normalisierung übergeben. Spätere Implementierungen dürfen
 * lokal, remote oder serverseitig arbeiten; DTO-Mapping liegt außerhalb des
 * Ports. Der Port enthält keine Domainregeln.
 */
interface KnockoutProductSpecificationRepository {

    suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecification>
}
