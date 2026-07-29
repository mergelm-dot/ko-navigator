package de.konavigator.app.data.mapper

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationSnapshotMapperTest {

    @Test
    fun completeLongSnapshotMapsSuccessfully() {
        val result = success(snapshotDto(direction = "LONG"))

        assertEquals(TradeDirection.LONG, result.specification.direction)
    }

    @Test
    fun completeShortSnapshotMapsSuccessfully() {
        val result = success(snapshotDto(direction = "SHORT"))

        assertEquals(TradeDirection.SHORT, result.specification.direction)
    }

    @Test
    fun embeddedSpecificationFieldsAreMappedByExistingContract() {
        val result = success(
            snapshotDto(
                productIsin = "DE000TEST002",
                productWkn = "DEF456",
                issuerId = "issuer-b",
                underlyingId = "underlying-b",
                direction = "SHORT",
                basePrice = 120.25,
                knockoutBarrier = 125.75,
                ratio = 0.01,
                underlyingCurrency = "CHF",
                productCurrency = "USD"
            )
        ).specification

        assertEquals("DE000TEST002", result.productIsin)
        assertEquals("DEF456", result.productWkn)
        assertEquals("issuer-b", result.issuerId)
        assertEquals("underlying-b", result.underlyingId)
        assertEquals(TradeDirection.SHORT, result.direction)
        assertEquals(120.25, result.basePrice, 0.0)
        assertEquals(125.75, result.knockoutBarrier, 0.0)
        assertEquals(0.01, result.ratio, 0.0)
        assertEquals("CHF", result.underlyingCurrency)
        assertEquals("USD", result.productCurrency)
    }

    @Test
    fun sourceIdPreservesWhitespaceAndCase() {
        val sourceId = "  Provider-Source Id  "

        assertEquals(sourceId, success(snapshotDto(sourceId = sourceId)).sourceId)
    }

    @Test
    fun retrievedAtEpochMillisRemainsExact() {
        val retrievedAt = 9_223_372_036_854_775_000L

        assertEquals(
            retrievedAt,
            success(snapshotDto(retrievedAtEpochMillis = retrievedAt)).retrievedAtEpochMillis
        )
    }

    @Test
    fun sourceTimestampEpochMillisMayBeNullAndRemainsNull() {
        assertNull(success(snapshotDto(sourceTimestampEpochMillis = null)).sourceTimestampEpochMillis)
    }

    @Test
    fun retrievedAndSourceTimestampsRemainSeparate() {
        val result = success(
            snapshotDto(
                retrievedAtEpochMillis = 123L,
                sourceTimestampEpochMillis = 456L
            )
        )

        assertEquals(123L, result.retrievedAtEpochMillis)
        assertEquals(456L, result.sourceTimestampEpochMillis)
        assertNotEquals(result.retrievedAtEpochMillis, result.sourceTimestampEpochMillis)
    }

    @Test
    fun negativeNearZeroAndLargeTimestampsAreNotChanged() {
        listOf(
            Long.MIN_VALUE to -1L,
            -1L to 0L,
            0L to 1L,
            1L to Long.MAX_VALUE
        ).forEach { (retrievedAt, sourceTimestamp) ->
            val result = success(
                snapshotDto(
                    retrievedAtEpochMillis = retrievedAt,
                    sourceTimestampEpochMillis = sourceTimestamp
                )
            )

            assertEquals(retrievedAt, result.retrievedAtEpochMillis)
            assertEquals(sourceTimestamp, result.sourceTimestampEpochMillis)
        }
    }

    @Test
    fun emptySourceIdIsForwardedUnchanged() {
        assertEquals("", success(snapshotDto(sourceId = "")).sourceId)
    }

    @Test
    fun missingRequiredSpecificationFieldReturnsExistingTypedError() {
        val result = failure(snapshotDto(productIsin = null))

        assertEquals(
            listOf(missing(KnockoutProductSpecificationDtoField.PRODUCT_ISIN)),
            result.errors
        )
    }

    @Test
    fun multipleSpecificationErrorsRemainCompleteAndOrdered() {
        val result = failure(
            snapshotDto(
                productIsin = null,
                direction = null,
                ratio = null,
                productCurrency = null
            )
        )

        assertEquals(
            listOf(
                missing(KnockoutProductSpecificationDtoField.PRODUCT_ISIN),
                missing(KnockoutProductSpecificationDtoField.DIRECTION),
                missing(KnockoutProductSpecificationDtoField.RATIO),
                missing(KnockoutProductSpecificationDtoField.PRODUCT_CURRENCY)
            ),
            result.errors
        )
    }

    @Test
    fun unsupportedDirectionPreservesExistingTypedError() {
        val result = failure(snapshotDto(direction = "SIDEWAYS"))

        assertEquals(
            listOf(
                KnockoutProductSpecificationMappingError(
                    field = KnockoutProductSpecificationDtoField.DIRECTION,
                    reason = KnockoutProductSpecificationMappingErrorReason.UNSUPPORTED_VALUE
                )
            ),
            result.errors
        )
    }

    @Test
    fun invalidSpecificationProducesNoDomainSnapshot() {
        val result = KnockoutProductSpecificationSnapshotMapper.map(
            snapshotDto(issuerId = null)
        )

        assertTrue(result is KnockoutProductSpecificationSnapshotMappingResult.Failure)
        assertTrue(result !is KnockoutProductSpecificationSnapshotMappingResult.Success)
    }

    @Test
    fun mappingDoesNotMutateSnapshotDtoOrEmbeddedSpecification() {
        val input = snapshotDto(
            productIsin = " de000test001 ",
            productWkn = null,
            issuerId = "",
            sourceId = " source ",
            retrievedAtEpochMillis = -1L,
            sourceTimestampEpochMillis = null
        )
        val embeddedBefore = input.specification.copy()
        val snapshotBefore = input.copy()

        KnockoutProductSpecificationSnapshotMapper.map(input)

        assertEquals(snapshotBefore, input)
        assertEquals(embeddedBefore, input.specification)
    }

    @Test
    fun equalInputsProduceDeterministicallyEqualResults() {
        val input = snapshotDto()

        assertEquals(
            KnockoutProductSpecificationSnapshotMapper.map(input),
            KnockoutProductSpecificationSnapshotMapper.map(input)
        )
    }

    private fun success(
        input: KnockoutProductSpecificationSnapshotDto
    ) = (KnockoutProductSpecificationSnapshotMapper.map(input) as
        KnockoutProductSpecificationSnapshotMappingResult.Success).snapshot

    private fun failure(
        input: KnockoutProductSpecificationSnapshotDto
    ) = KnockoutProductSpecificationSnapshotMapper.map(input) as
        KnockoutProductSpecificationSnapshotMappingResult.Failure

    private fun missing(
        field: KnockoutProductSpecificationDtoField
    ) = KnockoutProductSpecificationMappingError(
        field = field,
        reason = KnockoutProductSpecificationMappingErrorReason.MISSING_REQUIRED_VALUE
    )

    private fun snapshotDto(
        productIsin: String? = "DE000TEST001",
        productWkn: String? = "ABC123",
        issuerId: String? = "issuer-a",
        underlyingId: String? = "underlying-a",
        direction: String? = "LONG",
        basePrice: Double? = 80.123456789,
        knockoutBarrier: Double? = 82.987654321,
        ratio: Double? = 0.123456789,
        underlyingCurrency: String? = "USD",
        productCurrency: String? = "EUR",
        sourceId: String = "test-source",
        retrievedAtEpochMillis: Long = 100L,
        sourceTimestampEpochMillis: Long? = 90L
    ) = KnockoutProductSpecificationSnapshotDto(
        specification = KnockoutProductSpecificationDto(
            productIsin = productIsin,
            productWkn = productWkn,
            issuerId = issuerId,
            underlyingId = underlyingId,
            direction = direction,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = ratio,
            underlyingCurrency = underlyingCurrency,
            productCurrency = productCurrency
        ),
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )
}
