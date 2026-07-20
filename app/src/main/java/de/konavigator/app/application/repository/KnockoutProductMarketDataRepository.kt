package de.konavigator.app.application.repository

import de.konavigator.app.domain.model.KnockoutProductMarketData

/**
 * Serverneutraler Repository-Port für KO-Produktmarktdaten.
 *
 * Marktdaten und Produktspezifikation bleiben getrennt. Die Produkt-ISIN wird
 * exakt und ohne Normalisierung übergeben. Spätere Implementierungen dürfen
 * lokal, remote oder serverseitig arbeiten; DTO-Mapping liegt außerhalb des
 * Ports. Der Port enthält keine Domainregeln.
 */
interface KnockoutProductMarketDataRepository {

    suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductMarketData>
}
