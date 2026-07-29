package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto

/**
 * Read-only Mock-Provider für lokale KO-Produktspezifikations-Snapshots.
 *
 * Die Suche verwendet den exakt übergebenen Map-Schlüssel und normalisiert die Produkt-ISIN
 * nicht. Beim Erzeugen wird ein defensiver Snapshot der Map gebildet. Die enthaltenen
 * Snapshot-Objekte werden nicht kopiert, verändert, validiert oder normalisiert; Quelle und
 * Zeitwerte bleiben exakt erhalten.
 */
class InMemoryKnockoutProductSpecificationSnapshotProvider(
    snapshotsByProductIsin: Map<String, KnockoutProductSpecificationSnapshotDto>
) : KnockoutProductSpecificationSnapshotProvider {

    private val snapshotsByProductIsin = snapshotsByProductIsin.toMap()

    override suspend fun findByProductIsin(
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationSnapshotDto> =
        snapshotsByProductIsin[productIsin]
            ?.let { ProviderResult.Success(it) }
            ?: ProviderResult.NotFound
}
