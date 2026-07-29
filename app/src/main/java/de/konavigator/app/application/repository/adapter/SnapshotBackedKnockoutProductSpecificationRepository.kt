package de.konavigator.app.application.repository.adapter

import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationSnapshotRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.model.KnockoutProductSpecification

/**
 * Reine Kompatibilitätsbrücke für bestehende Application-Services.
 *
 * Der Adapter delegiert den Abruf an den neuen Snapshot-Repository-Port und reicht die
 * Produkt-ISIN exakt und ohne Normalisierung weiter. Bei Erfolg wird exakt die im Snapshot
 * enthaltene [KnockoutProductSpecification] zurückgegeben. Quelle und Zeitwerte werden nicht
 * verändert, können über den alten Repository-Port konstruktionsbedingt jedoch nicht
 * transportiert werden. Neue snapshotfähige Verbraucher sollen deshalb direkt den Snapshot-Port
 * verwenden.
 *
 * `NotFound`, `DataAccessFailure` und `InvalidData` bleiben unverändert erhalten. Der Adapter
 * enthält weder Freshness-Bewertung, Data-Quality-Entscheidung, Validierung, Normalisierung,
 * Zeitumrechnung oder Systemzeit noch Provider-, DTO-, Netzwerk- oder Dateikenntnis.
 */
class SnapshotBackedKnockoutProductSpecificationRepository(
    private val snapshotRepository: KnockoutProductSpecificationSnapshotRepository
) : KnockoutProductSpecificationRepository {

    override suspend fun findByProductIsin(
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecification> =
        when (val result = snapshotRepository.findByProductIsin(productIsin)) {
            is RepositoryResult.Success ->
                RepositoryResult.Success(result.value.specification)

            RepositoryResult.NotFound -> RepositoryResult.NotFound
            RepositoryResult.DataAccessFailure -> RepositoryResult.DataAccessFailure
            RepositoryResult.InvalidData -> RepositoryResult.InvalidData
        }
}
