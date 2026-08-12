package de.konavigator.app.debug.tradeplanner

import androidx.lifecycle.ViewModel
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.presentation.tradeplanner.TradePlannerBrokerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerIssuerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionCurrencyEvidence
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoTest {

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
    fun createsRepeatableDeviceDemoFactoryAndStableUiOptions() = runTest(mainDispatcher) {
        val first = HsbcDeutscheBoerseTradePlannerSelectionDeviceDemo.create(
            temporaryFolder.root
        )
        val second = HsbcDeutscheBoerseTradePlannerSelectionDeviceDemo.create(
            temporaryFolder.root
        )

        assertTrue(first is HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Success)
        assertTrue(second is HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Success)
        val result = second as HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Success
        val viewModel = result.viewModelFactory.create(TradePlannerSelectionViewModel::class.java)

        assertEquals(TradePlannerSelectionViewModel::class.java, viewModel.javaClass)
        assertEquals(
            listOf(TradePlannerBrokerUiOption("synthetic-broker", "Debug Broker")),
            result.brokerOptions
        )
        assertEquals(
            listOf(TradePlannerIssuerUiOption("synthetic-issuer", "Synthetic Issuer")),
            result.issuerOptions
        )
    }

    @Test
    fun nvidiaSelectionUsesFilesAndSelectsOnlyBrokerTradableProductA() =
        runTest(mainDispatcher) {
            val result = HsbcDeutscheBoerseTradePlannerSelectionDeviceDemo.create(
                temporaryFolder.root
            ) as HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Success
            val viewModel = result.viewModelFactory.create(TradePlannerSelectionViewModel::class.java)

            viewModel.onUnderlyingSelected("nvidia")
            viewModel.onBrokerSelected("synthetic-broker")
            viewModel.onEnabledIssuerIdsChanged(setOf("synthetic-issuer"))
            viewModel.onCurrentPriceChanged("100")
            viewModel.onPlannedEntryPriceChanged("100")
            viewModel.onTargetLeverageChanged("5")
            viewModel.onDirectionChanged(TradeDirection.LONG)
            viewModel.onCalculateClicked()
            runCurrent()

            val submission = viewModel.uiState.value.submission
            assertTrue(submission is TradePlannerSelectionUiSubmission.Completed)
            val selected = (submission as TradePlannerSelectionUiSubmission.Completed).result
                as TradePlannerSelectionUiResult.Selected
            val primary = selected.primaryCandidate

            assertEquals("DE000SYNTH01", primary.productIsin)
            assertEquals("SYN001", primary.productWkn)
            assertEquals("synthetic-issuer", primary.issuerId)
            assertEquals("USD", primary.productCurrency)
            assertEquals(1.0, primary.calculatedProductPriceAtPlannedEntry, 0.0)
            assertEquals(10.0, primary.calculatedLeverageAtPlannedEntry, 0.0)
            assertEquals(80.0, primary.knockoutBarrier, 0.0)
            assertSame(TradePlannerSelectionCurrencyEvidence.SameCurrency, primary.currencyEvidence)
            assertTrue(selected.alternativeCandidates.isEmpty())
            assertTrue(primary.productIsin != "DE000SYNTH02")
            assertTrue(selected.alternativeCandidates.none { it.productIsin == "DE000SYNTH02" })
        }
}
