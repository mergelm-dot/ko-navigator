package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HsbcKnockoutProductSpecificationRecordMapperTest {

    @Test
    fun completeCallRecordMapsEveryValueExactly() {
        val snapshot = mapSuccess(completeRecord(), RETRIEVED_AT)

        assertEquals("DE000SYNTH01", snapshot.specification.productIsin)
        assertEquals("SYN001", snapshot.specification.productWkn)
        assertEquals("synthetic-provider", snapshot.specification.issuerId)
        assertEquals("synthetic-underlying", snapshot.specification.underlyingId)
        assertEquals("LONG", snapshot.specification.direction)
        assertEquals(80.125, snapshot.specification.basePrice)
        assertEquals(82.5, snapshot.specification.knockoutBarrier)
        assertEquals(0.1, snapshot.specification.ratio)
        assertEquals("USD", snapshot.specification.underlyingCurrency)
        assertEquals("EUR", snapshot.specification.productCurrency)
        assertEquals(HsbcKnockoutProductSpecificationRecordMapper.SOURCE_ID, snapshot.sourceId)
        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertEquals(SOURCE_TIMESTAMP, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun putMapsExactlyToShort() {
        val snapshot = mapSuccess(completeRecord().copy(directionLabel = "Put"))

        assertEquals("SHORT", snapshot.specification.direction)
    }

    @Test
    fun nullDirectionRemainsNull() {
        val snapshot = mapSuccess(completeRecord().copy(directionLabel = null))

        assertNull(snapshot.specification.direction)
    }

    @Test
    fun unsupportedDirectionReturnsTypedFailure() {
        assertEquals(
            HsbcKnockoutProductSpecificationRecordMappingResult.Failure(
                listOf(
                    HsbcKnockoutProductSpecificationRecordMappingErrorCode
                        .UNSUPPORTED_DIRECTION_LABEL
                )
            ),
            HsbcKnockoutProductSpecificationRecordMapper.map(
                completeRecord().copy(directionLabel = "SyntheticDirection"),
                RETRIEVED_AT
            )
        )
    }

    @Test
    fun directionWhitespaceAndCaseAreNotNormalized() {
        listOf("CALL", "call", " Call ", "PUT", "put").forEach { directionLabel ->
            assertFailure(directionLabel)
        }
    }

    @Test
    fun nullableRecordFieldsRemainNull() {
        val snapshot = mapSuccess(nullRecord())

        assertNull(snapshot.specification.productIsin)
        assertNull(snapshot.specification.productWkn)
        assertNull(snapshot.specification.issuerId)
        assertNull(snapshot.specification.underlyingId)
        assertNull(snapshot.specification.direction)
        assertNull(snapshot.specification.basePrice)
        assertNull(snapshot.specification.knockoutBarrier)
        assertNull(snapshot.specification.ratio)
        assertNull(snapshot.specification.underlyingCurrency)
        assertNull(snapshot.specification.productCurrency)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun zeroAndNegativeNumbersRemainUnchanged() {
        val snapshot = mapSuccess(
            completeRecord().copy(
                basePrice = 0.0,
                knockoutBarrier = -1.0,
                ratio = 0.0
            )
        )

        assertEquals(0.0, snapshot.specification.basePrice)
        assertEquals(-1.0, snapshot.specification.knockoutBarrier)
        assertEquals(0.0, snapshot.specification.ratio)
    }

    @Test
    fun textWhitespaceAndCaseRemainUnchanged() {
        val snapshot = mapSuccess(
            completeRecord().copy(
                productIsin = " de000Synthetic01 ",
                issuerId = " Synthetic-Provider ",
                underlyingId = " Synthetic-Underlying ",
                directionLabel = "Call",
                underlyingCurrency = " usd ",
                productCurrency = "eUr"
            )
        )

        assertEquals(" de000Synthetic01 ", snapshot.specification.productIsin)
        assertEquals(" Synthetic-Provider ", snapshot.specification.issuerId)
        assertEquals(" Synthetic-Underlying ", snapshot.specification.underlyingId)
        assertEquals(" usd ", snapshot.specification.underlyingCurrency)
        assertEquals("eUr", snapshot.specification.productCurrency)
    }

    @Test
    fun retrievedAtEpochMillisIsCopiedExactly() {
        val snapshot = mapSuccess(completeRecord(), retrievedAtEpochMillis = -1L)

        assertEquals(-1L, snapshot.retrievedAtEpochMillis)
    }

    @Test
    fun nullSourceTimestampIsNotReplacedByRetrievedAt() {
        val snapshot = mapSuccess(
            completeRecord().copy(sourceTimestampEpochMillis = null),
            RETRIEVED_AT
        )

        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun sourceIdIsFixedAndExact() {
        assertEquals(
            "HSBC_RESEARCH_LOCAL",
            HsbcKnockoutProductSpecificationRecordMapper.SOURCE_ID
        )
        assertEquals(
            "HSBC_RESEARCH_LOCAL",
            mapSuccess(completeRecord()).sourceId
        )
    }

    @Test
    fun repeatedMappingIsDeterministic() {
        val record = completeRecord()

        val first = HsbcKnockoutProductSpecificationRecordMapper.map(record, RETRIEVED_AT)
        val second = HsbcKnockoutProductSpecificationRecordMapper.map(record, RETRIEVED_AT)

        assertEquals(first, second)
    }

    private fun mapSuccess(
        record: HsbcKnockoutProductSpecificationRecord,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ): KnockoutProductSpecificationSnapshotDto =
        (HsbcKnockoutProductSpecificationRecordMapper.map(record, retrievedAtEpochMillis) as
            HsbcKnockoutProductSpecificationRecordMappingResult.Success).snapshotDto

    private fun assertFailure(directionLabel: String) {
        val result = HsbcKnockoutProductSpecificationRecordMapper.map(
            completeRecord().copy(directionLabel = directionLabel),
            RETRIEVED_AT
        )

        assertEquals(
            HsbcKnockoutProductSpecificationRecordMappingResult.Failure(
                listOf(
                    HsbcKnockoutProductSpecificationRecordMappingErrorCode
                        .UNSUPPORTED_DIRECTION_LABEL
                )
            ),
            result
        )
    }

    private fun completeRecord() = HsbcKnockoutProductSpecificationRecord(
        productIsin = "DE000SYNTH01",
        productWkn = "SYN001",
        issuerId = "synthetic-provider",
        underlyingId = "synthetic-underlying",
        directionLabel = "Call",
        basePrice = 80.125,
        knockoutBarrier = 82.5,
        ratio = 0.1,
        underlyingCurrency = "USD",
        productCurrency = "EUR",
        sourceTimestampEpochMillis = SOURCE_TIMESTAMP
    )

    private fun nullRecord() = HsbcKnockoutProductSpecificationRecord(
        productIsin = null,
        productWkn = null,
        issuerId = null,
        underlyingId = null,
        directionLabel = null,
        basePrice = null,
        knockoutBarrier = null,
        ratio = null,
        underlyingCurrency = null,
        productCurrency = null,
        sourceTimestampEpochMillis = null
    )

    private companion object {
        const val RETRIEVED_AT = 1_700_000_000_500L
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L
    }
}
