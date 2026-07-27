package de.konavigator.app.data.remote.provider.deutscheboerse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeutscheBoerseXfraTradableInstrumentRecordTest {

    @Test
    fun completeRecordPreservesEveryRawValueExactly() {
        val record = DeutscheBoerseXfraTradableInstrumentRecord(
            productStatus = "Active",
            instrumentStatus = "Tradable",
            instrumentName = "Synthetic Long Warrant",
            isin = "DE000SYNTH02",
            wkn = "SYN002",
            micCode = "XFRA",
            instrumentType = "Warrant",
            settlementCurrency = "EUR",
            currency = "USD",
            warrantType = "Call",
            quotingPeriodStart = "2026-01-02",
            quotingPeriodEnd = "2026-12-30"
        )

        assertEquals("Active", record.productStatus)
        assertEquals("Tradable", record.instrumentStatus)
        assertEquals("Synthetic Long Warrant", record.instrumentName)
        assertEquals("DE000SYNTH02", record.isin)
        assertEquals("SYN002", record.wkn)
        assertEquals("XFRA", record.micCode)
        assertEquals("Warrant", record.instrumentType)
        assertEquals("EUR", record.settlementCurrency)
        assertEquals("USD", record.currency)
        assertEquals("Call", record.warrantType)
        assertEquals("2026-01-02", record.quotingPeriodStart)
        assertEquals("2026-12-30", record.quotingPeriodEnd)
    }

    @Test
    fun currencyAndSettlementCurrencyRemainIndependent() {
        val record = recordWith(
            settlementCurrency = "EUR",
            currency = "MXN"
        )

        assertEquals("MXN", record.currency)
        assertEquals("EUR", record.settlementCurrency)
    }

    @Test
    fun missingExternalValuesRemainNullWithoutReplacement() {
        val record = recordWith(
            settlementCurrency = null,
            currency = null
        )

        assertNull(record.productStatus)
        assertNull(record.instrumentStatus)
        assertNull(record.instrumentName)
        assertNull(record.isin)
        assertNull(record.wkn)
        assertNull(record.micCode)
        assertNull(record.instrumentType)
        assertNull(record.settlementCurrency)
        assertNull(record.currency)
        assertNull(record.warrantType)
        assertNull(record.quotingPeriodStart)
        assertNull(record.quotingPeriodEnd)
    }

    private fun recordWith(
        settlementCurrency: String?,
        currency: String?
    ) = DeutscheBoerseXfraTradableInstrumentRecord(
        productStatus = null,
        instrumentStatus = null,
        instrumentName = null,
        isin = null,
        wkn = null,
        micCode = null,
        instrumentType = null,
        settlementCurrency = settlementCurrency,
        currency = currency,
        warrantType = null,
        quotingPeriodStart = null,
        quotingPeriodEnd = null
    )
}
