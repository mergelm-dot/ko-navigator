package de.konavigator.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnockoutProductMarketDataTest {

    @Test
    fun completeQuoteStoresAllFields() {
        val marketData = createMarketData()

        assertEquals("DE000TEST001", marketData.productIsin)
        assertEquals(1.95, marketData.bid!!, TOLERANCE)
        assertEquals(2.00, marketData.ask!!, TOLERANCE)
        assertEquals(1_700_000_000_000L, marketData.bidTimestampEpochMillis)
        assertEquals(1_700_000_000_100L, marketData.askTimestampEpochMillis)
        assertEquals("EUR", marketData.currency)
        assertEquals("issuer-demo", marketData.sourceId)
    }

    @Test
    fun bidOnlyQuoteCanBeRepresented() {
        val marketData = createMarketData(
            ask = null,
            askTimestampEpochMillis = null
        )

        assertEquals(1.95, marketData.bid!!, TOLERANCE)
        assertEquals(1_700_000_000_000L, marketData.bidTimestampEpochMillis)
        assertNull(marketData.ask)
        assertNull(marketData.askTimestampEpochMillis)
    }

    @Test
    fun askOnlyQuoteCanBeRepresented() {
        val marketData = createMarketData(
            bid = null,
            bidTimestampEpochMillis = null
        )

        assertNull(marketData.bid)
        assertNull(marketData.bidTimestampEpochMillis)
        assertEquals(2.00, marketData.ask!!, TOLERANCE)
        assertEquals(1_700_000_000_100L, marketData.askTimestampEpochMillis)
    }

    @Test
    fun bothQuoteSidesCanBeMissing() {
        val marketData = createMarketData(
            bid = null,
            ask = null,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )

        assertNull(marketData.bid)
        assertNull(marketData.ask)
        assertNull(marketData.bidTimestampEpochMillis)
        assertNull(marketData.askTimestampEpochMillis)
    }

    @Test
    fun bidAndAskCanHaveDifferentTimestamps() {
        val marketData = createMarketData(
            bidTimestampEpochMillis = 1_700_000_000_000L,
            askTimestampEpochMillis = 1_700_000_000_100L
        )

        assertNotEquals(
            marketData.bidTimestampEpochMillis,
            marketData.askTimestampEpochMillis
        )
    }

    @Test
    fun usdQuoteCurrencyIsStoredUnchanged() {
        val marketData = createMarketData(currency = "USD")

        assertEquals("USD", marketData.currency)
    }

    @Test
    fun usdQuoteCurrencyIsNotNormalizedToEur() {
        val marketData = createMarketData(currency = "USD")

        assertNotEquals("EUR", marketData.currency)
    }

    @Test
    fun sourceIdIsStoredUnchanged() {
        val marketData = createMarketData(sourceId = "broker-feed-1")

        assertEquals("broker-feed-1", marketData.sourceId)
    }

    @Test
    fun productIsinRemainsAPlainProductReference() {
        val marketData = createMarketData(productIsin = "DE000OTHER01")

        assertEquals("DE000OTHER01", marketData.productIsin)
    }

    @Test
    fun identicalMarketDataInstancesAreEqual() {
        val first = createMarketData()
        val second = createMarketData()

        assertEquals(first, second)
    }

    @Test
    fun copyCanChangeOnlyAskPrice() {
        val original = createMarketData()
        val updated = original.copy(ask = 2.05)

        assertEquals(2.05, updated.ask!!, TOLERANCE)
        assertEquals(original.productIsin, updated.productIsin)
        assertEquals(original.bid, updated.bid)
        assertEquals(original.bidTimestampEpochMillis, updated.bidTimestampEpochMillis)
        assertEquals(original.askTimestampEpochMillis, updated.askTimestampEpochMillis)
        assertEquals(original.currency, updated.currency)
        assertEquals(original.sourceId, updated.sourceId)
    }

    @Test
    fun copyCanChangeOnlyAskTimestamp() {
        val original = createMarketData()
        val updated = original.copy(askTimestampEpochMillis = 1_700_000_000_200L)

        assertEquals(1_700_000_000_200L, updated.askTimestampEpochMillis)
        assertEquals(original.productIsin, updated.productIsin)
        assertEquals(original.bid, updated.bid)
        assertEquals(original.ask, updated.ask)
        assertEquals(original.bidTimestampEpochMillis, updated.bidTimestampEpochMillis)
        assertEquals(original.currency, updated.currency)
        assertEquals(original.sourceId, updated.sourceId)
    }

    @Test
    fun missingQuotesRemainNullInsteadOfZero() {
        val marketData = createMarketData(
            bid = null,
            ask = null,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )

        assertNull(marketData.bid)
        assertNull(marketData.ask)
    }

    private fun createMarketData(
        productIsin: String = "DE000TEST001",
        bid: Double? = 1.95,
        ask: Double? = 2.00,
        bidTimestampEpochMillis: Long? = 1_700_000_000_000L,
        askTimestampEpochMillis: Long? = 1_700_000_000_100L,
        currency: String = "EUR",
        sourceId: String = "issuer-demo"
    ): KnockoutProductMarketData {
        return KnockoutProductMarketData(
            productIsin = productIsin,
            bid = bid,
            ask = ask,
            bidTimestampEpochMillis = bidTimestampEpochMillis,
            askTimestampEpochMillis = askTimestampEpochMillis,
            currency = currency,
            sourceId = sourceId
        )
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
