package de.konavigator.app.data.mapper

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot

sealed interface KnockoutProductSpecificationSnapshotMappingResult {

    data class Success(
        val snapshot: KnockoutProductSpecificationSnapshot
    ) : KnockoutProductSpecificationSnapshotMappingResult

    data class Failure(
        val errors: List<KnockoutProductSpecificationMappingError>
    ) : KnockoutProductSpecificationSnapshotMappingResult
}

/**
 * Übersetzt ausschließlich den technischen Transport-Snapshot in den entsprechenden
 * Domain-Snapshot.
 *
 * Das Mapping der Produktausstattung wird vollständig an den bestehenden
 * [KnockoutProductSpecificationMapper] delegiert. Dessen typisierte Mappingfehler werden weder
 * ersetzt, ergänzt, entfernt noch in andere Fehlertypen übersetzt. Bei erfolgreichem Mapping
 * werden Quellenkennung und beide Zeitfelder exakt übernommen; [KnockoutProductSpecificationSnapshotDto.retrievedAtEpochMillis]
 * wird niemals still als Anbieterzeitpunkt eingesetzt.
 *
 * Der Mapper führt keine Freshness-Bewertung durch, validiert oder normalisiert weder
 * Quellenkennung noch Zeitwerte und liest keine Systemzeit.
 */
object KnockoutProductSpecificationSnapshotMapper {

    fun map(
        dto: KnockoutProductSpecificationSnapshotDto
    ): KnockoutProductSpecificationSnapshotMappingResult =
        when (
            val specificationResult =
                KnockoutProductSpecificationMapper.map(dto.specification)
        ) {
            is KnockoutProductSpecificationMappingResult.Success ->
                KnockoutProductSpecificationSnapshotMappingResult.Success(
                    KnockoutProductSpecificationSnapshot(
                        specification = specificationResult.specification,
                        sourceId = dto.sourceId,
                        retrievedAtEpochMillis = dto.retrievedAtEpochMillis,
                        sourceTimestampEpochMillis = dto.sourceTimestampEpochMillis
                    )
                )

            is KnockoutProductSpecificationMappingResult.Failure ->
                KnockoutProductSpecificationSnapshotMappingResult.Failure(
                    errors = specificationResult.errors
                )
        }
}
