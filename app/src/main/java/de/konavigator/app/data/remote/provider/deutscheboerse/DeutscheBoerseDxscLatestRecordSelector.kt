package de.konavigator.app.data.remote.provider.deutscheboerse

enum class DeutscheBoerseDxscRecordSelectionErrorCode {
    MISSING_UPDATE_DATE_AND_TIME,
    INVALID_UPDATE_DATE_AND_TIME
}

sealed interface DeutscheBoerseDxscLatestRecordSelectionResult {

    data class Success(
        val record: DeutscheBoerseDxscPretradeRecord
    ) : DeutscheBoerseDxscLatestRecordSelectionResult

    data object NotFound : DeutscheBoerseDxscLatestRecordSelectionResult

    data class Failure(
        val errors: List<DeutscheBoerseDxscRecordSelectionErrorCode>
    ) : DeutscheBoerseDxscLatestRecordSelectionResult
}

object DeutscheBoerseDxscLatestRecordSelector {

    fun select(
        records: Iterable<DeutscheBoerseDxscPretradeRecord>,
        productIsin: String
    ): DeutscheBoerseDxscLatestRecordSelectionResult {
        var matchingRecordFound = false
        var missingTimestampFound = false
        var invalidTimestampFound = false
        var latestRecord: DeutscheBoerseDxscPretradeRecord? = null
        var latestTimestamp: DeutscheBoerseUtcTimestamp? = null

        records.forEach { record ->
            if (record.instrumentIdentificationCode == productIsin) {
                matchingRecordFound = true
                val rawTimestamp = record.updateDateAndTime
                if (rawTimestamp == null) {
                    missingTimestampFound = true
                } else {
                    val timestamp = DeutscheBoerseUtcTimestampParser.parse(rawTimestamp)
                    if (timestamp == null) {
                        invalidTimestampFound = true
                    } else {
                        val currentLatestTimestamp = latestTimestamp
                        if (
                            currentLatestTimestamp == null ||
                            timestamp > currentLatestTimestamp
                        ) {
                            latestTimestamp = timestamp
                            latestRecord = record
                        }
                    }
                }
            }
        }

        if (!matchingRecordFound) {
            return DeutscheBoerseDxscLatestRecordSelectionResult.NotFound
        }

        val errors = buildList {
            if (missingTimestampFound) {
                add(
                    DeutscheBoerseDxscRecordSelectionErrorCode
                        .MISSING_UPDATE_DATE_AND_TIME
                )
            }
            if (invalidTimestampFound) {
                add(
                    DeutscheBoerseDxscRecordSelectionErrorCode
                        .INVALID_UPDATE_DATE_AND_TIME
                )
            }
        }
        if (errors.isNotEmpty()) {
            return DeutscheBoerseDxscLatestRecordSelectionResult.Failure(errors)
        }

        return DeutscheBoerseDxscLatestRecordSelectionResult.Success(
            checkNotNull(latestRecord)
        )
    }
}
