package de.konavigator.app.debug.marketdata

import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryLoader
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseDxscNdjsonLoadingError
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseDxscNdjsonLoadingErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraCsvLoadingError
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraCsvLoadingErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraRequiredColumn
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class DeutscheBoerseCompressedFileMarketDataDemoCompositionTest {

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
    fun compositionUsesInjectedRepositoryLoaderDispatcher() {
        val dispatcher = StandardTestDispatcher()
        val loader = DeutscheBoerseCompressedFileMarketDataRepositoryLoader(dispatcher)

        assertSame(dispatcher, readField<CoroutineDispatcher>(loader, "ioDispatcher"))
    }

    @Test
    fun createFactoryIsNotCompletedBeforeTestDispatcherRuns() = runTest(mainDispatcher) {
        val files = validFiles()
        val deferred = createDeferred(files)

        assertFalse(deferred.isCompleted)
    }

    @Test
    fun advanceUntilIdleDeliversTypedResult() = runTest(mainDispatcher) {
        val deferred = createDeferred(validFiles())

        advanceUntilIdle()

        assertTrue(deferred.await() is DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success)
    }

    @Test
    fun validCompressedFilesProduceSuccess() = runTest(mainDispatcher) {
        assertTrue(successType.isInstance(createResult(validFiles())))
    }

    @Test
    fun successContainsViewModelFactory() = runTest(mainDispatcher) {
        assertNotNull(createSuccess(validFiles()).viewModelFactory)
    }

    @Test
    fun factoryCreatesMarketDataCalculationViewModel() = runTest(mainDispatcher) {
        val viewModel = createSuccess(validFiles()).viewModelFactory
            .create(MarketDataCalculationViewModel::class.java)

        assertEquals(MarketDataCalculationViewModel::class.java, viewModel.javaClass)
    }

    @Test
    fun purchasePriceCalculationProducesTwoPointZeroFiveEuro() = runTest(mainDispatcher) {
        val result = calculate(
            factory = createSuccess(validFiles()).viewModelFactory,
            productIsin = PRODUCT_ISIN,
            calculationType = MarketDataCalculationType.PURCHASE_PRICE
        )

        assertEquals(2.05, (result as MarketDataCalculationUiResult.PurchasePrice).value, 0.0)
        assertEquals("EUR", result.currency)
    }

    @Test
    fun newestDxscAskIsUsedInUiResult() = runTest(mainDispatcher) {
        val result = calculate(
            factory = createSuccess(validFiles()).viewModelFactory,
            productIsin = PRODUCT_ISIN,
            calculationType = MarketDataCalculationType.PURCHASE_PRICE
        ) as MarketDataCalculationUiResult.PurchasePrice

        assertEquals(2.05, result.value, 0.0)
        assertFalse(result.value == 2.0)
    }

    @Test
    fun successfulUiResultContainsPassedDataQuality() = runTest(mainDispatcher) {
        val result = calculate(
            factory = createSuccess(validFiles()).viewModelFactory,
            productIsin = PRODUCT_ISIN,
            calculationType = MarketDataCalculationType.PURCHASE_PRICE
        )

        assertFalse(result is MarketDataCalculationUiResult.Failure)
        assertEquals(MarketDataCalculationUiDataQualityStatus.PASSED, result.dataQuality?.status)
    }

    @Test
    fun configuredDeutscheBoerseSourceIdAllowsPurchasePrice() = runTest(mainDispatcher) {
        val result = calculate(
            factory = createSuccess(validFiles()).viewModelFactory,
            productIsin = PRODUCT_ISIN,
            calculationType = MarketDataCalculationType.PURCHASE_PRICE
        )

        assertTrue(result is MarketDataCalculationUiResult.PurchasePrice)
    }

    @Test
    fun sourceRuleAllowsAllFourCalculationTypes() = runTest(mainDispatcher) {
        val factory = createSuccess(validFiles()).viewModelFactory
        val expectedTypes = mapOf(
            MarketDataCalculationType.PURCHASE_PRICE to
                MarketDataCalculationUiResult.PurchasePrice::class.java,
            MarketDataCalculationType.SALE_PRICE to
                MarketDataCalculationUiResult.SalePrice::class.java,
            MarketDataCalculationType.SPREAD to MarketDataCalculationUiResult.Spread::class.java,
            MarketDataCalculationType.MID to MarketDataCalculationUiResult.MidPrice::class.java
        )

        expectedTypes.forEach { (calculationType, expectedType) ->
            assertTrue(
                expectedType.isInstance(calculate(factory, PRODUCT_ISIN, calculationType))
            )
        }
    }

    @Test
    fun unknownSpecificationIsinProducesProductNotFound() = runTest(mainDispatcher) {
        val factory = createSuccess(
            files = validFiles(),
            specificationDtos = emptyMap()
        ).viewModelFactory

        assertEquals(
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.PRODUCT_NOT_FOUND,
                dataQuality = null
            ),
            calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE)
        )
    }

    @Test
    fun specificationWithoutRequestedMarketDataProducesMarketDataNotFound() =
        runTest(mainDispatcher) {
            val factory = createSuccess(
                files = validFiles(),
                requestedProductIsins = emptySet()
            ).viewModelFactory

            assertEquals(
                MarketDataCalculationUiResult.Failure(
                    error = MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND,
                    dataQuality = null
                ),
                calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE)
            )
        }

    @Test
    fun missingXfraFilePreservesFailure() = runTest(mainDispatcher) {
        val result = createResult(
            files = CompressedFiles(
                dxsc = missingFile("dxsc.gz"),
                xfra = missingFile("xfra.zip")
            )
        ) as DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Failure

        assertEquals(DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED, result.error.code)
        assertEquals(
            DeutscheBoerseXfraCsvLoadingError(
                code = DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            result.error.xfraLoadingError
        )
    }

    @Test
    fun validXfraAndMissingDxscPreserveFailure() = runTest(mainDispatcher) {
        val result = createResult(
            files = CompressedFiles(
                dxsc = missingFile("dxsc.gz"),
                xfra = writeXfraZip(listOf(xfraLine(PRODUCT_ISIN)))
            )
        ) as DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Failure

        assertEquals(DeutscheBoerseSnapshotProviderCreationErrorCode.DXSC_LOADING_FAILED, result.error.code)
        assertEquals(
            DeutscheBoerseDxscNdjsonLoadingError(
                code = DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
                lineNumber = 1L
            ),
            result.error.dxscLoadingError
        )
    }

    @Test
    fun failureContainsNoViewModelFactory() = runTest(mainDispatcher) {
        val result = createResult(
            CompressedFiles(missingFile("dxsc.gz"), missingFile("xfra.zip"))
        )

        assertTrue(result is DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Failure)
        assertFalse(result is DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success)
    }

    @Test
    fun specificationMapIsCopiedBeforeRepositoryLoaderSuspends() = runTest(mainDispatcher) {
        val files = validFiles()
        val specifications = mutableMapOf(PRODUCT_ISIN to specificationDto(PRODUCT_ISIN))
        val deferred = createDeferred(files, specificationDtos = specifications)

        specifications.clear()
        specifications[OTHER_ISIN] = specificationDto(OTHER_ISIN)
        advanceUntilIdle()

        val factory = deferred.await().success().viewModelFactory
        assertTrue(
            calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE) is
                MarketDataCalculationUiResult.PurchasePrice
        )
    }

    @Test
    fun requestedIsinsAreCopiedBeforeRepositoryLoaderSuspends() = runTest(mainDispatcher) {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(OTHER_ISIN)),
            dxscLines = listOf(dxscLine(PRODUCT_ISIN), dxscLine(OTHER_ISIN))
        )
        val requestedIsins = mutableSetOf(PRODUCT_ISIN)
        val specifications = mapOf(
            PRODUCT_ISIN to specificationDto(PRODUCT_ISIN),
            OTHER_ISIN to specificationDto(OTHER_ISIN)
        )
        val deferred = createDeferred(
            files = files,
            requestedProductIsins = requestedIsins,
            specificationDtos = specifications
        )

        requestedIsins.clear()
        requestedIsins.add(OTHER_ISIN)
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
    fun whitespaceAndCaseInIsinsAreNotNormalized() = runTest(mainDispatcher) {
        val whitespaceIsin = " $PRODUCT_ISIN "
        val lowercaseIsin = PRODUCT_ISIN.lowercase()
        val files = files(
            xfraLines = listOf(xfraLine(whitespaceIsin), xfraLine(lowercaseIsin)),
            dxscLines = listOf(dxscLine(whitespaceIsin), dxscLine(lowercaseIsin))
        )
        val factory = createSuccess(
            files = files,
            requestedProductIsins = setOf(whitespaceIsin, lowercaseIsin),
            specificationDtos = mapOf(
                whitespaceIsin to specificationDto(whitespaceIsin),
                lowercaseIsin to specificationDto(lowercaseIsin)
            )
        ).viewModelFactory

        listOf(whitespaceIsin, lowercaseIsin).forEach { exactIsin ->
            val result = calculate(
                factory,
                exactIsin,
                MarketDataCalculationType.PURCHASE_PRICE
            )
            assertTrue(result is MarketDataCalculationUiResult.PurchasePrice)
        }
        assertEquals(
            MarketDataCalculationUiError.PRODUCT_NOT_FOUND,
            (calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE) as
                MarketDataCalculationUiResult.Failure).error
        )
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterCompositionAndCalculation() = runTest(mainDispatcher) {
        val files = validFiles()
        val factory = createSuccess(files).viewModelFactory

        calculate(factory, PRODUCT_ISIN, MarketDataCalculationType.PURCHASE_PRICE)

        assertTrue(files.xfra.delete())
        assertTrue(files.dxsc.delete())
    }

    @Test
    fun existingInMemoryDemoCompositionRemainsOperational() = runTest(mainDispatcher) {
        val factory = MarketDataCalculationDemoComposition.createFactory()
        val result = calculate(
            factory = factory,
            productIsin = "DE000DEMO001",
            calculationType = MarketDataCalculationType.PURCHASE_PRICE,
            evaluationTimeEpochMillis = 1_700_000_000_000L
        )

        assertEquals(2.0, (result as MarketDataCalculationUiResult.PurchasePrice).value, 0.0)
    }

    private fun TestScope.createDeferred(
        files: CompressedFiles,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN),
        specificationDtos: Map<String, KnockoutProductSpecificationDto> =
            mapOf(PRODUCT_ISIN to specificationDto(PRODUCT_ISIN))
    ) = async(start = CoroutineStart.UNDISPATCHED) {
        DeutscheBoerseCompressedFileMarketDataDemoComposition.createFactory(
            dxscGzipFile = files.dxsc,
            xfraZipFile = files.xfra,
            requestedProductIsins = requestedProductIsins,
            specificationDtos = specificationDtos,
            freshnessThresholds = freshnessThresholds(),
            repositoryLoader = DeutscheBoerseCompressedFileMarketDataRepositoryLoader(
                StandardTestDispatcher(testScheduler)
            )
        )
    }

    private suspend fun TestScope.createResult(
        files: CompressedFiles,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN),
        specificationDtos: Map<String, KnockoutProductSpecificationDto> =
            mapOf(PRODUCT_ISIN to specificationDto(PRODUCT_ISIN))
    ): DeutscheBoerseCompressedFileMarketDataDemoCompositionResult {
        val deferred = createDeferred(files, requestedProductIsins, specificationDtos)
        advanceUntilIdle()
        return deferred.await()
    }

    private suspend fun TestScope.createSuccess(
        files: CompressedFiles,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN),
        specificationDtos: Map<String, KnockoutProductSpecificationDto> =
            mapOf(PRODUCT_ISIN to specificationDto(PRODUCT_ISIN))
    ): DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success =
        createResult(files, requestedProductIsins, specificationDtos).success()

    private suspend fun TestScope.calculate(
        factory: MarketDataCalculationViewModelFactory,
        productIsin: String,
        calculationType: MarketDataCalculationType,
        evaluationTimeEpochMillis: Long = evaluationTime()
    ): MarketDataCalculationUiResult {
        val viewModel = factory.create(MarketDataCalculationViewModel::class.java)
        viewModel.onProductIsinChanged(productIsin)
        viewModel.onCalculationTypeChanged(calculationType)
        viewModel.onEvaluationTimeChanged(evaluationTimeEpochMillis.toString())
        viewModel.onCalculateClicked()
        advanceUntilIdle()
        return (viewModel.uiState.value.submission as
            MarketDataCalculationUiSubmission.Completed).result
    }

    private fun DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.success():
        DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success =
        this as DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success

    private fun specificationDto(productIsin: String) = KnockoutProductSpecificationDto(
        productIsin = productIsin,
        productWkn = "TEST01",
        issuerId = "test-issuer",
        underlyingId = "test-underlying",
        direction = "LONG",
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )

    private fun freshnessThresholds() = MarketDataFreshnessThresholds(
        maxBidAgeMillis = FRESHNESS_LIMIT_MILLIS,
        maxAskAgeMillis = FRESHNESS_LIMIT_MILLIS,
        maxBidAskDifferenceMillis = FRESHNESS_LIMIT_MILLIS,
        allowedFutureSkewMillis = 0L
    )

    private fun validFiles(): CompressedFiles = files(
        xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
        dxscLines = listOf(
            dxscLine(PRODUCT_ISIN, bid = 1.80, ask = 2.00, timestamp = OLDER_TIMESTAMP),
            dxscLine(PRODUCT_ISIN, bid = 1.85, ask = 2.05, timestamp = NEWER_TIMESTAMP)
        )
    )

    private fun files(
        xfraLines: List<String>,
        dxscLines: List<String>
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
        listOf("Market:;XFRA", "Date Last Update:;27.07.2026", validHeader())
            .plus(lines)
            .joinToString(separator = "\n", postfix = "\n")

    private fun validHeader(): String = DeutscheBoerseXfraRequiredColumn.entries
        .joinToString(";") { it.headerName }

    private fun xfraLine(isin: String): String = listOf(
        "Active",
        "Tradable",
        "Synthetic Instrument",
        isin,
        "TEST01",
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
        bid: Double = 1.85,
        ask: Double = 2.05,
        timestamp: String = NEWER_TIMESTAMP
    ): String =
        """{"messageId":"pretrade","instrumentIdentificationCode":"$isin","bestBid":$bid,"bestAsk":$ask,"updateDateAndTime":"$timestamp"}"""

    private fun evaluationTime(): Long = Instant.parse(NEWER_TIMESTAMP).toEpochMilli()

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
        const val PRODUCT_ISIN = "DE000TEST001"
        const val OTHER_ISIN = "DE000TEST002"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val FRESHNESS_LIMIT_MILLIS = 1_000L

        val successType =
            DeutscheBoerseCompressedFileMarketDataDemoCompositionResult.Success::class.java
    }
}
