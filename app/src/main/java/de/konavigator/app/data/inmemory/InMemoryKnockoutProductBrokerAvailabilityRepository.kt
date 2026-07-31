package de.konavigator.app.data.inmemory

import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityResult
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository

/**
 * Read-only In-Memory-Adapter für den bestehenden
 * [KnockoutProductBrokerAvailabilityRepository]-Port.
 *
 * Er dient ausschließlich deterministischen lokalen Tests und kontrollierten Demo- und
 * Entwicklungsszenarien. Die Konstruktorzuordnung beschreibt bestätigte Handelbarkeit als
 * Broker-ID zu Produkt-ISIN-Menge. Beim Erzeugen wird ein defensiver Snapshot der Map gebildet;
 * zusätzlich erzeugt `toSet()` einen defensiven Snapshot jeder enthaltenen ISIN-Menge. Spätere
 * Änderungen an der ursprünglichen Map oder ihren Sets beeinflussen den Adapter nicht.
 *
 * Broker-ID und Produkt-ISINs werden exakt sowie case- und whitespace-sensitiv verglichen, ohne
 * Normalisierung, `trim()` oder Änderung der Groß-/Kleinschreibung. Das Ergebnis enthält nur
 * angefragte Produkt-ISINs; hinterlegte, aber nicht angefragte ISINs werden nicht zurückgegeben.
 * Query-Duplikate führen aufgrund des Ergebnisvertrags nur zu einer Mitgliedschaft in der
 * Ergebnismenge. Diese definiert keine fachliche Ranking- oder Anzeigereihenfolge.
 *
 * Ein unbekannter Broker, eine leere Query-Liste oder eine leere Konstruktorzuordnung ergibt
 * [KnockoutProductBrokerAvailabilityResult.Success] mit leerer Menge. Es gibt kein `NotFound`,
 * und der Adapter erzeugt weder [KnockoutProductBrokerAvailabilityResult.DataAccessFailure] noch
 * [KnockoutProductBrokerAvailabilityResult.InvalidData].
 *
 * Der Adapter führt keine Broker-, Produkt- oder Domainvalidierung und keine Ableitung von ISINs
 * aus WKN oder anderen Kennungen durch. Er fragt weder Produktspezifikationen noch Marktdaten ab
 * und trifft keine Emittenten-, Zielhebel-, Spread-, Score- oder Rankingentscheidung. Er enthält
 * keine Netzwerk- oder Datenbanklogik und keine Android- oder Compose-Abhängigkeit. Er wird nicht
 * automatisch in eine Release-Composition eingebunden und ersetzt keine spätere echte
 * Broker-Verfügbarkeitsquelle.
 */
class InMemoryKnockoutProductBrokerAvailabilityRepository(
    tradableProductIsinsByBrokerId: Map<String, Set<String>>
) : KnockoutProductBrokerAvailabilityRepository {

    private val tradableProductIsinsByBrokerId: Map<String, Set<String>> =
        tradableProductIsinsByBrokerId.entries.associate {
            (brokerId, productIsins) ->
            brokerId to productIsins.toSet()
        }

    override suspend fun findTradableProductIsins(
        query: KnockoutProductBrokerAvailabilityQuery
    ): KnockoutProductBrokerAvailabilityResult {
        val tradableProductIsins =
            tradableProductIsinsByBrokerId[query.brokerId] ?: emptySet()

        val requestedTradableProductIsins =
            query.productIsins.filterTo(linkedSetOf()) { productIsin ->
                productIsin in tradableProductIsins
            }

        return KnockoutProductBrokerAvailabilityResult.Success(
            tradableProductIsins = requestedTradableProductIsins
        )
    }
}
