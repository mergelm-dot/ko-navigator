package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.provider.ProviderResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeutscheBoerseSnapshotKnockoutProductMarketDataProviderTest {

    @Test
    fun uniqueXfraAndValidDxscRecordProduceCompleteDto() = runTest {
        val result = provider(
            dxscRecords = listOf(dxscRecord()),
            xfraRecords = listOf(xfraRecord())
        ).findByProductIsin(PRODUCT_ISIN)

        assertEquals(
            ProviderResult.Success(
                KnockoutProductMarketDataDto(
                    productIsin = PRODUCT_ISIN,
                    bid = 2.343,
                    ask = 2.344,
                    bidTimestampEpochMillis = 1_785_180_597_363L,
                    askTimestampEpochMillis = 1_785_180_597_363L,
                    currency = "EUR",
                    sourceId = DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID
                )
            ),
            result
        )
    }

    @Test
    fun newestOfMultipleMatchingDxscRecordsIsMapped() = runTest {
        val older = dxscRecord(
            bestBid = 1.0,
            timestamp = "2026-07-27T19:29:56.999999999Z"
        )
        val newer = dxscRecord(
            bestBid = 2.0,
            timestamp = "2026-07-27T19:29:57Z"
        )

        val dto = successDto(provider(listOf(older, newer), listOf(xfraRecord())))

        assertEquals(2.0, dto.bid)
    }

    @Test
    fun updatesWithinSameMillisecondAreSelectedByNanoseconds() = runTest {
        val older = dxscRecord(
            bestBid = 1.0,
            timestamp = "2026-07-27T19:29:57.363600000Z"
        )
        val newer = dxscRecord(
            bestBid = 2.0,
            timestamp = "2026-07-27T19:29:57.363600001Z"
        )

        val dto = successDto(provider(listOf(older, newer), listOf(xfraRecord())))

        assertEquals(2.0, dto.bid)
    }

    @Test
    fun nullBidAndZeroAskRemainUnchanged() = runTest {
        val dto = successDto(
            provider(
                dxscRecords = listOf(dxscRecord(bestBid = null, bestAsk = 0.0)),
                xfraRecords = listOf(xfraRecord())
            )
        )

        assertNull(dto.bid)
        assertEquals(0.0, dto.ask)
    }

    @Test
    fun currencyRemainsIndependentFromSettlementCurrency() = runTest {
        val dto = successDto(
            provider(
                dxscRecords = listOf(dxscRecord()),
                xfraRecords = listOf(
                    xfraRecord(currency = "MXN", settlementCurrency = "EUR")
                )
            )
        )

        assertEquals("MXN", dto.currency)
    }

    @Test
    fun missingCurrencyIsNotReplacedBySettlementCurrency() = runTest {
        val dto = successDto(
            provider(
                dxscRecords = listOf(dxscRecord()),
                xfraRecords = listOf(
                    xfraRecord(currency = null, settlementCurrency = "EUR")
                )
            )
        )

        assertNull(dto.currency)
    }

    @Test
    fun missingXfraRecordProducesNotFound() = runTest {
        val result = provider(emptyList(), emptyList()).findByProductIsin(PRODUCT_ISIN)

        assertEquals(ProviderResult.NotFound, result)
    }

    @Test
    fun existingDxscWithoutXfraReferenceProducesNotFound() = runTest {
        val result = provider(
            dxscRecords = listOf(dxscRecord()),
            xfraRecords = emptyList()
        ).findByProductIsin(PRODUCT_ISIN)

        assertEquals(ProviderResult.NotFound, result)
    }

    @Test
    fun uniqueXfraWithoutMatchingDxscProducesNotFound() = runTest {
        val result = provider(
            dxscRecords = emptyList(),
            xfraRecords = listOf(xfraRecord())
        ).findByProductIsin(PRODUCT_ISIN)

        assertEquals(ProviderResult.NotFound, result)
    }

    @Test
    fun twoMatchingXfraRecordsProduceDataAccessFailure() = runTest {
        val result = provider(
            dxscRecords = listOf(dxscRecord()),
            xfraRecords = listOf(xfraRecord(), xfraRecord(currency = "USD"))
        ).findByProductIsin(PRODUCT_ISIN)

        assertEquals(ProviderResult.DataAccessFailure, result)
    }

    @Test
    fun identicalDuplicateXfraRecordsProduceDataAccessFailure() = runTest {
        val xfraRecord = xfraRecord()
        val result = provider(
            dxscRecords = listOf(dxscRecord()),
            xfraRecords = listOf(xfraRecord, xfraRecord.copy())
        ).findByProductIsin(PRODUCT_ISIN)

        assertEquals(ProviderResult.DataAccessFailure, result)
    }

    @Test
    fun matchingDxscWithoutTimestampProducesDataAccessFailure() = runTest {
        val result = provider(
            dxscRecords = listOf(dxscRecord(timestamp = null)),
            xfraRecords = listOf(xfraRecord())
        ).findByProductIsin(PRODUCT_ISIN)

        assertEquals(ProviderResult.DataAccessFailure, result)
    }

    @Test
    fun matchingDxscWithInvalidTimestampProducesDataAccessFailure() = runTest {
        val result = provider(
            dxscRecords = listOf(dxscRecord(timestamp = "invalid")),
            xfraRecords = listOf(xfraRecord())
        ).findByProductIsin(PRODUCT_ISIN)

        assertEquals(ProviderResult.DataAccessFailure, result)
    }

    @Test
    fun invalidNonMatchingDxscRecordsDoNotAffectValidQuery() = runTest {
        val dto = successDto(
            provider(
                dxscRecords = listOf(
                    dxscRecord(isin = "DE000OTHER01", timestamp = null),
                    dxscRecord(isin = null, timestamp = "invalid"),
                    dxscRecord()
                ),
                xfraRecords = listOf(xfraRecord())
            )
        )

        assertEquals(PRODUCT_ISIN, dto.productIsin)
    }

    @Test
    fun isinComparisonDoesNotNormalizeCaseOrWhitespace() = runTest {
        val provider = provider(
            dxscRecords = listOf(dxscRecord()),
            xfraRecords = listOf(xfraRecord())
        )

        assertEquals(ProviderResult.NotFound, provider.findByProductIsin("de000snapshot01"))
        assertEquals(ProviderResult.NotFound, provider.findByProductIsin(" $PRODUCT_ISIN "))
    }

    @Test
    fun xfraRecordsWithNullIsinAreIgnored() = runTest {
        val dto = successDto(
            provider(
                dxscRecords = listOf(dxscRecord()),
                xfraRecords = listOf(xfraRecord(isin = null), xfraRecord())
            )
        )

        assertEquals(PRODUCT_ISIN, dto.productIsin)
    }

    @Test
    fun mutationsOfOriginalListsDoNotChangeProviderSnapshot() = runTest {
        val dxscRecords = mutableListOf(dxscRecord())
        val xfraRecords = mutableListOf(xfraRecord())
        val provider = provider(dxscRecords, xfraRecords)
        dxscRecords.clear()
        xfraRecords.clear()
        dxscRecords += dxscRecord(isin = "DE000OTHER01")
        xfraRecords += xfraRecord(isin = "DE000OTHER01")

        val dto = successDto(provider)

        assertEquals(PRODUCT_ISIN, dto.productIsin)
    }

    @Test
    fun successfulDtoUsesExistingDeutscheBoerseSourceId() = runTest {
        val dto = successDto(
            provider(
                dxscRecords = listOf(dxscRecord()),
                xfraRecords = listOf(xfraRecord())
            )
        )

        assertEquals(
            DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID,
            dto.sourceId
        )
    }

    private fun provider(
        dxscRecords: Iterable<DeutscheBoerseDxscPretradeRecord>,
        xfraRecords: Iterable<DeutscheBoerseXfraTradableInstrumentRecord>
    ) = DeutscheBoerseSnapshotKnockoutProductMarketDataProvider(
        dxscRecords = dxscRecords,
        xfraRecords = xfraRecords
    )

    private suspend fun successDto(
        provider: DeutscheBoerseSnapshotKnockoutProductMarketDataProvider
    ): KnockoutProductMarketDataDto =
        (provider.findByProductIsin(PRODUCT_ISIN) as
            ProviderResult.Success).value

    private fun dxscRecord(
        isin: String? = PRODUCT_ISIN,
        bestBid: Double? = 2.343,
        bestAsk: Double? = 2.344,
        timestamp: String? = "2026-07-27T19:29:57.363600000Z"
    ) = DeutscheBoerseDxscPretradeRecord(
        messageId = "pretrade",
        instrumentIdentificationCode = isin,
        bestBid = bestBid,
        bestBidQuantity = 80_000.0,
        bestAsk = bestAsk,
        bestAskQuantity = 80_000.0,
        updateDateAndTime = timestamp
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
        wkn = "SYN011",
        micCode = "XFRA",
        instrumentType = "Warrant",
        settlementCurrency = settlementCurrency,
        currency = currency,
        warrantType = "Call",
        quotingPeriodStart = "08:00",
        quotingPeriodEnd = "22:00"
    )

    private companion object {
        const val PRODUCT_ISIN = "DE000SNAPSHOT01"
    }
}
