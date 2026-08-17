package de.konavigator.app.screens

import android.content.ClipboardManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import de.konavigator.app.R
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.presentation.tradeplanner.TradePlannerBrokerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerIssuerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectedProductUiModel
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionCurrencyEvidence
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiDiagnostics
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiInputError
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiMappingError
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiNoSelectionReason
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiResult
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiState
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionUiSubmission
import de.konavigator.app.ui.theme.KONavigatorTheme
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TradePlannerSelectionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var previousLocale: Locale
    private lateinit var recordingClipboard: RecordingClipboard

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        recordingClipboard = RecordingClipboard()
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun scenario01PrimarySelectionIsCompactByDefault() {
        setScreen()

        composeRule.onNodeWithText("Hauptauswahl").performScrollTo().assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(PRIMARY_CARD_TAG) and
                hasAnyDescendant(hasText("Synthetic Issuer"))
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("SYN001").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2,00 USD").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Berechnungsdetails anzeigen")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Alternativen anzeigen")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onAllNodesWithText("ISIN").assertCountEquals(0)
        composeRule.onAllNodesWithText("Berechneter Hebel am Einstieg").assertCountEquals(0)
        composeRule.onAllNodesWithText("Alternative 1").assertCountEquals(0)
        composeRule.onAllNodesWithText("ALT001").assertCountEquals(0)
    }

    @Test
    fun scenario02CalculationDetailsExpandAndCollapse() {
        setScreen()

        composeRule.onNodeWithText("Berechnungsdetails anzeigen")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Berechnungsdetails ausblenden")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(PRIMARY_ISIN).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Berechneter Hebel am Einstieg")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Alle Ziel-Toleranzen erfüllt")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Keine FX-Umrechnung erforderlich")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText("Berechnungsdetails ausblenden")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Berechnungsdetails anzeigen")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithText(PRIMARY_ISIN).assertCountEquals(0)
        composeRule.onAllNodesWithText("Berechneter Hebel am Einstieg").assertCountEquals(0)
    }

    @Test
    fun scenario03AlternativesExpandAndCollapse() {
        setScreen()

        composeRule.onAllNodesWithText("Alternative 1").assertCountEquals(0)
        composeRule.onNodeWithText("Alternativen anzeigen")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Alternativen ausblenden")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Alternative 1").performScrollTo().assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(ALTERNATIVE_CARD_TAG) and
                hasAnyDescendant(hasText("Alternative Issuer"))
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ALT001").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2,50 USD").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Alternativen ausblenden")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Alternativen anzeigen")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Alternative 1").assertCountEquals(0)
        composeRule.onAllNodesWithText("ALT001").assertCountEquals(0)
    }

    @Test
    fun scenario04ExpansionStatesAreIndependent() {
        setScreen()

        composeRule.onNodeWithText("Berechnungsdetails anzeigen")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Alternativen anzeigen")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText(PRIMARY_ISIN).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Alternative 1").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Berechnungsdetails ausblenden")
            .performScrollTo()
            .performClick()

        composeRule.onAllNodesWithText(PRIMARY_ISIN).assertCountEquals(0)
        composeRule.onNodeWithText("Alternative 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Alternativen ausblenden")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText("Berechnungsdetails anzeigen")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Alternativen ausblenden")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText(PRIMARY_ISIN).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Berechnungsdetails ausblenden")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Alternative 1").assertCountEquals(0)
    }

    @Test
    fun scenario05WknCopyUsesExactRawValue() {
        setScreen()

        composeRule.onNodeWithContentDescription("WKN kopieren")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals("SYN001", recordingClipboard.copiedText())
        }
    }

    @Test
    fun scenario06PriceCopyUsesFormattedNumericValueWithoutCurrency() {
        setScreen()

        composeRule.onNodeWithText("2,00 USD").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Preis am Einstieg kopieren")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            val copiedText = recordingClipboard.copiedText()
            assertEquals("2,00", copiedText)
            assertFalse(copiedText.orEmpty().contains("USD"))
        }
    }

    @Test
    fun scenario07MissingWknHasNoCopyAction() {
        setScreen(
            state = selectedState(
                primaryCandidate = primaryCandidate().copy(productWkn = null),
                alternativeCandidates = emptyList()
            )
        )

        composeRule.onNodeWithText("Nicht verfügbar").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("WKN kopieren").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Preis am Einstieg kopieren")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun scenario08OnlySelectedResultClearsFocus() {
        lateinit var updateSubmission: (TradePlannerSelectionUiSubmission) -> Unit
        composeRule.setContent {
            var state by remember { mutableStateOf(TradePlannerSelectionUiState()) }
            updateSubmission = { submission -> state = state.copy(submission = submission) }
            TestContent(state)
        }

        composeRule.onNode(
            hasSetTextAction() and hasAnyAncestor(hasTestTag(UNDERLYING_INPUT_TAG))
        )
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assertIsFocused()

        val submissionsThatKeepFocus = listOf(
            TradePlannerSelectionUiSubmission.InvalidInput(
                listOf(TradePlannerSelectionUiInputError.UNDERLYING_REQUIRED)
            ),
            TradePlannerSelectionUiSubmission.Loading,
            TradePlannerSelectionUiSubmission.Completed(
                TradePlannerSelectionUiResult.NoSelection(
                    reason = TradePlannerSelectionUiNoSelectionReason.NO_CATALOG_CANDIDATES,
                    diagnostics = TradePlannerSelectionUiDiagnostics()
                )
            ),
            TradePlannerSelectionUiSubmission.Completed(
                TradePlannerSelectionUiResult.InconsistentData(
                    TradePlannerSelectionUiMappingError.CALCULATION_PIPELINE_STOPPED_WITH_SUCCESS_RESULT
                )
            )
        )
        submissionsThatKeepFocus.forEach { submission ->
            composeRule.runOnIdle { updateSubmission(submission) }
            composeRule.onNode(
                hasSetTextAction() and hasAnyAncestor(hasTestTag(UNDERLYING_INPUT_TAG))
            ).assertIsFocused()
        }

        composeRule.runOnIdle {
            updateSubmission(selectedState().submission)
        }
        composeRule.onNode(
            hasSetTextAction() and hasAnyAncestor(hasTestTag(UNDERLYING_INPUT_TAG))
        ).assertIsNotFocused()
    }

    @Test
    fun scenario09AlternativeControlIsAbsentWithoutAlternatives() {
        setScreen(
            state = selectedState(alternativeCandidates = emptyList())
        )

        composeRule.onAllNodesWithText("Alternativen anzeigen").assertCountEquals(0)
        composeRule.onAllNodesWithText("Alternativen ausblenden").assertCountEquals(0)
        composeRule.onNodeWithText("Hauptauswahl").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scenario10DebugNoticeUsesCorrectEnDash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals(
            "DEBUG – kontrollierte synthetische Daten. " +
                "Keine Live-Marktdaten und keine reale Broker-Handelbarkeit.",
            context.getString(R.string.trade_planner_selection_demo_notice)
        )
    }

    private fun setScreen(
        state: TradePlannerSelectionUiState = selectedState()
    ) {
        composeRule.setContent {
            TestContent(state)
        }
    }

    @androidx.compose.runtime.Composable
    private fun TestContent(state: TradePlannerSelectionUiState) {
        CompositionLocalProvider(LocalClipboard provides recordingClipboard) {
            KONavigatorTheme(dynamicColor = false) {
                TradePlannerSelectionScreen(
                    state = state,
                    brokerOptions = listOf(
                        TradePlannerBrokerUiOption("debug-broker", "Debug Broker")
                    ),
                    issuerOptions = listOf(
                        TradePlannerIssuerUiOption("issuer-primary", "Synthetic Issuer"),
                        TradePlannerIssuerUiOption("issuer-alternative", "Alternative Issuer")
                    ),
                    onUnderlyingSelected = {},
                    onBrokerSelected = {},
                    onEnabledIssuerIdsChanged = {},
                    onCurrentPriceChanged = {},
                    onPlannedEntryPriceChanged = {},
                    onTargetLeverageChanged = {},
                    onDirectionChanged = {},
                    onCalculateClicked = {}
                )
            }
        }
    }

    private fun selectedState(
        primaryCandidate: TradePlannerSelectedProductUiModel = primaryCandidate(),
        alternativeCandidates: List<TradePlannerSelectedProductUiModel> =
            listOf(alternativeCandidate())
    ) = TradePlannerSelectionUiState(
        selectedUnderlyingId = "nvidia",
        selectedBrokerId = "debug-broker",
        enabledIssuerIds = setOf("issuer-primary", "issuer-alternative"),
        currentUnderlyingPriceInput = "100",
        plannedEntryPriceInput = "100",
        targetLeverageInput = "5",
        direction = TradeDirection.LONG,
        submission = TradePlannerSelectionUiSubmission.Completed(
            TradePlannerSelectionUiResult.Selected(
                primaryCandidate = primaryCandidate,
                alternativeCandidates = alternativeCandidates,
                diagnostics = TradePlannerSelectionUiDiagnostics()
            )
        )
    )

    private fun primaryCandidate() = TradePlannerSelectedProductUiModel(
        productIsin = PRIMARY_ISIN,
        productWkn = "SYN001",
        issuerId = "issuer-primary",
        productCurrency = "USD",
        calculatedProductPriceAtPlannedEntry = 2.0,
        calculatedLeverageAtPlannedEntry = 5.0,
        knockoutBarrier = 80.0,
        knockoutDistanceAbsolute = 20.0,
        knockoutDistancePercent = 20.0,
        relativeLeverageDeviationPercent = 0.0,
        barrierDeviationPercentOfPlannedEntry = 0.0,
        leverageWithinTolerance = true,
        barrierWithinTolerance = true,
        withinAllTargetTolerances = true,
        currencyEvidence = TradePlannerSelectionCurrencyEvidence.SameCurrency
    )

    private fun alternativeCandidate() = TradePlannerSelectedProductUiModel(
        productIsin = "DE000ALTERNATIVE",
        productWkn = "ALT001",
        issuerId = "issuer-alternative",
        productCurrency = "USD",
        calculatedProductPriceAtPlannedEntry = 2.5,
        calculatedLeverageAtPlannedEntry = 4.0,
        knockoutBarrier = 75.0,
        knockoutDistanceAbsolute = 25.0,
        knockoutDistancePercent = 25.0,
        relativeLeverageDeviationPercent = 20.0,
        barrierDeviationPercentOfPlannedEntry = 5.0,
        leverageWithinTolerance = true,
        barrierWithinTolerance = true,
        withinAllTargetTolerances = true,
        currencyEvidence = TradePlannerSelectionCurrencyEvidence.CrossCurrency(
            sourceId = "synthetic-fx",
            observedAtEpochMillis = 1234L
        )
    )

    private class RecordingClipboard : Clipboard {
        private var lastEntry: ClipEntry? = null

        override suspend fun getClipEntry(): ClipEntry? = lastEntry

        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            lastEntry = clipEntry
        }

        override val nativeClipboard: ClipboardManager
            get() = error("Native clipboard is not used by this test")

        fun copiedText(): String? = lastEntry
            ?.clipData
            ?.getItemAt(0)
            ?.text
            ?.toString()
    }

    private companion object {
        const val PRIMARY_ISIN = "DE000SYNTH01"
        const val UNDERLYING_INPUT_TAG = "trade_planner_selection_underlying_input"
        const val PRIMARY_CARD_TAG = "trade_planner_selection_primary_candidate"
        const val ALTERNATIVE_CARD_TAG =
            "trade_planner_selection_alternative_candidate_DE000ALTERNATIVE"
    }
}
