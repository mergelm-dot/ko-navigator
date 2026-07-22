package de.konavigator.app.presentation.tradeplanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import de.konavigator.app.application.tradeplanning.TradePlanningApplicationService
import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.ui.theme.KONavigatorTheme
import java.lang.reflect.Modifier as JavaModifier
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TradePlannerRouteTest {

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
    fun scenario01InitialViewModelStateIsRendered() {
        setRoute(createViewModel())

        composeRule.onNodeWithTag(CURRENT_PRICE_TAG).assertTextContains("100,00")
        composeRule.onNodeWithTag(PLANNED_ENTRY_PRICE_TAG).assertTextContains("95,00")
        composeRule.onNodeWithTag(TARGET_LEVERAGE_TAG).assertTextContains("3")
    }

    @Test
    fun scenario02InputActionsChangeExactlyTheViewModelState() {
        val viewModel = createViewModel()
        setRoute(viewModel)

        composeRule.onNodeWithTag(CURRENT_PRICE_TAG).performTextReplacement("105,50")
        composeRule.onNodeWithTag(PLANNED_ENTRY_PRICE_TAG).performTextReplacement("99,25")
        composeRule.onNodeWithTag(TARGET_LEVERAGE_TAG).performTextReplacement("4,5")
        composeRule.onNode(
            isSelectable() and hasAnyDescendant(hasText("Short"))
        ).performClick()

        composeRule.runOnIdle {
            assertEquals(
                TradePlannerUiState(
                    currentUnderlyingPriceInput = "105,50",
                    plannedEntryPriceInput = "99,25",
                    targetLeverageInput = "4,5",
                    direction = TradeDirection.SHORT,
                    submission = TradePlannerUiSubmission.Idle
                ),
                viewModel.uiState.value
            )
        }
    }

    @Test
    fun scenario03CalculateCreatesAndRendersViewModelResult() {
        val viewModel = createViewModel()
        setRoute(viewModel)

        composeRule.onNodeWithTag(CALCULATE_TAG).performScrollTo().performClick()

        composeRule.runOnIdle {
            val submission = viewModel.uiState.value.submission
            assertTrue(submission is TradePlannerUiSubmission.Completed)
            assertTrue(
                (submission as TradePlannerUiSubmission.Completed).result is
                    TradePlannerUiResult.Success
            )
        }
        composeRule.onNodeWithTag(RESULT_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scenario04RouteHasNoFactoryCompositionServiceOrEngineDependency() {
        val routeClass = Class.forName(
            "de.konavigator.app.presentation.tradeplanner.TradePlannerRouteKt"
        )
        val routeMethods = routeClass.declaredMethods.filter {
            JavaModifier.isPublic(it.modifiers) && it.name == "TradePlannerRoute"
        }
        val referencedApiTypes = routeMethods.flatMap { method ->
            method.parameterTypes.map { it.name } +
                method.genericParameterTypes.map { it.typeName }
        } + routeClass.declaredFields.map { it.type.name }
        val forbidden = listOf(
            "Factory",
            "Composition",
            "ApplicationService",
            "TradeCalculationEngine"
        )

        assertEquals(1, routeMethods.size)
        assertTrue(
            referencedApiTypes.none { typeName ->
                forbidden.any { typeName.contains(it, ignoreCase = true) }
            }
        )
    }

    private fun setRoute(viewModel: TradePlannerViewModel) {
        composeRule.setContent {
            KONavigatorTheme(dynamicColor = false) {
                TradePlannerRoute(viewModel)
            }
        }
    }

    private fun createViewModel() = TradePlannerViewModel(
        TradePlanningApplicationService(TradeCalculationEngine)
    )

    private companion object {
        const val CURRENT_PRICE_TAG = "trade_planner_current_price"
        const val PLANNED_ENTRY_PRICE_TAG = "trade_planner_planned_entry_price"
        const val TARGET_LEVERAGE_TAG = "trade_planner_target_leverage"
        const val CALCULATE_TAG = "trade_planner_calculate"
        const val RESULT_TAG = "trade_planner_result"
    }
}
