package de.konavigator.app.data.remote.provider.hsbc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HsbcKnockoutProductSpecificationRecordTest {

    @Test
    fun completeRecordPreservesEveryValueExactly() {
        val record = completeRecord()

        assertEquals("DE000SYNTH01", record.productIsin)
        assertEquals("SYN001", record.productWkn)
        assertEquals("synthetic-hsbc", record.issuerId)
        assertEquals("synthetic-underlying", record.underlyingId)
        assertEquals("Call", record.directionLabel)
        assertEquals(80.125, record.basePrice)
        assertEquals(82.5, record.knockoutBarrier)
        assertEquals(0.1, record.ratio)
        assertEquals("USD", record.underlyingCurrency)
        assertEquals("EUR", record.productCurrency)
        assertEquals(1_700_000_000_250L, record.sourceTimestampEpochMillis)
    }

    @Test
    fun nullZeroAndNegativeValuesRemainDistinct() {
        val record = HsbcKnockoutProductSpecificationRecord(
            productIsin = null,
            productWkn = null,
            issuerId = null,
            underlyingId = null,
            directionLabel = null,
            basePrice = 0.0,
            knockoutBarrier = -1.0,
            ratio = 0.0,
            underlyingCurrency = null,
            productCurrency = null,
            sourceTimestampEpochMillis = null
        )

        assertNull(record.productIsin)
        assertNull(record.productWkn)
        assertNull(record.issuerId)
        assertNull(record.underlyingId)
        assertNull(record.directionLabel)
        assertEquals(0.0, record.basePrice)
        assertEquals(-1.0, record.knockoutBarrier)
        assertEquals(0.0, record.ratio)
        assertNull(record.underlyingCurrency)
        assertNull(record.productCurrency)
        assertNull(record.sourceTimestampEpochMillis)
    }

    @Test
    fun whitespaceAndCaseRemainUnchanged() {
        val record = HsbcKnockoutProductSpecificationRecord(
            productIsin = " de000Synthetic01 ",
            productWkn = " sYn001 ",
            issuerId = " HSBC-Research ",
            underlyingId = " Synthetic-Underlying ",
            directionLabel = "cAlL",
            basePrice = null,
            knockoutBarrier = null,
            ratio = null,
            underlyingCurrency = " usd ",
            productCurrency = "eUr",
            sourceTimestampEpochMillis = null
        )

        assertEquals(" de000Synthetic01 ", record.productIsin)
        assertEquals(" sYn001 ", record.productWkn)
        assertEquals(" HSBC-Research ", record.issuerId)
        assertEquals(" Synthetic-Underlying ", record.underlyingId)
        assertEquals("cAlL", record.directionLabel)
        assertEquals(" usd ", record.underlyingCurrency)
        assertEquals("eUr", record.productCurrency)
    }

    @Test
    fun copyCreatesOnlyExplicitlyRequestedChanges() {
        val original = completeRecord()

        val changed = original.copy(knockoutBarrier = 83.75)

        assertEquals(83.75, changed.knockoutBarrier)
        assertEquals(original.copy(knockoutBarrier = 83.75), changed)
        assertEquals(original.productIsin, changed.productIsin)
        assertEquals(original.productWkn, changed.productWkn)
        assertEquals(original.issuerId, changed.issuerId)
        assertEquals(original.underlyingId, changed.underlyingId)
        assertEquals(original.directionLabel, changed.directionLabel)
        assertEquals(original.basePrice, changed.basePrice)
        assertEquals(original.ratio, changed.ratio)
        assertEquals(original.underlyingCurrency, changed.underlyingCurrency)
        assertEquals(original.productCurrency, changed.productCurrency)
        assertEquals(
            original.sourceTimestampEpochMillis,
            changed.sourceTimestampEpochMillis
        )
    }

    private fun completeRecord() = HsbcKnockoutProductSpecificationRecord(
        productIsin = "DE000SYNTH01",
        productWkn = "SYN001",
        issuerId = "synthetic-hsbc",
        underlyingId = "synthetic-underlying",
        directionLabel = "Call",
        basePrice = 80.125,
        knockoutBarrier = 82.5,
        ratio = 0.1,
        underlyingCurrency = "USD",
        productCurrency = "EUR",
        sourceTimestampEpochMillis = 1_700_000_000_250L
    )
}
