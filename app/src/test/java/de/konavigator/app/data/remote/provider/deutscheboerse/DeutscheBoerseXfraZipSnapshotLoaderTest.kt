package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseXfraZipSnapshotLoaderTest {

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
                ByteArrayInputStream(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))
            },
            requestedProductIsins = emptySet()
        )

        assertEquals(0, openCalls)
    }

    @Test
    fun validZipCsvSourceIsLoaded() {
        val result = load(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))

        assertEquals(PRODUCT_ISIN, result.records.single().isin)
    }

    @Test
    fun existingFixtureCanBeZippedAndLoaded() {
        val fixture = checkNotNull(
            javaClass.getResourceAsStream(
                "/deutscheboerse/xfra-tradable-instruments-sample.csv"
            )
        ).bufferedReader().use { it.readText() }

        val result = load(
            zipBytes(fixture),
            setOf("DE000SYNTH07", "DE000SYNTH08", "DE000SYNTH09")
        )

        assertEquals(3, result.records.size)
    }

    @Test
    fun onlyExactlyRequestedIsinsAreRetained() {
        val result = load(
            zipBytes(csvContent(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN)))
        )

        assertEquals(listOf(PRODUCT_ISIN), result.records.map { it.isin })
    }

    @Test
    fun multipleRequestedIsinsAreSupported() {
        val result = load(
            zipBytes(csvContent(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN))),
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertEquals(listOf(PRODUCT_ISIN, OTHER_ISIN), result.records.map { it.isin })
    }

    @Test
    fun repeatedRequestedIsinRecordsRemainCompleteAndOrdered() {
        val result = load(
            zipBytes(
                csvContent(
                    xfraLine(PRODUCT_ISIN, instrumentName = "first"),
                    xfraLine(PRODUCT_ISIN, instrumentName = "second"),
                    xfraLine(PRODUCT_ISIN, instrumentName = "third")
                )
            )
        )

        assertEquals(
            listOf("first", "second", "third"),
            result.records.map { it.instrumentName }
        )
    }

    @Test
    fun currencyAndSettlementCurrencyRemainSeparate() {
        val record = load(
            zipBytes(
                csvContent(
                    xfraLine(
                        PRODUCT_ISIN,
                        settlementCurrency = "EUR",
                        currency = "MXN"
                    )
                )
            )
        ).records.single()

        assertEquals("EUR", record.settlementCurrency)
        assertEquals("MXN", record.currency)
    }

    @Test
    fun emptyCurrencyIsNotReplacedBySettlementCurrency() {
        val record = load(
            zipBytes(
                csvContent(
                    xfraLine(
                        PRODUCT_ISIN,
                        settlementCurrency = "EUR",
                        currency = ""
                    )
                )
            )
        ).records.single()

        assertEquals("EUR", record.settlementCurrency)
        assertNull(record.currency)
    }

    @Test
    fun isinComparisonPreservesCaseAndWhitespace() {
        val exactIsin = " $PRODUCT_ISIN "
        val result = load(
            zipBytes(
                csvContent(
                    xfraLine(PRODUCT_ISIN.lowercase()),
                    xfraLine(exactIsin),
                    xfraLine(PRODUCT_ISIN)
                )
            ),
            setOf(exactIsin)
        )

        assertEquals(listOf(exactIsin), result.records.map { it.isin })
    }

    @Test
    fun normalQueryOpensCompressedInputExactlyOnce() {
        var openCalls = 0

        load(
            openCompressedInput = {
                openCalls++
                ByteArrayInputStream(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))
            },
            requestedProductIsins = setOf(PRODUCT_ISIN)
        )

        assertEquals(1, openCalls)
    }

    @Test
    fun directoryEntriesBeforeCsvEntryAreSkipped() {
        val archive = zipBytes(
            TestZipEntry.directory("folder/"),
            TestZipEntry.file("folder/reference", csvContent(xfraLine(PRODUCT_ISIN)))
        )

        val result = load(archive)

        assertEquals(PRODUCT_ISIN, result.records.single().isin)
    }

    @Test
    fun firstRegularArchiveEntryIsUsed() {
        val archive = zipBytes(
            TestZipEntry.file("first", csvContent(xfraLine(PRODUCT_ISIN))),
            TestZipEntry.file("second", csvContent(xfraLine(OTHER_ISIN)))
        )

        val result = load(archive, setOf(PRODUCT_ISIN, OTHER_ISIN))

        assertEquals(listOf(PRODUCT_ISIN), result.records.map { it.isin })
    }

    @Test
    fun laterRegularEntryIsNotMergedWithFirstEntry() {
        val archive = zipBytes(
            TestZipEntry.file("first", csvContent(xfraLine(OTHER_ISIN))),
            TestZipEntry.file("second", csvContent(xfraLine(PRODUCT_ISIN)))
        )

        val result = load(archive)

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun emptyZipArchiveProducesLineOneSourceFailure() {
        val result = loadFailure { ByteArrayInputStream(zipBytes()) }

        assertSourceFailure(result, expectedLineNumber = 1L)
    }

    @Test
    fun directoryOnlyArchiveProducesLineOneSourceFailure() {
        val archive = zipBytes(
            TestZipEntry.directory("first/"),
            TestZipEntry.directory("second/")
        )

        val result = loadFailure { ByteArrayInputStream(archive) }

        assertSourceFailure(result, expectedLineNumber = 1L)
    }

    @Test
    fun ioExceptionOpeningSourceProducesLineOneSourceFailure() {
        val result = loadFailure { throw IOException("synthetic open failure") }

        assertSourceFailure(result, expectedLineNumber = 1L)
    }

    @Test
    fun invalidZipArchiveProducesLineOneSourceFailure() {
        val result = loadFailure {
            ByteArrayInputStream("not zip".toByteArray(StandardCharsets.UTF_8))
        }

        assertSourceFailure(result, expectedLineNumber = 1L)
    }

    @Test
    fun invalidHeaderFailureIsReturnedUnchanged() {
        val result = loadFailure {
            ByteArrayInputStream(zipBytes("metadata one\nmetadata two\n\n"))
        }

        assertEquals(
            DeutscheBoerseXfraCsvLoadingErrorCode.HEADER_PREPARATION_FAILED,
            result.error.code
        )
        assertEquals(3L, result.error.lineNumber)
        assertEquals(
            listOf(
                DeutscheBoerseXfraCsvRowParsingError(
                    DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_HEADER
                )
            ),
            result.error.parsingErrors
        )
    }

    @Test
    fun invalidDataRowFailureIsReturnedUnchanged() {
        val result = loadFailure {
            ByteArrayInputStream(zipBytes(csvContent("invalid")))
        }

        assertEquals(
            DeutscheBoerseXfraCsvLoadingErrorCode.DATA_ROW_PARSING_FAILED,
            result.error.code
        )
        assertEquals(4L, result.error.lineNumber)
        assertEquals(
            listOf(
                DeutscheBoerseXfraCsvRowParsingError(
                    DeutscheBoerseXfraCsvRowParsingErrorCode.COLUMN_COUNT_MISMATCH
                )
            ),
            result.error.parsingErrors
        )
    }

    @Test
    fun ioExceptionWhileReadingLinesPreservesCsvLoaderLineNumber() {
        val content = csvContent(
            xfraLine(PRODUCT_ISIN),
            xfraLine(PRODUCT_ISIN, instrumentName = "second"),
            xfraLine(PRODUCT_ISIN, instrumentName = "third")
        ) + (1..200).joinToString(separator = "\n", postfix = "\n") {
            xfraLine(PRODUCT_ISIN, instrumentName = "record-$it-${it * 7919}")
        }
        val complete = zipBytes(content)
        val truncated = complete.copyOf(complete.size / 2)

        val result = loadFailure { ByteArrayInputStream(truncated) }

        assertEquals(DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED, result.error.code)
        assertTrue(checkNotNull(result.error.lineNumber) >= 4L)
        assertTrue(result.error.parsingErrors.isEmpty())
    }

    @Test
    fun compressedInputClosesAfterSuccess() {
        val input = TrackingInputStream(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))

        load({ input }, setOf(PRODUCT_ISIN))

        assertTrue(input.closed)
        assertEquals(1, input.closeCalls)
    }

    @Test
    fun compressedInputClosesAfterTypedParserFailure() {
        val input = TrackingInputStream(zipBytes(csvContent("invalid")))

        loadFailure { input }

        assertTrue(input.closed)
        assertEquals(1, input.closeCalls)
    }

    @Test
    fun closingFailureAfterSuccessProducesSourceFailureWithoutLineNumber() {
        val input = CloseFailingInputStream(
            zipBytes(csvContent(xfraLine(PRODUCT_ISIN)))
        )

        val result = loadFailure { input }

        assertSourceFailure(result, expectedLineNumber = null)
        assertEquals(1, input.closeCalls)
    }

    @Test
    fun closingFailureDoesNotReplaceExistingTypedFailure() {
        val input = CloseFailingInputStream(zipBytes(csvContent("invalid")))

        val result = loadFailure { input }

        assertEquals(
            DeutscheBoerseXfraCsvLoadingErrorCode.DATA_ROW_PARSING_FAILED,
            result.error.code
        )
        assertEquals(4L, result.error.lineNumber)
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
            xfraLine(
                OTHER_ISIN,
                instrumentName = "record-$index-${index * 7919}"
            )
        }
        val content = listOf("metadata one", "metadata two", "invalid")
            .plus(remainingLines)
            .joinToString(separator = "\n", postfix = "\n")
        val compressed = zipBytes(content)
        val input = TrackingInputStream(compressed)

        loadFailure { input }

        assertTrue(input.bytesRead < compressed.size)
    }

    private fun load(
        compressedBytes: ByteArray,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseXfraCsvLoadingResult.Success =
        load(
            openCompressedInput = { ByteArrayInputStream(compressedBytes) },
            requestedProductIsins = requestedProductIsins
        )

    private fun load(
        openCompressedInput: () -> InputStream,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseXfraCsvLoadingResult.Success =
        DeutscheBoerseXfraZipSnapshotLoader.load(
            openCompressedInput = openCompressedInput,
            requestedProductIsins = requestedProductIsins
        ) as DeutscheBoerseXfraCsvLoadingResult.Success

    private fun loadFailure(
        openCompressedInput: () -> InputStream
    ): DeutscheBoerseXfraCsvLoadingResult.Failure =
        DeutscheBoerseXfraZipSnapshotLoader.load(
            openCompressedInput = openCompressedInput,
            requestedProductIsins = setOf(PRODUCT_ISIN)
        ) as DeutscheBoerseXfraCsvLoadingResult.Failure

    private fun assertSourceFailure(
        result: DeutscheBoerseXfraCsvLoadingResult.Failure,
        expectedLineNumber: Long?
    ) {
        assertEquals(DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED, result.error.code)
        assertEquals(expectedLineNumber, result.error.lineNumber)
        assertTrue(result.error.parsingErrors.isEmpty())
    }

    private fun csvContent(vararg dataLines: String): String =
        listOf("metadata one", "metadata two", validHeader())
            .plus(dataLines)
            .joinToString(separator = "\n", postfix = "\n")

    private fun validHeader(): String = DeutscheBoerseXfraRequiredColumn.entries
        .joinToString(";") { it.headerName }

    private fun xfraLine(
        isin: String,
        instrumentName: String = "Synthetic Instrument",
        settlementCurrency: String = "EUR",
        currency: String = "EUR"
    ): String = listOf(
        "Active",
        "Tradable",
        instrumentName,
        isin,
        "SYN001",
        "XFRA",
        "Warrant",
        settlementCurrency,
        currency,
        "Call",
        "08:00",
        "22:00"
    ).joinToString(";")

    private fun zipBytes(content: String): ByteArray =
        zipBytes(TestZipEntry.file("reference", content))

    private fun zipBytes(vararg entries: TestZipEntry): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
            entries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.name))
                entry.content?.let { content ->
                    zip.write(content.toByteArray(StandardCharsets.UTF_8))
                }
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private data class TestZipEntry(
        val name: String,
        val content: String?
    ) {
        companion object {
            fun file(name: String, content: String) = TestZipEntry(name, content)
            fun directory(name: String) = TestZipEntry(name, null)
        }
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
        const val PRODUCT_ISIN = "DE000ZIP001"
        const val OTHER_ISIN = "DE000ZIP002"
    }
}
