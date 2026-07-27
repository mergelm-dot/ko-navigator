package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto

enum class DeutscheBoerseMarketDataMappingErrorCode {
    MISSING_DXSC_ISIN,
    MISSING_XFRA_ISIN,
    ISIN_MISMATCH,
    INVALID_UPDATE_DATE_AND_TIME
}

sealed interface DeutscheBoerseMarketDataMappingResult {

    data class Success(
        val dto: KnockoutProductMarketDataDto
    ) : DeutscheBoerseMarketDataMappingResult

    data class Failure(
        val errors: List<DeutscheBoerseMarketDataMappingErrorCode>
    ) : DeutscheBoerseMarketDataMappingResult
}

/** Maps compatible DXSC quotes and XFRA reference data without domain evaluation. */
object DeutscheBoerseKnockoutProductMarketDataMapper {

    const val SOURCE_ID = "DEUTSCHE_BOERSE_DXSC_DELAYED"

    fun map(
        dxscRecord: DeutscheBoerseDxscPretradeRecord,
        xfraRecord: DeutscheBoerseXfraTradableInstrumentRecord
    ): DeutscheBoerseMarketDataMappingResult {
        val dxscIsin = dxscRecord.instrumentIdentificationCode
        val xfraIsin = xfraRecord.isin
        val timestamp = dxscRecord.updateDateAndTime
        val parsedTimestamp = timestamp?.let(DeutscheBoerseUtcTimestampParser::parse)
        val timestampEpochMillis = parsedTimestamp?.epochMillis
        val errors = buildList {
            if (dxscIsin == null) {
                add(DeutscheBoerseMarketDataMappingErrorCode.MISSING_DXSC_ISIN)
            }
            if (xfraIsin == null) {
                add(DeutscheBoerseMarketDataMappingErrorCode.MISSING_XFRA_ISIN)
            }
            if (dxscIsin != null && xfraIsin != null && dxscIsin != xfraIsin) {
                add(DeutscheBoerseMarketDataMappingErrorCode.ISIN_MISMATCH)
            }
            if (timestamp != null && parsedTimestamp == null) {
                add(
                    DeutscheBoerseMarketDataMappingErrorCode
                        .INVALID_UPDATE_DATE_AND_TIME
                )
            }
        }
        if (errors.isNotEmpty()) {
            return DeutscheBoerseMarketDataMappingResult.Failure(errors)
        }

        return DeutscheBoerseMarketDataMappingResult.Success(
            KnockoutProductMarketDataDto(
                productIsin = dxscIsin,
                bid = dxscRecord.bestBid,
                ask = dxscRecord.bestAsk,
                bidTimestampEpochMillis = timestampEpochMillis,
                askTimestampEpochMillis = timestampEpochMillis,
                currency = xfraRecord.currency,
                sourceId = SOURCE_ID
            )
        )
    }

}
