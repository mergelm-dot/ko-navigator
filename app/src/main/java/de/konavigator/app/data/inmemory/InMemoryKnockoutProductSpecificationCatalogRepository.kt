package de.konavigator.app.data.inmemory

import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogResult
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Read-only In-Memory-Adapter für den bestehenden
 * [KnockoutProductSpecificationCatalogRepository]-Port.
 *
 * Er dient ausschließlich deterministischen lokalen Tests und kontrollierten Demo- und
 * Entwicklungsszenarien. Beim Erzeugen bildet [toList] einen defensiven Snapshot der
 * übergebenen Liste; die übergebenen Snapshot-Objekte selbst werden nicht verändert.
 *
 * Die Suche verwendet `underlyingId` exakt sowie case- und whitespace-sensitiv, ohne
 * Normalisierung, `trim()` oder Änderung der Groß-/Kleinschreibung. `TradeDirection` wird exakt
 * verglichen. Suchergebnisse behalten Reihenfolge und Duplikate der gespeicherten Liste. Auch
 * mehrere Produkte desselben Emittenten bleiben erhalten. Es erfolgen weder Sortierung,
 * Gruppierung, Deduplizierung noch eine Begrenzung der Trefferzahl.
 *
 * Es gibt kein `NotFound`: Kein Treffer ergibt [KnockoutProductSpecificationCatalogResult.Success]
 * mit leerer Liste. Der Adapter erzeugt weder
 * [KnockoutProductSpecificationCatalogResult.DataAccessFailure] noch
 * [KnockoutProductSpecificationCatalogResult.InvalidData]. Er führt keine Domainvalidierung und
 * keine Broker-, Emittenten-, Marktdaten-, Hebel-, Spread- oder Rankingentscheidung durch.
 *
 * Der Adapter enthält keine Netzwerk- oder Datenbanklogik und keine Android- oder
 * Compose-Abhängigkeit. Er wird nicht automatisch in eine Release-Composition eingebunden und
 * ersetzt keine spätere echte Katalog- oder Provideranbindung.
 */
class InMemoryKnockoutProductSpecificationCatalogRepository(
    snapshots: List<KnockoutProductSpecificationSnapshot>
) : KnockoutProductSpecificationCatalogRepository {

    private val snapshots: List<KnockoutProductSpecificationSnapshot> =
        snapshots.toList()

    override suspend fun findCandidates(
        query: KnockoutProductSpecificationCatalogQuery
    ): KnockoutProductSpecificationCatalogResult =
        KnockoutProductSpecificationCatalogResult.Success(
            candidates = snapshots.filter { snapshot ->
                snapshot.specification.underlyingId == query.underlyingId &&
                    snapshot.specification.direction == query.direction
            }
        )
}
