package de.konavigator.app.application.repository

import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

/**
 * Serverneutraler Application-Layer-Port für KO-Produktspezifikations-Snapshots.
 *
 * Eine erfolgreiche Antwort enthält Produktspezifikation, Quelle und Zeitbezug gemeinsam.
 * Produktspezifikations-Snapshot und Produktmarktdaten bleiben getrennte fachliche Daten. Die
 * Produkt-ISIN wird exakt und ohne Normalisierung übergeben. Implementierungen dürfen lokal,
 * remote oder später serverseitig arbeiten; Provider-, DTO- und Mappingdetails liegen außerhalb
 * des Ports.
 *
 * Freshness-Bewertung und Data-Quality-Entscheidungen gehören nicht in diesen Vertrag. Der Port
 * enthält keine Domainregeln oder UI-Texte.
 */
interface KnockoutProductSpecificationSnapshotRepository {

    suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecificationSnapshot>
}
