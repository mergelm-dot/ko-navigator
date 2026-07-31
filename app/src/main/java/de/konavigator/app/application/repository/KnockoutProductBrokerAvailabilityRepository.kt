package de.konavigator.app.application.repository

import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityResult

/**
 * Serverneutraler Application-Layer-Port zur Prüfung der Handelbarkeit konkreter Produkt-ISINs
 * bei genau einem Broker.
 *
 * Konkrete Implementierungen dürfen lokal, remote oder später serverseitig arbeiten. Die Query
 * wird exakt und ohne Normalisierung übergeben. Die Broker-ID wird nicht aus Produkt-,
 * Emittenten- oder Marktdaten abgeleitet.
 *
 * Der Port sucht keine Produktspezifikationen, lädt keine Produktmarktdaten, führt keine
 * Data-Quality- oder Freshness-Prüfung durch, berechnet keinen Zielhebel, bewertet keinen Spread
 * und führt kein Ranking durch. Er verändert keine ursprüngliche Kandidatenreihenfolge und wählt
 * weder Hauptzertifikat noch Alternativen.
 *
 * Provider-, DTO- und Mappingdetails sowie Domainregeln und UI-Texte liegen außerhalb dieses
 * Ports. Er besitzt keine Android- oder Compose-Abhängigkeit.
 */
interface KnockoutProductBrokerAvailabilityRepository {

    suspend fun findTradableProductIsins(
        query: KnockoutProductBrokerAvailabilityQuery
    ): KnockoutProductBrokerAvailabilityResult
}
