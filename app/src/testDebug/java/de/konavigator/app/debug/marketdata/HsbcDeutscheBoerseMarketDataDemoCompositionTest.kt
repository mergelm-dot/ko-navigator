package de.konavigator.app.debug.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationService
import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.application.repository.adapter.SnapshotBackedKnockoutProductSpecificationRepository
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryLoader
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationSnapshotRepository
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseDxscNdjsonLoadingError
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseDxscNdjsonLoadingErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraRequiredColumn
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationRecordMappingErrorCode
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiDataQualityStatus
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiError
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiResult
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiSubmission
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModel
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HsbcDeutscheBoerseMarketDataDemoCompositionTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun validLocalFilesCreateViewModelFactory() = runTest(mainDispatcher) {
        assertNotNull(createSuccess(validFiles()).viewModelFactory)
    }

    @Test
    fun factoryCreatesMarketDataCalculationViewModel() = runTest(mainDispatcher) {
        val viewModel = createSuccess(validFiles()).viewModelFactory
            .create(MarketDataCalculationViewModel::class.java)

        assertEquals(MarketDataCalculationViewModel::class.java, viewModel.javaClass)
    }

    @Test
    fun validHsbcCallSpecificationAndDeutscheBoerseAskProducePurchasePrice() =
        runTest(mainDispatcher) {
            val result = calculate(
                createSuccess(validFiles()).viewModelFactory,
                PRODUCT_ISIN,
                MarketDataCalculationType.PURCHASE_PRICE
            ) as MarketDataCalculationUiResult.PurchasePrice

            assertEquals(2.05, result.value, 0.0)
            assertEquals("EUR", result.currency)
        }

    @Test
    fun newestDeutscheBoerseAskRemainsSelected() = runTest(mainDispatcher) {
        val result = calculate(
            createSuccess(validFiles()).viewModelFactory,
            PRODUCT_ISIN,
            MarketDataCalculationType.PURCHASE_PRICE
        ) as MarketDataCalculationUiResult.PurchasePrice

        assertEquals(2.05, result.value, 0.0)
        assertFalse(result.value == 2.0)
    }

    @Test
    fun allFourCalculationTypesRemainSupported() = runTest(mainDispatcher) {
        val factory = createSuccess(validFiles()).viewModelFactory
        val expected = mapOf(
            MarketDataCalculationType.PURCHASE_PRICE to
                MarketDataCalculationUiResult.PurchasePrice::class.java,
            MarketDataCalculationType.SALE_PRICE to
                MarketDataCalculationUiResult.SalePrice::class.java,
            MarketDataCalculationType.SPREAD to MarketDataCalculationUiResult.Spread::class.java,
            MarketDataCalculationType.MID to MarketDataCalculationUiResult.MidPrice::class.java
        )

        expected.forEach { (type, resultClass) ->
            assertTrue(resultClass.isInstance(calculate(factory, PRODUCT_ISIN, type)))
        }
    }

    @Test
    fun successfulCalculationContainsPassedDataQuality() = runTest(mainDispatcher) {
        val result = calculate(
            createSuccess(validFiles()).viewModelFactory,
            PRODUCT_ISIN,
            MarketDataCalculationType.PURCHASE_PRICE
        )

        assertFalse(result is MarketDataCalculationUiResult.Failure)
        assertEquals(MarketDataCalculationUiDataQualityStatus.PASSED, result.dataQuality?.status)
    }

    @Test
    fun hsbcSpecificationRepositoryIsUsed() = runTest(mainDispatcher) {
        val factory = createSuccess(validFiles()).viewModelFactory
        val specificationRepository = specificationRepository(factory)

        assertTrue(
            specificationRepository is SnapshotBackedKnockoutProductSpecificationRepository
        )
        val specification = (specificationRepository.findByProductIsin(PRODUCT_ISIN) as
            RepositoryResult.Success).value
        assertEquals("SYN001", specification.productWkn)
        assertEquals("synthetic-provider", specification.issuerId)
        assertEquals(TradeDirection.LONG, specification.direction)
    }

    @Test
    fun specificationSnapshotMetadataRemainInUnderlyingPath() = runTest(mainDispatcher) {
        val snapshot = snapshot(
            createSuccess(validFiles()).viewModelFactory,
            PRODUCT_ISIN
        )

        assertEquals("HSBC_RESEARCH_LOCAL", snapshot.sourceId)
        assertEquals(SPECIFICATION_RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertEquals(SOURCE_TIMESTAMP, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun missingDeutscheBoerseFilesPreserveMarketDataFailure() = runTest(mainDispatcher) {
        val result = createResult(
            files = CompressedFiles(missingFile("dxsc.gz"), missingFile("xfra.zip")),
            hsbcFiles = mapOf(PRODUCT_ISIN to missingFile("hsbc.json"))
        ) as HsbcDeutscheBoerseMarketDataDemoCompositionResult.Failure

        assertTrue(result.error is HsbcDeutscheBoerseMarketDataDemoCompositionError.MarketDataLoading)
        assertFalse(
            result.error is HsbcDeutscheBoerseMarketDataDemoCompositionError.SpecificationLoading
        )
    }

    @Test
    fun validXfraAndMissingDxscPreserveMarketDataFailure() = runTest(mainDispatcher) {
        val result = createResult(
            files = CompressedFiles(
                dxsc = missingFile("dxsc.gz"),
                xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
            )
        ) as HsbcDeutscheBoerseMarketDataDemoCompositionResult.Failure
        val error = (result.error as
            HsbcDeutscheBoerseMarketDataDemoCompositionError.MarketDataLoading).error

        assertEquals(DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED, error.code)
        assertEquals(
            DeutscheBoerseDxscNdjsonLoadingError(
                DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
                1L
            ),
            error.dxscLoadingError
        )
    }

    @Test
    fun missingHsbcFilePreservesSpecificationFileFailure() = runTest(mainDispatcher) {
        val result = createResult(
            validFiles(),
            hsbcFiles = mapOf(PRODUCT_ISIN to missingFile("hsbc.json"))
        ) as HsbcDeutscheBoerseMarketDataDemoCompositionResult.Failure
        val loading = (result.error as
            HsbcDeutscheBoerseMarketDataDemoCompositionError.SpecificationLoading).error as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                .FileLoading

        assertEquals(PRODUCT_ISIN, loading.errors.single().productIsinKey)
        assertEquals(
            HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode.FILE_READING_FAILED,
            loading.errors.single().code
        )
    }

    @Test
    fun malformedHsbcJsonPreservesParsingFailure() = runTest(mainDispatcher) {
        val processing = specificationProcessingFailure(
            createResult(
                validFiles(),
                hsbcFiles = mapOf(
                    PRODUCT_ISIN to writeTextFile("malformed.json", "{\"productIsin\":")
                )
            )
        )
        val parsing = processing.error as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Parsing

        assertEquals(PRODUCT_ISIN, processing.productIsinKey)
        assertEquals(
            listOf(HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_JSON),
            parsing.errors
        )
    }

    @Test
    fun unsupportedHsbcDirectionPreservesMappingFailure() = runTest(mainDispatcher) {
        val processing = specificationProcessingFailure(
            createResult(
                validFiles(),
                hsbcFiles = mapOf(
                    PRODUCT_ISIN to writeHsbcJson(PRODUCT_ISIN, "SyntheticDirection")
                )
            )
        )
        val mapping = processing.error as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Mapping

        assertEquals(
            listOf(
                HsbcKnockoutProductSpecificationRecordMappingErrorCode
                    .UNSUPPORTED_DIRECTION_LABEL
            ),
            mapping.errors
        )
    }

    @Test
    fun mismatchingHsbcProductIsinPreservesMismatchFailure() = runTest(mainDispatcher) {
        val result = createResult(
            validFiles(),
            hsbcFiles = mapOf(PRODUCT_ISIN to writeHsbcJson(OTHER_ISIN, "Call"))
        ) as HsbcDeutscheBoerseMarketDataDemoCompositionResult.Failure
        val providerCreation = (result.error as
            HsbcDeutscheBoerseMarketDataDemoCompositionError.SpecificationLoading).error as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                .ProviderCreation

        assertEquals(
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                .ProductIsinMismatch(PRODUCT_ISIN, OTHER_ISIN),
            providerCreation.errors.single()
        )
    }

    @Test
    fun unknownSpecificationProducesProductNotFound() = runTest(mainDispatcher) {
        val factory = createSuccess(validFiles(), hsbcFiles = emptyMap()).viewModelFactory

        assertEquals(
            MarketDataCalculationUiError.PRODUCT_NOT_FOUND,
            (calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE) as
                MarketDataCalculationUiResult.Failure).error
        )
    }

    @Test
    fun specificationWithoutRequestedMarketDataProducesMarketDataNotFound() =
        runTest(mainDispatcher) {
            val factory = createSuccess(
                validFiles(),
                requestedProductIsins = emptySet()
            ).viewModelFactory

            assertEquals(
                MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND,
                (calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE) as
                    MarketDataCalculationUiResult.Failure).error
            )
        }

    @Test
    fun requestedProductIsinsAreCopiedBeforeMarketDataLoadingSuspends() =
        runTest(mainDispatcher) {
            val files = files(
                xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN)),
                dxscLines = listOf(dxscLine(PRODUCT_ISIN), dxscLine(OTHER_ISIN))
            )
            val requested = mutableSetOf(PRODUCT_ISIN)
            val deferred = createDeferred(
                files = files,
                requestedProductIsins = requested,
                hsbcFiles = mapOf(
                    PRODUCT_ISIN to writeHsbcJson(PRODUCT_ISIN, "Call"),
                    OTHER_ISIN to writeHsbcJson(OTHER_ISIN, "Put")
                )
            )

            requested.clear()
            requested += OTHER_ISIN
            advanceUntilIdle()

            val factory = deferred.await().success().viewModelFactory
            assertTrue(
                calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE) is
                    MarketDataCalculationUiResult.PurchasePrice
            )
            assertEquals(
                MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND,
                (calculate(factory, OTHER_ISIN, MarketDataCalculationType.PURCHASE_PRICE) as
                    MarketDataCalculationUiResult.Failure).error
            )
        }

    @Test
    fun hsbcFileMapIsCopiedBeforeFirstSuspension() = runTest(mainDispatcher) {
        val hsbcFiles = mutableMapOf(PRODUCT_ISIN to writeHsbcJson(PRODUCT_ISIN, "Call"))
        val deferred = createDeferred(validFiles(), hsbcFiles = hsbcFiles)

        hsbcFiles.clear()
        hsbcFiles[OTHER_ISIN] = writeHsbcJson(OTHER_ISIN, "Put")
        advanceUntilIdle()

        val factory = deferred.await().success().viewModelFactory
        assertTrue(
            calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE) is
                MarketDataCalculationUiResult.PurchasePrice
        )
        assertEquals(
            MarketDataCalculationUiError.PRODUCT_NOT_FOUND,
            (calculate(factory, OTHER_ISIN, MarketDataCalculationType.PURCHASE_PRICE) as
                MarketDataCalculationUiResult.Failure).error
        )
    }

    @Test
    fun injectedSpecificationDispatcherControlsHsbcLoading() = runTest(mainDispatcher) {
        val specificationDispatcher = StandardTestDispatcher(testScheduler)
        val deferred = createDeferred(
            validFiles(),
            specificationDispatcher = specificationDispatcher,
            marketDataDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        assertFalse(deferred.isCompleted)
        advanceUntilIdle()
        assertTrue(deferred.isCompleted)
        assertTrue(deferred.await() is HsbcDeutscheBoerseMarketDataDemoCompositionResult.Success)
    }

    @Test
    fun specificationRetrievedAtIsPreservedExactly() = runTest(mainDispatcher) {
        val factory = createSuccess(
            validFiles(),
            specificationRetrievedAt = -1L
        ).viewModelFactory

        assertEquals(-1L, snapshot(factory, PRODUCT_ISIN).retrievedAtEpochMillis)
    }

    @Test
    fun nullHsbcSourceTimestampIsNotReplaced() = runTest(mainDispatcher) {
        val factory = createSuccess(
            validFiles(),
            hsbcFiles = mapOf(
                PRODUCT_ISIN to writeHsbcJson(
                    PRODUCT_ISIN,
                    "Call",
                    sourceTimestampLiteral = "null"
                )
            )
        ).viewModelFactory
        val snapshot = snapshot(factory, PRODUCT_ISIN)

        assertEquals(SPECIFICATION_RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertEquals(null, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun temporaryHsbcAndMarketDataFilesCanBeDeletedAfterSuccess() = runTest(mainDispatcher) {
        val files = validFiles()
        val hsbcFile = writeHsbcJson(PRODUCT_ISIN, "Call")
        val factory = createSuccess(
            files,
            hsbcFiles = mapOf(PRODUCT_ISIN to hsbcFile)
        ).viewModelFactory

        calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE)

        assertTrue(hsbcFile.delete())
        assertTrue(files.dxsc.delete())
        assertTrue(files.xfra.delete())
    }

    @Test
    fun repeatedCompositionProducesEquivalentCalculationResults() = runTest(mainDispatcher) {
        val files = validFiles()
        val hsbcFile = writeHsbcJson(PRODUCT_ISIN, "Call")
        val hsbcFiles = mapOf(PRODUCT_ISIN to hsbcFile)

        val first = calculate(
            createSuccess(files, hsbcFiles = hsbcFiles).viewModelFactory,
            PRODUCT_ISIN,
            MarketDataCalculationType.PURCHASE_PRICE
        )
        val second = calculate(
            createSuccess(files, hsbcFiles = hsbcFiles).viewModelFactory,
            PRODUCT_ISIN,
            MarketDataCalculationType.PURCHASE_PRICE
        )

        assertEquals(first, second)
    }

    private fun TestScope.createDeferred(
        files: CompressedFiles,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN),
        hsbcFiles: Map<String, File> = mapOf(
            PRODUCT_ISIN to writeHsbcJson(PRODUCT_ISIN, "Call")
        ),
        specificationRetrievedAt: Long = SPECIFICATION_RETRIEVED_AT,
        specificationDispatcher: CoroutineDispatcher =
            UnconfinedTestDispatcher(testScheduler),
        marketDataDispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler)
    ) = async(start = CoroutineStart.UNDISPATCHED) {
        HsbcDeutscheBoerseMarketDataDemoComposition.createFactory(
            dxscGzipFile = files.dxsc,
            xfraZipFile = files.xfra,
            requestedProductIsins = requestedProductIsins,
            hsbcResearchJsonFilesByProductIsin = hsbcFiles,
            specificationRetrievedAtEpochMillis = specificationRetrievedAt,
            freshnessThresholds = freshnessThresholds(),
            specificationFileDispatcher = specificationDispatcher,
            marketDataRepositoryLoader =
                DeutscheBoerseCompressedFileMarketDataRepositoryLoader(
                    marketDataDispatcher
                )
        )
    }

    private suspend fun TestScope.createResult(
        files: CompressedFiles,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN),
        hsbcFiles: Map<String, File> = mapOf(
            PRODUCT_ISIN to writeHsbcJson(PRODUCT_ISIN, "Call")
        ),
        specificationRetrievedAt: Long = SPECIFICATION_RETRIEVED_AT
    ): HsbcDeutscheBoerseMarketDataDemoCompositionResult {
        val deferred = createDeferred(
            files = files,
            requestedProductIsins = requestedProductIsins,
            hsbcFiles = hsbcFiles,
            specificationRetrievedAt = specificationRetrievedAt
        )
        advanceUntilIdle()
        return deferred.await()
    }

    private suspend fun TestScope.createSuccess(
        files: CompressedFiles,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN),
        hsbcFiles: Map<String, File> = mapOf(
            PRODUCT_ISIN to writeHsbcJson(PRODUCT_ISIN, "Call")
        ),
        specificationRetrievedAt: Long = SPECIFICATION_RETRIEVED_AT
    ) = createResult(
        files,
        requestedProductIsins,
        hsbcFiles,
        specificationRetrievedAt
    ).success()

    private suspend fun TestScope.calculate(
        factory: MarketDataCalculationViewModelFactory,
        productIsin: String,
        calculationType: MarketDataCalculationType
    ): MarketDataCalculationUiResult {
        val viewModel = factory.create(MarketDataCalculationViewModel::class.java)
        viewModel.onProductIsinChanged(productIsin)
        viewModel.onCalculationTypeChanged(calculationType)
        viewModel.onEvaluationTimeChanged(evaluationTime().toString())
        viewModel.onCalculateClicked()
        advanceUntilIdle()
        return (viewModel.uiState.value.submission as
            MarketDataCalculationUiSubmission.Completed).result
    }

    private fun specificationRepository(
        factory: MarketDataCalculationViewModelFactory
    ): KnockoutProductSpecificationRepository {
        val service = readField<MarketDataCalculationApplicationService>(
            factory,
            "applicationService"
        )
        return readField(service, "specificationRepository")
    }

    private fun snapshot(
        factory: MarketDataCalculationViewModelFactory,
        productIsin: String
    ): KnockoutProductSpecificationSnapshotDto {
        val bridge = specificationRepository(factory) as
            SnapshotBackedKnockoutProductSpecificationRepository
        val remote = readField<RemoteKnockoutProductSpecificationSnapshotRepository>(
            bridge,
            "snapshotRepository"
        )
        val provider = readField<InMemoryKnockoutProductSpecificationSnapshotProvider>(
            remote,
            "provider"
        )
        val snapshots = readField<Map<String, KnockoutProductSpecificationSnapshotDto>>(
            provider,
            "snapshotsByProductIsin"
        )
        return snapshots.getValue(productIsin)
    }

    private fun specificationProcessingFailure(
        result: HsbcDeutscheBoerseMarketDataDemoCompositionResult
    ): HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
        .ProcessingFailure {
        val failure = result as HsbcDeutscheBoerseMarketDataDemoCompositionResult.Failure
        val loading = (failure.error as
            HsbcDeutscheBoerseMarketDataDemoCompositionError.SpecificationLoading).error as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                .ProviderCreation
        return loading.errors.single() as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                .ProcessingFailure
    }

    private fun HsbcDeutscheBoerseMarketDataDemoCompositionResult.success() =
        this as HsbcDeutscheBoerseMarketDataDemoCompositionResult.Success

    private fun freshnessThresholds() = MarketDataFreshnessThresholds(
        maxBidAgeMillis = FRESHNESS_LIMIT_MILLIS,
        maxAskAgeMillis = FRESHNESS_LIMIT_MILLIS,
        maxBidAskDifferenceMillis = FRESHNESS_LIMIT_MILLIS,
        allowedFutureSkewMillis = 0L
    )

    private fun validFiles() = files(
        xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
        dxscLines = listOf(
            dxscLine(PRODUCT_ISIN, bid = 1.80, ask = 2.00, timestamp = OLDER_TIMESTAMP),
            dxscLine(PRODUCT_ISIN, bid = 2.00, ask = 2.05, timestamp = NEWER_TIMESTAMP)
        )
    )

    private fun files(
        xfraLines: List<String>,
        dxscLines: List<String>
    ) = CompressedFiles(
        dxsc = writeDxscGzip(dxscLines),
        xfra = writeXfraZip(xfraLines)
    )

    private fun writeHsbcJson(
        productIsin: String,
        directionLabel: String,
        sourceTimestampLiteral: String = SOURCE_TIMESTAMP.toString()
    ) = writeTextFile(
        "hsbc-${fileCounter++}.json",
        """{
            "productIsin":"$productIsin",
            "productWkn":"SYN001",
            "issuerId":"synthetic-provider",
            "underlyingId":"synthetic-underlying",
            "directionLabel":"$directionLabel",
            "basePrice":80.125,
            "knockoutBarrier":82.5,
            "ratio":0.1,
            "underlyingCurrency":"USD",
            "productCurrency":"EUR",
            "sourceTimestampEpochMillis":$sourceTimestampLiteral
        }""".trimIndent()
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

    private fun writeTextFile(name: String, content: String): File =
        temporaryFolder.newFile(name).also { it.writeText(content, StandardCharsets.UTF_8) }

    private fun missingFile(name: String) = File(temporaryFolder.root, name)

    private fun csvContent(lines: List<String>) =
        listOf("Market:;XFRA", "Date Last Update:;27.07.2026", validHeader())
            .plus(lines)
            .joinToString(separator = "\n", postfix = "\n")

    private fun validHeader() = DeutscheBoerseXfraRequiredColumn.entries
        .joinToString(";") { it.headerName }

    private fun xfraLine(isin: String) = listOf(
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
        bid: Double = 2.00,
        ask: Double = 2.05,
        timestamp: String = NEWER_TIMESTAMP
    ) =
        """{"messageId":"pretrade","instrumentIdentificationCode":"$isin","bestBid":$bid,"bestAsk":$ask,"updateDateAndTime":"$timestamp"}"""

    private fun evaluationTime() = Instant.parse(NEWER_TIMESTAMP).toEpochMilli()

    @Suppress("UNCHECKED_CAST")
    private fun <T> readField(instance: Any, fieldName: String): T =
        instance.javaClass.getDeclaredField(fieldName)
            .apply { isAccessible = true }
            .get(instance) as T

    private data class CompressedFiles(
        val dxsc: File,
        val xfra: File
    )

    private var fileCounter = 0

    private companion object {
        const val PRODUCT_ISIN = "DE000SYNTH01"
        const val OTHER_ISIN = "DE000SYNTH02"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val FRESHNESS_LIMIT_MILLIS = 1_000L
        const val SPECIFICATION_RETRIEVED_AT = 1_700_000_000_500L
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L
    }
}
