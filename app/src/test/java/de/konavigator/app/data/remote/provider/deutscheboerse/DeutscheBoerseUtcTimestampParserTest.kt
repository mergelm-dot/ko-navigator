package de.konavigator.app.data.remote.provider.deutscheboerse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeutscheBoerseUtcTimestampParserTest {

    @Test
    fun timestampWithoutFractionIsAccepted() {
        val result = DeutscheBoerseUtcTimestampParser.parse("2026-07-27T19:29:57Z")

        assertNotNull(result)
        assertEquals(0, result?.nanoOfSecond)
    }

    @Test
    fun timestampWithOneFractionDigitIsAccepted() {
        val result = DeutscheBoerseUtcTimestampParser.parse("2026-07-27T19:29:57.1Z")

        assertEquals(100_000_000, result?.nanoOfSecond)
    }

    @Test
    fun timestampWithNineFractionDigitsIsPreservedCompletely() {
        val result = DeutscheBoerseUtcTimestampParser.parse(
            "2026-07-27T19:29:57.123456789Z"
        )

        assertEquals(123_456_789, result?.nanoOfSecond)
    }

    @Test
    fun referenceTimestampProducesExpectedEpochMillisAndNanoseconds() {
        val result = DeutscheBoerseUtcTimestampParser.parse(
            "2026-07-27T19:29:57.363600000Z"
        )

        assertEquals(1_785_180_597_363L, result?.epochMillis)
        assertEquals(363_600_000, result?.nanoOfSecond)
    }

    @Test
    fun subMillisecondsAreTruncatedInsteadOfRounded() {
        val result = DeutscheBoerseUtcTimestampParser.parse(
            "2026-07-27T19:29:57.363999999Z"
        )

        assertEquals(1_785_180_597_363L, result?.epochMillis)
        assertEquals(363_999_999, result?.nanoOfSecond)
    }

    @Test
    fun syntacticallyInvalidTimestampIsRejected() {
        assertNull(
            DeutscheBoerseUtcTimestampParser.parse("2026-07-27 19:29:57Z")
        )
    }

    @Test
    fun calendarInvalidTimestampIsRejected() {
        assertNull(
            DeutscheBoerseUtcTimestampParser.parse("2026-02-30T19:29:57Z")
        )
    }

    @Test
    fun moreThanNineFractionDigitsAreRejected() {
        assertNull(
            DeutscheBoerseUtcTimestampParser.parse(
                "2026-07-27T19:29:57.1234567890Z"
            )
        )
    }

    @Test
    fun missingUtcSuffixIsRejected() {
        assertNull(
            DeutscheBoerseUtcTimestampParser.parse("2026-07-27T19:29:57.1")
        )
    }
}
