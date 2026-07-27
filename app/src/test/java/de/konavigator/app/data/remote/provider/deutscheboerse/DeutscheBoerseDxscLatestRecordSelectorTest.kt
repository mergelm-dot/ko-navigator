package de.konavigator.app.data.remote.provider.deutscheboerse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseDxscLatestRecordSelectorTest {

    @Test
    fun noMatchingIsinProducesNotFound() {
        val result = select(record(isin = "DE000OTHER01"))

        assertEquals(DeutscheBoerseDxscLatestRecordSelectionResult.NotFound, result)
    }

    @Test
    fun isinComparisonIsExactAndNotNormalized() {
        val result = select(record(isin = " de000select01 "))

        assertEquals(DeutscheBoerseDxscLatestRecordSelectionResult.NotFound, result)
    }

    @Test
    fun singleMatchingValidRecordIsSelected() {
        val record = record(messageId = "single")

        val result = select(record) as DeutscheBoerseDxscLatestRecordSelectionResult.Success

        assertSame(record, result.record)
    }

    @Test
    fun newestOfMultipleMatchingRecordsIsSelected() {
        val older = record(
            messageId = "older",
            timestamp = "2026-07-27T19:29:56.999999999Z"
        )
        val newer = record(
            messageId = "newer",
            timestamp = "2026-07-27T19:29:57Z"
        )

        val result = select(older, newer) as
            DeutscheBoerseDxscLatestRecordSelectionResult.Success

        assertSame(newer, result.record)
    }

    @Test
    fun newestRecordWithinSameMillisecondIsSelectedByNanoseconds() {
        val older = record(
            messageId = "older",
            timestamp = "2026-07-27T19:29:57.363600000Z"
        )
        val newer = record(
            messageId = "newer",
            timestamp = "2026-07-27T19:29:57.363600001Z"
        )

        val result = select(older, newer) as
            DeutscheBoerseDxscLatestRecordSelectionResult.Success

        assertSame(newer, result.record)
    }

    @Test
    fun invalidTimestampsOfNonMatchingRecordsDoNotAffectSelection() {
        val matching = record(messageId = "matching")
        val otherMissing = record(isin = "DE000OTHER01", timestamp = null)
        val otherInvalid = record(isin = null, timestamp = "invalid")

        val result = select(otherMissing, matching, otherInvalid) as
            DeutscheBoerseDxscLatestRecordSelectionResult.Success

        assertSame(matching, result.record)
    }

    @Test
    fun matchingRecordWithoutTimestampProducesMissingError() {
        val result = select(record(timestamp = null)) as
            DeutscheBoerseDxscLatestRecordSelectionResult.Failure

        assertEquals(
            listOf(
                DeutscheBoerseDxscRecordSelectionErrorCode
                    .MISSING_UPDATE_DATE_AND_TIME
            ),
            result.errors
        )
    }

    @Test
    fun matchingRecordWithInvalidTimestampProducesInvalidError() {
        val result = select(record(timestamp = "invalid")) as
            DeutscheBoerseDxscLatestRecordSelectionResult.Failure

        assertEquals(
            listOf(
                DeutscheBoerseDxscRecordSelectionErrorCode
                    .INVALID_UPDATE_DATE_AND_TIME
            ),
            result.errors
        )
    }

    @Test
    fun missingAndInvalidTimestampsProduceBothErrorsInStableOrder() {
        val result = select(
            record(messageId = "missing", timestamp = null),
            record(messageId = "invalid", timestamp = "invalid")
        ) as DeutscheBoerseDxscLatestRecordSelectionResult.Failure

        assertEquals(
            listOf(
                DeutscheBoerseDxscRecordSelectionErrorCode.MISSING_UPDATE_DATE_AND_TIME,
                DeutscheBoerseDxscRecordSelectionErrorCode.INVALID_UPDATE_DATE_AND_TIME
            ),
            result.errors
        )
    }

    @Test
    fun invalidMatchingRecordPreventsPartialSuccessFromValidRecord() {
        val result = select(
            record(messageId = "valid"),
            record(messageId = "invalid", timestamp = "invalid")
        )

        assertTrue(result is DeutscheBoerseDxscLatestRecordSelectionResult.Failure)
    }

    @Test
    fun identicalTimestampsKeepFirstMatchingRecord() {
        val first = record(messageId = "first")
        val second = record(messageId = "second")

        val result = select(first, second) as
            DeutscheBoerseDxscLatestRecordSelectionResult.Success

        assertSame(first, result.record)
    }

    @Test
    fun selectionPreservesBidAskAndZeroValues() {
        val record = record(bestBid = 0.0, bestAsk = 2.5)

        val result = select(record) as DeutscheBoerseDxscLatestRecordSelectionResult.Success

        assertSame(record, result.record)
        assertEquals(0.0, result.record.bestBid)
        assertEquals(2.5, result.record.bestAsk)
    }

    private fun select(
        vararg records: DeutscheBoerseDxscPretradeRecord
    ): DeutscheBoerseDxscLatestRecordSelectionResult =
        DeutscheBoerseDxscLatestRecordSelector.select(
            records = records.asList(),
            productIsin = PRODUCT_ISIN
        )

    private fun record(
        messageId: String = "pretrade",
        isin: String? = PRODUCT_ISIN,
        timestamp: String? = "2026-07-27T19:29:57.363600000Z",
        bestBid: Double? = 2.343,
        bestAsk: Double? = 2.344
    ) = DeutscheBoerseDxscPretradeRecord(
        messageId = messageId,
        instrumentIdentificationCode = isin,
        bestBid = bestBid,
        bestBidQuantity = 80_000.0,
        bestAsk = bestAsk,
        bestAskQuantity = 80_000.0,
        updateDateAndTime = timestamp
    )

    private companion object {
        const val PRODUCT_ISIN = "DE000SELECT01"
    }
}
