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
import java.util.concurrent.CancellationException
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DeutscheBoerseCompressedFileMarketDataRepositoryLoaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun injectedDispatcherDefersFactoryExecution() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            loader(dispatcher).load(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())
        }

        assertFalse(deferred.isCompleted)
        runCurrent()
        assertTrue(deferred.isCompleted)
    }

    @Test
    fun dispatcherExecutionDeliversFactoryResult() = runTest {
        val result = load(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())

        assertTrue(result is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success)
    }

    @Test
    fun emptyRequestedIsinsAndMissingFilesProduceSuccess() = runTest {
        val result = load(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())

        assertTrue(result is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success)
    }

    @Test
    fun emptyRepositoryReturnsNotFound() = runTest {
        val repository = loadSuccess(
            missingFile("dxsc.gz"),
            missingFile("xfra.zip"),
            emptySet()
        )

        assertEquals(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN))
    }

    @Test
    fun validCompressedFilesProduceSuccess() = runTest {
        val files = validFiles()

        assertTrue(
            load(files.dxsc, files.xfra) is
                DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success
        )
    }

    @Test
    fun requestedIsinProducesRepositorySuccess() = runTest {
        val files = validFiles()
        val repository = loadSuccess(files.dxsc, files.xfra)

        assertTrue(repository.findByProductIsin(PRODUCT_ISIN) is RepositoryResult.Success)
    }

    @Test
    fun marketDataFieldsRemainUnchanged() = runTest {
        val marketData = successfulMarketData(validFiles())

        assertEquals(PRODUCT_ISIN, marketData.productIsin)
        assertEquals(2.343, marketData.bid)
        assertEquals(2.344, marketData.ask)
        assertEquals(1_785_180_597_000L, marketData.bidTimestampEpochMillis)
        assertEquals(1_785_180_597_000L, marketData.askTimestampEpochMillis)
        assertEquals("EUR", marketData.currency)
        assertEquals(DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID, marketData.sourceId)
    }

    @Test
    fun requestedIsinsAreCopiedBeforeDispatcherSwitch() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val requestedIsins = mutableSetOf(PRODUCT_ISIN)
        val files = files(
            dxscLines = listOf(dxscLine(PRODUCT_ISIN), dxscLine(OTHER_ISIN)),
            xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN))
        )
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            loader(dispatcher).load(files.dxsc, files.xfra, requestedIsins)
        }

        requestedIsins.clear()
        requestedIsins.add(OTHER_ISIN)
        advanceUntilIdle()

        val repository = deferred.await().successRepository()
        assertTrue(repository.findByProductIsin(PRODUCT_ISIN) is RepositoryResult.Success)
        assertEquals(RepositoryResult.NotFound, repository.findByProductIsin(OTHER_ISIN))
    }

    @Test
    fun requestedIsinsAreNotNormalized() = runTest {
        val whitespaceIsin = " $PRODUCT_ISIN "
        val lowercaseIsin = PRODUCT_ISIN.lowercase()
        val files = files(
            dxscLines = listOf(dxscLine(whitespaceIsin), dxscLine(lowercaseIsin)),
            xfraLines = listOf(xfraLine(whitespaceIsin), xfraLine(lowercaseIsin))
        )
        val repository = loadSuccess(
            files.dxsc,
            files.xfra,
            setOf(whitespaceIsin, lowercaseIsin)
        )

        assertTrue(repository.findByProductIsin(whitespaceIsin) is RepositoryResult.Success)
        assertTrue(repository.findByProductIsin(lowercaseIsin) is RepositoryResult.Success)
        assertEquals(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN))
    }

    @Test
    fun missingXfraFilePreservesXfraFailureCode() = runTest {
        val failure = loadFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
            failure.error.code
        )
    }

    @Test
    fun validXfraAndMissingDxscPreserveDxscFailureCode() = runTest {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val failure = loadFailure(missingFile("dxsc.gz"), xfra)

        assertEquals(
            DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED,
            failure.error.code
        )
    }

    @Test
    fun nestedXfraLoaderErrorRemainsUnchanged() = runTest {
        val failure = loadFailure(missingFile("dxsc.gz"), missingFile("xfra.zip"))

        assertEquals(
            DeutscheBoerseXfraCsvLoadingError(
                code = DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            failure.error.xfraLoadingError
        )
        assertNull(failure.error.dxscLoadingError)
    }

    @Test
    fun nestedDxscLoaderErrorRemainsUnchanged() = runTest {
        val xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
        val failure = loadFailure(missingFile("dxsc.gz"), xfra)

        assertEquals(
            DeutscheBoerseDxscNdjsonLoadingError(
                code = DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            failure.error.dxscLoadingError
        )
        assertNull(failure.error.xfraLoadingError)
    }

    @Test
    fun cancellationBeforeDispatcherExecutionPropagatesCancellationException() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            loader(dispatcher).load(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())
        }

        deferred.cancel()
        advanceUntilIdle()

        try {
            deferred.await()
            fail("CancellationException expected")
        } catch (_: CancellationException) {
            assertTrue(deferred.isCancelled)
        }
    }

    @Test
    fun cancellationDoesNotInventFailureResult() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            loader(dispatcher).load(missingFile("dxsc.gz"), missingFile("xfra.zip"), emptySet())
        }

        deferred.cancel()
        advanceUntilIdle()

        assertTrue(deferred.isCancelled)
        assertFalse(deferred.isCompleted && !deferred.isCancelled)
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterLoadingAndRepositoryQuery() = runTest {
        val files = validFiles()
        val repository = loadSuccess(files.dxsc, files.xfra)

        repository.findByProductIsin(PRODUCT_ISIN)

        assertTrue(files.xfra.delete())
        assertTrue(files.dxsc.delete())
    }

    @Test
    fun loaderOnlyCoordinatesDispatcherAndFactoryDelegation() {
        val source = loaderSourceFile().readText()

        assertEquals(
            1,
            source.windowed("DeutscheBoerseCompressedFileMarketDataRepositoryFactory.create".length)
                .count { it == "DeutscheBoerseCompressedFileMarketDataRepositoryFactory.create" }
        )
        listOf(
            "FileInputStream",
            "GZIPInputStream",
            "ZipInputStream",
            "runCatching",
            "RepositoryResult",
            "DataQuality",
            "GlobalScope",
            "CoroutineScope(",
            "async(",
            "launch(",
            "catch ("
        ).forEach { forbidden ->
            assertFalse("Unexpected production logic: $forbidden", source.contains(forbidden))
        }
        assertTrue(source.contains("requestedProductIsins.toSet()"))
        assertTrue(source.contains("withContext(ioDispatcher)"))
    }

    private fun loader(dispatcher: CoroutineDispatcher) =
        DeutscheBoerseCompressedFileMarketDataRepositoryLoader(dispatcher)

    private suspend fun TestScope.load(
        dxsc: File,
        xfra: File,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult =
        DeutscheBoerseCompressedFileMarketDataRepositoryLoader(
            UnconfinedTestDispatcher(testScheduler)
        ).load(dxsc, xfra, requestedIsins)

    private suspend fun TestScope.loadSuccess(
        dxsc: File,
        xfra: File,
        requestedIsins: Set<String> = setOf(PRODUCT_ISIN)
    ): KnockoutProductMarketDataRepository =
        load(dxsc, xfra, requestedIsins).successRepository()

    private suspend fun TestScope.loadFailure(
        dxsc: File,
        xfra: File
    ): DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure =
        load(dxsc, xfra) as
            DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure

    private suspend fun TestScope.successfulMarketData(
        files: CompressedFiles
    ): KnockoutProductMarketData =
        (loadSuccess(files.dxsc, files.xfra).findByProductIsin(PRODUCT_ISIN) as
            RepositoryResult.Success).value

    private fun DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.successRepository():
        KnockoutProductMarketDataRepository =
        (this as DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success).repository

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

    private fun dxscLine(isin: String): String =
        """{"messageId":"pretrade","instrumentIdentificationCode":"$isin","bestBid":2.343,"bestAsk":2.344,"updateDateAndTime":"$TIMESTAMP"}"""

    private fun loaderSourceFile(): File = listOf(
        File("src/main/java/de/konavigator/app/data/remote/DeutscheBoerseCompressedFileMarketDataRepositoryLoader.kt"),
        File("app/src/main/java/de/konavigator/app/data/remote/DeutscheBoerseCompressedFileMarketDataRepositoryLoader.kt")
    ).first { it.isFile }

    private data class CompressedFiles(
        val dxsc: File,
        val xfra: File
    )

    private var fileCounter = 0

    private companion object {
        const val PRODUCT_ISIN = "DE000LOADER001"
        const val OTHER_ISIN = "DE000LOADER002"
        const val TIMESTAMP = "2026-07-27T19:29:57Z"
    }
}
