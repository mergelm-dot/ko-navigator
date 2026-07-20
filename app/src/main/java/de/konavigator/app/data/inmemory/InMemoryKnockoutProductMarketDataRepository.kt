package de.konavigator.app.data.inmemory

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.model.KnockoutProductMarketData

/**
 * Read-only In-Memory-Data-Adapter für den bestehenden
 * [KnockoutProductMarketDataRepository]-Port.
 *
 * Die Suche verwendet die Produkt-ISIN exakt, case- und whitespace-sensitiv
 * und ohne Normalisierung. Beim Erzeugen wird ein defensiver Map-Snapshot der
 * übergebenen Daten gebildet; exakt doppelte ISINs sind unzulässig. Lookups
 * liefern ausschließlich [RepositoryResult.Success] oder
 * [RepositoryResult.NotFound].
 *
 * Der Adapter validiert keine Domainmodelle und enthält weder Netzwerk- noch
 * Datenbanklogik. Er enthält keine Produktionsdaten, wird nicht automatisch in
 * eine Release- oder Demo-Composition eingebunden und ersetzt keine echte
 * Provideranbindung.
 */
class InMemoryKnockoutProductMarketDataRepository(
    marketData: List<KnockoutProductMarketData>
) : KnockoutProductMarketDataRepository {

    private val marketDataByProductIsin: Map<String, KnockoutProductMarketData> =
        marketData.toList().let { snapshot ->
            require(snapshot.map { it.productIsin }.distinct().size == snapshot.size) {
                "marketData must contain unique productIsin values"
            }
            snapshot.associateBy { it.productIsin }
        }

    override suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductMarketData> =
        marketDataByProductIsin[productIsin]
            ?.let { RepositoryResult.Success(it) }
            ?: RepositoryResult.NotFound
}
