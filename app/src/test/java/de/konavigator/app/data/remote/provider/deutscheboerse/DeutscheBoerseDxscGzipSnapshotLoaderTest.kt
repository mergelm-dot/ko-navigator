package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseDxscGzipSnapshotLoaderTest {

    @Test
    fun emptyRequestedIsinsProduceEmptySuccess() {
        val result = load({ error("must not open") }, emptySet())

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun emptyRequestedIsinsDoNotOpenCompressedInput() {
        var openCalls = 0

        load(
            openCompressedInput = {
                openCalls++
                ByteArrayInputStream(gzipBytes(jsonRecord(PRODUCT_ISIN)))
            },
            requestedProductIsins = emptySet()
        )

        assertEquals(0, openCalls)
    }

    @Test
    fun validGzipNdjsonSourceIsLoaded() {
        val result = load(gzipBytes(jsonRecord(PRODUCT_ISIN)))

        assertEquals(PRODUCT_ISIN, result.records.single().instrumentIdentificationCode)
    }

    @Test
    fun existingFixtureCanBeGzippedAndLoaded() {
        val fixture = checkNotNull(
            javaClass.getResourceAsStream(
                "/deutscheboerse/dxsc-pretrade-sample.ndjson"
            )
        ).bufferedReader().use { it.readText() }

        val result = load(
            gzipBytes(fixture),
            setOf("DE000SYNTH04", "DE000SYNTH05", "DE000SYNTH06")
        )

        assertEquals(3, result.records.size)
    }

    @Test
    fun onlyExactlyRequestedIsinsAreRetained() {
        val result = load(
            gzipBytes(jsonRecord(PRODUCT_ISIN), jsonRecord(OTHER_ISIN))
        )

        assertEquals(listOf(PRODUCT_ISIN), result.records.map { it.instrumentIdentificationCode })
    }

    @Test
    fun repeatedRequestedIsinRecordsRemainCompleteAndOrdered() {
        val result = load(
            gzipBytes(
                jsonRecord(PRODUCT_ISIN, messageId = "first"),
                jsonRecord(PRODUCT_ISIN, messageId = "second"),
                jsonRecord(PRODUCT_ISIN, messageId = "third")
            )
        )

        assertEquals(listOf("first", "second", "third"), result.records.map { it.messageId })
    }

    @Test
    fun adapterDoesNotSelectNewestRecord() {
        val result = load(
            gzipBytes(
                jsonRecord(PRODUCT_ISIN, messageId = "newer", timestamp = NEWER_TIMESTAMP),
                jsonRecord(PRODUCT_ISIN, messageId = "older", timestamp = OLDER_TIMESTAMP)
            )
        )

        assertEquals(listOf("newer", "older"), result.records.map { it.messageId })
    }

    @Test
    fun nullBidAndZeroAskRemainUnchanged() {
        val record = load(
            gzipBytes(jsonRecord(PRODUCT_ISIN, bestBid = null, bestAsk = 0.0))
        ).records.single()

        assertNull(record.bestBid)
        assertEquals(0.0, record.bestAsk)
    }

    @Test
    fun isinComparisonPreservesCaseAndWhitespace() {
        val exactIsin = " $PRODUCT_ISIN "
        val result = load(
            gzipBytes(
                jsonRecord(PRODUCT_ISIN.lowercase()),
                jsonRecord(exactIsin),
                jsonRecord(PRODUCT_ISIN)
            ),
            setOf(exactIsin)
        )

        assertEquals(listOf(exactIsin), result.records.map { it.instrumentIdentificationCode })
    }

    @Test
    fun normalQueryOpensCompressedInputExactlyOnce() {
        var openCalls = 0

        load(
            openCompressedInput = {
                openCalls++
                ByteArrayInputStream(gzipBytes(jsonRecord(PRODUCT_ISIN)))
            },
            requestedProductIsins = setOf(PRODUCT_ISIN)
        )

        assertEquals(1, openCalls)
    }

    @Test
    fun compressedInputClosesAfterSuccess() {
        val input = TrackingInputStream(gzipBytes(jsonRecord(PRODUCT_ISIN)))

        load({ input }, setOf(PRODUCT_ISIN))

        assertTrue(input.closed)
        assertEquals(1, input.closeCalls)
    }

    @Test
    fun compressedInputClosesAfterTypedJsonFailure() {
        val input = TrackingInputStream(gzipBytes(MALFORMED_JSON))

        loadFailure({ input })

        assertTrue(input.closed)
        assertEquals(1, input.closeCalls)
    }

    @Test
    fun ioExceptionOpeningSourceProducesLineOneSourceFailure() {
        val result = loadFailure { throw IOException("synthetic open failure") }

        assertSourceFailure(result, expectedLineNumber = 1L)
    }

    @Test
    fun invalidGzipHeaderProducesLineOneSourceFailure() {
        val result = loadFailure {
            ByteArrayInputStream("not gzip".toByteArray(StandardCharsets.UTF_8))
        }

        assertSourceFailure(result, expectedLineNumber = 1L)
    }

    @Test
    fun jsonParserFailureIsReturnedUnchanged() {
        val result = loadFailure { ByteArrayInputStream(gzipBytes(MALFORMED_JSON)) }

        assertEquals(DeutscheBoerseDxscNdjsonLoadingErrorCode.LINE_PARSING_FAILED, result.error.code)
        assertEquals(1L, result.error.lineNumber)
        assertEquals(
            listOf(DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON),
            result.error.parsingErrors
        )
    }

    @Test
    fun ioExceptionWhileReadingLinesPreservesNdjsonLoaderLineNumber() {
        val complete = gzipBytes(
            jsonRecord(PRODUCT_ISIN),
            jsonRecord(PRODUCT_ISIN, messageId = "second")
        )
        val truncated = complete.copyOf(complete.size - GZIP_TRAILER_SIZE)

        val result = loadFailure { ByteArrayInputStream(truncated) }

        assertEquals(DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED, result.error.code)
        assertEquals(3L, result.error.lineNumber)
        assertTrue(result.error.parsingErrors.isEmpty())
    }

    @Test
    fun closingFailureAfterSuccessProducesSourceFailureWithoutLineNumber() {
        val input = CloseFailingInputStream(gzipBytes(jsonRecord(PRODUCT_ISIN)))

        val result = loadFailure { input }

        assertSourceFailure(result, expectedLineNumber = null)
        assertEquals(1, input.closeCalls)
    }

    @Test
    fun closingFailureDoesNotReplaceExistingJsonFailure() {
        val input = CloseFailingInputStream(gzipBytes(MALFORMED_JSON))

        val result = loadFailure { input }

        assertEquals(DeutscheBoerseDxscNdjsonLoadingErrorCode.LINE_PARSING_FAILED, result.error.code)
        assertEquals(1L, result.error.lineNumber)
        assertEquals(
            listOf(DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON),
            result.error.parsingErrors
        )
    }

    @Test
    fun unexpectedRuntimeFailureOpeningSourceIsNotSwallowed() {
        val expected = IllegalStateException("synthetic runtime failure")

        val actual = assertThrows(IllegalStateException::class.java) {
            load({ throw expected }, setOf(PRODUCT_ISIN))
        }

        assertTrue(actual === expected)
    }

    @Test
    fun openedInputClosesWhenProcessingPropagatesRuntimeFailure() {
        val expected = IllegalStateException("synthetic read failure")
        val input = RuntimeFailingInputStream(expected)

        val actual = assertThrows(IllegalStateException::class.java) {
            load({ input }, setOf(PRODUCT_ISIN))
        }

        assertTrue(actual === expected)
        assertTrue(input.closed)
        assertEquals(1, input.closeCalls)
    }

    @Test
    fun compressedSourceIsNotFullyMaterializedBeforeEarlyFailure() {
        val remainingLines = (1..2_000).map { index ->
            jsonRecord(
                OTHER_ISIN,
                messageId = "message-$index-${index * 7919}"
            )
        }
        val compressed = gzipBytes(listOf(MALFORMED_JSON) + remainingLines)
        val input = TrackingInputStream(compressed)

        loadFailure { input }

        assertTrue(input.bytesRead < compressed.size)
    }

    private fun load(
        compressedBytes: ByteArray,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseDxscNdjsonLoadingResult.Success =
        load(
            openCompressedInput = { ByteArrayInputStream(compressedBytes) },
            requestedProductIsins = requestedProductIsins
        )

    private fun load(
        openCompressedInput: () -> InputStream,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseDxscNdjsonLoadingResult.Success =
        DeutscheBoerseDxscGzipSnapshotLoader.load(
            openCompressedInput = openCompressedInput,
            requestedProductIsins = requestedProductIsins
        ) as DeutscheBoerseDxscNdjsonLoadingResult.Success

    private fun loadFailure(
        openCompressedInput: () -> InputStream
    ): DeutscheBoerseDxscNdjsonLoadingResult.Failure =
        DeutscheBoerseDxscGzipSnapshotLoader.load(
            openCompressedInput = openCompressedInput,
            requestedProductIsins = setOf(PRODUCT_ISIN)
        ) as DeutscheBoerseDxscNdjsonLoadingResult.Failure

    private fun assertSourceFailure(
        result: DeutscheBoerseDxscNdjsonLoadingResult.Failure,
        expectedLineNumber: Long?
    ) {
        assertEquals(DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED, result.error.code)
        assertEquals(expectedLineNumber, result.error.lineNumber)
        assertTrue(result.error.parsingErrors.isEmpty())
    }

    private fun gzipBytes(vararg lines: String): ByteArray = gzipBytes(lines.toList())

    private fun gzipBytes(lines: List<String>): ByteArray =
        gzipBytes(lines.joinToString(separator = "\n", postfix = "\n"))

    private fun gzipBytes(content: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(content)
        }
        return output.toByteArray()
    }

    private fun jsonRecord(
        isin: String,
        messageId: String = "pretrade",
        timestamp: String = NEWER_TIMESTAMP,
        bestBid: Double? = 2.343,
        bestAsk: Double? = 2.344
    ): String {
        val jsonBid = bestBid?.toString() ?: "null"
        val jsonAsk = bestAsk?.toString() ?: "null"
        return """{"messageId":"$messageId","instrumentIdentificationCode":"$isin","bestBid":$jsonBid,"bestAsk":$jsonAsk,"updateDateAndTime":"$timestamp"}"""
    }

    private open class TrackingInputStream(
        bytes: ByteArray
    ) : InputStream() {
        private val delegate = ByteArrayInputStream(bytes)
        var bytesRead: Int = 0
            private set
        var closeCalls: Int = 0
            private set
        var closed: Boolean = false
            private set

        override fun read(): Int = delegate.read().also { value ->
            if (value >= 0) {
                bytesRead++
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate.read(buffer, offset, length).also { count ->
                if (count > 0) {
                    bytesRead += count
                }
            }

        override fun close() {
            closeCalls++
            closed = true
            delegate.close()
        }
    }

    private class CloseFailingInputStream(
        bytes: ByteArray
    ) : TrackingInputStream(bytes) {
        override fun close() {
            super.close()
            throw IOException("synthetic close failure")
        }
    }

    private class RuntimeFailingInputStream(
        private val failure: RuntimeException
    ) : InputStream() {
        var closeCalls: Int = 0
            private set
        var closed: Boolean = false
            private set

        override fun read(): Int = throw failure

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            throw failure

        override fun close() {
            closeCalls++
            closed = true
        }
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000GZIP01"
        const val OTHER_ISIN = "DE000GZIP02"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val MALFORMED_JSON = "{\"messageId\":"
        const val GZIP_TRAILER_SIZE = 8
    }
}
