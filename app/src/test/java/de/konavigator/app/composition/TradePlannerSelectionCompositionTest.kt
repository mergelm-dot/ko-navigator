package de.konavigator.app.composition

import androidx.lifecycle.ViewModel
import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityResult
import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogResult
import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.FxRateProviderResult
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.FxRateQuote
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectedProductUiModel
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionCurrencyEvidence
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionEvaluationTimeProvider
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionExecutionSettings
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiNoSelectionReason
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiDiagnostics
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiResult
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiSubmission
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TradePlannerSelectionCompositionTest {
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
    fun sameCurrencyCompositionCompletesSelectedWithoutCallingFxProvider() = runTest {
        val snapshot = snapshot(
            productIsin = "SYNTHETIC-SAME-ISIN",
            productWkn = "SYN001",
            issuerId = "synthetic-issuer",
            underlyingCurrency = "EUR",
            productCurrency = "EUR",
            knockoutBarrier = 90.0
        )
        val catalogRepository = RecordingCatalogRepository(listOf(snapshot))
        val brokerRepository = RecordingBrokerAvailabilityRepository(
            mapOf("synthetic-broker" to setOf(snapshot.specification.productIsin))
        )
        val marketDataRepository = RecordingMarketDataRepository(
            mapOf(snapshot.specification.productIsin to marketData(snapshot, "EUR"))
        )
        val fxRateProvider = RecordingFxRateProvider(FxRateProviderResult.NotFound)
        val viewModel = createViewModel(
            catalogRepository = catalogRepository,
            brokerRepository = brokerRepository,
            marketDataRepository = marketDataRepository,
            fxRateProvider = fxRateProvider
        )

        configureValidInput(viewModel)
        viewModel.onCalculateClicked()

        assertSame(TradePlannerSelectionUiSubmission.Loading, viewModel.uiState.value.submission)
        runCurrent()

        val primary = selectedPrimary(viewModel)
        assertEquals("SYNTHETIC-SAME-ISIN", primary.productIsin)
        assertEquals("SYN001", primary.productWkn)
        assertEquals("synthetic-issuer", primary.issuerId)
        assertEquals("EUR", primary.productCurrency)
        assertEquals(1.0, primary.calculatedProductPriceAtPlannedEntry, 0.0)
        assertEquals(10.0, primary.calculatedLeverageAtPlannedEntry, 0.0)
        assertEquals(90.0, primary.knockoutBarrier, 0.0)
        assertSame(TradePlannerSelectionCurrencyEvidence.SameCurrency, primary.currencyEvidence)
        assertEquals(1, catalogRepository.queries.size)
        assertEquals(1, brokerRepository.queries.size)
        assertEquals(1, marketDataRepository.productIsins.size)
        assertTrue(fxRateProvider.requests.isEmpty())
    }

    @Test
    fun crossCurrencyCompositionUsesInjectedFxProviderAndPreservesEvidence() = runTest {
        val snapshot = snapshot(
            productIsin = "SYNTHETIC-CROSS-ISIN",
            productWkn = "SYN002",
            issuerId = "synthetic-issuer",
            underlyingCurrency = "USD",
            productCurrency = "EUR",
            knockoutBarrier = 90.0
        )
        val expectedQuote = FxRateQuote(
            underlyingCurrency = currency("USD"),
            productCurrency = currency("EUR"),
            underlyingCurrencyPerProductCurrencyRate = 1.2,
            sourceId = "synthetic-fx-source",
            observedAtEpochMillis = 9_995L
        )
        val fxRateProvider = RecordingFxRateProvider(FxRateProviderResult.Success(expectedQuote))
        val viewModel = createViewModel(
            catalogRepository = RecordingCatalogRepository(listOf(snapshot)),
            brokerRepository = RecordingBrokerAvailabilityRepository(
                mapOf("synthetic-broker" to setOf(snapshot.specification.productIsin))
            ),
            marketDataRepository = RecordingMarketDataRepository(
                mapOf(snapshot.specification.productIsin to marketData(snapshot, "EUR"))
            ),
            fxRateProvider = fxRateProvider
        )

        configureValidInput(viewModel)
        viewModel.onCalculateClicked()
        runCurrent()

        val primary = selectedPrimary(viewModel)
        assertEquals("SYNTHETIC-CROSS-ISIN", primary.productIsin)
        assertEquals("SYN002", primary.productWkn)
        assertEquals("synthetic-issuer", primary.issuerId)
        assertEquals("EUR", primary.productCurrency)
        assertEquals(90.0, primary.knockoutBarrier, 0.0)
        assertEquals(1, fxRateProvider.requests.size)
        assertEquals(currency("USD"), fxRateProvider.requests.single().underlyingCurrency)
        assertEquals(currency("EUR"), fxRateProvider.requests.single().productCurrency)
        val evidence = primary.currencyEvidence
        assertTrue(evidence is TradePlannerSelectionCurrencyEvidence.CrossCurrency)
        evidence as TradePlannerSelectionCurrencyEvidence.CrossCurrency
        assertEquals("synthetic-fx-source", evidence.sourceId)
        assertEquals(9_995L, evidence.observedAtEpochMillis)
    }

    @Test
    fun emptyCatalogCompositionCompletesWithNoCatalogCandidates() = runTest {
        val catalogRepository = RecordingCatalogRepository(emptyList())
        val brokerRepository = RecordingBrokerAvailabilityRepository(emptyMap())
        val marketDataRepository = RecordingMarketDataRepository(emptyMap())
        val fxRateProvider = RecordingFxRateProvider(FxRateProviderResult.NotFound)
        val viewModel = createViewModel(
            catalogRepository = catalogRepository,
            brokerRepository = brokerRepository,
            marketDataRepository = marketDataRepository,
            fxRateProvider = fxRateProvider
        )

        configureValidInput(viewModel)
        viewModel.onCalculateClicked()
        runCurrent()

        val submission = viewModel.uiState.value.submission
        assertTrue(submission is TradePlannerSelectionUiSubmission.Completed)
        val result = (submission as TradePlannerSelectionUiSubmission.Completed).result
        assertEquals(
            TradePlannerSelectionUiResult.NoSelection(
                reason = TradePlannerSelectionUiNoSelectionReason.NO_CATALOG_CANDIDATES,
                diagnostics = TradePlannerSelectionUiDiagnostics()
            ),
            result
        )
        assertEquals(1, catalogRepository.queries.size)
        assertTrue(brokerRepository.queries.isEmpty())
        assertTrue(marketDataRepository.productIsins.isEmpty())
        assertTrue(fxRateProvider.requests.isEmpty())
    }

    private fun createViewModel(
        catalogRepository: RecordingCatalogRepository,
        brokerRepository: RecordingBrokerAvailabilityRepository,
        marketDataRepository: RecordingMarketDataRepository,
        fxRateProvider: RecordingFxRateProvider
    ): TradePlannerSelectionViewModel {
        val factory = TradePlannerSelectionComposition.createViewModelFactory(
            TradePlannerSelectionCompositionDependencies(
                specificationCatalogRepository = catalogRepository,
                brokerAvailabilityRepository = brokerRepository,
                marketDataRepository = marketDataRepository,
                fxRateProvider = fxRateProvider,
                freshnessPolicy = MarketDataFreshnessPolicy(
                    MarketDataFreshnessThresholds(
                        maxBidAgeMillis = 20L,
                        maxAskAgeMillis = 20L,
                        maxBidAskDifferenceMillis = 20L,
                        allowedFutureSkewMillis = 0L
                    )
                ),
                sourcePolicy = MarketDataSourcePolicy(
                    MarketDataSourcePolicyConfig(
                        rules = listOf(
                            MarketDataSourceRule(
                                sourceId = "synthetic-market-source",
                                supportedCalculationTypes = setOf(MarketDataCalculationType.MID)
                            )
                        )
                    )
                ),
                executionSettings = TradePlannerSelectionExecutionSettings(
                    calculationType = MarketDataCalculationType.MID,
                    maxFxAgeMillis = 20L,
                    maxRelativeLeverageDeviationPercent = 100.0,
                    maxBarrierDeviationPercentOfPlannedEntry = 10.0
                ),
                evaluationTimeProvider = TradePlannerSelectionEvaluationTimeProvider {
                    10_000L
                }
            )
        )

        val viewModel = factory.create(TradePlannerSelectionViewModel::class.java)
        assertEquals(TradePlannerSelectionViewModel::class.java, viewModel.javaClass)
        return viewModel
    }

    private fun configureValidInput(viewModel: TradePlannerSelectionViewModel) {
        viewModel.onUnderlyingSelected("synthetic-underlying")
        viewModel.onBrokerSelected("synthetic-broker")
        viewModel.onEnabledIssuerIdsChanged(setOf("synthetic-issuer"))
        viewModel.onCurrentPriceChanged("100")
        viewModel.onPlannedEntryPriceChanged("100")
        viewModel.onTargetLeverageChanged("5")
        viewModel.onDirectionChanged(TradeDirection.LONG)
    }

    private fun selectedPrimary(
        viewModel: TradePlannerSelectionViewModel
    ): TradePlannerSelectedProductUiModel {
        val submission = viewModel.uiState.value.submission
        assertTrue(submission is TradePlannerSelectionUiSubmission.Completed)
        val result = (submission as TradePlannerSelectionUiSubmission.Completed).result
        assertTrue(result is TradePlannerSelectionUiResult.Selected)
        return (result as TradePlannerSelectionUiResult.Selected).primaryCandidate
    }

    private fun snapshot(
        productIsin: String,
        productWkn: String,
        issuerId: String,
        underlyingCurrency: String,
        productCurrency: String,
        knockoutBarrier: Double
    ) = KnockoutProductSpecificationSnapshot(
        specification = KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = productWkn,
            issuerId = issuerId,
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.LONG,
            basePrice = 90.0,
            knockoutBarrier = knockoutBarrier,
            ratio = 0.1,
            underlyingCurrency = underlyingCurrency,
            productCurrency = productCurrency
        ),
        sourceId = "synthetic-specification-source",
        retrievedAtEpochMillis = 9_990L,
        sourceTimestampEpochMillis = 9_990L
    )

    private fun marketData(
        snapshot: KnockoutProductSpecificationSnapshot,
        currency: String
    ) = KnockoutProductMarketData(
        productIsin = snapshot.specification.productIsin,
        bid = 1.0,
        ask = 1.1,
        bidTimestampEpochMillis = 9_995L,
        askTimestampEpochMillis = 9_995L,
        currency = currency,
        sourceId = "synthetic-market-source"
    )

    private fun currency(value: String): CurrencyCode = when (val result = CurrencyCode.create(value)) {
        is CurrencyCodeCreationResult.Success -> result.currencyCode
        is CurrencyCodeCreationResult.Failure ->
            error("Unexpected invalid synthetic currency: ${result.error}")
    }

    private class RecordingCatalogRepository(
        private val snapshots: List<KnockoutProductSpecificationSnapshot>
    ) : KnockoutProductSpecificationCatalogRepository {
        val queries = mutableListOf<KnockoutProductSpecificationCatalogQuery>()

        override suspend fun findCandidates(
            query: KnockoutProductSpecificationCatalogQuery
        ): KnockoutProductSpecificationCatalogResult {
            queries += query
            return KnockoutProductSpecificationCatalogResult.Success(
                candidates = snapshots.filter { snapshot ->
                    snapshot.specification.underlyingId == query.underlyingId &&
                        snapshot.specification.direction == query.direction
                }
            )
        }
    }

    private class RecordingBrokerAvailabilityRepository(
        private val tradableProductIsinsByBrokerId: Map<String, Set<String>>
    ) : KnockoutProductBrokerAvailabilityRepository {
        val queries = mutableListOf<KnockoutProductBrokerAvailabilityQuery>()

        override suspend fun findTradableProductIsins(
            query: KnockoutProductBrokerAvailabilityQuery
        ): KnockoutProductBrokerAvailabilityResult {
            queries += query
            val tradableProductIsins = tradableProductIsinsByBrokerId[query.brokerId]
                ?: emptySet()
            return KnockoutProductBrokerAvailabilityResult.Success(
                tradableProductIsins = query.productIsins.filterTo(linkedSetOf()) {
                    it in tradableProductIsins
                }
            )
        }
    }

    private class RecordingMarketDataRepository(
        private val marketDataByProductIsin: Map<String, KnockoutProductMarketData>
    ) : KnockoutProductMarketDataRepository {
        val productIsins = mutableListOf<String>()

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductMarketData> {
            productIsins += productIsin
            return marketDataByProductIsin[productIsin]
                ?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.NotFound
        }
    }

    private class RecordingFxRateProvider(
        private val result: FxRateProviderResult
    ) : FxRateProvider {
        val requests = mutableListOf<FxRequest>()

        override suspend fun findRate(
            underlyingCurrency: CurrencyCode,
            productCurrency: CurrencyCode
        ): FxRateProviderResult {
            requests += FxRequest(underlyingCurrency, productCurrency)
            return result
        }
    }

    private data class FxRequest(
        val underlyingCurrency: CurrencyCode,
        val productCurrency: CurrencyCode
    )
}
