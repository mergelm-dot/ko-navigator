package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseXfraCsvSnapshotLoaderTest {

    @Test
    fun emptyRequestedIsinsProduceEmptySuccess() {
        val result = load(emptySequence(), emptySet())

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun emptyRequestedIsinsDoNotConsumeSequence() {
        var iteratorCalls = 0
        val lines = object : Sequence<String> {
            override fun iterator(): Iterator<String> {
                iteratorCalls++
                return emptySequence<String>().iterator()
            }
        }

        load(lines, emptySet())

        assertEquals(0, iteratorCalls)
    }

    @Test
    fun fixtureIsLoadedCompletelyLineByLine() {
        val result = load(
            fixtureLines(),
            setOf("DE000SYNTH07", "DE000SYNTH08", "DE000SYNTH09")
        )

        assertEquals(3, result.records.size)
    }

    @Test
    fun firstTwoFixtureLinesAreIgnoredAsMetadata() {
        val lines = fixtureLines().toMutableList().apply {
            this[0] = ""
            this[1] = "not validated metadata"
        }.asSequence()

        val result = load(lines, setOf("DE000SYNTH07"))

        assertEquals("DE000SYNTH07", result.records.single().isin)
    }

    @Test
    fun thirdFixtureLineIsConsumedOnceAsHeader() {
        val consumptionCounts = mutableMapOf<Int, Int>()
        val lines = fixtureLines().mapIndexed { index, line ->
            consumptionCounts[index] = consumptionCounts.getOrDefault(index, 0) + 1
            line
        }

        load(lines, setOf("DE000SYNTH07"))

        assertEquals(1, consumptionCounts[2])
    }

    @Test
    fun onlyExactlyRequestedIsinsAreRetained() {
        val result = load(
            source(dataLine(PRODUCT_ISIN), dataLine(OTHER_ISIN)),
            setOf(PRODUCT_ISIN)
        )

        assertEquals(listOf(PRODUCT_ISIN), result.records.map { it.isin })
    }

    @Test
    fun multipleRequestedIsinsAreSupported() {
        val result = load(
            source(dataLine(PRODUCT_ISIN), dataLine(OTHER_ISIN)),
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertEquals(listOf(PRODUCT_ISIN, OTHER_ISIN), result.records.map { it.isin })
    }

    @Test
    fun nonRequestedRecordsAreIgnored() {
        val result = load(source(dataLine(OTHER_ISIN)), setOf(PRODUCT_ISIN))

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun recordWithEmptyIsinIsIgnored() {
        val result = load(source(dataLine("")), setOf(PRODUCT_ISIN, ""))

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun noMatchProducesEmptySuccess() {
        val result = load(source(dataLine(OTHER_ISIN)), setOf(PRODUCT_ISIN))

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun repeatedRequestedIsinRecordsRemainCompleteAndOrdered() {
        val result = load(
            source(
                dataLine(PRODUCT_ISIN, instrumentName = "first"),
                dataLine(PRODUCT_ISIN, instrumentName = "second"),
                dataLine(PRODUCT_ISIN, instrumentName = "third")
            ),
            setOf(PRODUCT_ISIN)
        )

        assertEquals(
            listOf("first", "second", "third"),
            result.records.map { it.instrumentName }
        )
    }

    @Test
    fun loaderContinuesAfterFirstMatchToRetainLaterDuplicate() {
        var consumed = 0
        val lines = source(
            dataLine(PRODUCT_ISIN, instrumentName = "first"),
            dataLine(OTHER_ISIN),
            dataLine(PRODUCT_ISIN, instrumentName = "second")
        ).onEach { consumed++ }

        val result = load(lines, setOf(PRODUCT_ISIN))

        assertEquals(listOf("first", "second"), result.records.map { it.instrumentName })
        assertEquals(6, consumed)
    }

    @Test
    fun isinComparisonPreservesCaseAndWhitespace() {
        val spacedIsin = " $PRODUCT_ISIN "
        val result = load(
            source(
                dataLine(PRODUCT_ISIN.lowercase()),
                dataLine(spacedIsin),
                dataLine(PRODUCT_ISIN)
            ),
            setOf(spacedIsin)
        )

        assertEquals(listOf(spacedIsin), result.records.map { it.isin })
    }

    @Test
    fun currencyAndSettlementCurrencyRemainSeparate() {
        val result = load(
            source(dataLine(PRODUCT_ISIN, settlementCurrency = "EUR", currency = "MXN")),
            setOf(PRODUCT_ISIN)
        )

        assertEquals("EUR", result.records.single().settlementCurrency)
        assertEquals("MXN", result.records.single().currency)
    }

    @Test
    fun emptyCurrencyIsNotReplacedBySettlementCurrency() {
        val result = load(
            source(dataLine(PRODUCT_ISIN, settlementCurrency = "EUR", currency = "")),
            setOf(PRODUCT_ISIN)
        )

        assertEquals("EUR", result.records.single().settlementCurrency)
        assertNull(result.records.single().currency)
    }

    @Test
    fun emptySourceProducesHeaderMissingAtLineOne() {
        assertHeaderMissing(emptySequence(), 1L)
    }

    @Test
    fun oneLineSourceProducesHeaderMissingAtLineTwo() {
        assertHeaderMissing(sequenceOf("metadata"), 2L)
    }

    @Test
    fun twoLineSourceProducesHeaderMissingAtLineThree() {
        assertHeaderMissing(sequenceOf("metadata one", "metadata two"), 3L)
    }

    @Test
    fun invalidHeaderReportsPreparationFailureAndAllParserErrors() {
        val header = DeutscheBoerseXfraRequiredColumn.entries
            .filterNot {
                it == DeutscheBoerseXfraRequiredColumn.ISIN ||
                    it == DeutscheBoerseXfraRequiredColumn.WKN
            }
            .joinToString(";") { it.headerName }
        val result = loadFailure(sequenceOf("meta one", "meta two", header))

        assertEquals(
            DeutscheBoerseXfraCsvLoadingErrorCode.HEADER_PREPARATION_FAILED,
            result.error.code
        )
        assertEquals(3L, result.error.lineNumber)
        assertEquals(
            listOf(
                missing(DeutscheBoerseXfraRequiredColumn.ISIN),
                missing(DeutscheBoerseXfraRequiredColumn.WKN)
            ),
            result.error.parsingErrors
        )
    }

    @Test
    fun dataRowsAreNotConsumedAfterInvalidHeader() {
        var consumed = 0
        val lines = sequenceOf("meta one", "meta two", "invalid", dataLine(PRODUCT_ISIN))
            .onEach { consumed++ }

        loadFailure(lines)

        assertEquals(3, consumed)
    }

    @Test
    fun invalidFirstDataRowReportsLineFour() {
        val result = loadFailure(source("invalid"))

        assertEquals(DeutscheBoerseXfraCsvLoadingErrorCode.DATA_ROW_PARSING_FAILED, result.error.code)
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
    fun invalidLaterDataRowReportsCorrectLine() {
        val result = loadFailure(source(dataLine(PRODUCT_ISIN), "invalid"))

        assertEquals(5L, result.error.lineNumber)
    }

    @Test
    fun invalidDataRowReturnsFailureInsteadOfPartialSuccess() {
        val result = DeutscheBoerseXfraCsvSnapshotLoader.load(
            lines = source(dataLine(PRODUCT_ISIN), "invalid"),
            requestedProductIsins = setOf(PRODUCT_ISIN)
        )

        assertTrue(result is DeutscheBoerseXfraCsvLoadingResult.Failure)
    }

    @Test
    fun linesAfterInvalidDataRowAreNotConsumed() {
        var consumed = 0
        val lines = source("invalid", dataLine(PRODUCT_ISIN)).onEach { consumed++ }

        loadFailure(lines)

        assertEquals(4, consumed)
    }

    @Test
    fun ioExceptionCreatingIteratorReportsSourceFailureAtLineOne() {
        val lines = object : Sequence<String> {
            override fun iterator(): Iterator<String> =
                throw IOException("synthetic iterator failure")
        }

        assertSourceFailure(lines, 1L)
    }

    @Test
    fun ioExceptionReadingMetadataReportsNextExpectedLine() {
        assertSourceFailure(ioFailingSequence(listOf("meta one")), 2L)
    }

    @Test
    fun ioExceptionReadingHeaderReportsExpectedLineThree() {
        assertSourceFailure(
            ioFailingSequence(listOf("meta one", "meta two")),
            3L
        )
    }

    @Test
    fun ioExceptionReadingDataReportsCorrectLine() {
        assertSourceFailure(
            ioFailingSequence(
                listOf("meta one", "meta two", validHeader(), dataLine(PRODUCT_ISIN))
            ),
            5L
        )
    }

    @Test
    fun sourceReadingFailureContainsNoParsingErrors() {
        val result = loadFailure(ioFailingSequence(listOf("meta one")))

        assertTrue(result.error.parsingErrors.isEmpty())
    }

    @Test
    fun successfulSequenceIsConsumedExactlyOnce() {
        var iteratorCalls = 0
        var consumedLines = 0
        val lines = object : Sequence<String> {
            override fun iterator(): Iterator<String> {
                iteratorCalls++
                return source(dataLine(PRODUCT_ISIN), dataLine(OTHER_ISIN))
                    .onEach { consumedLines++ }
                    .iterator()
            }
        }

        load(lines, setOf(PRODUCT_ISIN))

        assertEquals(1, iteratorCalls)
        assertEquals(5, consumedLines)
    }

    @Test
    fun retainedRecordsPreserveInputOrderAcrossIsins() {
        val result = load(
            source(
                dataLine(OTHER_ISIN, instrumentName = "other first"),
                dataLine(PRODUCT_ISIN, instrumentName = "product second"),
                dataLine(OTHER_ISIN, instrumentName = "other third")
            ),
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertEquals(
            listOf("other first", "product second", "other third"),
            result.records.map { it.instrumentName }
        )
    }

    @Test
    fun returnedRecordsListCannotBeMutated() {
        val result = load(source(dataLine(PRODUCT_ISIN)), setOf(PRODUCT_ISIN))

        assertThrows(UnsupportedOperationException::class.java) {
            (result.records as MutableList).clear()
        }
    }

    private fun load(
        lines: Sequence<String>,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseXfraCsvLoadingResult.Success =
        DeutscheBoerseXfraCsvSnapshotLoader.load(
            lines = lines,
            requestedProductIsins = requestedProductIsins
        ) as DeutscheBoerseXfraCsvLoadingResult.Success

    private fun loadFailure(
        lines: Sequence<String>
    ): DeutscheBoerseXfraCsvLoadingResult.Failure =
        DeutscheBoerseXfraCsvSnapshotLoader.load(
            lines = lines,
            requestedProductIsins = setOf(PRODUCT_ISIN)
        ) as DeutscheBoerseXfraCsvLoadingResult.Failure

    private fun assertHeaderMissing(
        lines: Sequence<String>,
        expectedLineNumber: Long
    ) {
        val result = loadFailure(lines)

        assertEquals(DeutscheBoerseXfraCsvLoadingErrorCode.HEADER_MISSING, result.error.code)
        assertEquals(expectedLineNumber, result.error.lineNumber)
        assertTrue(result.error.parsingErrors.isEmpty())
    }

    private fun assertSourceFailure(
        lines: Sequence<String>,
        expectedLineNumber: Long
    ) {
        val result = loadFailure(lines)

        assertEquals(
            DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
            result.error.code
        )
        assertEquals(expectedLineNumber, result.error.lineNumber)
        assertTrue(result.error.parsingErrors.isEmpty())
    }

    private fun fixtureLines(): Sequence<String> =
        checkNotNull(
            javaClass.getResourceAsStream(
                "/deutscheboerse/xfra-tradable-instruments-sample.csv"
            )
        ).bufferedReader().use { reader -> reader.readLines().asSequence() }

    private fun source(vararg dataLines: String): Sequence<String> =
        sequenceOf("metadata one", "metadata two", validHeader(), *dataLines)

    private fun validHeader(): String = DeutscheBoerseXfraRequiredColumn.entries
        .joinToString(";") { it.headerName }

    private fun dataLine(
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

    private fun missing(
        column: DeutscheBoerseXfraRequiredColumn
    ) = DeutscheBoerseXfraCsvRowParsingError(
        code = DeutscheBoerseXfraCsvRowParsingErrorCode.MISSING_REQUIRED_COLUMN,
        column = column
    )

    private fun ioFailingSequence(
        linesBeforeFailure: List<String>
    ): Sequence<String> = Sequence {
        object : Iterator<String> {
            private var index = 0

            override fun hasNext(): Boolean {
                if (index < linesBeforeFailure.size) {
                    return true
                }
                throw IOException("synthetic read failure")
            }

            override fun next(): String = linesBeforeFailure[index++]
        }
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000LOAD01"
        const val OTHER_ISIN = "DE000LOAD02"
    }
}
