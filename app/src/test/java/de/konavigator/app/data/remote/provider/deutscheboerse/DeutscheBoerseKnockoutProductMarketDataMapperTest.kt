package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeutscheBoerseKnockoutProductMarketDataMapperTest {

    @Test
    fun completeCompatibleRecordsMapAllTargetFieldsExactly() {
        val result = map(
            dxscRecord(
                bestBid = 2.343,
                bestAsk = 2.344,
                updateDateAndTime = "2026-07-27T19:29:57.363600000Z"
            ),
            xfraRecord(currency = "EUR")
        )

        assertEquals(
            KnockoutProductMarketDataDto(
                productIsin = PRODUCT_ISIN,
                bid = 2.343,
                ask = 2.344,
                bidTimestampEpochMillis = 1_785_180_597_363L,
                askTimestampEpochMillis = 1_785_180_597_363L,
                currency = "EUR",
                sourceId = DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID
            ),
            result.dto
        )
    }

    @Test
    fun nullBidAndZeroAskRemainDistinct() {
        val result = map(
            dxscRecord(bestBid = null, bestAsk = 0.0),
            xfraRecord()
        )

        assertNull(result.dto.bid)
        assertEquals(0.0, result.dto.ask)
    }

    @Test
    fun productCurrencyDoesNotUseSettlementCurrency() {
        val result = map(
            dxscRecord(),
            xfraRecord(currency = "MXN", settlementCurrency = "EUR")
        )

        assertEquals("MXN", result.dto.currency)
    }

    @Test
    fun missingProductCurrencyIsNotReplacedBySettlementCurrency() {
        val result = map(
            dxscRecord(),
            xfraRecord(currency = null, settlementCurrency = "EUR")
        )

        assertNull(result.dto.currency)
    }

    @Test
    fun missingDxscIsinProducesTypedFailure() {
        val result = mapFailure(
            dxscRecord(instrumentIdentificationCode = null),
            xfraRecord()
        )

        assertEquals(
            listOf(DeutscheBoerseMarketDataMappingErrorCode.MISSING_DXSC_ISIN),
            result.errors
        )
    }

    @Test
    fun missingXfraIsinProducesTypedFailure() {
        val result = mapFailure(
            dxscRecord(),
            xfraRecord(isin = null)
        )

        assertEquals(
            listOf(DeutscheBoerseMarketDataMappingErrorCode.MISSING_XFRA_ISIN),
            result.errors
        )
    }

    @Test
    fun exactIsinMismatchProducesTypedFailure() {
        val result = mapFailure(
            dxscRecord(instrumentIdentificationCode = "de000SYNTH03"),
            xfraRecord(isin = "DE000SYNTH03")
        )

        assertEquals(
            listOf(DeutscheBoerseMarketDataMappingErrorCode.ISIN_MISMATCH),
            result.errors
        )
    }

    @Test
    fun bothMissingIsinsProduceBothErrorsInStableOrder() {
        val result = mapFailure(
            dxscRecord(instrumentIdentificationCode = null),
            xfraRecord(isin = null)
        )

        assertEquals(
            listOf(
                DeutscheBoerseMarketDataMappingErrorCode.MISSING_DXSC_ISIN,
                DeutscheBoerseMarketDataMappingErrorCode.MISSING_XFRA_ISIN
            ),
            result.errors
        )
    }

    @Test
    fun missingTimestampMapsBothTimestampsToNull() {
        val result = map(
            dxscRecord(updateDateAndTime = null),
            xfraRecord()
        )

        assertNull(result.dto.bidTimestampEpochMillis)
        assertNull(result.dto.askTimestampEpochMillis)
    }

    @Test
    fun syntacticallyInvalidTimestampProducesTypedFailure() {
        val result = mapFailure(
            dxscRecord(updateDateAndTime = "2026-07-27 19:29:57Z"),
            xfraRecord()
        )

        assertEquals(
            listOf(
                DeutscheBoerseMarketDataMappingErrorCode.INVALID_UPDATE_DATE_AND_TIME
            ),
            result.errors
        )
    }

    @Test
    fun calendarInvalidTimestampProducesTypedFailure() {
        val result = mapFailure(
            dxscRecord(updateDateAndTime = "2026-02-30T19:29:57.000000000Z"),
            xfraRecord()
        )

        assertEquals(
            listOf(
                DeutscheBoerseMarketDataMappingErrorCode.INVALID_UPDATE_DATE_AND_TIME
            ),
            result.errors
        )
    }

    @Test
    fun timestampWithOneFractionDigitMapsWithoutRounding() {
        val result = map(
            dxscRecord(updateDateAndTime = "2026-07-27T19:29:57.1Z"),
            xfraRecord()
        )

        assertEquals(1_785_180_597_100L, result.dto.bidTimestampEpochMillis)
        assertEquals(1_785_180_597_100L, result.dto.askTimestampEpochMillis)
    }

    private fun map(
        dxscRecord: DeutscheBoerseDxscPretradeRecord,
        xfraRecord: DeutscheBoerseXfraTradableInstrumentRecord
    ): DeutscheBoerseMarketDataMappingResult.Success =
        DeutscheBoerseKnockoutProductMarketDataMapper.map(
            dxscRecord = dxscRecord,
            xfraRecord = xfraRecord
        ) as DeutscheBoerseMarketDataMappingResult.Success

    private fun mapFailure(
        dxscRecord: DeutscheBoerseDxscPretradeRecord,
        xfraRecord: DeutscheBoerseXfraTradableInstrumentRecord
    ): DeutscheBoerseMarketDataMappingResult.Failure =
        DeutscheBoerseKnockoutProductMarketDataMapper.map(
            dxscRecord = dxscRecord,
            xfraRecord = xfraRecord
        ) as DeutscheBoerseMarketDataMappingResult.Failure

    private fun dxscRecord(
        instrumentIdentificationCode: String? = PRODUCT_ISIN,
        bestBid: Double? = 2.343,
        bestAsk: Double? = 2.344,
        updateDateAndTime: String? = "2026-07-27T19:29:57.363600000Z"
    ) = DeutscheBoerseDxscPretradeRecord(
        messageId = "pretrade",
        instrumentIdentificationCode = instrumentIdentificationCode,
        bestBid = bestBid,
        bestBidQuantity = 80_000.0,
        bestAsk = bestAsk,
        bestAskQuantity = 80_000.0,
        updateDateAndTime = updateDateAndTime
    )

    private fun xfraRecord(
        isin: String? = PRODUCT_ISIN,
        settlementCurrency: String? = "EUR",
        currency: String? = "EUR"
    ) = DeutscheBoerseXfraTradableInstrumentRecord(
        productStatus = "Active",
        instrumentStatus = "Tradable",
        instrumentName = "Synthetic Warrant",
        isin = isin,
        wkn = "SYN003",
        micCode = "XFRA",
        instrumentType = "Warrant",
        settlementCurrency = settlementCurrency,
        currency = currency,
        warrantType = "Call",
        quotingPeriodStart = "2026-01-02",
        quotingPeriodEnd = "2026-12-30"
    )

    private companion object {
        const val PRODUCT_ISIN = "DE000SYNTH03"
    }
}
