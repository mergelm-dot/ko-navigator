package de.konavigator.app.data.mapper

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.domain.model.KnockoutProductMarketData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductMarketDataMapperTest {

    @Test
    fun completeDtoMapsEveryFieldExactly() {
        assertEquals(
            KnockoutProductMarketData(
                productIsin = "DE000TEST001",
                bid = 1.23456789,
                ask = 1.34567891,
                bidTimestampEpochMillis = 1_700_000_000_001L,
                askTimestampEpochMillis = 1_700_000_000_002L,
                currency = "EUR",
                sourceId = "source-a"
            ),
            success(dto())
        )
    }

    @Test
    fun allOptionalQuoteAndTimestampFieldsMayBeNull() {
        val result = success(
            dto(
                bid = null,
                ask = null,
                bidTimestampEpochMillis = null,
                askTimestampEpochMillis = null
            )
        )

        assertNull(result.bid)
        assertNull(result.ask)
        assertNull(result.bidTimestampEpochMillis)
        assertNull(result.askTimestampEpochMillis)
    }

    @Test
    fun bidOnlyWithItsTimestampRemainsUnchanged() {
        val result = success(
            dto(
                bid = -1.25,
                ask = null,
                bidTimestampEpochMillis = 11L,
                askTimestampEpochMillis = null
            )
        )

        assertEquals(-1.25, result.bid!!, 0.0)
        assertNull(result.ask)
        assertEquals(11L, result.bidTimestampEpochMillis)
        assertNull(result.askTimestampEpochMillis)
    }

    @Test
    fun askOnlyWithItsTimestampRemainsUnchanged() {
        val result = success(
            dto(
                bid = null,
                ask = 2.5,
                bidTimestampEpochMillis = null,
                askTimestampEpochMillis = 22L
            )
        )

        assertNull(result.bid)
        assertEquals(2.5, result.ask!!, 0.0)
        assertNull(result.bidTimestampEpochMillis)
        assertEquals(22L, result.askTimestampEpochMillis)
    }

    @Test
    fun missingBidTimestampIsNotAddedAndOrphanedTimestampIsNotRemoved() {
        val result = success(
            dto(
                bid = 1.0,
                ask = null,
                bidTimestampEpochMillis = null,
                askTimestampEpochMillis = 33L
            )
        )

        assertNull(result.bidTimestampEpochMillis)
        assertNull(result.ask)
        assertEquals(33L, result.askTimestampEpochMillis)
    }

    @Test
    fun everyMissingRequiredFieldProducesItsTypedError() {
        val cases = listOf(
            KnockoutProductMarketDataDtoField.PRODUCT_ISIN to dto(productIsin = null),
            KnockoutProductMarketDataDtoField.CURRENCY to dto(currency = null),
            KnockoutProductMarketDataDtoField.SOURCE_ID to dto(sourceId = null)
        )

        cases.forEach { (field, input) ->
            assertEquals(listOf(missing(field)), failure(input).errors)
        }
    }

    @Test
    fun multipleMissingRequiredFieldsRemainCompleteAndOrdered() {
        assertEquals(
            listOf(
                missing(KnockoutProductMarketDataDtoField.PRODUCT_ISIN),
                missing(KnockoutProductMarketDataDtoField.CURRENCY),
                missing(KnockoutProductMarketDataDtoField.SOURCE_ID)
            ),
            failure(dto(productIsin = null, currency = null, sourceId = null)).errors
        )
    }

    @Test
    fun emptyStringsAreForwardedWithoutCorrection() {
        val result = success(dto(productIsin = "", currency = "", sourceId = ""))

        assertEquals("", result.productIsin)
        assertEquals("", result.currency)
        assertEquals("", result.sourceId)
    }

    @Test
    fun currencyAndSourceIdAreNotNormalized() {
        val result = success(dto(currency = " eur ", sourceId = " SOURCE-a "))

        assertEquals(" eur ", result.currency)
        assertEquals(" SOURCE-a ", result.sourceId)
    }

    @Test
    fun negativeNanAndInfinityQuotesRemainUnchanged() {
        val negative = success(dto(bid = -1.0, ask = Double.NEGATIVE_INFINITY))
        val nonFinite = success(dto(bid = Double.NaN, ask = Double.POSITIVE_INFINITY))

        assertEquals(-1.0, negative.bid!!, 0.0)
        assertEquals(Double.NEGATIVE_INFINITY, negative.ask!!, 0.0)
        assertTrue(nonFinite.bid!!.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, nonFinite.ask!!, 0.0)
    }

    @Test
    fun bidGreaterThanAskIsNotRejected() {
        val result = success(dto(bid = 2.0, ask = 1.0))

        assertEquals(2.0, result.bid!!, 0.0)
        assertEquals(1.0, result.ask!!, 0.0)
    }

    @Test
    fun equalInputsProduceEqualResultsAndMappingDoesNotMutateDto() {
        val input = dto(
            productIsin = " de000test001 ",
            bid = Double.NaN,
            askTimestampEpochMillis = 44L,
            currency = " eur ",
            sourceId = ""
        )
        val snapshot = input.copy()

        assertEquals(
            KnockoutProductMarketDataMapper.map(input),
            KnockoutProductMarketDataMapper.map(input)
        )
        assertEquals(snapshot, input)
    }

    private fun success(input: KnockoutProductMarketDataDto): KnockoutProductMarketData {
        val result = KnockoutProductMarketDataMapper.map(input)

        assertTrue(result is KnockoutProductMarketDataMappingResult.Success)
        return (result as KnockoutProductMarketDataMappingResult.Success).marketData
    }

    private fun failure(
        input: KnockoutProductMarketDataDto
    ): KnockoutProductMarketDataMappingResult.Failure {
        val result = KnockoutProductMarketDataMapper.map(input)

        assertTrue(result is KnockoutProductMarketDataMappingResult.Failure)
        return result as KnockoutProductMarketDataMappingResult.Failure
    }

    private fun missing(
        field: KnockoutProductMarketDataDtoField
    ) = KnockoutProductMarketDataMappingError(
        field = field,
        reason = KnockoutProductMarketDataMappingErrorReason.MISSING_REQUIRED_VALUE
    )

    private fun dto(
        productIsin: String? = "DE000TEST001",
        bid: Double? = 1.23456789,
        ask: Double? = 1.34567891,
        bidTimestampEpochMillis: Long? = 1_700_000_000_001L,
        askTimestampEpochMillis: Long? = 1_700_000_000_002L,
        currency: String? = "EUR",
        sourceId: String? = "source-a"
    ) = KnockoutProductMarketDataDto(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = bidTimestampEpochMillis,
        askTimestampEpochMillis = askTimestampEpochMillis,
        currency = currency,
        sourceId = sourceId
    )
}
