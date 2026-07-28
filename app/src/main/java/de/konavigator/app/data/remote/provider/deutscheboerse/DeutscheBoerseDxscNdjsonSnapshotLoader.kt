package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.IOException
import java.util.Collections

enum class DeutscheBoerseDxscNdjsonLoadingErrorCode {
    LINE_PARSING_FAILED,
    SOURCE_READING_FAILED
}

data class DeutscheBoerseDxscNdjsonLoadingError(
    val code: DeutscheBoerseDxscNdjsonLoadingErrorCode,
    val lineNumber: Long? = null,
    val parsingErrors: List<DeutscheBoerseDxscJsonLineParsingErrorCode> = emptyList()
)

sealed interface DeutscheBoerseDxscNdjsonLoadingResult {

    data class Success(
        val records: List<DeutscheBoerseDxscPretradeRecord>
    ) : DeutscheBoerseDxscNdjsonLoadingResult

    data class Failure(
        val error: DeutscheBoerseDxscNdjsonLoadingError
    ) : DeutscheBoerseDxscNdjsonLoadingResult
}

object DeutscheBoerseDxscNdjsonSnapshotLoader {

    fun load(
        lines: Sequence<String>,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseDxscNdjsonLoadingResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        if (requestedProductIsinsSnapshot.isEmpty()) {
            return DeutscheBoerseDxscNdjsonLoadingResult.Success(emptyList())
        }

        val iterator = try {
            lines.iterator()
        } catch (_: IOException) {
            return sourceReadingFailure(lineNumber = 1L)
        }
        val records = mutableListOf<DeutscheBoerseDxscPretradeRecord>()
        var nextLineNumber = 1L

        while (true) {
            val hasNext = try {
                iterator.hasNext()
            } catch (_: IOException) {
                return sourceReadingFailure(lineNumber = nextLineNumber)
            }
            if (!hasNext) {
                break
            }

            val line = try {
                iterator.next()
            } catch (_: IOException) {
                return sourceReadingFailure(lineNumber = nextLineNumber)
            }
            val currentLineNumber = nextLineNumber
            nextLineNumber++

            when (val parsingResult = DeutscheBoerseDxscPretradeJsonLineParser.parse(line)) {
                is DeutscheBoerseDxscJsonLineParsingResult.Success -> {
                    val record = parsingResult.record
                    if (record.instrumentIdentificationCode in requestedProductIsinsSnapshot) {
                        records += record
                    }
                }

                is DeutscheBoerseDxscJsonLineParsingResult.Failure ->
                    return DeutscheBoerseDxscNdjsonLoadingResult.Failure(
                        DeutscheBoerseDxscNdjsonLoadingError(
                            code =
                                DeutscheBoerseDxscNdjsonLoadingErrorCode
                                    .LINE_PARSING_FAILED,
                            lineNumber = currentLineNumber,
                            parsingErrors = parsingResult.errors
                        )
                    )
            }
        }

        return DeutscheBoerseDxscNdjsonLoadingResult.Success(
            Collections.unmodifiableList(records.toList())
        )
    }

    private fun sourceReadingFailure(
        lineNumber: Long
    ) = DeutscheBoerseDxscNdjsonLoadingResult.Failure(
        DeutscheBoerseDxscNdjsonLoadingError(
            code = DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
            lineNumber = lineNumber
        )
    )
}
