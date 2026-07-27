package de.konavigator.app.data.remote.provider.deutscheboerse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseXfraTradableInstrumentCsvRowParserTest {

    @Test
    fun firstFixtureDataLineMapsEveryRequiredFieldExactly() {
        val record = parseFixtureDataLine(3)

        assertEquals("Active", record.productStatus)
        assertEquals("Tradable", record.instrumentStatus)
        assertEquals("Synthetic Long Warrant", record.instrumentName)
        assertEquals("DE000SYNTH07", record.isin)
        assertEquals("SYN007", record.wkn)
        assertEquals("XFRA", record.micCode)
        assertEquals("Warrant", record.instrumentType)
        assertEquals("EUR", record.settlementCurrency)
        assertEquals("EUR", record.currency)
        assertEquals("Call", record.warrantType)
        assertEquals("08:00", record.quotingPeriodStart)
        assertEquals("22:00", record.quotingPeriodEnd)
    }

    @Test
    fun currencyAndSettlementCurrencyRemainIndependent() {
        val record = parseFixtureDataLine(4)

        assertEquals("MXN", record.currency)
        assertEquals("EUR", record.settlementCurrency)
    }

    @Test
    fun thirdFixtureDataLineMapsEmptyKnownCellsToNull() {
        val record = parseFixtureDataLine(5)

        assertNull(record.instrumentStatus)
        assertNull(record.wkn)
        assertNull(record.currency)
        assertNull(record.quotingPeriodStart)
        assertNull(record.quotingPeriodEnd)
    }

    @Test
    fun unknownAdditionalColumnsAreIgnored() {
        val record = parseFixtureDataLine(5)

        assertEquals("Sparse Instrument", record.instrumentName)
        assertEquals("DE000SYNTH09", record.isin)
    }

    @Test
    fun metadataLinesAreNotNeededWhenHeaderLineIsPassedDirectly() {
        val lines = fixtureLines()

        assertEquals("Market:;XFRA", lines[0])
        assertEquals("Date Last Update:;27.07.2026", lines[1])
        assertTrue(parse(lines[2], lines[3]) is DeutscheBoerseXfraCsvRowParsingResult.Success)
    }

    @Test
    fun changedHeaderOrderIsResolvedByExactNames() {
        val header = DeutscheBoerseXfraRequiredColumn.entries
            .reversed()
            .joinToString(";") { it.headerName }
        val data = listOf(
            "22:00",
            "08:00",
            "Call",
            "USD",
            "EUR",
            "Warrant",
            "XFRA",
            "SYN010",
            "DE000SYNTH10",
            "Reordered Instrument",
            "Tradable",
            "Active"
        ).joinToString(";")

        val record = parseSuccess(header, data)

        assertEquals("Active", record.productStatus)
        assertEquals("DE000SYNTH10", record.isin)
        assertEquals("USD", record.currency)
        assertEquals("08:00", record.quotingPeriodStart)
    }

    @Test
    fun leadingAndTrailingSpacesInNonEmptyCellRemainUnchanged() {
        val data = validData().toMutableList().apply {
            this[DeutscheBoerseXfraRequiredColumn.CURRENCY.ordinal] = " EUR "
        }.joinToString(";")

        val record = parseSuccess(validHeader(), data)

        assertEquals(" EUR ", record.currency)
    }

    @Test
    fun missingRequiredHeaderProducesTypedColumnError() {
        val header = headerWithout(DeutscheBoerseXfraRequiredColumn.CURRENCY)

        assertFailure(
            headerLine = header,
            dataLine = "unused",
            expectedErrors = listOf(
                missing(DeutscheBoerseXfraRequiredColumn.CURRENCY)
            )
        )
    }

    @Test
    fun multipleMissingHeadersProduceAllErrorsInStableOrder() {
        val missingColumns = setOf(
            DeutscheBoerseXfraRequiredColumn.PRODUCT_STATUS,
            DeutscheBoerseXfraRequiredColumn.WKN,
            DeutscheBoerseXfraRequiredColumn.CURRENCY
        )
        val header = DeutscheBoerseXfraRequiredColumn.entries
            .filterNot(missingColumns::contains)
            .joinToString(";") { it.headerName }

        assertFailure(
            headerLine = header,
            dataLine = "unused",
            expectedErrors = listOf(
                missing(DeutscheBoerseXfraRequiredColumn.PRODUCT_STATUS),
                missing(DeutscheBoerseXfraRequiredColumn.WKN),
                missing(DeutscheBoerseXfraRequiredColumn.CURRENCY)
            )
        )
    }

    @Test
    fun duplicateRequiredHeaderProducesTypedColumnError() {
        val header = validHeader() + ";ISIN"

        assertFailure(
            headerLine = header,
            dataLine = "unused",
            expectedErrors = listOf(
                DeutscheBoerseXfraCsvRowParsingError(
                    code =
                        DeutscheBoerseXfraCsvRowParsingErrorCode
                            .DUPLICATE_REQUIRED_COLUMN,
                    column = DeutscheBoerseXfraRequiredColumn.ISIN
                )
            )
        )
    }

    @Test
    fun emptyHeaderProducesInvalidHeader() {
        assertFailure("", "unused", listOf(error(DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_HEADER)))
    }

    @Test
    fun emptyDataRowProducesInvalidDataRow() {
        assertFailure(validHeader(), "", listOf(error(DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_DATA_ROW)))
    }

    @Test
    fun tooFewDataColumnsProduceCountMismatch() {
        assertFailure(
            validHeader(),
            validData().dropLast(1).joinToString(";"),
            listOf(error(DeutscheBoerseXfraCsvRowParsingErrorCode.COLUMN_COUNT_MISMATCH))
        )
    }

    @Test
    fun tooManyDataColumnsProduceCountMismatch() {
        assertFailure(
            validHeader(),
            validData().plus("extra").joinToString(";"),
            listOf(error(DeutscheBoerseXfraCsvRowParsingErrorCode.COLUMN_COUNT_MISMATCH))
        )
    }

    @Test
    fun trailingEmptyColumnIsPreservedForColumnCount() {
        val header = validHeader() + ";Unknown Tail"
        val data = validData().joinToString(";") + ";"

        assertTrue(parse(header, data) is DeutscheBoerseXfraCsvRowParsingResult.Success)
    }

    @Test
    fun quoteInHeaderProducesInvalidHeader() {
        assertFailure(
            validHeader() + ";\"Unknown\"",
            "unused",
            listOf(error(DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_HEADER))
        )
    }

    @Test
    fun quoteInDataRowProducesInvalidDataRow() {
        val data = validData().toMutableList().apply {
            this[DeutscheBoerseXfraRequiredColumn.INSTRUMENT_NAME.ordinal] =
                "\"Quoted Instrument\""
        }.joinToString(";")

        assertFailure(
            validHeader(),
            data,
            listOf(error(DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_DATA_ROW))
        )
    }

    @Test
    fun missingCurrencyIsNotReplacedBySettlementCurrency() {
        val data = validData().toMutableList().apply {
            this[DeutscheBoerseXfraRequiredColumn.SETTLEMENT_CURRENCY.ordinal] = "EUR"
            this[DeutscheBoerseXfraRequiredColumn.CURRENCY.ordinal] = ""
        }.joinToString(";")

        val record = parseSuccess(validHeader(), data)

        assertEquals("EUR", record.settlementCurrency)
        assertNull(record.currency)
    }

    private fun parseFixtureDataLine(index: Int): DeutscheBoerseXfraTradableInstrumentRecord {
        val lines = fixtureLines()
        return parseSuccess(lines[2], lines[index])
    }

    private fun fixtureLines(): List<String> =
        checkNotNull(
            javaClass.getResourceAsStream(
                "/deutscheboerse/xfra-tradable-instruments-sample.csv"
            )
        ).bufferedReader().use { it.readLines() }

    private fun validHeader(): String = DeutscheBoerseXfraRequiredColumn.entries
        .joinToString(";") { it.headerName }

    private fun headerWithout(column: DeutscheBoerseXfraRequiredColumn): String =
        DeutscheBoerseXfraRequiredColumn.entries
            .filterNot { it == column }
            .joinToString(";") { it.headerName }

    private fun validData(): List<String> = listOf(
        "Active",
        "Tradable",
        "Synthetic Instrument",
        "DE000SYNTH10",
        "SYN010",
        "XFRA",
        "Warrant",
        "EUR",
        "EUR",
        "Call",
        "08:00",
        "22:00"
    )

    private fun parseSuccess(
        headerLine: String,
        dataLine: String
    ): DeutscheBoerseXfraTradableInstrumentRecord =
        (parse(headerLine, dataLine) as
            DeutscheBoerseXfraCsvRowParsingResult.Success).record

    private fun parse(
        headerLine: String,
        dataLine: String
    ): DeutscheBoerseXfraCsvRowParsingResult =
        DeutscheBoerseXfraTradableInstrumentCsvRowParser.parse(
            headerLine = headerLine,
            dataLine = dataLine
        )

    private fun assertFailure(
        headerLine: String,
        dataLine: String,
        expectedErrors: List<DeutscheBoerseXfraCsvRowParsingError>
    ) {
        val result = parse(headerLine, dataLine) as
            DeutscheBoerseXfraCsvRowParsingResult.Failure

        assertEquals(expectedErrors, result.errors)
    }

    private fun missing(
        column: DeutscheBoerseXfraRequiredColumn
    ) = DeutscheBoerseXfraCsvRowParsingError(
        code = DeutscheBoerseXfraCsvRowParsingErrorCode.MISSING_REQUIRED_COLUMN,
        column = column
    )

    private fun error(
        code: DeutscheBoerseXfraCsvRowParsingErrorCode
    ) = DeutscheBoerseXfraCsvRowParsingError(code = code)
}
