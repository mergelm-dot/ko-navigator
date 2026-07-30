package de.konavigator.app.application.repository

import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogResult

/**
 * Serverneutraler Application-Layer-Port zur Suche nach Produktspezifikations-Snapshots anhand
 * eines eindeutigen Basiswerts und einer Handelsrichtung.
 *
 * Konkrete Implementierungen dürfen lokal, remote oder später serverseitig arbeiten. Die Query
 * wird exakt und ohne Normalisierung übergeben. Der Port nimmt keine Broker-ID entgegen;
 * Broker-Verfügbarkeit wird später als separater verpflichtender Filter angewendet. Er führt
 * kein Ranking durch, verwendet weder Zielhebel noch Basiswertkurs, lädt keine
 * Produktmarktdaten, bewertet keinen Spread und wählt weder Hauptzertifikat noch Alternativen.
 *
 * Provider-, DTO- und Mappingdetails sowie Domainregeln und UI-Texte liegen außerhalb dieses
 * Ports. Er besitzt keine Android- oder Compose-Abhängigkeit.
 */
interface KnockoutProductSpecificationCatalogRepository {

    suspend fun findCandidates(
        query: KnockoutProductSpecificationCatalogQuery
    ): KnockoutProductSpecificationCatalogResult
}
