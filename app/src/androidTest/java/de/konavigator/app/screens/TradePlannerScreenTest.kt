package de.konavigator.app.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.tradeplanning.EntryPriceRelation
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiCalculationError
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiInputError
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiResult
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiState
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiSubmission
import de.konavigator.app.ui.theme.KONavigatorTheme
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TradePlannerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun scenario01ThreeInitialInputsAreRenderedFromState() {
        setScreen(
            TradePlannerUiState(
                currentUnderlyingPriceInput = "123,45",
                plannedEntryPriceInput = "120,00",
                targetLeverageInput = "4,5"
            )
        )

        composeRule.onNodeWithTag(CURRENT_PRICE_TAG).assertTextContains("123,45")
        composeRule.onNodeWithTag(PLANNED_ENTRY_PRICE_TAG).assertTextContains("120,00")
        composeRule.onNodeWithTag(TARGET_LEVERAGE_TAG).assertTextContains("4,5")
    }

    @Test
    fun scenario02CurrentPriceCallbackReceivesExactText() {
        var received = ""
        setScreen(onCurrentPriceChanged = { received = it })

        composeRule.onNodeWithTag(CURRENT_PRICE_TAG).performTextReplacement(" 101,25 ")

        composeRule.runOnIdle { assertEquals(" 101,25 ", received) }
    }

    @Test
    fun scenario03PlannedEntryCallbackReceivesExactText() {
        var received = ""
        setScreen(onPlannedEntryPriceChanged = { received = it })

        composeRule.onNodeWithTag(PLANNED_ENTRY_PRICE_TAG)
            .performTextReplacement(" 98,75 ")

        composeRule.runOnIdle { assertEquals(" 98,75 ", received) }
    }

    @Test
    fun scenario04TargetLeverageCallbackReceivesExactText() {
        var received = ""
        setScreen(onTargetLeverageChanged = { received = it })

        composeRule.onNodeWithTag(TARGET_LEVERAGE_TAG).performTextReplacement(" 3,5 ")

        composeRule.runOnIdle { assertEquals(" 3,5 ", received) }
    }

    @Test
    fun scenario05LongDelegatesTypedDirection() {
        var received: TradeDirection? = null
        setScreen(
            state = TradePlannerUiState(direction = TradeDirection.SHORT),
            onDirectionChanged = { received = it }
        )

        composeRule.onNode(
            isSelectable() and hasAnyDescendant(hasText("Long"))
        ).performClick()

        composeRule.runOnIdle { assertEquals(TradeDirection.LONG, received) }
    }

    @Test
    fun scenario06ShortDelegatesTypedDirection() {
        var received: TradeDirection? = null
        setScreen(onDirectionChanged = { received = it })

        composeRule.onNode(
            isSelectable() and hasAnyDescendant(hasText("Short"))
        ).performClick()

        composeRule.runOnIdle { assertEquals(TradeDirection.SHORT, received) }
    }

    @Test
    fun scenario07CalculateCallbackIsTriggered() {
        var callCount = 0
        setScreen(onCalculateClicked = { callCount++ })

        composeRule.onNodeWithTag(CALCULATE_TAG).performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, callCount) }
    }

    @Test
    fun scenario08AllSixInputErrorsAreMappedNextToTheirFields() {
        lateinit var showError: (TradePlannerUiInputError) -> Unit
        composeRule.setContent {
            var state by remember { mutableStateOf(TradePlannerUiState()) }
            showError = { error ->
                state = state.copy(
                    submission = TradePlannerUiSubmission.InvalidInput(listOf(error))
                )
            }
            KONavigatorTheme(dynamicColor = false) {
                statelessScreen(state)
            }
        }
        val expectations = mapOf(
            TradePlannerUiInputError.CURRENT_PRICE_REQUIRED to
                (CURRENT_PRICE_TAG to "Aktueller Kurs ist erforderlich."),
            TradePlannerUiInputError.CURRENT_PRICE_INVALID to
                (CURRENT_PRICE_TAG to "Aktueller Kurs muss eine endliche Zahl größer als 0 sein."),
            TradePlannerUiInputError.PLANNED_ENTRY_PRICE_REQUIRED to
                (PLANNED_ENTRY_PRICE_TAG to "Geplanter Einstiegskurs ist erforderlich."),
            TradePlannerUiInputError.PLANNED_ENTRY_PRICE_INVALID to
                (PLANNED_ENTRY_PRICE_TAG to
                    "Geplanter Einstiegskurs muss eine endliche Zahl größer als 0 sein."),
            TradePlannerUiInputError.TARGET_LEVERAGE_REQUIRED to
                (TARGET_LEVERAGE_TAG to "Zielhebel ist erforderlich."),
            TradePlannerUiInputError.TARGET_LEVERAGE_INVALID to
                (TARGET_LEVERAGE_TAG to "Zielhebel muss eine endliche Zahl größer als 1 sein.")
        )

        expectations.forEach { (error, expectation) ->
            composeRule.runOnIdle { showError(error) }
            composeRule.onNode(
                hasTestTag(expectation.first) and
                    hasAnyDescendant(hasText(expectation.second))
            ).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun scenario09SuccessBoxShowsRelationAndFourCalculationValues() {
        setScreen(successState(EntryPriceRelation.BELOW_CURRENT))

        val matcher = listOf(
            "Der geplante Einstieg liegt unter dem aktuellen Basiswertkurs.",
            "1,2500",
            "75,00",
            "20,00",
            "21,0526 %"
        ).fold(hasTestTag(RESULT_TAG)) { resultMatcher, text ->
            resultMatcher and hasAnyDescendant(hasText(text))
        }

        composeRule.onNode(matcher).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scenario10AllSevenCalculationErrorsAreMapped() {
        lateinit var showError: (TradePlannerUiCalculationError) -> Unit
        composeRule.setContent {
            var state by remember {
                mutableStateOf(failureState(TradePlannerUiCalculationError.INVALID_PLANNED_ENTRY_PRICE))
            }
            showError = { error -> state = failureState(error) }
            KONavigatorTheme(dynamicColor = false) {
                statelessScreen(state)
            }
        }
        val expectations = mapOf(
            TradePlannerUiCalculationError.INVALID_PLANNED_ENTRY_PRICE to
                "Der geplante Einstiegskurs konnte nicht verarbeitet werden.",
            TradePlannerUiCalculationError.INVALID_TARGET_LEVERAGE to
                "Der Zielhebel konnte nicht verarbeitet werden.",
            TradePlannerUiCalculationError.INVALID_RATIO to
                "Das Bezugsverhältnis konnte nicht verarbeitet werden.",
            TradePlannerUiCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE to
                "Die theoretische KO-Barriere konnte nicht berechnet werden.",
            TradePlannerUiCalculationError.INVALID_EXCHANGE_RATE to
                "Der Währungskontext konnte nicht verarbeitet werden.",
            TradePlannerUiCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE to
                "Der theoretische Produktwert konnte nicht berechnet werden.",
            TradePlannerUiCalculationError.INCONSISTENT_CALCULATION_RESULT to
                "Das Berechnungsergebnis ist unvollständig und kann nicht angezeigt werden."
        )

        expectations.forEach { (error, text) ->
            composeRule.runOnIdle { showError(error) }
            composeRule.onNode(
                hasTestTag(ERROR_TAG) and hasAnyDescendant(hasText(text))
            ).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun scenario11BelowCurrentRelationIsNeutral() {
        assertRelation(
            EntryPriceRelation.BELOW_CURRENT,
            "Der geplante Einstieg liegt unter dem aktuellen Basiswertkurs."
        )
    }

    @Test
    fun scenario12AtCurrentRelationIsNeutral() {
        assertRelation(
            EntryPriceRelation.AT_CURRENT,
            "Der geplante Einstieg entspricht dem aktuellen Basiswertkurs."
        )
    }

    @Test
    fun scenario13AboveCurrentRelationIsNeutral() {
        assertRelation(
            EntryPriceRelation.ABOVE_CURRENT,
            "Der geplante Einstieg liegt über dem aktuellen Basiswertkurs."
        )
    }

    @Test
    fun scenario14UnderlyingSearchRemainsVisible() {
        setScreen()

        composeRule.onNodeWithText("Basiswert").assertIsDisplayed()
        composeRule.onNodeWithText("NVIDIA").assertIsDisplayed()
    }

    @Test
    fun scenario15BrokerAndIssuerAreasRemainVisible() {
        setScreen()

        composeRule.onNodeWithText("Broker").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Emittenten").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scenario16OldOrderTypesAreNotVisible() {
        setScreen()

        listOf("Buy Limit", "Buy Stop", "Sell Limit", "Sell Stop", "Market").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(0)
        }
    }

    @Test
    fun scenario17ContentRemainsScrollable() {
        setScreen()

        composeRule.onNode(hasScrollAction()).assertExists()
        composeRule.onNodeWithTag(CALCULATE_TAG).performScrollTo().assertIsDisplayed()
    }

    private fun setScreen(
        state: TradePlannerUiState = TradePlannerUiState(),
        onCurrentPriceChanged: (String) -> Unit = {},
        onPlannedEntryPriceChanged: (String) -> Unit = {},
        onTargetLeverageChanged: (String) -> Unit = {},
        onDirectionChanged: (TradeDirection) -> Unit = {},
        onCalculateClicked: () -> Unit = {}
    ) {
        composeRule.setContent {
            KONavigatorTheme(dynamicColor = false) {
                TradePlannerScreen(
                    state = state,
                    onCurrentPriceChanged = onCurrentPriceChanged,
                    onPlannedEntryPriceChanged = onPlannedEntryPriceChanged,
                    onTargetLeverageChanged = onTargetLeverageChanged,
                    onDirectionChanged = onDirectionChanged,
                    onCalculateClicked = onCalculateClicked
                )
            }
        }
    }

    @Composable
    private fun statelessScreen(state: TradePlannerUiState) {
        TradePlannerScreen(
            state = state,
            onCurrentPriceChanged = {},
            onPlannedEntryPriceChanged = {},
            onTargetLeverageChanged = {},
            onDirectionChanged = {},
            onCalculateClicked = {}
        )
    }

    private fun assertRelation(relation: EntryPriceRelation, expectedText: String) {
        setScreen(successState(relation))

        composeRule.onNodeWithText(expectedText).performScrollTo().assertIsDisplayed()
    }

    private fun successState(relation: EntryPriceRelation) = TradePlannerUiState(
        submission = TradePlannerUiSubmission.Completed(
            TradePlannerUiResult.Success(
                relation = relation,
                theoreticalProductValue = 1.25,
                knockoutPrice = 75.0,
                distanceToKnockoutAbsolute = 20.0,
                distanceToKnockoutPercent = 21.0526
            )
        )
    )

    private fun failureState(error: TradePlannerUiCalculationError) = TradePlannerUiState(
        submission = TradePlannerUiSubmission.Completed(
            TradePlannerUiResult.Failure(error)
        )
    )

    private companion object {
        const val CURRENT_PRICE_TAG = "trade_planner_current_price"
        const val PLANNED_ENTRY_PRICE_TAG = "trade_planner_planned_entry_price"
        const val TARGET_LEVERAGE_TAG = "trade_planner_target_leverage"
        const val CALCULATE_TAG = "trade_planner_calculate"
        const val RESULT_TAG = "trade_planner_result"
        const val ERROR_TAG = "trade_planner_error"
    }
}
