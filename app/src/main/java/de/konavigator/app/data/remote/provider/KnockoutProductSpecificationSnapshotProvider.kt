package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto

/**
 * Providerneutraler Vertrag für den technischen Abruf von KO-Produktspezifikations-Snapshots.
 *
 * Eine erfolgreiche Antwort enthält Produktausstattung, Quellenkennung und Zeitbezug gemeinsam
 * in einem Snapshot. Die übergebene Produkt-ISIN wird vom Vertrag nicht normalisiert.
 * [ProviderResult.NotFound] und [ProviderResult.DataAccessFailure] bleiben technisch getrennte
 * Ergebnisse.
 *
 * Freshness-Bewertung, Domainvalidierung und Mapping gehören nicht in diesen Vertrag. Der Vertrag
 * beschreibt weder Netzwerktransport noch Dateiformat oder einen konkreten Emittenten.
 */
interface KnockoutProductSpecificationSnapshotProvider {

    suspend fun findByProductIsin(
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationSnapshotDto>
}
