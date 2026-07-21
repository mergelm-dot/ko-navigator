package de.konavigator.app.debug.marketdata

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiError
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiInputError
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiResult
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiState
import de.konavigator.app.presentation.marketdata.MarketDataCalculationUiSubmission
import de.konavigator.app.ui.theme.KONavigatorTheme
import java.lang.reflect.Modifier
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MarketDataCalculationDemoScreenTest {

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
    fun scenario01HeadingIsVisible() {
        setScreen()

        composeRule.onNodeWithText("Engine-Demo").assertIsDisplayed()
    }

    @Test
    fun scenario02ProductIsinInputIsVisible() {
        setScreen()

        composeRule.onNodeWithTag(PRODUCT_ISIN_TAG).assertIsDisplayed()
    }

    @Test
    fun scenario03EvaluationTimeInputIsVisible() {
        setScreen()

        composeRule.onNodeWithTag(EVALUATION_TIME_TAG).assertIsDisplayed()
    }

    @Test
    fun scenario04AllFourCalculationTypesAreVisible() {
        setScreen()

        composeRule.onNodeWithTag(PURCHASE_PRICE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SALE_PRICE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SPREAD_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(MID_TAG).assertIsDisplayed()
    }

    @Test
    fun scenario05CalculateButtonIsInitiallyDisabled() {
        setScreen()

        composeRule.onNodeWithTag(CALCULATE_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun scenario06ValidStateEnablesCalculateButton() {
        setScreen(state = validState())

        composeRule.onNodeWithTag(CALCULATE_BUTTON_TAG).assertIsEnabled()
    }

    @Test
    fun scenario07ProductIsinCallbackReceivesExactText() {
        var received = ""
        setScreen(onProductIsinChanged = { received = it })

        composeRule.onNodeWithTag(PRODUCT_ISIN_TAG).performTextInput("  de000Demo001  ")

        composeRule.runOnIdle {
            assertEquals("  de000Demo001  ", received)
        }
    }

    @Test
    fun scenario08EvaluationTimeCallbackReceivesExactText() {
        var received = ""
        setScreen(onEvaluationTimeChanged = { received = it })

        composeRule.onNodeWithTag(EVALUATION_TIME_TAG)
            .performTextInput("-9223372036854775808")

        composeRule.runOnIdle {
            assertEquals("-9223372036854775808", received)
        }
    }

    @Test
    fun scenario09CalculationTypeCallbackReceivesExactEnum() {
        var received: MarketDataCalculationType? = null
        setScreen(onCalculationTypeChanged = { received = it })

        composeRule.onNodeWithTag(SALE_PRICE_TAG).performClick()

        composeRule.runOnIdle {
            assertEquals(MarketDataCalculationType.SALE_PRICE, received)
        }
    }

    @Test
    fun scenario10CalculateCallbackIsTriggered() {
        var callCount = 0
        setScreen(
            state = validState(),
            onCalculateClicked = { callCount++ }
        )

        composeRule.onNodeWithTag(CALCULATE_BUTTON_TAG).performClick()

        composeRule.runOnIdle {
            assertEquals(1, callCount)
        }
    }

    @Test
    fun scenario11LoadingIndicatorIsVisible() {
        setScreen(
            state = validState(
                submission = MarketDataCalculationUiSubmission.Loading
            )
        )

        composeRule.onNodeWithTag(LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Berechnung läuft …").assertIsDisplayed()
    }

    @Test
    fun scenario12PurchasePriceIsDisplayed() {
        setScreen(completedState(MarketDataCalculationUiResult.PurchasePrice(2.0, "EUR")))

        composeRule.onNodeWithTag(RESULT_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Kaufpreis").assertIsDisplayed()
        composeRule.onNodeWithText("2,0000 EUR").assertIsDisplayed()
    }

    @Test
    fun scenario13SalePriceIsDisplayed() {
        setScreen(completedState(MarketDataCalculationUiResult.SalePrice(1.8, "EUR")))

        composeRule.onNodeWithText("Verkaufspreis").assertIsDisplayed()
        composeRule.onNodeWithText("1,8000 EUR").assertIsDisplayed()
    }

    @Test
    fun scenario14AbsoluteAndRelativeSpreadAreDisplayed() {
        setScreen(
            completedState(
                MarketDataCalculationUiResult.Spread(
                    absoluteSpread = 0.2,
                    relativeSpreadToAskPercent = 10.0,
                    currency = "EUR"
                )
            )
        )

        composeRule.onNodeWithText("Spread").assertIsDisplayed()
        composeRule.onNodeWithText("Absolut: 0,2000 EUR").assertIsDisplayed()
        composeRule.onNodeWithText("Relativ: 10,00 %").assertIsDisplayed()
    }

    @Test
    fun scenario15MidPriceIsDisplayed() {
        setScreen(completedState(MarketDataCalculationUiResult.MidPrice(1.9, "EUR")))

        composeRule.onNodeWithText("Mittelpreis").assertIsDisplayed()
        composeRule.onNodeWithText("1,9000 EUR").assertIsDisplayed()
    }

    @Test
    fun scenario16InputErrorsAreDisplayedAsVisibleTexts() {
        setScreen(
            state = validState(
                submission = MarketDataCalculationUiSubmission.InvalidInput(
                    listOf(
                        MarketDataCalculationUiInputError.PRODUCT_ISIN_REQUIRED,
                        MarketDataCalculationUiInputError.EVALUATION_TIME_REQUIRED,
                        MarketDataCalculationUiInputError.EVALUATION_TIME_INVALID
                    )
                )
            )
        )

        composeRule.onNodeWithTag(ERROR_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Produkt-ISIN ist erforderlich.").assertIsDisplayed()
        composeRule.onNodeWithText("Bewertungszeitpunkt ist erforderlich.").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Bewertungszeitpunkt muss eine ganze Zahl in Millisekunden sein."
        ).assertIsDisplayed()
    }

    @Test
    fun scenario17CondensedApplicationAndDomainErrorsAreDisplayedNeutrally() {
        lateinit var updateState: (MarketDataCalculationUiState) -> Unit
        composeRule.setContent {
            var state by remember {
                mutableStateOf(failureState(MarketDataCalculationUiError.PRODUCT_NOT_FOUND))
            }
            updateState = { state = it }
            KONavigatorTheme(dynamicColor = false) {
                MarketDataCalculationDemoScreen(
                    state = state,
                    onProductIsinChanged = {},
                    onCalculationTypeChanged = {},
                    onEvaluationTimeChanged = {},
                    onCalculateClicked = {}
                )
            }
        }
        val expectedTexts = mapOf(
            MarketDataCalculationUiError.PRODUCT_NOT_FOUND to
                "Das Demo-Produkt wurde nicht gefunden.",
            MarketDataCalculationUiError.MARKET_DATA_NOT_FOUND to
                "Für das Demo-Produkt sind keine Marktdaten vorhanden.",
            MarketDataCalculationUiError.DATA_ACCESS_FAILURE to
                "Die lokalen Daten konnten nicht geladen werden.",
            MarketDataCalculationUiError.INVALID_SPECIFICATION to
                "Die Produktdaten sind ungültig.",
            MarketDataCalculationUiError.INVALID_MARKET_DATA to
                "Die Marktdaten sind ungültig.",
            MarketDataCalculationUiError.INCOMPATIBLE_PRODUCT_DATA to
                "Produkt- und Marktdaten passen nicht zusammen.",
            MarketDataCalculationUiError.REQUIRED_QUOTE_UNAVAILABLE to
                "Der erforderliche Geld- oder Briefkurs fehlt.",
            MarketDataCalculationUiError.MARKET_DATA_NOT_FRESH to
                "Die Marktdaten sind für diesen Bewertungszeitpunkt nicht aktuell genug.",
            MarketDataCalculationUiError.SOURCE_UNAVAILABLE to
                "Die Datenquelle ist für diese Berechnung nicht freigegeben.",
            MarketDataCalculationUiError.CALCULATION_FAILED to
                "Die Berechnung konnte nicht durchgeführt werden.",
            MarketDataCalculationUiError.UNEXPECTED_FAILURE to
                "Ein unerwarteter Fehler ist aufgetreten."
        )

        expectedTexts.forEach { (error, text) ->
            composeRule.runOnIdle {
                updateState(failureState(error))
            }
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun scenario18InternalErrorCodeNamesAreNotVisible() {
        setScreen(failureState(MarketDataCalculationUiError.SOURCE_UNAVAILABLE))

        MarketDataCalculationUiError.entries.forEach { error ->
            composeRule.onAllNodesWithText(error.name).assertCountEquals(0)
        }
        MarketDataCalculationUiInputError.entries.forEach { error ->
            composeRule.onAllNodesWithText(error.name).assertCountEquals(0)
        }
    }

    @Test
    fun scenario19LocalTestDataAndNoAdviceNoticeIsVisible() {
        setScreen()

        composeRule.onNodeWithText(
            "Lokale Testdaten – keine echten Marktdaten und keine Anlageberatung."
        ).assertIsDisplayed()
    }

    @Test
    fun scenario20StatelessScreenApiHasNoServiceRepositoryPolicyOrViewModelDependency() {
        val screenMethods = Class.forName(
            "de.konavigator.app.debug.marketdata.MarketDataCalculationDemoScreenKt"
        ).declaredMethods.filter {
            Modifier.isPublic(it.modifiers) &&
                it.name == "MarketDataCalculationDemoScreen"
        }
        val apiTypeNames = screenMethods.flatMap { method ->
            method.parameterTypes.map { it.name } +
                method.genericParameterTypes.map { it.typeName }
        }
        val forbidden = listOf(
            "ApplicationService",
            "Repository",
            "Policy",
            "Orchestrator",
            "ViewModel"
        )

        assertEquals(1, screenMethods.size)
        assertTrue(
            apiTypeNames.none { typeName ->
                forbidden.any { typeName.contains(it, ignoreCase = true) }
            }
        )
    }

    private fun setScreen(
        state: MarketDataCalculationUiState = MarketDataCalculationUiState(),
        onProductIsinChanged: (String) -> Unit = {},
        onCalculationTypeChanged: (MarketDataCalculationType) -> Unit = {},
        onEvaluationTimeChanged: (String) -> Unit = {},
        onCalculateClicked: () -> Unit = {}
    ) {
        composeRule.setContent {
            KONavigatorTheme(dynamicColor = false) {
                MarketDataCalculationDemoScreen(
                    state = state,
                    onProductIsinChanged = onProductIsinChanged,
                    onCalculationTypeChanged = onCalculationTypeChanged,
                    onEvaluationTimeChanged = onEvaluationTimeChanged,
                    onCalculateClicked = onCalculateClicked
                )
            }
        }
    }

    private fun validState(
        submission: MarketDataCalculationUiSubmission =
            MarketDataCalculationUiSubmission.Idle
    ) = MarketDataCalculationUiState(
        productIsinInput = "DE000DEMO001",
        evaluationTimeEpochMillisInput = "1700000000000",
        submission = submission
    )

    private fun completedState(
        result: MarketDataCalculationUiResult
    ) = validState(
        submission = MarketDataCalculationUiSubmission.Completed(result)
    )

    private fun failureState(
        error: MarketDataCalculationUiError
    ) = completedState(MarketDataCalculationUiResult.Failure(error))

    private companion object {
        const val PRODUCT_ISIN_TAG = "market_data_demo_product_isin"
        const val EVALUATION_TIME_TAG = "market_data_demo_evaluation_time"
        const val PURCHASE_PRICE_TAG =
            "market_data_demo_calculation_type_purchase_price"
        const val SALE_PRICE_TAG = "market_data_demo_calculation_type_sale_price"
        const val SPREAD_TAG = "market_data_demo_calculation_type_spread"
        const val MID_TAG = "market_data_demo_calculation_type_mid"
        const val CALCULATE_BUTTON_TAG = "market_data_demo_calculate"
        const val LOADING_TAG = "market_data_demo_loading"
        const val RESULT_CARD_TAG = "market_data_demo_result_card"
        const val ERROR_CARD_TAG = "market_data_demo_error_card"
    }
}
