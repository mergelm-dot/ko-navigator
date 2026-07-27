package de.konavigator.app.data.remote.provider.deutscheboerse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeutscheBoerseDxscPretradeRecordTest {

    @Test
    fun completeRecordPreservesEveryRawValueExactly() {
        val record = DeutscheBoerseDxscPretradeRecord(
            messageId = "pretrade",
            instrumentIdentificationCode = "DE000SYNTH01",
            bestBid = 2.343,
            bestBidQuantity = 80_000.0,
            bestAsk = 2.344,
            bestAskQuantity = 79_500.0,
            updateDateAndTime = "2026-07-27T19:29:57.363600000Z"
        )

        assertEquals("pretrade", record.messageId)
        assertEquals("DE000SYNTH01", record.instrumentIdentificationCode)
        assertEquals(2.343, record.bestBid)
        assertEquals(80_000.0, record.bestBidQuantity)
        assertEquals(2.344, record.bestAsk)
        assertEquals(79_500.0, record.bestAskQuantity)
        assertEquals("2026-07-27T19:29:57.363600000Z", record.updateDateAndTime)
    }

    @Test
    fun nullZeroAndQuantitiesRemainDistinctAndUnchanged() {
        val record = DeutscheBoerseDxscPretradeRecord(
            messageId = null,
            instrumentIdentificationCode = null,
            bestBid = null,
            bestBidQuantity = 0.0,
            bestAsk = 0.0,
            bestAskQuantity = -25.5,
            updateDateAndTime = null
        )

        assertNull(record.bestBid)
        assertEquals(0.0, record.bestAsk)
        assertEquals(0.0, record.bestBidQuantity)
        assertEquals(-25.5, record.bestAskQuantity)
    }
}
