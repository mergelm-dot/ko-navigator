package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
        val timestampEpochMillis = timestamp?.let(::parseUtcEpochMillis)
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
            if (timestamp != null && timestampEpochMillis == null) {
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

    private fun parseUtcEpochMillis(value: String): Long? {
        val match = UTC_TIMESTAMP_PATTERN.matchEntire(value) ?: return null
        val seconds = match.groupValues[1]
        val fraction = match.groupValues[2]
        val formatter = SimpleDateFormat(UTC_SECONDS_PATTERN, Locale.ROOT).apply {
            isLenient = false
            timeZone = UTC_TIME_ZONE
        }
        val position = ParsePosition(0)
        val date = formatter.parse(seconds, position) ?: return null
        if (position.index != seconds.length || position.errorIndex >= 0) {
            return null
        }
        val milliseconds = fraction
            .padEnd(MILLISECOND_DIGITS, '0')
            .take(MILLISECOND_DIGITS)
            .toLong()
        return date.time + milliseconds
    }

    private val UTC_TIMESTAMP_PATTERN =
        Regex("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d{1,9}))?Z$")
    private const val UTC_SECONDS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
    private const val MILLISECOND_DIGITS = 3
    private val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")
}
