package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto

sealed interface HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError {

    data class Parsing(
        val errors: List<HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode>
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError

    data class Mapping(
        val errors: List<HsbcKnockoutProductSpecificationRecordMappingErrorCode>
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
}

sealed interface HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult {

    data class Success(
        val snapshotDto: KnockoutProductSpecificationSnapshotDto
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult

    data class Failure(
        val error: HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult
}

/**
 * Verbindet ausschließlich den kontrollierten Forschungs-JSON-Parser mit dem bestehenden
 * HSBC-Record-Mapper.
 *
 * Der Processor verarbeitet nur vorab bereinigte und ausdrücklich freigegebene lokale
 * Forschungsdaten und keine originale HSBC-Webseiten- oder Vaadin-Antwort. Parsing- und
 * Mappingfehler bleiben typisiert getrennt; Fehlerlisten werden vollständig und in
 * unveränderter Reihenfolge weitergereicht. Bei Parsingfehlern findet kein Mapping statt, und
 * bei Fehlern entsteht kein partieller Snapshot.
 *
 * `retrievedAtEpochMillis` stammt ausschließlich vom Aufrufer, während der Anbieterzeitpunkt
 * ausschließlich aus dem geparsten Record stammt. Zwischen beiden Zeitfeldern findet kein
 * stiller Ersatz statt. Der Processor enthält keine Normalisierung, Korrektur, fachliche
 * Validierung, Data-Quality-, Freshness- oder Berechnungsentscheidung und besitzt keine Datei-,
 * Netzwerk-, Provider- oder Repository-Verantwortung.
 */
object HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessor {

    fun process(
        json: String,
        retrievedAtEpochMillis: Long
    ): HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult =
        when (
            val parsingResult =
                HsbcKnockoutProductSpecificationResearchJsonParser.parse(json)
        ) {
            is HsbcKnockoutProductSpecificationResearchJsonParsingResult.Failure ->
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult.Failure(
                    HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Parsing(
                        errors = parsingResult.errors
                    )
                )

            is HsbcKnockoutProductSpecificationResearchJsonParsingResult.Success ->
                when (
                    val mappingResult = HsbcKnockoutProductSpecificationRecordMapper.map(
                        record = parsingResult.record,
                        retrievedAtEpochMillis = retrievedAtEpochMillis
                    )
                ) {
                    is HsbcKnockoutProductSpecificationRecordMappingResult.Failure ->
                        HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult
                            .Failure(
                                HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
                                    .Mapping(errors = mappingResult.errors)
                            )

                    is HsbcKnockoutProductSpecificationRecordMappingResult.Success ->
                        HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult
                            .Success(snapshotDto = mappingResult.snapshotDto)
                }
        }
}
