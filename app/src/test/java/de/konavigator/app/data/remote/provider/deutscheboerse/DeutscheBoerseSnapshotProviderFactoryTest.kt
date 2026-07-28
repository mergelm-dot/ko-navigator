package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.provider.ProviderResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseSnapshotProviderFactoryTest {

    @Test
    fun emptyRequestedIsinsProduceSuccessfulProvider() = runTest {
        val provider = createSuccess(emptySequence(), emptySequence(), emptySet())

        assertEquals(ProviderResult.NotFound, provider.findByProductIsin(PRODUCT_ISIN))
    }

    @Test
    fun emptyRequestedIsinsDoNotConsumeXfraSequence() {
        var iteratorCalls = 0
        val xfraLines = countingSequence(emptySequence()) { iteratorCalls++ }

        createSuccess(emptySequence(), xfraLines, emptySet())

        assertEquals(0, iteratorCalls)
    }

    @Test
    fun emptyRequestedIsinsDoNotConsumeDxscSequence() {
        var iteratorCalls = 0
        val dxscLines = countingSequence(emptySequence()) { iteratorCalls++ }

        createSuccess(dxscLines, emptySequence(), emptySet())

        assertEquals(0, iteratorCalls)
    }

    @Test
    fun validSyntheticSourcesProduceProvider() {
        val result = create(
            dxscSource(dxscLine(PRODUCT_ISIN)),
            xfraSource(xfraLine(PRODUCT_ISIN))
        )

        assertTrue(result is DeutscheBoerseSnapshotProviderCreationResult.Success)
    }

    @Test
    fun createdProviderReturnsCorrectlyMappedDto() = runTest {
        val dto = successDto(
            createSuccess(
                dxscSource(dxscLine(PRODUCT_ISIN, bestBid = 2.343, bestAsk = 2.344)),
                xfraSource(xfraLine(PRODUCT_ISIN, currency = "EUR"))
            )
        )

        assertEquals(PRODUCT_ISIN, dto.productIsin)
        assertEquals(2.343, dto.bid)
        assertEquals(2.344, dto.ask)
        assertEquals("EUR", dto.currency)
        assertEquals(DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID, dto.sourceId)
    }

    @Test
    fun createdProviderSelectsNewestDxscRecord() = runTest {
        val provider = createSuccess(
            dxscSource(
                dxscLine(PRODUCT_ISIN, bestBid = 1.0, timestamp = OLDER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bestBid = 2.0, timestamp = NEWER_TIMESTAMP)
            ),
            xfraSource(xfraLine(PRODUCT_ISIN))
        )

        assertEquals(2.0, successDto(provider).bid)
    }

    @Test
    fun currencyAndSettlementCurrencyRemainSeparate() = runTest {
        val dto = successDto(
            createSuccess(
                dxscSource(dxscLine(PRODUCT_ISIN)),
                xfraSource(
                    xfraLine(
                        PRODUCT_ISIN,
                        settlementCurrency = "EUR",
                        currency = "MXN"
                    )
                )
            )
        )

        assertEquals("MXN", dto.currency)
    }

    @Test
    fun nullBidAndZeroAskRemainUnchanged() = runTest {
        val dto = successDto(
            createSuccess(
                dxscSource(dxscLine(PRODUCT_ISIN, bestBid = null, bestAsk = 0.0)),
                xfraSource(xfraLine(PRODUCT_ISIN))
            )
        )

        assertNull(dto.bid)
        assertEquals(0.0, dto.ask)
    }

    @Test
    fun missingIsinReturnsNotFoundThroughProvider() = runTest {
        val provider = createSuccess(
            dxscSource(dxscLine(PRODUCT_ISIN)),
            xfraSource(xfraLine(PRODUCT_ISIN))
        )

        assertEquals(ProviderResult.NotFound, provider.findByProductIsin(OTHER_ISIN))
    }

    @Test
    fun duplicateXfraRecordsFailOnlyWhenProviderIsQueried() = runTest {
        val provider = createSuccess(
            dxscSource(dxscLine(PRODUCT_ISIN)),
            xfraSource(xfraLine(PRODUCT_ISIN), xfraLine(PRODUCT_ISIN))
        )

        assertEquals(
            ProviderResult.DataAccessFailure,
            provider.findByProductIsin(PRODUCT_ISIN)
        )
    }

    @Test
    fun factoryDoesNotRemoveDuplicateXfraRecords() = runTest {
        val provider = createSuccess(
            dxscSource(dxscLine(PRODUCT_ISIN)),
            xfraSource(
                xfraLine(PRODUCT_ISIN, currency = "EUR"),
                xfraLine(PRODUCT_ISIN, currency = "USD")
            )
        )

        assertEquals(
            ProviderResult.DataAccessFailure,
            provider.findByProductIsin(PRODUCT_ISIN)
        )
    }

    @Test
    fun xfraLoadingFailureProducesTypedCreationFailure() {
        val result = createFailure(dxscSource(), invalidXfraSource())

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun xfraLoadingFailureIsPreservedCompletely() {
        val result = createFailure(dxscSource(), invalidXfraSource())

        assertEquals(
            DeutscheBoerseXfraCsvLoadingError(
                code = DeutscheBoerseXfraCsvLoadingErrorCode.HEADER_PREPARATION_FAILED,
                lineNumber = 3L,
                parsingErrors = listOf(
                    DeutscheBoerseXfraCsvRowParsingError(
                        DeutscheBoerseXfraCsvRowParsingErrorCode.INVALID_HEADER
                    )
                )
            ),
            result.error.xfraLoadingError
        )
    }

    @Test
    fun xfraLoadingFailureContainsNoDxscError() {
        val result = createFailure(dxscSource(), invalidXfraSource())

        assertNull(result.error.dxscLoadingError)
    }

    @Test
    fun xfraLoadingFailureDoesNotConsumeDxscSequence() {
        var iteratorCalls = 0
        val dxscLines = countingSequence(dxscSource()) { iteratorCalls++ }

        createFailure(dxscLines, invalidXfraSource())

        assertEquals(0, iteratorCalls)
    }

    @Test
    fun dxscLoadingFailureProducesTypedCreationFailure() {
        val result = createFailure(sequenceOf(MALFORMED_JSON), xfraSource())

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun dxscLoadingFailureIsPreservedCompletely() {
        val result = createFailure(sequenceOf(MALFORMED_JSON), xfraSource())

        assertEquals(
            DeutscheBoerseDxscNdjsonLoadingError(
                code = DeutscheBoerseDxscNdjsonLoadingErrorCode.LINE_PARSING_FAILED,
                lineNumber = 1L,
                parsingErrors = listOf(
                    DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON
                )
            ),
            result.error.dxscLoadingError
        )
    }

    @Test
    fun dxscLoadingFailureContainsNoXfraError() {
        val result = createFailure(sequenceOf(MALFORMED_JSON), xfraSource())

        assertNull(result.error.xfraLoadingError)
    }

    @Test
    fun dxscLoadingFailureReturnsNoProvider() {
        val result = create(sequenceOf(MALFORMED_JSON), xfraSource())

        assertTrue(result is DeutscheBoerseSnapshotProviderCreationResult.Failure)
    }

    @Test
    fun successfulCreationConsumesEachSequenceExactlyOnce() {
        var dxscIteratorCalls = 0
        var xfraIteratorCalls = 0
        val dxscLines = countingSequence(dxscSource(dxscLine(PRODUCT_ISIN))) {
            dxscIteratorCalls++
        }
        val xfraLines = countingSequence(xfraSource(xfraLine(PRODUCT_ISIN))) {
            xfraIteratorCalls++
        }

        createSuccess(dxscLines, xfraLines)

        assertEquals(1, xfraIteratorCalls)
        assertEquals(1, dxscIteratorCalls)
    }

    @Test
    fun requestedIsinIsUsedExactlyForBothLoaders() = runTest {
        val exactIsin = " $PRODUCT_ISIN "
        val provider = createSuccess(
            dxscSource(dxscLine(exactIsin), dxscLine(PRODUCT_ISIN.lowercase())),
            xfraSource(xfraLine(exactIsin), xfraLine(PRODUCT_ISIN.lowercase())),
            setOf(exactIsin)
        )

        val dto = successDto(provider, exactIsin)

        assertEquals(exactIsin, dto.productIsin)
    }

    @Test
    fun emptyXfraResultDoesNotPreventDxscLoading() {
        var consumedDxscLines = 0
        val dxscLines = dxscSource(dxscLine(PRODUCT_ISIN)).onEach {
            consumedDxscLines++
        }

        createSuccess(
            dxscLines = dxscLines,
            xfraLines = xfraSource(xfraLine(OTHER_ISIN))
        )

        assertEquals(1, consumedDxscLines)
    }

    @Test
    fun factoryDoesNotSelectLatestDxscRecord() = runTest {
        val provider = createSuccess(
            dxscSource(
                dxscLine(PRODUCT_ISIN, timestamp = null),
                dxscLine(PRODUCT_ISIN, timestamp = null)
            ),
            xfraSource(xfraLine(PRODUCT_ISIN))
        )

        assertEquals(
            ProviderResult.DataAccessFailure,
            provider.findByProductIsin(PRODUCT_ISIN)
        )
    }

    @Test
    fun factoryDoesNotMapDto() = runTest {
        val provider = createSuccess(
            dxscSource(dxscLine(PRODUCT_ISIN)),
            xfraSource(xfraLine(PRODUCT_ISIN, currency = ""))
        )

        val dto = successDto(provider)

        assertNull(dto.currency)
        assertEquals(
            DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID,
            dto.sourceId
        )
    }

    private fun create(
        dxscLines: Sequence<String>,
        xfraLines: Sequence<String>,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseSnapshotProviderCreationResult =
        DeutscheBoerseSnapshotProviderFactory.create(
            dxscLines = dxscLines,
            xfraLines = xfraLines,
            requestedProductIsins = requestedProductIsins
        )

    private fun createSuccess(
        dxscLines: Sequence<String>,
        xfraLines: Sequence<String>,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseSnapshotKnockoutProductMarketDataProvider =
        (create(dxscLines, xfraLines, requestedProductIsins) as
            DeutscheBoerseSnapshotProviderCreationResult.Success).provider

    private fun createFailure(
        dxscLines: Sequence<String>,
        xfraLines: Sequence<String>
    ): DeutscheBoerseSnapshotProviderCreationResult.Failure =
        create(dxscLines, xfraLines) as
            DeutscheBoerseSnapshotProviderCreationResult.Failure

    private suspend fun successDto(
        provider: DeutscheBoerseSnapshotKnockoutProductMarketDataProvider,
        productIsin: String = PRODUCT_ISIN
    ): KnockoutProductMarketDataDto =
        (provider.findByProductIsin(productIsin) as ProviderResult.Success).value

    private fun countingSequence(
        delegate: Sequence<String>,
        onIterator: () -> Unit
    ): Sequence<String> = Sequence {
        onIterator()
        delegate.iterator()
    }

    private fun invalidXfraSource(): Sequence<String> =
        sequenceOf("metadata one", "metadata two", "")

    private fun xfraSource(vararg dataLines: String): Sequence<String> =
        sequenceOf("metadata one", "metadata two", xfraHeader(), *dataLines)

    private fun xfraHeader(): String = DeutscheBoerseXfraRequiredColumn.entries
        .joinToString(";") { it.headerName }

    private fun xfraLine(
        isin: String,
        settlementCurrency: String = "EUR",
        currency: String = "EUR"
    ): String = listOf(
        "Active",
        "Tradable",
        "Synthetic Instrument",
        isin,
        "SYN001",
        "XFRA",
        "Warrant",
        settlementCurrency,
        currency,
        "Call",
        "08:00",
        "22:00"
    ).joinToString(";")

    private fun dxscSource(vararg lines: String): Sequence<String> =
        sequenceOf(*lines)

    private fun dxscLine(
        isin: String,
        bestBid: Double? = 2.343,
        bestAsk: Double? = 2.344,
        timestamp: String? = NEWER_TIMESTAMP
    ): String {
        val jsonBid = bestBid?.toString() ?: "null"
        val jsonAsk = bestAsk?.toString() ?: "null"
        val jsonTimestamp = timestamp?.let { "\"$it\"" } ?: "null"
        return """{"messageId":"pretrade","instrumentIdentificationCode":"$isin","bestBid":$jsonBid,"bestAsk":$jsonAsk,"updateDateAndTime":$jsonTimestamp}"""
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000FACTORY01"
        const val OTHER_ISIN = "DE000FACTORY02"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val MALFORMED_JSON = "{\"messageId\":"
    }
}
