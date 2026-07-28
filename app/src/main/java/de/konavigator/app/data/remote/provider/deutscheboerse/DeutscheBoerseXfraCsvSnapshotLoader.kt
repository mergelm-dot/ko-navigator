package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.IOException
import java.util.Collections

enum class DeutscheBoerseXfraCsvLoadingErrorCode {
    HEADER_MISSING,
    HEADER_PREPARATION_FAILED,
    DATA_ROW_PARSING_FAILED,
    SOURCE_READING_FAILED
}

data class DeutscheBoerseXfraCsvLoadingError(
    val code: DeutscheBoerseXfraCsvLoadingErrorCode,
    val lineNumber: Long? = null,
    val parsingErrors: List<DeutscheBoerseXfraCsvRowParsingError> = emptyList()
)

sealed interface DeutscheBoerseXfraCsvLoadingResult {

    data class Success(
        val records: List<DeutscheBoerseXfraTradableInstrumentRecord>
    ) : DeutscheBoerseXfraCsvLoadingResult

    data class Failure(
        val error: DeutscheBoerseXfraCsvLoadingError
    ) : DeutscheBoerseXfraCsvLoadingResult
}

object DeutscheBoerseXfraCsvSnapshotLoader {

    fun load(
        lines: Sequence<String>,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseXfraCsvLoadingResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        if (requestedProductIsinsSnapshot.isEmpty()) {
            return DeutscheBoerseXfraCsvLoadingResult.Success(emptyList())
        }

        val iterator = try {
            lines.iterator()
        } catch (_: IOException) {
            return sourceReadingFailure(lineNumber = 1L)
        }
        var nextLineNumber = 1L

        repeat(METADATA_LINE_COUNT) {
            when (val lineResult = iterator.readNext(nextLineNumber)) {
                is LineReadingResult.EndOfSource ->
                    return headerMissing(lineNumber = nextLineNumber)

                is LineReadingResult.Failure ->
                    return sourceReadingFailure(lineNumber = nextLineNumber)

                is LineReadingResult.Success -> nextLineNumber++
            }
        }

        val headerLine = when (val lineResult = iterator.readNext(nextLineNumber)) {
            is LineReadingResult.EndOfSource ->
                return headerMissing(lineNumber = nextLineNumber)

            is LineReadingResult.Failure ->
                return sourceReadingFailure(lineNumber = nextLineNumber)

            is LineReadingResult.Success -> lineResult.line
        }
        val headerLineNumber = nextLineNumber
        nextLineNumber++

        val preparedHeader = when (
            val preparationResult =
                DeutscheBoerseXfraTradableInstrumentCsvRowParser.prepareHeader(
                    headerLine
                )
        ) {
            is DeutscheBoerseXfraHeaderPreparationResult.Failure ->
                return DeutscheBoerseXfraCsvLoadingResult.Failure(
                    DeutscheBoerseXfraCsvLoadingError(
                        code =
                            DeutscheBoerseXfraCsvLoadingErrorCode
                                .HEADER_PREPARATION_FAILED,
                        lineNumber = headerLineNumber,
                        parsingErrors = preparationResult.errors
                    )
                )

            is DeutscheBoerseXfraHeaderPreparationResult.Success ->
                preparationResult.preparedHeader
        }

        val records = mutableListOf<DeutscheBoerseXfraTradableInstrumentRecord>()
        while (true) {
            val dataLine = when (val lineResult = iterator.readNext(nextLineNumber)) {
                is LineReadingResult.EndOfSource -> break
                is LineReadingResult.Failure ->
                    return sourceReadingFailure(lineNumber = nextLineNumber)

                is LineReadingResult.Success -> lineResult.line
            }
            val dataLineNumber = nextLineNumber
            nextLineNumber++

            when (
                val parsingResult =
                    DeutscheBoerseXfraTradableInstrumentCsvRowParser.parse(
                        preparedHeader = preparedHeader,
                        dataLine = dataLine
                    )
            ) {
                is DeutscheBoerseXfraCsvRowParsingResult.Failure ->
                    return DeutscheBoerseXfraCsvLoadingResult.Failure(
                        DeutscheBoerseXfraCsvLoadingError(
                            code =
                                DeutscheBoerseXfraCsvLoadingErrorCode
                                    .DATA_ROW_PARSING_FAILED,
                            lineNumber = dataLineNumber,
                            parsingErrors = parsingResult.errors
                        )
                    )

                is DeutscheBoerseXfraCsvRowParsingResult.Success -> {
                    val record = parsingResult.record
                    if (record.isin in requestedProductIsinsSnapshot) {
                        records += record
                    }
                }
            }
        }

        return DeutscheBoerseXfraCsvLoadingResult.Success(
            Collections.unmodifiableList(records.toList())
        )
    }

    private fun Iterator<String>.readNext(
        lineNumber: Long
    ): LineReadingResult = try {
        if (hasNext()) {
            LineReadingResult.Success(next())
        } else {
            LineReadingResult.EndOfSource
        }
    } catch (_: IOException) {
        LineReadingResult.Failure(lineNumber)
    }

    private fun headerMissing(
        lineNumber: Long
    ) = DeutscheBoerseXfraCsvLoadingResult.Failure(
        DeutscheBoerseXfraCsvLoadingError(
            code = DeutscheBoerseXfraCsvLoadingErrorCode.HEADER_MISSING,
            lineNumber = lineNumber
        )
    )

    private fun sourceReadingFailure(
        lineNumber: Long
    ) = DeutscheBoerseXfraCsvLoadingResult.Failure(
        DeutscheBoerseXfraCsvLoadingError(
            code = DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
            lineNumber = lineNumber
        )
    )

    private sealed interface LineReadingResult {
        data class Success(val line: String) : LineReadingResult
        data class Failure(val lineNumber: Long) : LineReadingResult
        data object EndOfSource : LineReadingResult
    }

    private const val METADATA_LINE_COUNT = 2
}
