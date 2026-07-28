package de.konavigator.app.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KnockoutProductSpecificationSnapshotDtoTest {

    @Test
    fun allValuesAreStoredExactly() {
        val specification = completeSpecification()
        val snapshot = KnockoutProductSpecificationSnapshotDto(
            specification = specification,
            sourceId = "provider-source",
            retrievedAtEpochMillis = 1_700_000_000_123L,
            sourceTimestampEpochMillis = 1_699_999_999_987L
        )

        assertSame(specification, snapshot.specification)
        assertEquals("provider-source", snapshot.sourceId)
        assertEquals(1_700_000_000_123L, snapshot.retrievedAtEpochMillis)
        assertEquals(1_699_999_999_987L, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun completeSpecificationRemainsUnchanged() {
        val specification = completeSpecification()
        val snapshot = snapshot(specification = specification)

        assertSame(specification, snapshot.specification)
        assertEquals(completeSpecification(), snapshot.specification)
    }

    @Test
    fun sourceIdPreservesCaseAndWhitespace() {
        val sourceId = "  Provider-Source Id  "

        assertEquals(sourceId, snapshot(sourceId = sourceId).sourceId)
    }

    @Test
    fun retrievedAtEpochMillisRemainsExact() {
        val retrievedAt = 9_223_372_036_854_775_000L

        assertEquals(retrievedAt, snapshot(retrievedAtEpochMillis = retrievedAt).retrievedAtEpochMillis)
    }

    @Test
    fun sourceTimestampEpochMillisMayBeNull() {
        val snapshot = snapshot(sourceTimestampEpochMillis = null)

        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun retrievedAndSourceTimestampsRemainDistinguishable() {
        val snapshot = snapshot(
            retrievedAtEpochMillis = 123L,
            sourceTimestampEpochMillis = 456L
        )

        assertEquals(123L, snapshot.retrievedAtEpochMillis)
        assertEquals(456L, snapshot.sourceTimestampEpochMillis)
        assertNotEquals(snapshot.retrievedAtEpochMillis, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun negativeAndNearZeroTimestampsAreNotChangedOrValidated() {
        listOf(
            Long.MIN_VALUE to -1L,
            -1L to 0L,
            0L to 1L,
            1L to Long.MIN_VALUE
        ).forEach { (retrievedAt, sourceTimestamp) ->
            val snapshot = snapshot(
                retrievedAtEpochMillis = retrievedAt,
                sourceTimestampEpochMillis = sourceTimestamp
            )

            assertEquals(retrievedAt, snapshot.retrievedAtEpochMillis)
            assertEquals(sourceTimestamp, snapshot.sourceTimestampEpochMillis)
        }
    }

    private fun snapshot(
        specification: KnockoutProductSpecificationDto = completeSpecification(),
        sourceId: String = "test-source",
        retrievedAtEpochMillis: Long = 100L,
        sourceTimestampEpochMillis: Long? = 90L
    ) = KnockoutProductSpecificationSnapshotDto(
        specification = specification,
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )

    private fun completeSpecification() = KnockoutProductSpecificationDto(
        productIsin = "DE000TEST001",
        productWkn = "TEST01",
        issuerId = "test-issuer",
        underlyingId = "test-underlying",
        direction = "LONG",
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )
}
