package de.konavigator.app.data.remote.provider.deutscheboerse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeutscheBoerseDxscPretradeJsonLineParserTest {

    @Test
    fun firstFixtureLineMapsEveryKnownFieldExactly() {
        val record = parseFixtureLine(0)

        assertEquals("pretrade", record.messageId)
        assertEquals("DE000SYNTH04", record.instrumentIdentificationCode)
        assertEquals(2.343, record.bestBid)
        assertEquals(80_000.0, record.bestBidQuantity)
        assertEquals(2.344, record.bestAsk)
        assertEquals(80_000.0, record.bestAskQuantity)
        assertEquals("2026-07-27T19:29:57.363600000Z", record.updateDateAndTime)
    }

    @Test
    fun secondFixtureLinePreservesNullAndZeroValues() {
        val record = parseFixtureLine(1)

        assertNull(record.bestBid)
        assertEquals(0.0, record.bestAsk)
        assertEquals(80_000.0, record.bestBidQuantity)
        assertEquals(0.0, record.bestAskQuantity)
    }

    @Test
    fun thirdFixtureLineKeepsMissingFieldsNullAndIgnoresUnknownField() {
        val record = parseFixtureLine(2)

        assertEquals("pretrade", record.messageId)
        assertEquals("DE000SYNTH06", record.instrumentIdentificationCode)
        assertEquals(1.25, record.bestBid)
        assertNull(record.bestBidQuantity)
        assertNull(record.bestAsk)
        assertNull(record.bestAskQuantity)
        assertNull(record.updateDateAndTime)
    }

    @Test
    fun explicitJsonNullValuesMapToNull() {
        val record = parseSuccess(
            """{"messageId":null,"instrumentIdentificationCode":null,"bestBid":null,"bestBidQty":null,"bestAsk":null,"bestAskQty":null,"updateDateAndTime":null}"""
        )

        assertEquals(
            DeutscheBoerseDxscPretradeRecord(
                messageId = null,
                instrumentIdentificationCode = null,
                bestBid = null,
                bestBidQuantity = null,
                bestAsk = null,
                bestAskQuantity = null,
                updateDateAndTime = null
            ),
            record
        )
    }

    @Test
    fun emptyStringProducesInvalidJson() {
        assertFailure(
            line = "",
            expectedErrors = listOf(
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON
            )
        )
    }

    @Test
    fun syntacticallyInvalidJsonProducesInvalidJson() {
        assertFailure(
            line = "{\"messageId\":",
            expectedErrors = listOf(
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON
            )
        )
    }

    @Test
    fun jsonArrayRootProducesRootNotObject() {
        assertFailure(
            line = "[]",
            expectedErrors = listOf(
                DeutscheBoerseDxscJsonLineParsingErrorCode.ROOT_NOT_OBJECT
            )
        )
    }

    @Test
    fun jsonStringRootProducesRootNotObject() {
        assertFailure(
            line = "\"pretrade\"",
            expectedErrors = listOf(
                DeutscheBoerseDxscJsonLineParsingErrorCode.ROOT_NOT_OBJECT
            )
        )
    }

    @Test
    fun stringInNumericFieldProducesTypedError() {
        assertFailure(
            line = """{"bestBid":"2.343"}""",
            expectedErrors = listOf(
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_BEST_BID_TYPE
            )
        )
    }

    @Test
    fun numberInStringFieldProducesTypedError() {
        assertFailure(
            line = """{"messageId":123}""",
            expectedErrors = listOf(
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_MESSAGE_ID_TYPE
            )
        )
    }

    @Test
    fun multipleInvalidFieldTypesProduceAllErrorsInStableOrder() {
        assertFailure(
            line =
                """{"messageId":1,"instrumentIdentificationCode":true,"bestBid":"2.343","bestBidQty":false,"bestAsk":"2.344","bestAskQty":{},"updateDateAndTime":9}""",
            expectedErrors = listOf(
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_MESSAGE_ID_TYPE,
                DeutscheBoerseDxscJsonLineParsingErrorCode
                    .INVALID_INSTRUMENT_IDENTIFICATION_CODE_TYPE,
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_BEST_BID_TYPE,
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_BEST_BID_QUANTITY_TYPE,
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_BEST_ASK_TYPE,
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_BEST_ASK_QUANTITY_TYPE,
                DeutscheBoerseDxscJsonLineParsingErrorCode
                    .INVALID_UPDATE_DATE_AND_TIME_TYPE
            )
        )
    }

    @Test
    fun semanticallyInvalidTimestampRemainsUnchangedRawString() {
        val record = parseSuccess(
            """{"updateDateAndTime":"not-a-timestamp"}"""
        )

        assertEquals("not-a-timestamp", record.updateDateAndTime)
    }

    @Test
    fun scientificJsonNumbersMapToFiniteDoubles() {
        val record = parseSuccess(
            """{"bestBid":2.343e0,"bestAskQty":8e4}"""
        )

        assertEquals(2.343, record.bestBid)
        assertEquals(80_000.0, record.bestAskQuantity)
    }

    @Test
    fun integerZeroAndDecimalZeroRemainValidNumbers() {
        val record = parseSuccess(
            """{"bestBid":0,"bestAsk":0.0}"""
        )

        assertEquals(0.0, record.bestBid)
        assertEquals(0.0, record.bestAsk)
    }

    private fun parseFixtureLine(index: Int): DeutscheBoerseDxscPretradeRecord =
        parseSuccess(fixtureLines()[index])

    private fun fixtureLines(): List<String> =
        checkNotNull(
            javaClass.getResourceAsStream("/deutscheboerse/dxsc-pretrade-sample.ndjson")
        ).bufferedReader().use { it.readLines() }

    private fun parseSuccess(line: String): DeutscheBoerseDxscPretradeRecord =
        (DeutscheBoerseDxscPretradeJsonLineParser.parse(line) as
            DeutscheBoerseDxscJsonLineParsingResult.Success).record

    private fun assertFailure(
        line: String,
        expectedErrors: List<DeutscheBoerseDxscJsonLineParsingErrorCode>
    ) {
        val result = DeutscheBoerseDxscPretradeJsonLineParser.parse(line) as
            DeutscheBoerseDxscJsonLineParsingResult.Failure

        assertEquals(expectedErrors, result.errors)
    }
}
