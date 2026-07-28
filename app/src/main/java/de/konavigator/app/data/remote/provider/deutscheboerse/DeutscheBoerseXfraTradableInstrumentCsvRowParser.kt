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

internal class DeutscheBoerseXfraPreparedHeader internal constructor(
    internal val columnCount: Int,
    requiredColumnIndices: Map<DeutscheBoerseXfraRequiredColumn, Int>
) {
    internal val requiredColumnIndices:
        Map<DeutscheBoerseXfraRequiredColumn, Int> =
        requiredColumnIndices.toMap()
}

internal sealed interface DeutscheBoerseXfraHeaderPreparationResult {

    data class Success(
        val preparedHeader: DeutscheBoerseXfraPreparedHeader
    ) : DeutscheBoerseXfraHeaderPreparationResult

    data class Failure(
        val errors: List<DeutscheBoerseXfraCsvRowParsingError>
    ) : DeutscheBoerseXfraHeaderPreparationResult
}

object DeutscheBoerseXfraTradableInstrumentCsvRowParser {

    fun parse(
        headerLine: String,
        dataLine: String
    ): DeutscheBoerseXfraCsvRowParsingResult =
        when (val headerResult = prepareHeader(headerLine)) {
            is DeutscheBoerseXfraHeaderPreparationResult.Failure ->
                DeutscheBoerseXfraCsvRowParsingResult.Failure(
                    errors = headerResult.errors
                )

            is DeutscheBoerseXfraHeaderPreparationResult.Success ->
                parse(
                    preparedHeader = headerResult.preparedHeader,
                    dataLine = dataLine
                )
        }

    internal fun prepareHeader(
        headerLine: String
    ): DeutscheBoerseXfraHeaderPreparationResult {
        if (headerLine.isEmpty() || QUOTE in headerLine) {
            return headerFailure(
                DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_HEADER
            )
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
            return DeutscheBoerseXfraHeaderPreparationResult.Failure(
                requiredColumnErrors
            )
        }

        val requiredColumnIndices =
            DeutscheBoerseXfraRequiredColumn.entries.associateWith {
                headerColumns.indexOf(it.headerName)
            }

        return DeutscheBoerseXfraHeaderPreparationResult.Success(
            DeutscheBoerseXfraPreparedHeader(
                columnCount = headerColumns.size,
                requiredColumnIndices = requiredColumnIndices
            )
        )
    }

    internal fun parse(
        preparedHeader: DeutscheBoerseXfraPreparedHeader,
        dataLine: String
    ): DeutscheBoerseXfraCsvRowParsingResult {
        if (dataLine.isEmpty() || QUOTE in dataLine) {
            return failure(DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_DATA_ROW)
        }
        val dataColumns = splitColumns(dataLine)
        if (dataColumns.size != preparedHeader.columnCount) {
            return failure(
                DeutscheBoerseXfraCsvRowParsingErrorCode.COLUMN_COUNT_MISMATCH
            )
        }

        return DeutscheBoerseXfraCsvRowParsingResult.Success(
            DeutscheBoerseXfraTradableInstrumentRecord(
                productStatus = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.PRODUCT_STATUS),
                instrumentStatus = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.INSTRUMENT_STATUS),
                instrumentName = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.INSTRUMENT_NAME),
                isin = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.ISIN),
                wkn = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.WKN),
                micCode = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.MIC_CODE),
                instrumentType = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.INSTRUMENT_TYPE),
                settlementCurrency = dataColumns.cell(
                    preparedHeader,
                    DeutscheBoerseXfraRequiredColumn.SETTLEMENT_CURRENCY
                ),
                currency = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.CURRENCY),
                warrantType = dataColumns.cell(preparedHeader, DeutscheBoerseXfraRequiredColumn.WARRANT_TYPE),
                quotingPeriodStart = dataColumns.cell(
                    preparedHeader,
                    DeutscheBoerseXfraRequiredColumn.QUOTING_PERIOD_START
                ),
                quotingPeriodEnd = dataColumns.cell(
                    preparedHeader,
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
        preparedHeader: DeutscheBoerseXfraPreparedHeader,
        column: DeutscheBoerseXfraRequiredColumn
    ): String? = this[
        checkNotNull(preparedHeader.requiredColumnIndices[column])
    ].ifEmpty { null }

    private fun headerFailure(
        code: DeutscheBoerseXfraCsvRowParsingErrorCode
    ) = DeutscheBoerseXfraHeaderPreparationResult.Failure(
        listOf(DeutscheBoerseXfraCsvRowParsingError(code = code))
    )

    private fun failure(
        code: DeutscheBoerseXfraCsvRowParsingErrorCode
    ) = DeutscheBoerseXfraCsvRowParsingResult.Failure(
        listOf(DeutscheBoerseXfraCsvRowParsingError(code = code))
    )

    private const val SEPARATOR = ';'
    private const val QUOTE = '"'
}
