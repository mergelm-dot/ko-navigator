package de.konavigator.app.data.remote.provider.deutscheboerse

enum class DeutscheBoerseXfraRequiredColumn(
    val headerName: String
) {
    PRODUCT_STATUS("Product Status"),
    INSTRUMENT_STATUS("Instrument Status"),
    INSTRUMENT_NAME("Instrument"),
    ISIN("ISIN"),
    WKN("WKN"),
    MIC_CODE("MIC Code"),
    INSTRUMENT_TYPE("Instrument Type"),
    SETTLEMENT_CURRENCY("Settlement Currency"),
    CURRENCY("Currency"),
    WARRANT_TYPE("Warrant Type"),
    QUOTING_PERIOD_START("Quoting Period Start"),
    QUOTING_PERIOD_END("Quoting Period End")
}

enum class DeutscheBoerseXfraCsvRowParsingErrorCode {
    INVALID_HEADER,
    INVALID_DATA_ROW,
    COLUMN_COUNT_MISMATCH,
    MISSING_REQUIRED_COLUMN,
    DUPLICATE_REQUIRED_COLUMN
}

data class DeutscheBoerseXfraCsvRowParsingError(
    val code: DeutscheBoerseXfraCsvRowParsingErrorCode,
    val column: DeutscheBoerseXfraRequiredColumn? = null
)

sealed interface DeutscheBoerseXfraCsvRowParsingResult {

    data class Success(
        val record: DeutscheBoerseXfraTradableInstrumentRecord
    ) : DeutscheBoerseXfraCsvRowParsingResult

    data class Failure(
        val errors: List<DeutscheBoerseXfraCsvRowParsingError>
    ) : DeutscheBoerseXfraCsvRowParsingResult
}

object DeutscheBoerseXfraTradableInstrumentCsvRowParser {

    fun parse(
        headerLine: String,
        dataLine: String
    ): DeutscheBoerseXfraCsvRowParsingResult {
        if (headerLine.isEmpty() || QUOTE in headerLine) {
            return failure(DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_HEADER)
        }

        val headerColumns = splitColumns(headerLine)
        val requiredColumnErrors = buildList {
            DeutscheBoerseXfraRequiredColumn.entries.forEach { requiredColumn ->
                val occurrenceCount = headerColumns.count {
                    it == requiredColumn.headerName
                }
                when {
                    occurrenceCount == 0 -> add(
                        DeutscheBoerseXfraCsvRowParsingError(
                            code =
                                DeutscheBoerseXfraCsvRowParsingErrorCode
                                    .MISSING_REQUIRED_COLUMN,
                            column = requiredColumn
                        )
                    )

                    occurrenceCount > 1 -> add(
                        DeutscheBoerseXfraCsvRowParsingError(
                            code =
                                DeutscheBoerseXfraCsvRowParsingErrorCode
                                    .DUPLICATE_REQUIRED_COLUMN,
                            column = requiredColumn
                        )
                    )
                }
            }
        }
        if (requiredColumnErrors.isNotEmpty()) {
            return DeutscheBoerseXfraCsvRowParsingResult.Failure(requiredColumnErrors)
        }

        if (dataLine.isEmpty() || QUOTE in dataLine) {
            return failure(DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_DATA_ROW)
        }
        val dataColumns = splitColumns(dataLine)
        if (dataColumns.size != headerColumns.size) {
            return failure(
                DeutscheBoerseXfraCsvRowParsingErrorCode.COLUMN_COUNT_MISMATCH
            )
        }

        val indices = DeutscheBoerseXfraRequiredColumn.entries.associateWith {
            headerColumns.indexOf(it.headerName)
        }

        return DeutscheBoerseXfraCsvRowParsingResult.Success(
            DeutscheBoerseXfraTradableInstrumentRecord(
                productStatus = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.PRODUCT_STATUS),
                instrumentStatus = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.INSTRUMENT_STATUS),
                instrumentName = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.INSTRUMENT_NAME),
                isin = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.ISIN),
                wkn = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.WKN),
                micCode = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.MIC_CODE),
                instrumentType = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.INSTRUMENT_TYPE),
                settlementCurrency = dataColumns.cell(
                    indices,
                    DeutscheBoerseXfraRequiredColumn.SETTLEMENT_CURRENCY
                ),
                currency = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.CURRENCY),
                warrantType = dataColumns.cell(indices, DeutscheBoerseXfraRequiredColumn.WARRANT_TYPE),
                quotingPeriodStart = dataColumns.cell(
                    indices,
                    DeutscheBoerseXfraRequiredColumn.QUOTING_PERIOD_START
                ),
                quotingPeriodEnd = dataColumns.cell(
                    indices,
                    DeutscheBoerseXfraRequiredColumn.QUOTING_PERIOD_END
                )
            )
        )
    }

    private fun splitColumns(line: String): List<String> = buildList {
        var columnStart = 0
        line.forEachIndexed { index, character ->
            if (character == SEPARATOR) {
                add(line.substring(columnStart, index))
                columnStart = index + 1
            }
        }
        add(line.substring(columnStart))
    }

    private fun List<String>.cell(
        indices: Map<DeutscheBoerseXfraRequiredColumn, Int>,
        column: DeutscheBoerseXfraRequiredColumn
    ): String? = this[checkNotNull(indices[column])].ifEmpty { null }

    private fun failure(
        code: DeutscheBoerseXfraCsvRowParsingErrorCode
    ) = DeutscheBoerseXfraCsvRowParsingResult.Failure(
        listOf(DeutscheBoerseXfraCsvRowParsingError(code = code))
    )

    private const val SEPARATOR = ';'
    private const val QUOTE = '"'
}
