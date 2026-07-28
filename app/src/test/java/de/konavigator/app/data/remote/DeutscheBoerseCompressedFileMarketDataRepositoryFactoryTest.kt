package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseDxscNdjsonLoadingError
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseDxscNdjsonLoadingErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseKnockoutProductMarketDataMapper
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraCsvLoadingError
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraCsvLoadingErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraRequiredColumn
import de.konavigator.app.domain.model.KnockoutProductMarketData
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

class DeutscheBoerseCompressedFileMarketDataRepositoryFactoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyRequestedIsinsAndMissingFilesProduceSuccess() {
        val result = create(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())

        assertTrue(result is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success)
    }

    @Test
    fun successPublishesRepositoryThroughExistingPort() {
        val result = createSuccess(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())

        val repository: KnockoutProductMarketDataRepository = result
        assertTrue(repository === result)
    }

    @Test
    fun emptyRepositoryReturnsNotFound() = runTest {
        val repository = createSuccess(
            missingFile("dxsc.gz"),
            missingFile("xfra.zip"),
            emptySet()
        )

        assertEquals(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN))
    }

    @Test
    fun validTemporaryFilesProduceRepository() {
        val files = validFiles()

        assertTrue(
            create(files.dxsc, files.xfra) is
                DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success
        )
    }

    @Test
    fun existingIsinReturnsRepositorySuccess() = runTest {
        val files = validFiles()
        val repository = createSuccess(files.dxsc, files.xfra)

        assertTrue(repository.findByProductIsin(PRODUCT_ISIN) is RepositoryResult.Success)
    }

    @Test
    fun domainModelContainsCorrectProductIsin() = runTest {
        val marketData = successfulMarketData(validFiles())

        assertEquals(PRODUCT_ISIN, marketData.productIsin)
    }

    @Test
    fun bidAndAskRemainUnchangedInDomainModel() = runTest {
        val marketData = successfulMarketData(validFiles())

        assertEquals(2.343, marketData.bid)
        assertEquals(2.344, marketData.ask)
    }

    @Test
    fun bidAndAskTimestampsAreMappedCorrectly() = runTest {
        val marketData = successfulMarketData(validFiles())

        assertEquals(1_785_180_597_000L, marketData.bidTimestampEpochMillis)
        assertEquals(1_785_180_597_000L, marketData.askTimestampEpochMillis)
    }

    @Test
    fun currencyIsMappedCorrectly() = runTest {
        val marketData = successfulMarketData(validFiles())

        assertEquals("EUR", marketData.currency)
    }

    @Test
    fun existingDeutscheBoerseSourceIdIsMappedCorrectly() = runTest {
        val marketData = successfulMarketData(validFiles())

        assertEquals(DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID, marketData.sourceId)
    }

    @Test
    fun newestDxscUpdateIsSelectedThroughExistingProviderChain() = runTest {
        val files = files(
            dxscLines = listOf(
                dxscLine(PRODUCT_ISIN, bid = 1.0, timestamp = OLDER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bid = 2.0, timestamp = NEWER_TIMESTAMP)
            ),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN))
        )

        assertEquals(2.0, successfulMarketData(files).bid)
    }

    @Test
    fun repositoryFactoryDoesNotPerformLatestSelection() = runTest {
        val files = files(
            dxscLines = listOf(
                dxscLine(PRODUCT_ISIN, bid = 1.0, timestamp = NEWER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bid = 2.0, timestamp = NEWER_TIMESTAMP)
            ),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN))
        )

        assertEquals(1.0, successfulMarketData(files).bid)
    }

    @Test
    fun nullBidRemainsNullInSuccessfulDomainModel() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN, bid = null)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN))
        )

        assertNull(successfulMarketData(files).bid)
    }

    @Test
    fun zeroAskRemainsZeroInSuccessfulDomainModel() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN, ask = 0.0)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN))
        )

        assertEquals(0.0, successfulMarketData(files).ask)
    }

    @Test
    fun multipleRequestedIsinsCanBeQueriedFromSameRepository() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN), dxscLine(OTHER_ISIN)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN))
        )
        val repository = createSuccess(
            files.dxsc,
            files.xfra,
            setOf(PRODUCT_ISIN, OTHER_ISIN)
        )

        assertTrue(repository.findByProductIsin(PRODUCT_ISIN) is RepositoryResult.Success)
        assertTrue(repository.findByProductIsin(OTHER_ISIN) is RepositoryResult.Success)
    }

    @Test
    fun missingSnapshotIsinReturnsRepositoryNotFound() = runTest {
        val files = validFiles()
        val repository = createSuccess(files.dxsc, files.xfra)

        assertEquals(RepositoryResult.NotFound, repository.findByProductIsin(OTHER_ISIN))
    }

    @Test
    fun requestedAndQueriedIsinsAreNotNormalized() = runTest {
        val exactIsin = " $PRODUCT_ISIN "
        val files = files(
            dxscLines = listOf(dxscLine(exactIsin), dxscLine(PRODUCT_ISIN.lowercase())),
            xfraLines = listOf(xfraLine(exactIsin), xfraLine(PRODUCT_ISIN.lowercase()))
        )
        val repository = createSuccess(files.dxsc, files.xfra, setOf(exactIsin))

        val marketData = repository.findByProductIsin(exactIsin).successValue()
        assertEquals(exactIsin, marketData.productIsin)
        assertEquals(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN))
    }

    @Test
    fun duplicateXfraRecordsProduceRepositoryDataAccessFailure() = runTest {
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(PRODUCT_ISIN))
        )
        val repository = createSuccess(files.dxsc, files.xfra)

        assertEquals(
            RepositoryResult.DataAccessFailure,
            repository.findByProductIsin(PRODUCT_ISIN)
        )
    }

    @Test
    fun missingXfraFileProducesRepositoryCreationFailure() {
        assertTrue(
            create(missingFile("dxsc.gz"), missingFile("xfra.zip")) is
                DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure
        )
    }

    @Test
    fun xfraFailureCodeRemainsUnchanged() {
        val failure = createFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            failure.error.code
        )
    }

    @Test
    fun nestedXfraSourceFailureRemainsUnchanged() {
        val failure = createFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertEquals(
            DeutscheBoerseXfraCsvLoadingError(
                code = DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            failure.error.xfraLoadingError
        )
    }

    @Test
    fun xfraFailureContainsNoDxscError() {
        val failure = createFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertNull(failure.error.dxscLoadingError)
    }

    @Test
    fun validXfraAndMissingDxscProduceRepositoryCreationFailure() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))

        assertTrue(
            create(missingFile("dxsc.gz"), xfra) is
                DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure
        )
    }

    @Test
    fun dxscFailureCodeRemainsUnchanged() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val failure = createFailure(missingFile("dxsc.gz"), xfra)

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED,
            failure.error.code
        )
    }

    @Test
    fun nestedDxscSourceFailureRemainsUnchanged() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val failure = createFailure(missingFile("dxsc.gz"), xfra)

        assertEquals(
            DeutscheBoerseDxscNdjsonLoadingError(
                code = DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            failure.error.dxscLoadingError
        )
    }

    @Test
    fun dxscFailureContainsNoXfraError() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val failure = createFailure(missingFile("dxsc.gz"), xfra)

        assertNull(failure.error.xfraLoadingError)
    }

    @Test
    fun creationFailureDoesNotExposeRepository() {
        val result = create(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertTrue(result is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure)
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterCreationAndRepositoryQuery() = runTest {
        val files = validFiles()
        val repository = createSuccess(files.dxsc, files.xfra)

        repository.findByProductIsin(PRODUCT_ISIN)

        assertTrue(files.xfra.delete())
        assertTrue(files.dxsc.delete())
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterTypedCreationFailure() {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val dxsc = writeDxscGzip(listOf(MALFORMED_JSON))

        createFailure(dxsc, xfra)

        assertTrue(xfra.delete())
        assertTrue(dxsc.delete())
    }

    @Test
    fun successfulFactoryUsesExistingRemoteRepositoryAdapter() {
        val files = validFiles()
        val result = create(files.dxsc, files.xfra) as
            DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success

        assertTrue(result.repository is RemoteKnockoutProductMarketDataRepository)
    }

    private fun create(
        dxsc: File,
        xfra: File,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult =
        DeutscheBoerseCompressedFileMarketDataRepositoryFactory.create(
            dxscGzipFile = dxsc,
            xfraZipFile = xfra,
            requestedProductIsins = requestedIsins
        )

    private fun createSuccess(
        dxsc: File,
        xfra: File,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): KnockoutProductMarketDataRepository =
        (create(dxsc, xfra, requestedIsins) as
            DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success).repository

    private fun createFailure(
        dxsc: File,
        xfra: File
    ): DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure =
        create(dxsc, xfra) as
            DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure

    private suspend fun successfulMarketData(
        files: CompressedFiles
    ): KnockoutProductMarketData =
        createSuccess(files.dxsc, files.xfra)
            .findByProductIsin(PRODUCT_ISIN)
            .successValue()

    private fun RepositoryResult<KnockoutProductMarketData>.successValue():
        KnockoutProductMarketData =
        (this as RepositoryResult.Success).value

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

    private fun xfraLine(isin: String): String = listOf(
        "Active",
        "Tradable",
        "Synthetic Instrument",
        isin,
        "SYN001",
        "XFRA",
        "Warrant",
        "EUR",
        "EUR",
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
        const val PRODUCT_ISIN = "DE000REPO001"
        const val OTHER_ISIN = "DE000REPO002"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val MALFORMED_JSON = "{\"messageId\":"
    }
}
