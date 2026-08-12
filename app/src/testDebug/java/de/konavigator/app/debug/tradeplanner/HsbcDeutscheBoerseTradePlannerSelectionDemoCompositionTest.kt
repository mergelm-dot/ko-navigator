package de.konavigator.app.debug.tradeplanner

import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.FxRateProviderResult
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryLoader
import de.konavigator.app.data.remote.RemoteKnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProvider
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProviderResult
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseKnockoutProductMarketDataMapper
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationErrorCode
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraRequiredColumn
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode
import de.konavigator.app.data.remote.provider.hsbc.HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionCurrencyEvidence
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionEvaluationTimeProvider
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionExecutionSettings
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiNoSelectionReason
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiResult
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiSubmission
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionViewModel
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionTest {

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
    fun controlledLocalPathsSelectOnlyBrokerTradableCandidate() = runTest(mainDispatcher) {
        val brokerProvider = RecordingBrokerAvailabilityProvider(
            KnockoutProductBrokerAvailabilityProviderResult.Success(setOf(PRODUCT_A))
        )
        val fxRateProvider = RecordingFxRateProvider()
        val factory = createSuccess(
            files = validFiles(PRODUCT_A, PRODUCT_B),
            hsbcFiles = matchingAndNonMatchingHsbcFiles(),
            brokerAvailabilityRepository = RemoteKnockoutProductBrokerAvailabilityRepository(
                brokerProvider
            ),
            fxRateProvider = fxRateProvider
        ).viewModelFactory

        val viewModel = factory.create(TradePlannerSelectionViewModel::class.java)
        assertEquals(TradePlannerSelectionViewModel::class.java, viewModel.javaClass)

        configureValidInput(viewModel)
        viewModel.onCalculateClicked()
        runCurrent()

        val result = completedResult(viewModel)
        assertTrue(result is TradePlannerSelectionUiResult.Selected)
        val selected = result as TradePlannerSelectionUiResult.Selected
        val primary = selected.primaryCandidate
        assertEquals(PRODUCT_A, primary.productIsin)
        assertEquals("SYN001", primary.productWkn)
        assertEquals("synthetic-issuer", primary.issuerId)
        assertEquals("EUR", primary.productCurrency)
        assertEquals(1.0, primary.calculatedProductPriceAtPlannedEntry, 0.0)
        assertEquals(10.0, primary.calculatedLeverageAtPlannedEntry, 0.0)
        assertEquals(80.0, primary.knockoutBarrier, 0.0)
        assertSame(TradePlannerSelectionCurrencyEvidence.SameCurrency, primary.currencyEvidence)
        assertTrue(selected.alternativeCandidates.isEmpty())
        assertEquals(BROKER_ID, brokerProvider.brokerIds.single())
        assertEquals(listOf(PRODUCT_A, PRODUCT_B), brokerProvider.productIsinLists.single())
        assertTrue(PRODUCT_OTHER_UNDERLYING !in brokerProvider.productIsinLists.single())
        assertTrue(PRODUCT_SHORT !in brokerProvider.productIsinLists.single())
        assertTrue(fxRateProvider.requests.isEmpty())
    }

    @Test
    fun noBrokerTradableProductsCompletesWithoutFxRequest() = runTest(mainDispatcher) {
        val brokerProvider = RecordingBrokerAvailabilityProvider(
            KnockoutProductBrokerAvailabilityProviderResult.Success(emptySet())
        )
        val fxRateProvider = RecordingFxRateProvider()
        val factory = createSuccess(
            files = validFiles(PRODUCT_A, PRODUCT_B),
            hsbcFiles = matchingAndNonMatchingHsbcFiles(),
            brokerAvailabilityRepository = RemoteKnockoutProductBrokerAvailabilityRepository(
                brokerProvider
            ),
            fxRateProvider = fxRateProvider
        ).viewModelFactory
        val viewModel = factory.create(TradePlannerSelectionViewModel::class.java)

        configureValidInput(viewModel)
        viewModel.onCalculateClicked()
        runCurrent()

        assertNoSelection(
            viewModel = viewModel,
            reason = TradePlannerSelectionUiNoSelectionReason.NO_BROKER_TRADABLE_CANDIDATES
        )
        assertEquals(listOf(PRODUCT_A, PRODUCT_B), brokerProvider.productIsinLists.single())
        assertTrue(fxRateProvider.requests.isEmpty())
    }

    @Test
    fun noCatalogCandidatesDoesNotQueryBrokerOrFx() = runTest(mainDispatcher) {
        val brokerProvider = RecordingBrokerAvailabilityProvider(
            KnockoutProductBrokerAvailabilityProviderResult.Success(setOf(PRODUCT_A))
        )
        val fxRateProvider = RecordingFxRateProvider()
        val factory = createSuccess(
            files = validFiles(PRODUCT_OTHER_UNDERLYING, PRODUCT_SHORT),
            hsbcFiles = linkedMapOf(
                PRODUCT_OTHER_UNDERLYING to writeHsbcJson(
                    productIsin = PRODUCT_OTHER_UNDERLYING,
                    underlyingId = OTHER_UNDERLYING_ID,
                    directionLabel = "Call",
                    productWkn = "SYN003"
                ),
                PRODUCT_SHORT to writeHsbcJson(
                    productIsin = PRODUCT_SHORT,
                    underlyingId = UNDERLYING_ID,
                    directionLabel = "Put",
                    productWkn = "SYN004"
                )
            ),
            brokerAvailabilityRepository = RemoteKnockoutProductBrokerAvailabilityRepository(
                brokerProvider
            ),
            fxRateProvider = fxRateProvider
        ).viewModelFactory
        val viewModel = factory.create(TradePlannerSelectionViewModel::class.java)

        configureValidInput(viewModel)
        viewModel.onCalculateClicked()
        runCurrent()

        assertNoSelection(
            viewModel = viewModel,
            reason = TradePlannerSelectionUiNoSelectionReason.NO_CATALOG_CANDIDATES
        )
        assertTrue(brokerProvider.brokerIds.isEmpty())
        assertTrue(fxRateProvider.requests.isEmpty())
    }

    @Test
    fun missingHsbcFilePreservesTypedFileLoadingFailure() = runTest(mainDispatcher) {
        val result = createResult(
            files = validFiles(PRODUCT_A),
            hsbcFiles = mapOf(PRODUCT_A to missingFile("missing-hsbc.json"))
        )

        assertTrue(result is HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure)
        val error = (result as HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure)
            .error as HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError.HsbcFileLoading
        assertEquals(PRODUCT_A, error.errors.single().productIsinKey)
        assertEquals(
            HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode.FILE_READING_FAILED,
            error.errors.single().code
        )
    }

    @Test
    fun mismatchingHsbcMapKeyAndEmbeddedIsinPreservesTypedCatalogCreationFailure() =
        runTest(mainDispatcher) {
            val result = createResult(
                files = validFiles(PRODUCT_A),
                hsbcFiles = mapOf(
                    PRODUCT_A to writeHsbcJson(
                        productIsin = PRODUCT_B,
                        underlyingId = UNDERLYING_ID,
                        directionLabel = "Call",
                        productWkn = "SYN001"
                    )
                )
            )

            assertTrue(
                result is HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure
            )
            val error =
                (result as HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure)
                    .error as
                    HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError
                        .HsbcCatalogProviderCreation
            assertEquals(
                HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                    .ProductIsinMismatch(PRODUCT_A, PRODUCT_B),
                error.errors.single()
            )
        }

    @Test
    fun missingDeutscheBoerseFilesPreserveTypedMarketDataLoadingFailure() =
        runTest(mainDispatcher) {
            val result = createResult(
                files = CompressedFiles(
                    dxsc = missingFile("missing-dxsc.gz"),
                    xfra = missingFile("missing-xfra.zip")
                ),
                hsbcFiles = mapOf(
                    PRODUCT_A to writeHsbcJson(
                        productIsin = PRODUCT_A,
                        underlyingId = UNDERLYING_ID,
                        directionLabel = "Call",
                        productWkn = "SYN001"
                    )
                )
            )

            assertTrue(
                result is HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure
            )
            val error =
                (result as HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure)
                    .error as
                    HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError.MarketDataLoading
            assertEquals(
                DeutscheBoerseSnapshotProviderCreationErrorCode.XFRA_LOADING_FAILED,
                error.error.code
            )
        }

    @Test
    fun hsbcFailureOccursBeforeDeutscheBoerseLoading() = runTest(mainDispatcher) {
        val result = createResult(
            files = CompressedFiles(
                dxsc = missingFile("missing-dxsc.gz"),
                xfra = missingFile("missing-xfra.zip")
            ),
            hsbcFiles = mapOf(PRODUCT_A to missingFile("missing-hsbc.json"))
        )

        assertTrue(result is HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure)
        assertTrue(
            (result as HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure)
                .error is
                HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError.HsbcFileLoading
        )
    }

    @Test
    fun copiesHsbcFileMapBeforeFirstSuspension() = runTest(mainDispatcher) {
        val hsbcFiles = linkedMapOf(
            PRODUCT_A to writeHsbcJson(
                productIsin = PRODUCT_A,
                underlyingId = UNDERLYING_ID,
                directionLabel = "Call",
                productWkn = "SYN001"
            )
        )
        val brokerProvider = RecordingBrokerAvailabilityProvider(
            KnockoutProductBrokerAvailabilityProviderResult.Success(setOf(PRODUCT_A))
        )
        val deferred = createDeferred(
            files = validFiles(PRODUCT_A),
            hsbcFiles = hsbcFiles,
            brokerAvailabilityRepository = RemoteKnockoutProductBrokerAvailabilityRepository(
                brokerProvider
            ),
            specificationFileDispatcher = StandardTestDispatcher(testScheduler)
        )

        hsbcFiles.clear()
        hsbcFiles[PRODUCT_B] = writeHsbcJson(
            productIsin = PRODUCT_B,
            underlyingId = UNDERLYING_ID,
            directionLabel = "Call",
            productWkn = "SYN002"
        )
        advanceUntilIdle()

        val factory = deferred.await().success().viewModelFactory
        val viewModel = factory.create(TradePlannerSelectionViewModel::class.java)
        configureValidInput(viewModel)
        viewModel.onCalculateClicked()
        runCurrent()

        val result = completedResult(viewModel)
        assertTrue(result is TradePlannerSelectionUiResult.Selected)
        assertEquals(PRODUCT_A, (result as TradePlannerSelectionUiResult.Selected).primaryCandidate.productIsin)
        assertEquals(listOf(PRODUCT_A), brokerProvider.productIsinLists.single())
    }

    private fun TestScope.createDeferred(
        files: CompressedFiles,
        hsbcFiles: Map<String, File>,
        brokerAvailabilityRepository: RemoteKnockoutProductBrokerAvailabilityRepository =
            RemoteKnockoutProductBrokerAvailabilityRepository(
                RecordingBrokerAvailabilityProvider(
                    KnockoutProductBrokerAvailabilityProviderResult.Success(setOf(PRODUCT_A))
                )
            ),
        fxRateProvider: RecordingFxRateProvider = RecordingFxRateProvider(),
        specificationFileDispatcher: CoroutineDispatcher =
            UnconfinedTestDispatcher(testScheduler),
        marketDataDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
    ) = async(start = CoroutineStart.UNDISPATCHED) {
        HsbcDeutscheBoerseTradePlannerSelectionDemoComposition.createFactory(
            dxscGzipFile = files.dxsc,
            xfraZipFile = files.xfra,
            hsbcResearchJsonFilesByProductIsin = hsbcFiles,
            specificationRetrievedAtEpochMillis = SPECIFICATION_RETRIEVED_AT,
            brokerAvailabilityRepository = brokerAvailabilityRepository,
            fxRateProvider = fxRateProvider,
            freshnessPolicy = freshnessPolicy(),
            sourcePolicy = sourcePolicy(),
            executionSettings = executionSettings(),
            evaluationTimeProvider = TradePlannerSelectionEvaluationTimeProvider {
                evaluationTimeEpochMillis()
            },
            specificationFileDispatcher = specificationFileDispatcher,
            marketDataRepositoryLoader = DeutscheBoerseCompressedFileMarketDataRepositoryLoader(
                marketDataDispatcher
            )
        )
    }

    private suspend fun TestScope.createResult(
        files: CompressedFiles,
        hsbcFiles: Map<String, File>,
        brokerAvailabilityRepository: RemoteKnockoutProductBrokerAvailabilityRepository =
            RemoteKnockoutProductBrokerAvailabilityRepository(
                RecordingBrokerAvailabilityProvider(
                    KnockoutProductBrokerAvailabilityProviderResult.Success(setOf(PRODUCT_A))
                )
            ),
        fxRateProvider: RecordingFxRateProvider = RecordingFxRateProvider()
    ): HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult {
        val deferred = createDeferred(
            files = files,
            hsbcFiles = hsbcFiles,
            brokerAvailabilityRepository = brokerAvailabilityRepository,
            fxRateProvider = fxRateProvider
        )
        advanceUntilIdle()
        return deferred.await()
    }

    private suspend fun TestScope.createSuccess(
        files: CompressedFiles,
        hsbcFiles: Map<String, File>,
        brokerAvailabilityRepository: RemoteKnockoutProductBrokerAvailabilityRepository =
            RemoteKnockoutProductBrokerAvailabilityRepository(
                RecordingBrokerAvailabilityProvider(
                    KnockoutProductBrokerAvailabilityProviderResult.Success(setOf(PRODUCT_A))
                )
            ),
        fxRateProvider: RecordingFxRateProvider = RecordingFxRateProvider()
    ): HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Success =
        createResult(
            files = files,
            hsbcFiles = hsbcFiles,
            brokerAvailabilityRepository = brokerAvailabilityRepository,
            fxRateProvider = fxRateProvider
        ).success()

    private fun configureValidInput(viewModel: TradePlannerSelectionViewModel) {
        viewModel.onUnderlyingSelected(UNDERLYING_ID)
        viewModel.onBrokerSelected(BROKER_ID)
        viewModel.onEnabledIssuerIdsChanged(setOf("synthetic-issuer"))
        viewModel.onCurrentPriceChanged("100")
        viewModel.onPlannedEntryPriceChanged("100")
        viewModel.onTargetLeverageChanged("5")
        viewModel.onDirectionChanged(TradeDirection.LONG)
    }

    private fun completedResult(viewModel: TradePlannerSelectionViewModel): TradePlannerSelectionUiResult {
        val submission = viewModel.uiState.value.submission
        assertTrue(submission is TradePlannerSelectionUiSubmission.Completed)
        return (submission as TradePlannerSelectionUiSubmission.Completed).result
    }

    private fun assertNoSelection(
        viewModel: TradePlannerSelectionViewModel,
        reason: TradePlannerSelectionUiNoSelectionReason
    ) {
        val result = completedResult(viewModel)
        assertTrue(result is TradePlannerSelectionUiResult.NoSelection)
        assertEquals(reason, (result as TradePlannerSelectionUiResult.NoSelection).reason)
    }

    private fun HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.success() =
        this as HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Success

    private fun matchingAndNonMatchingHsbcFiles() = linkedMapOf(
        PRODUCT_A to writeHsbcJson(
            productIsin = PRODUCT_A,
            underlyingId = UNDERLYING_ID,
            directionLabel = "Call",
            productWkn = "SYN001"
        ),
        PRODUCT_B to writeHsbcJson(
            productIsin = PRODUCT_B,
            underlyingId = UNDERLYING_ID,
            directionLabel = "Call",
            productWkn = "SYN002"
        ),
        PRODUCT_OTHER_UNDERLYING to writeHsbcJson(
            productIsin = PRODUCT_OTHER_UNDERLYING,
            underlyingId = OTHER_UNDERLYING_ID,
            directionLabel = "Call",
            productWkn = "SYN003"
        ),
        PRODUCT_SHORT to writeHsbcJson(
            productIsin = PRODUCT_SHORT,
            underlyingId = UNDERLYING_ID,
            directionLabel = "Put",
            productWkn = "SYN004"
        )
    )

    private fun validFiles(vararg productIsins: String): CompressedFiles = files(
        dxscLines = productIsins.map(::dxscLine),
        xfraLines = productIsins.map(::xfraLine)
    )

    private fun files(
        dxscLines: List<String>,
        xfraLines: List<String>
    ) = CompressedFiles(
        dxsc = writeDxscGzip(dxscLines),
        xfra = writeXfraZip(xfraLines)
    )

    private fun writeHsbcJson(
        productIsin: String,
        underlyingId: String,
        directionLabel: String,
        productWkn: String
    ) = writeTextFile(
        "hsbc-${fileCounter++}.json",
        """{
            "productIsin":"$productIsin",
            "productWkn":"$productWkn",
            "issuerId":"synthetic-issuer",
            "underlyingId":"$underlyingId",
            "directionLabel":"$directionLabel",
            "basePrice":90.0,
            "knockoutBarrier":80.0,
            "ratio":0.1,
            "underlyingCurrency":"EUR",
            "productCurrency":"EUR",
            "sourceTimestampEpochMillis":$SOURCE_TIMESTAMP
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

    private fun dxscLine(isin: String) =
        """{"messageId":"pretrade","instrumentIdentificationCode":"$isin","bestBid":1.0,"bestAsk":1.1,"updateDateAndTime":"$MARKET_DATA_TIMESTAMP"}"""

    private fun freshnessPolicy() = MarketDataFreshnessPolicy(
        MarketDataFreshnessThresholds(
            maxBidAgeMillis = 1_000L,
            maxAskAgeMillis = 1_000L,
            maxBidAskDifferenceMillis = 1_000L,
            allowedFutureSkewMillis = 0L
        )
    )

    private fun sourcePolicy() = MarketDataSourcePolicy(
        MarketDataSourcePolicyConfig(
            rules = listOf(
                MarketDataSourceRule(
                    sourceId = DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID,
                    supportedCalculationTypes = setOf(MarketDataCalculationType.MID)
                )
            )
        )
    )

    private fun executionSettings() = TradePlannerSelectionExecutionSettings(
        calculationType = MarketDataCalculationType.MID,
        maxFxAgeMillis = 1_000L,
        maxRelativeLeverageDeviationPercent = 100.0,
        maxBarrierDeviationPercentOfPlannedEntry = 10.0
    )

    private fun evaluationTimeEpochMillis() = Instant.parse(MARKET_DATA_TIMESTAMP).toEpochMilli()

    private class RecordingBrokerAvailabilityProvider(
        private val result: KnockoutProductBrokerAvailabilityProviderResult
    ) : KnockoutProductBrokerAvailabilityProvider {
        val brokerIds = mutableListOf<String>()
        val productIsinLists = mutableListOf<List<String>>()

        override suspend fun findTradableProductIsins(
            brokerId: String,
            productIsins: List<String>
        ): KnockoutProductBrokerAvailabilityProviderResult {
            brokerIds += brokerId
            productIsinLists += productIsins
            return result
        }
    }

    private class RecordingFxRateProvider : FxRateProvider {
        val requests = mutableListOf<Pair<CurrencyCode, CurrencyCode>>()

        override suspend fun findRate(
            underlyingCurrency: CurrencyCode,
            productCurrency: CurrencyCode
        ): FxRateProviderResult {
            requests += underlyingCurrency to productCurrency
            return FxRateProviderResult.NotFound
        }
    }

    private data class CompressedFiles(
        val dxsc: File,
        val xfra: File
    )

    private var fileCounter = 0

    private companion object {
        const val PRODUCT_A = "DE000SYNTH01"
        const val PRODUCT_B = "DE000SYNTH02"
        const val PRODUCT_OTHER_UNDERLYING = "DE000SYNTH03"
        const val PRODUCT_SHORT = "DE000SYNTH04"
        const val UNDERLYING_ID = "synthetic-underlying"
        const val OTHER_UNDERLYING_ID = "other-underlying"
        const val BROKER_ID = "synthetic-broker"
        const val MARKET_DATA_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L
        const val SPECIFICATION_RETRIEVED_AT = 1_700_000_000_500L
    }
}
