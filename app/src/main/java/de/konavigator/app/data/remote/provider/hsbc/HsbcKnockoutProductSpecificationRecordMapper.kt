package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto

enum class HsbcKnockoutProductSpecificationRecordMappingErrorCode {
    UNSUPPORTED_DIRECTION_LABEL
}

sealed interface HsbcKnockoutProductSpecificationRecordMappingResult {

    data class Success(
        val snapshotDto: KnockoutProductSpecificationSnapshotDto
    ) : HsbcKnockoutProductSpecificationRecordMappingResult

    data class Failure(
        val errors: List<HsbcKnockoutProductSpecificationRecordMappingErrorCode>
    ) : HsbcKnockoutProductSpecificationRecordMappingResult
}

/**
 * Mappt einen bereits bereinigten lokalen HSBC-Forschungs-Record in den providerneutralen
 * Produktspezifikations-Snapshot-DTO.
 *
 * Der Mapper verarbeitet keine originale HSBC- oder Vaadin-Antwort und unterstützt
 * ausschließlich die exakten Richtungslabels `Call` und `Put`; ein `null`-Label bleibt `null`.
 * Fehlende Pflichtwerte werden nicht hier bewertet, sondern an den bestehenden generischen
 * Produktspezifikations-Mapper weitergereicht. Kennungen, Produktwerte und Währungen werden
 * exakt übernommen. [SOURCE_ID] kennzeichnet ausschließlich diesen kontrollierten lokalen
 * Forschungsweg.
 *
 * `retrievedAtEpochMillis` wird ausschließlich vom Aufrufer bereitgestellt. Der
 * Anbieterzeitpunkt wird exakt übernommen und niemals durch den Abrufzeitpunkt ersetzt. Der
 * Mapper verwendet keine Systemzeit, Normalisierung, Korrektur, fachliche Validierung,
 * Data-Quality-, Freshness- oder Berechnungsentscheidung und besitzt keine Datei-, Netzwerk-,
 * Provider- oder Repository-Verantwortung.
 */
object HsbcKnockoutProductSpecificationRecordMapper {

    const val SOURCE_ID = "HSBC_RESEARCH_LOCAL"

    fun map(
        record: HsbcKnockoutProductSpecificationRecord,
        retrievedAtEpochMillis: Long
    ): HsbcKnockoutProductSpecificationRecordMappingResult {
        val mappedDirection = when (record.directionLabel) {
            null -> null
            "Call" -> "LONG"
            "Put" -> "SHORT"
            else -> return HsbcKnockoutProductSpecificationRecordMappingResult.Failure(
                listOf(
                    HsbcKnockoutProductSpecificationRecordMappingErrorCode
                        .UNSUPPORTED_DIRECTION_LABEL
                )
            )
        }

        return HsbcKnockoutProductSpecificationRecordMappingResult.Success(
            KnockoutProductSpecificationSnapshotDto(
                specification = KnockoutProductSpecificationDto(
                    productIsin = record.productIsin,
                    productWkn = record.productWkn,
                    issuerId = record.issuerId,
                    underlyingId = record.underlyingId,
                    direction = mappedDirection,
                    basePrice = record.basePrice,
                    knockoutBarrier = record.knockoutBarrier,
                    ratio = record.ratio,
                    underlyingCurrency = record.underlyingCurrency,
                    productCurrency = record.productCurrency
                ),
                sourceId = SOURCE_ID,
                retrievedAtEpochMillis = retrievedAtEpochMillis,
                sourceTimestampEpochMillis = record.sourceTimestampEpochMillis
            )
        )
    }
}
