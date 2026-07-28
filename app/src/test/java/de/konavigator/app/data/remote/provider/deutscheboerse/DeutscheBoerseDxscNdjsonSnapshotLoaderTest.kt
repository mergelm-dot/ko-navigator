package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseDxscNdjsonSnapshotLoaderTest {

    @Test
    fun emptyRequestedIsinsProduceEmptySuccess() {
        val result = load(emptySequence(), emptySet())

        assertEquals(emptyList<DeutscheBoerseDxscPretradeRecord>(), result.records)
    }

    @Test
    fun emptyRequestedIsinsDoNotConsumeSequence() {
        var consumed = 0
        val lines = sequence {
            consumed++
            yield("invalid")
        }

        load(lines, emptySet())

        assertEquals(0, consumed)
    }

    @Test
    fun existingSyntheticFixtureIsLoadedLineByLine() {
        val result = fixtureReader().use { reader ->
            load(
                lines = reader.lineSequence(),
                requestedProductIsins =
                    setOf("DE000SYNTH04", "DE000SYNTH05", "DE000SYNTH06")
            )
        }

        assertEquals(3, result.records.size)
    }

    @Test
    fun onlyExactlyRequestedIsinIsRetained() {
        val result = load(
            sequenceOf(jsonRecord(PRODUCT_ISIN), jsonRecord(OTHER_ISIN)),
            setOf(PRODUCT_ISIN)
        )

        assertEquals(listOf(PRODUCT_ISIN), result.records.map { it.instrumentIdentificationCode })
    }

    @Test
    fun multipleRequestedIsinsAreSupported() {
        val result = load(
            sequenceOf(jsonRecord(PRODUCT_ISIN), jsonRecord(OTHER_ISIN)),
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertEquals(
            listOf(PRODUCT_ISIN, OTHER_ISIN),
            result.records.map { it.instrumentIdentificationCode }
        )
    }

    @Test
    fun nonRequestedRecordsAreIgnored() {
        val result = load(sequenceOf(jsonRecord(OTHER_ISIN)), setOf(PRODUCT_ISIN))

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun recordWithNullIsinIsIgnored() {
        val result = load(
            sequenceOf(jsonRecord(isin = null)),
            setOf(PRODUCT_ISIN)
        )

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun noMatchProducesEmptySuccess() {
        val result = load(emptySequence(), setOf(PRODUCT_ISIN))

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun repeatedRequestedIsinRecordsRemainCompleteAndOrdered() {
        val result = load(
            sequenceOf(
                jsonRecord(PRODUCT_ISIN, messageId = "first"),
                jsonRecord(PRODUCT_ISIN, messageId = "second"),
                jsonRecord(PRODUCT_ISIN, messageId = "third")
            ),
            setOf(PRODUCT_ISIN)
        )

        assertEquals(listOf("first", "second", "third"), result.records.map { it.messageId })
    }

    @Test
    fun loaderDoesNotSelectLatestRecord() {
        val result = load(
            sequenceOf(
                jsonRecord(
                    PRODUCT_ISIN,
                    messageId = "newer",
                    timestamp = "2026-07-27T19:29:58Z"
                ),
                jsonRecord(
                    PRODUCT_ISIN,
                    messageId = "older",
                    timestamp = "2026-07-27T19:29:57Z"
                )
            ),
            setOf(PRODUCT_ISIN)
        )

        assertEquals(listOf("newer", "older"), result.records.map { it.messageId })
    }

    @Test
    fun isinComparisonPreservesCaseWhitespaceAndEmptyValues() {
        val spacedIsin = " $PRODUCT_ISIN "
        val result = load(
            sequenceOf(
                jsonRecord(PRODUCT_ISIN.lowercase()),
                jsonRecord(spacedIsin),
                jsonRecord("")
            ),
            setOf(spacedIsin, "")
        )

        assertEquals(
            listOf(spacedIsin, ""),
            result.records.map { it.instrumentIdentificationCode }
        )
    }

    @Test
    fun nullBidAndZeroAskRemainUnchanged() {
        val result = load(
            sequenceOf(jsonRecord(PRODUCT_ISIN, bestBid = null, bestAsk = 0.0)),
            setOf(PRODUCT_ISIN)
        )

        assertNull(result.records.single().bestBid)
        assertEquals(0.0, result.records.single().bestAsk)
    }

    @Test
    fun malformedFirstLineProducesParsingFailureAtLineOne() {
        val result = loadFailure(sequenceOf(MALFORMED_JSON))

        assertEquals(DeutscheBoerseDxscNdjsonLoadingErrorCode.LINE_PARSING_FAILED, result.error.code)
        assertEquals(1L, result.error.lineNumber)
        assertEquals(
            listOf(DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON),
            result.error.parsingErrors
        )
    }

    @Test
    fun malformedLaterLineReportsItsOneBasedLineNumber() {
        val result = loadFailure(
            sequenceOf(
                jsonRecord(PRODUCT_ISIN),
                jsonRecord(OTHER_ISIN),
                MALFORMED_JSON
            )
        )

        assertEquals(3L, result.error.lineNumber)
    }

    @Test
    fun malformedLineReturnsFailureInsteadOfPartialSuccess() {
        val result = DeutscheBoerseDxscNdjsonSnapshotLoader.load(
            lines = sequenceOf(jsonRecord(PRODUCT_ISIN), MALFORMED_JSON),
            requestedProductIsins = setOf(PRODUCT_ISIN)
        )

        assertTrue(result is DeutscheBoerseDxscNdjsonLoadingResult.Failure)
    }

    @Test
    fun linesAfterMalformedLineAreNotConsumed() {
        var consumed = 0
        val lines = sequenceOf(
            jsonRecord(PRODUCT_ISIN),
            MALFORMED_JSON,
            jsonRecord(PRODUCT_ISIN)
        )
            .onEach { consumed++ }

        loadFailure(lines)

        assertEquals(2, consumed)
    }

    @Test
    fun ioExceptionDuringIterationProducesSourceReadingFailure() {
        val result = loadFailure(ioFailingSequence())

        assertEquals(DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED, result.error.code)
        assertEquals(2L, result.error.lineNumber)
    }

    @Test
    fun sourceReadingFailureContainsNoInventedParsingErrors() {
        val result = loadFailure(ioFailingSequence())

        assertTrue(result.error.parsingErrors.isEmpty())
    }

    @Test
    fun successfulSequenceIsConsumedExactlyOnce() {
        var iteratorCalls = 0
        var consumedLines = 0
        val lines = object : Sequence<String> {
            override fun iterator(): Iterator<String> {
                iteratorCalls++
                return sequenceOf(jsonRecord(PRODUCT_ISIN), jsonRecord(OTHER_ISIN))
                    .onEach { consumedLines++ }
                    .iterator()
            }
        }

        load(lines, setOf(PRODUCT_ISIN))

        assertEquals(1, iteratorCalls)
        assertEquals(2, consumedLines)
    }

    @Test
    fun retainedRecordsPreserveInputOrderAcrossIsins() {
        val result = load(
            sequenceOf(
                jsonRecord(OTHER_ISIN, messageId = "other-first"),
                jsonRecord(PRODUCT_ISIN, messageId = "product-second"),
                jsonRecord(OTHER_ISIN, messageId = "other-third")
            ),
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertEquals(
            listOf("other-first", "product-second", "other-third"),
            result.records.map { it.messageId }
        )
        assertThrows(UnsupportedOperationException::class.java) {
            (result.records as MutableList).clear()
        }
    }

    private fun load(
        lines: Sequence<String>,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseDxscNdjsonLoadingResult.Success =
        DeutscheBoerseDxscNdjsonSnapshotLoader.load(
            lines = lines,
            requestedProductIsins = requestedProductIsins
        ) as DeutscheBoerseDxscNdjsonLoadingResult.Success

    private fun loadFailure(
        lines: Sequence<String>
    ): DeutscheBoerseDxscNdjsonLoadingResult.Failure =
        DeutscheBoerseDxscNdjsonSnapshotLoader.load(
            lines = lines,
            requestedProductIsins = setOf(PRODUCT_ISIN)
        ) as DeutscheBoerseDxscNdjsonLoadingResult.Failure

    private fun ioFailingSequence(): Sequence<String> = sequence {
        yield(jsonRecord(PRODUCT_ISIN))
        throw IOException("synthetic read failure")
    }

    private fun fixtureReader() = checkNotNull(
        javaClass.getResourceAsStream("/deutscheboerse/dxsc-pretrade-sample.ndjson")
    ).bufferedReader()

    private fun jsonRecord(
        isin: String?,
        messageId: String = "pretrade",
        timestamp: String = "2026-07-27T19:29:57.363600000Z",
        bestBid: Double? = 2.343,
        bestAsk: Double? = 2.344
    ): String {
        val jsonIsin = isin?.let { "\"$it\"" } ?: "null"
        val jsonBestBid = bestBid?.toString() ?: "null"
        val jsonBestAsk = bestAsk?.toString() ?: "null"
        return """{"messageId":"$messageId","instrumentIdentificationCode":$jsonIsin,"bestBid":$jsonBestBid,"bestAsk":$jsonBestAsk,"updateDateAndTime":"$timestamp"}"""
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000LOAD01"
        const val OTHER_ISIN = "DE000LOAD02"
        const val MALFORMED_JSON = "{\"messageId\":"
    }
}
