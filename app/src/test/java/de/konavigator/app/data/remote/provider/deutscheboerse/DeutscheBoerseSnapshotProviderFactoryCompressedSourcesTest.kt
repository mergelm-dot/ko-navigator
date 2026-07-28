package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.provider.ProviderResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DeutscheBoerseSnapshotProviderFactoryCompressedSourcesTest {

    @Test
    fun emptyRequestedIsinsProduceSuccess() {
        assertTrue(
            create(
                openDxsc = { error("must not open") },
                openXfra = { error("must not open") },
                requestedIsins = emptySet()
            ) is DeutscheBoerseSnapshotProviderCreationResult.Success
        )
    }

    @Test
    fun emptyRequestedIsinsProduceProviderWithoutRecords() = runTest {
        val provider = createSuccess(
            openDxsc = { error("must not open") },
            openXfra = { error("must not open") },
            requestedIsins = emptySet()
        )

        assertEquals(ProviderResult.NotFound, provider.findByProductIsin(PRODUCT_ISIN))
    }

    @Test
    fun emptyRequestedIsinsDoNotOpenXfraInput() {
        var calls = 0

        create(
            openDxsc = { error("must not open") },
            openXfra = { calls++; error("must not open") },
            requestedIsins = emptySet()
        )

        assertEquals(0, calls)
    }

    @Test
    fun emptyRequestedIsinsDoNotOpenDxscInput() {
        var calls = 0

        create(
            openDxsc = { calls++; error("must not open") },
            openXfra = { error("must not open") },
            requestedIsins = emptySet()
        )

        assertEquals(0, calls)
    }

    @Test
    fun validCompressedSourcesProduceProvider() {
        val result = create(validDxscOpen(), validXfraOpen())

        assertTrue(result is DeutscheBoerseSnapshotProviderCreationResult.Success)
    }

    @Test
    fun createdProviderReturnsSuccessForMatchingIsin() = runTest {
        val provider = createSuccess(validDxscOpen(), validXfraOpen())

        assertTrue(provider.findByProductIsin(PRODUCT_ISIN) is ProviderResult.Success)
    }

    @Test
    fun completeDtoIsDeliveredThroughExistingProviderChain() = runTest {
        val dto = successDto(createSuccess(validDxscOpen(), validXfraOpen()))

        assertEquals(2.343, dto.bid)
        assertEquals(2.344, dto.ask)
        assertEquals(1_785_180_597_000L, dto.bidTimestampEpochMillis)
        assertEquals(1_785_180_597_000L, dto.askTimestampEpochMillis)
        assertEquals("EUR", dto.currency)
        assertEquals(DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID, dto.sourceId)
    }

    @Test
    fun newestDxscUpdateIsSelectedOnlyByProvider() = runTest {
        val provider = createSuccess(
            openDxsc = gzipOpen(
                dxscLine(PRODUCT_ISIN, bid = 1.0, timestamp = OLDER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bid = 2.0, timestamp = NEWER_TIMESTAMP)
            ),
            openXfra = validXfraOpen()
        )

        assertEquals(2.0, successDto(provider).bid)
    }

    @Test
    fun factoryPreservesDxscRecordOrder() = runTest {
        val provider = createSuccess(
            openDxsc = gzipOpen(
                dxscLine(PRODUCT_ISIN, bid = 1.0, timestamp = NEWER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bid = 2.0, timestamp = NEWER_TIMESTAMP)
            ),
            openXfra = validXfraOpen()
        )

        assertEquals(1.0, successDto(provider).bid)
    }

    @Test
    fun currencyAndSettlementCurrencyRemainSeparate() = runTest {
        val provider = createSuccess(
            validDxscOpen(),
            zipOpen(csvContent(xfraLine(PRODUCT_ISIN, "EUR", "MXN")))
        )

        assertEquals("MXN", successDto(provider).currency)
    }

    @Test
    fun emptyCurrencyIsNotReplacedBySettlementCurrency() = runTest {
        val provider = createSuccess(
            validDxscOpen(),
            zipOpen(csvContent(xfraLine(PRODUCT_ISIN, "EUR", "")))
        )

        assertNull(successDto(provider).currency)
    }

    @Test
    fun nullBidAndZeroAskRemainUnchanged() = runTest {
        val provider = createSuccess(
            gzipOpen(dxscLine(PRODUCT_ISIN, bid = null, ask = 0.0)),
            validXfraOpen()
        )

        val dto = successDto(provider)
        assertNull(dto.bid)
        assertEquals(0.0, dto.ask)
    }

    @Test
    fun multipleRequestedIsinsAreSupported() = runTest {
        val provider = createSuccess(
            gzipOpen(dxscLine(PRODUCT_ISIN), dxscLine(OTHER_ISIN)),
            zipOpen(csvContent(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN))),
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertTrue(provider.findByProductIsin(PRODUCT_ISIN) is ProviderResult.Success)
        assertTrue(provider.findByProductIsin(OTHER_ISIN) is ProviderResult.Success)
    }

    @Test
    fun requestedIsinsAreNotNormalized() = runTest {
        val exactIsin = " $PRODUCT_ISIN "
        val provider = createSuccess(
            gzipOpen(dxscLine(exactIsin), dxscLine(PRODUCT_ISIN.lowercase())),
            zipOpen(csvContent(xfraLine(exactIsin), xfraLine(PRODUCT_ISIN.lowercase()))),
            setOf(exactIsin)
        )

        assertEquals(exactIsin, successDto(provider, exactIsin).productIsin)
    }

    @Test
    fun missingIsinReturnsNotFoundWhenProviderIsQueried() = runTest {
        val provider = createSuccess(validDxscOpen(), validXfraOpen())

        assertEquals(ProviderResult.NotFound, provider.findByProductIsin(OTHER_ISIN))
    }

    @Test
    fun duplicateXfraRecordsFailOnlyWhenProviderIsQueried() = runTest {
        val provider = createSuccess(
            validDxscOpen(),
            zipOpen(csvContent(xfraLine(PRODUCT_ISIN), xfraLine(PRODUCT_ISIN)))
        )

        assertEquals(
            ProviderResult.DataAccessFailure,
            provider.findByProductIsin(PRODUCT_ISIN)
        )
    }

    @Test
    fun successfulCreationOpensXfraExactlyOnce() {
        var calls = 0

        createSuccess(validDxscOpen(), { calls++; validXfraOpen().invoke() })

        assertEquals(1, calls)
    }

    @Test
    fun successfulCreationOpensDxscExactlyOnce() {
        var calls = 0

        createSuccess({ calls++; validDxscOpen().invoke() }, validXfraOpen())

        assertEquals(1, calls)
    }

    @Test
    fun xfraIsOpenedBeforeDxsc() {
        val order = mutableListOf<String>()

        createSuccess(
            openDxsc = { order += "DXSC"; validDxscOpen().invoke() },
            openXfra = { order += "XFRA"; validXfraOpen().invoke() }
        )

        assertEquals(listOf("XFRA", "DXSC"), order)
    }

    @Test
    fun xfraLoadingFailureProducesTypedCreationFailure() {
        val result = createFailure(validDxscOpen(), invalidXfraOpen())

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun completeXfraLoadingErrorIsPreserved() {
        val result = createFailure(validDxscOpen(), invalidXfraOpen())

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
        assertNull(createFailure(validDxscOpen(), invalidXfraOpen()).error.dxscLoadingError)
    }

    @Test
    fun xfraLoadingFailureDoesNotOpenDxsc() {
        var dxscCalls = 0

        createFailure({ dxscCalls++; validDxscOpen().invoke() }, invalidXfraOpen())

        assertEquals(0, dxscCalls)
    }

    @Test
    fun emptySuccessfulXfraSnapshotDoesNotPreventDxscLoading() {
        var dxscCalls = 0

        createSuccess(
            openDxsc = { dxscCalls++; validDxscOpen().invoke() },
            openXfra = zipOpen(csvContent(xfraLine(OTHER_ISIN)))
        )

        assertEquals(1, dxscCalls)
    }

    @Test
    fun dxscLoadingFailureProducesTypedCreationFailure() {
        val result = createFailure(invalidDxscOpen(), validXfraOpen())

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun completeDxscLoadingErrorIsPreserved() {
        val result = createFailure(invalidDxscOpen(), validXfraOpen())

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
        assertNull(createFailure(invalidDxscOpen(), validXfraOpen()).error.xfraLoadingError)
    }

    @Test
    fun dxscLoadingFailureReturnsNoProvider() {
        assertTrue(
            create(invalidDxscOpen(), validXfraOpen()) is
                DeutscheBoerseSnapshotProviderCreationResult.Failure
        )
    }

    @Test
    fun xfraClosingFailureIsPreservedAsXfraLoadingFailure() {
        val input = CloseFailingInputStream(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))
        val result = createFailure(validDxscOpen(), { input })

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            result.error.code
        )
        assertEquals(
            DeutscheBoerseXfraCsvLoadingError(
                code = DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = null
            ),
            result.error.xfraLoadingError
        )
    }

    @Test
    fun dxscClosingFailureIsPreservedAsDxscLoadingFailure() {
        val input = CloseFailingInputStream(gzipBytes(dxscLine(PRODUCT_ISIN)))
        val result = createFailure({ input }, validXfraOpen())

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED,
            result.error.code
        )
        assertEquals(
            DeutscheBoerseDxscNdjsonLoadingError(
                code = DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = null
            ),
            result.error.dxscLoadingError
        )
    }

    @Test
    fun bothCompressedStreamsCloseAfterSuccess() {
        val xfraInput = TrackingInputStream(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))
        val dxscInput = TrackingInputStream(gzipBytes(dxscLine(PRODUCT_ISIN)))

        createSuccess({ dxscInput }, { xfraInput })

        assertTrue(xfraInput.closed)
        assertTrue(dxscInput.closed)
    }

    @Test
    fun xfraStreamClosesAfterTypedXfraFailure() {
        val xfraInput = TrackingInputStream(zipBytes("metadata one\nmetadata two\n\n"))

        createFailure(validDxscOpen(), { xfraInput })

        assertTrue(xfraInput.closed)
    }

    @Test
    fun openedStreamsCloseAfterTypedDxscFailure() {
        val xfraInput = TrackingInputStream(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))
        val dxscInput = TrackingInputStream(gzipBytes(MALFORMED_JSON))

        createFailure({ dxscInput }, { xfraInput })

        assertTrue(xfraInput.closed)
        assertTrue(dxscInput.closed)
    }

    @Test
    fun xfraOpenRuntimeFailureIsNotSwallowed() {
        val expected = IllegalStateException("xfra runtime")

        val actual = assertThrows(IllegalStateException::class.java) {
            create(validDxscOpen(), { throw expected })
        }

        assertTrue(actual === expected)
    }

    @Test
    fun xfraRuntimeFailureDoesNotOpenDxsc() {
        var dxscCalls = 0

        assertThrows(IllegalStateException::class.java) {
            create(
                openDxsc = { dxscCalls++; validDxscOpen().invoke() },
                openXfra = { throw IllegalStateException("xfra runtime") }
            )
        }

        assertEquals(0, dxscCalls)
    }

    @Test
    fun dxscOpenRuntimeFailureIsNotSwallowed() {
        val expected = IllegalStateException("dxsc runtime")

        val actual = assertThrows(IllegalStateException::class.java) {
            create({ throw expected }, validXfraOpen())
        }

        assertTrue(actual === expected)
    }

    @Test
    fun factoryDoesNotMaterializeCompressedSources() {
        val xfraInput = TrackingInputStream(zipBytes(csvContent(xfraLine(PRODUCT_ISIN))))
        val dxscInput = TrackingInputStream(gzipBytes(dxscLine(PRODUCT_ISIN)))

        createSuccess({ dxscInput }, { xfraInput })

        assertEquals(1, xfraInput.closeCalls)
        assertEquals(1, dxscInput.closeCalls)
    }

    private fun create(
        openDxsc: () -> InputStream,
        openXfra: () -> InputStream,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseSnapshotProviderCreationResult =
        DeutscheBoerseSnapshotProviderFactory.createFromCompressedSources(
            openDxscCompressedInput = openDxsc,
            openXfraCompressedInput = openXfra,
            requestedProductIsins = requestedIsins
        )

    private fun createSuccess(
        openDxsc: () -> InputStream,
        openXfra: () -> InputStream,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseSnapshotKnockoutProductMarketDataProvider =
        (create(openDxsc, openXfra, requestedIsins) as
            DeutscheBoerseSnapshotProviderCreationResult.Success).provider

    private fun createFailure(
        openDxsc: () -> InputStream,
        openXfra: () -> InputStream
    ): DeutscheBoerseSnapshotProviderCreationResult.Failure =
        create(openDxsc, openXfra) as
            DeutscheBoerseSnapshotProviderCreationResult.Failure

    private suspend fun successDto(
        provider: DeutscheBoerseSnapshotKnockoutProductMarketDataProvider,
        isin: String = PRODUCT_ISIN
    ): KnockoutProductMarketDataDto =
        (provider.findByProductIsin(isin) as ProviderResult.Success).value

    private fun validDxscOpen(): () -> InputStream = gzipOpen(dxscLine(PRODUCT_ISIN))

    private fun validXfraOpen(): () -> InputStream =
        zipOpen(csvContent(xfraLine(PRODUCT_ISIN)))

    private fun invalidDxscOpen(): () -> InputStream = gzipOpen(MALFORMED_JSON)

    private fun invalidXfraOpen(): () -> InputStream =
        zipOpen("metadata one\nmetadata two\n\n")

    private fun gzipOpen(vararg lines: String): () -> InputStream = {
        ByteArrayInputStream(gzipBytes(*lines))
    }

    private fun zipOpen(content: String): () -> InputStream = {
        ByteArrayInputStream(zipBytes(content))
    }

    private fun gzipBytes(vararg lines: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(lines.joinToString(separator = "\n", postfix = "\n"))
        }
        return output.toByteArray()
    }

    private fun zipBytes(content: String): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
            zip.putNextEntry(ZipEntry("reference"))
            zip.write(content.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun csvContent(vararg dataLines: String): String =
        listOf("metadata one", "metadata two", validHeader())
            .plus(dataLines)
            .joinToString(separator = "\n", postfix = "\n")

    private fun validHeader(): String = DeutscheBoerseXfraRequiredColumn.entries
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

    private fun dxscLine(
        isin: String,
        bid: Double? = 2.343,
        ask: Double? = 2.344,
        timestamp: String = NEWER_TIMESTAMP
    ): String {
        val jsonBid = bid?.toString() ?: "null"
        val jsonAsk = ask?.toString() ?: "null"
        return """{"messageId":"pretrade","instrumentIdentificationCode":"$isin","bestBid":$jsonBid,"bestAsk":$jsonAsk,"updateDateAndTime":"$timestamp"}"""
    }

    private open class TrackingInputStream(
        bytes: ByteArray
    ) : InputStream() {
        private val delegate = ByteArrayInputStream(bytes)
        var closeCalls: Int = 0
            private set
        var closed: Boolean = false
            private set

        override fun read(): Int = delegate.read()

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate.read(buffer, offset, length)

        override fun close() {
            closeCalls++
            closed = true
            delegate.close()
        }
    }

    private class CloseFailingInputStream(
        bytes: ByteArray
    ) : TrackingInputStream(bytes) {
        override fun close() {
            super.close()
            throw IOException("synthetic close failure")
        }
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000COMP001"
        const val OTHER_ISIN = "DE000COMP002"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val MALFORMED_JSON = "{\"messageId\":"
    }
}
