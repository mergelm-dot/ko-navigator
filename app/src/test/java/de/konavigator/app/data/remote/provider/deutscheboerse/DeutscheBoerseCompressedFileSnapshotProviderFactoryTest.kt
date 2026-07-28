package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.provider.ProviderResult
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeutscheBoerseCompressedFileSnapshotProviderFactoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyRequestedIsinsSucceedWithMissingFiles() {
        val result = create(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())

        assertTrue(result is DeutscheBoerseSnapshotProviderCreationResult.Success)
    }

    @Test
    fun emptyRequestedIsinsProduceProviderWithoutRecords() = runTest {
        val provider = createSuccess(
            missingFile("dxsc.gz"),
            missingFile("xfra.zip"),
            emptySet()
        )

        assertEquals(ProviderResult.NotFound, provider.findByProductIsin(PRODUCT_ISIN))
    }

    @Test
    fun validTemporaryCompressedFilesProduceProvider() {
        val files = validFiles()

        assertTrue(
            create(files.dxsc, files.xfra) is
                DeutscheBoerseSnapshotProviderCreationResult.Success
        )
    }

    @Test
    fun createdProviderReturnsSuccessForRequestedIsin() = runTest {
        val files = validFiles()
        val provider = createSuccess(files.dxsc, files.xfra)

        assertTrue(provider.findByProductIsin(PRODUCT_ISIN) is ProviderResult.Success)
    }

    @Test
    fun completeDtoIsDeliveredThroughExistingChain() = runTest {
        val files = validFiles()
        val dto = successDto(createSuccess(files.dxsc, files.xfra))

        assertEquals(2.343, dto.bid)
        assertEquals(2.344, dto.ask)
        assertEquals(1_785_180_597_000L, dto.bidTimestampEpochMillis)
        assertEquals(1_785_180_597_000L, dto.askTimestampEpochMillis)
        assertEquals("EUR", dto.currency)
        assertEquals(DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID, dto.sourceId)
    }

    @Test
    fun newestDxscUpdateIsSelectedByExistingProvider() = runTest {
        val files = files(
            dxscLines = listOf(
                dxscLine(PRODUCT_ISIN, bid = 1.0, timestamp = OLDER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bid = 2.0, timestamp = NEWER_TIMESTAMP)
            ),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN))
        )

        assertEquals(2.0, successDto(createSuccess(files.dxsc, files.xfra)).bid)
    }

    @Test
    fun currencyAndSettlementCurrencyRemainSeparate() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN, "EUR", "MXN"))
        )

        assertEquals("MXN", successDto(createSuccess(files.dxsc, files.xfra)).currency)
    }

    @Test
    fun emptyCurrencyIsNotReplacedBySettlementCurrency() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN, "EUR", ""))
        )

        assertNull(successDto(createSuccess(files.dxsc, files.xfra)).currency)
    }

    @Test
    fun nullBidAndZeroAskRemainUnchanged() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN, bid = null, ask = 0.0)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN))
        )

        val dto = successDto(createSuccess(files.dxsc, files.xfra))
        assertNull(dto.bid)
        assertEquals(0.0, dto.ask)
    }

    @Test
    fun multipleRequestedIsinsAreSupported() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN), dxscLine(OTHER_ISIN)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN))
        )
        val provider = createSuccess(
            files.dxsc,
            files.xfra,
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertTrue(provider.findByProductIsin(PRODUCT_ISIN) is ProviderResult.Success)
        assertTrue(provider.findByProductIsin(OTHER_ISIN) is ProviderResult.Success)
    }

    @Test
    fun requestedIsinsAreNotNormalized() = runTest {
        val exactIsin = " $PRODUCT_ISIN "
        val files = files(
            dxscLines = listOf(dxscLine(exactIsin), dxscLine(PRODUCT_ISIN.lowercase())),
            xfraLines = listOf(xfraLine(exactIsin), xfraLine(PRODUCT_ISIN.lowercase()))
        )
        val provider = createSuccess(files.dxsc, files.xfra, setOf(exactIsin))

        assertEquals(exactIsin, successDto(provider, exactIsin).productIsin)
    }

    @Test
    fun missingXfraFileProducesXfraLoadingFailure() {
        val result = createFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun missingXfraFileContainsLineOneSourceFailure() {
        val result = createFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertEquals(
            DeutscheBoerseXfraCsvLoadingError(
                code = DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            result.error.xfraLoadingError
        )
    }

    @Test
    fun missingXfraFileIsNotClassifiedAsDxscFailure() {
        val result = createFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            result.error.code
        )
        assertNull(result.error.dxscLoadingError)
    }

    @Test
    fun missingDxscAfterValidXfraProducesDxscLoadingFailure() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val result = createFailure(missingFile("dxsc.gz"), xfra)

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun missingDxscContainsLineOneSourceFailure() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val result = createFailure(missingFile("dxsc.gz"), xfra)

        assertEquals(
            DeutscheBoerseDxscNdjsonLoadingError(
                code = DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            result.error.dxscLoadingError
        )
    }

    @Test
    fun invalidXfraZipProducesXfraLoadingFailure() {
        val invalidXfra = temporaryFolder.newFile("invalid.zip").apply {
            writeText("not zip")
        }
        val result = createFailure(missingFile("dxsc.gz"), invalidXfra)

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun invalidDxscGzipAfterValidXfraProducesDxscLoadingFailure() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val invalidDxsc = temporaryFolder.newFile("invalid.gz").apply {
            writeText("not gzip")
        }
        val result = createFailure(invalidDxsc, xfra)

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED,
            result.error.code
        )
    }

    @Test
    fun duplicateXfraRecordsFailOnlyWhenProviderIsQueried() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(PRODUCT_ISIN))
        )
        val provider = createSuccess(files.dxsc, files.xfra)

        assertEquals(
            ProviderResult.DataAccessFailure,
            provider.findByProductIsin(PRODUCT_ISIN)
        )
    }

    @Test
    fun xfraFileCanBeDeletedAfterSuccessfulProcessing() {
        val files = validFiles()

        createSuccess(files.dxsc, files.xfra)

        assertTrue(files.xfra.delete())
        assertFalse(files.xfra.exists())
    }

    @Test
    fun dxscFileCanBeDeletedAfterSuccessfulProcessing() {
        val files = validFiles()

        createSuccess(files.dxsc, files.xfra)

        assertTrue(files.dxsc.delete())
        assertFalse(files.dxsc.exists())
    }

    @Test
    fun bothFilesCanBeDeletedAfterTypedDxscFailure() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val dxsc = writeDxscGzip(listOf(MALFORMED_JSON))

        createFailure(dxsc, xfra)

        assertTrue(xfra.delete())
        assertTrue(dxsc.delete())
    }

    @Test
    fun adapterPreservesExistingSelectionAndMappingResponsibilities() = runTest {
        val files = files(
            dxscLines = listOf(
                dxscLine(PRODUCT_ISIN, bid = 1.0, timestamp = OLDER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bid = 2.0, timestamp = NEWER_TIMESTAMP)
            ),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN))
        )

        val dto = successDto(createSuccess(files.dxsc, files.xfra))

        assertEquals(2.0, dto.bid)
        assertEquals(DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID, dto.sourceId)
    }

    private fun create(
        dxsc: File,
        xfra: File,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseSnapshotProviderCreationResult =
        DeutscheBoerseCompressedFileSnapshotProviderFactory.create(
            dxscGzipFile = dxsc,
            xfraZipFile = xfra,
            requestedProductIsins = requestedIsins
        )

    private fun createSuccess(
        dxsc: File,
        xfra: File,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseSnapshotKnockoutProductMarketDataProvider =
        (create(dxsc, xfra, requestedIsins) as
            DeutscheBoerseSnapshotProviderCreationResult.Success).provider

    private fun createFailure(
        dxsc: File,
        xfra: File
    ): DeutscheBoerseSnapshotProviderCreationResult.Failure =
        create(dxsc, xfra) as DeutscheBoerseSnapshotProviderCreationResult.Failure

    private suspend fun successDto(
        provider: DeutscheBoerseSnapshotKnockoutProductMarketDataProvider,
        isin: String = PRODUCT_ISIN
    ): KnockoutProductMarketDataDto =
        (provider.findByProductIsin(isin) as ProviderResult.Success).value

    private fun validFiles(): CompressedFiles = files(
        dxscLines = listOf(dxscLine(PRODUCT_ISIN)),
        xfraLines = listOf(xfraLine(PRODUCT_ISIN))
    )

    private fun files(
        dxscLines: List<String>,
        xfraLines: List<String>
    ) = CompressedFiles(
        dxsc = writeDxscGzip(dxscLines),
        xfra = writeXfraZip(xfraLines)
    )

    private fun writeDxscGzip(lines: List<String>): File =
        temporaryFolder.newFile("dxsc-${fileCounter++}.json.gz").also { file ->
            GZIPOutputStream(FileOutputStream(file))
                .bufferedWriter(StandardCharsets.UTF_8)
                .use { writer ->
                    writer.write(lines.joinToString(separator = "\n", postfix = "\n"))
                }
        }

    private fun writeXfraZip(lines: List<String>): File =
        temporaryFolder.newFile("xfra-${fileCounter++}.zip").also { file ->
            ZipOutputStream(FileOutputStream(file), StandardCharsets.UTF_8).use { zip ->
                zip.putNextEntry(ZipEntry("reference"))
                zip.write(csvContent(lines).toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }

    private fun missingFile(name: String): File = File(temporaryFolder.root, name)

    private fun csvContent(lines: List<String>): String =
        listOf("metadata one", "metadata two", validHeader())
            .plus(lines)
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

    private data class CompressedFiles(
        val dxsc: File,
        val xfra: File
    )

    private var fileCounter = 0

    private companion object {
        const val PRODUCT_ISIN = "DE000FILE001"
        const val OTHER_ISIN = "DE000FILE002"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val MALFORMED_JSON = "{\"messageId\":"
    }
}
