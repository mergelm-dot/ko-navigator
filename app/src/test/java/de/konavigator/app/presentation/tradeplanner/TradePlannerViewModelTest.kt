package de.konavigator.app.presentation.tradeplanner

import de.konavigator.app.application.tradeplanning.TradePlanningApplicationService
import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.tradeplanning.EntryPriceRelation
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradePlannerViewModelTest {

    @Test
    fun scenario01InitialStateContainsDefaultsLongAndIdle() {
        val state = viewModel().uiState.value

        assertEquals("100,00", state.currentUnderlyingPriceInput)
        assertEquals("95,00", state.plannedEntryPriceInput)
        assertEquals("3", state.targetLeverageInput)
        assertEquals(TradeDirection.LONG, state.direction)
        assertEquals(TradePlannerUiSubmission.Idle, state.submission)
    }

    @Test
    fun scenario02CurrentPriceInputIsStoredExactly() {
        val viewModel = viewModel()
        val input = " 101,234500 "

        viewModel.onCurrentPriceChanged(input)

        assertEquals(input, viewModel.uiState.value.currentUnderlyingPriceInput)
    }

    @Test
    fun scenario03PlannedEntryPriceInputIsStoredExactly() {
        val viewModel = viewModel()
        val input = " 91.234500 "

        viewModel.onPlannedEntryPriceChanged(input)

        assertEquals(input, viewModel.uiState.value.plannedEntryPriceInput)
    }

    @Test
    fun scenario04TargetLeverageInputIsStoredExactly() {
        val viewModel = viewModel()
        val input = " 4,2500 "

        viewModel.onTargetLeverageChanged(input)

        assertEquals(input, viewModel.uiState.value.targetLeverageInput)
    }

    @Test
    fun scenario05DirectionIsChangedWithTypedValue() {
        val viewModel = viewModel()

        viewModel.onDirectionChanged(TradeDirection.SHORT)

        assertEquals(TradeDirection.SHORT, viewModel.uiState.value.direction)
    }

    @Test
    fun scenario06EveryInputOrDirectionChangeResetsSubmissionToIdle() {
        val changes = listOf<(TradePlannerViewModel) -> Unit>(
            { it.onCurrentPriceChanged("101") },
            { it.onPlannedEntryPriceChanged("94") },
            { it.onTargetLeverageChanged("4") },
            { it.onDirectionChanged(TradeDirection.SHORT) }
        )

        changes.forEach { change ->
            val viewModel = completedViewModel()

            change(viewModel)

            assertEquals(TradePlannerUiSubmission.Idle, viewModel.uiState.value.submission)
        }
    }

    @Test
    fun scenario07BlankCurrentPriceReturnsRequiredError() {
        val viewModel = viewModel()
        viewModel.onCurrentPriceChanged("   ")

        viewModel.onCalculateClicked()

        assertEquals(
            listOf(TradePlannerUiInputError.CURRENT_PRICE_REQUIRED),
            invalidInputErrors(viewModel)
        )
    }

    @Test
    fun scenario08InvalidCurrentPricesReturnInvalidErrorInStableOrder() {
        listOf("not-a-number", "NaN", "Infinity", "-Infinity", "0", "-1").forEach { input ->
            val viewModel = viewModel()
            viewModel.onCurrentPriceChanged(input)

            viewModel.onCalculateClicked()

            assertEquals(
                listOf(TradePlannerUiInputError.CURRENT_PRICE_INVALID),
                invalidInputErrors(viewModel)
            )
        }

        val allInvalid = viewModel().apply {
            onCurrentPriceChanged("invalid")
            onPlannedEntryPriceChanged("0")
            onTargetLeverageChanged("1")
            onCalculateClicked()
        }
        assertEquals(
            listOf(
                TradePlannerUiInputError.CURRENT_PRICE_INVALID,
                TradePlannerUiInputError.PLANNED_ENTRY_PRICE_INVALID,
                TradePlannerUiInputError.TARGET_LEVERAGE_INVALID
            ),
            invalidInputErrors(allInvalid)
        )
    }

    @Test
    fun scenario09BlankPlannedEntryPriceReturnsRequiredError() {
        val viewModel = viewModel()
        viewModel.onPlannedEntryPriceChanged("\t")

        viewModel.onCalculateClicked()

        assertEquals(
            listOf(TradePlannerUiInputError.PLANNED_ENTRY_PRICE_REQUIRED),
            invalidInputErrors(viewModel)
        )
    }

    @Test
    fun scenario10InvalidPlannedEntryPricesReturnInvalidError() {
        listOf("not-a-number", "NaN", "Infinity", "-Infinity", "0", "-1").forEach { input ->
            val viewModel = viewModel()
            viewModel.onPlannedEntryPriceChanged(input)

            viewModel.onCalculateClicked()

            assertEquals(
                listOf(TradePlannerUiInputError.PLANNED_ENTRY_PRICE_INVALID),
                invalidInputErrors(viewModel)
            )
        }
    }

    @Test
    fun scenario11BlankTargetLeverageReturnsRequiredError() {
        val viewModel = viewModel()
        viewModel.onTargetLeverageChanged("\r\n")

        viewModel.onCalculateClicked()

        assertEquals(
            listOf(TradePlannerUiInputError.TARGET_LEVERAGE_REQUIRED),
            invalidInputErrors(viewModel)
        )
    }

    @Test
    fun scenario12InvalidTargetLeveragesReturnInvalidError() {
        listOf("not-a-number", "NaN", "Infinity", "-Infinity", "-1", "0", "1").forEach {
                input ->
            val viewModel = viewModel()
            viewModel.onTargetLeverageChanged(input)

            viewModel.onCalculateClicked()

            assertEquals(
                listOf(TradePlannerUiInputError.TARGET_LEVERAGE_INVALID),
                invalidInputErrors(viewModel)
            )
        }
    }

    @Test
    fun scenario13DecimalCommaIsParsedWithoutChangingStoredInputs() {
        val viewModel = viewModel()
        viewModel.onCurrentPriceChanged(" 100,50 ")
        viewModel.onPlannedEntryPriceChanged(" 95,25 ")
        viewModel.onTargetLeverageChanged(" 3,50 ")

        viewModel.onCalculateClicked()

        assertEquals(" 100,50 ", viewModel.uiState.value.currentUnderlyingPriceInput)
        assertEquals(" 95,25 ", viewModel.uiState.value.plannedEntryPriceInput)
        assertEquals(" 3,50 ", viewModel.uiState.value.targetLeverageInput)
        assertEquals(EntryPriceRelation.BELOW_CURRENT, successResult(viewModel).relation)
    }

    @Test
    fun scenario14DecimalPointIsParsed() {
        val viewModel = viewModel()
        viewModel.onCurrentPriceChanged("100.50")
        viewModel.onPlannedEntryPriceChanged("95.25")
        viewModel.onTargetLeverageChanged("3.50")

        viewModel.onCalculateClicked()

        assertEquals(EntryPriceRelation.BELOW_CURRENT, successResult(viewModel).relation)
    }

    @Test
    fun scenario15EntryBelowCurrentReturnsBelowCurrent() {
        assertEquals(
            EntryPriceRelation.BELOW_CURRENT,
            calculatedRelation(currentPrice = "100", plannedEntryPrice = "90")
        )
    }

    @Test
    fun scenario16ExactEqualityReturnsAtCurrent() {
        assertEquals(
            EntryPriceRelation.AT_CURRENT,
            calculatedRelation(currentPrice = "100", plannedEntryPrice = "100")
        )
    }

    @Test
    fun scenario17EntryAboveCurrentReturnsAboveCurrent() {
        assertEquals(
            EntryPriceRelation.ABOVE_CURRENT,
            calculatedRelation(currentPrice = "100", plannedEntryPrice = "110")
        )
    }

    @Test
    fun scenario18LongMapsToTrue() {
        assertTrue(
            createTradeCalculationInput(100.0, 95.0, 3.0, TradeDirection.LONG).isLong
        )
    }

    @Test
    fun scenario19ShortMapsToFalse() {
        assertFalse(
            createTradeCalculationInput(100.0, 105.0, 3.0, TradeDirection.SHORT).isLong
        )
    }

    @Test
    fun scenario20CompleteTradeCalculationInputContainsExplicitTransitionalAssumptions() {
        val input = createTradeCalculationInput(
            currentUnderlyingPrice = 123.456789,
            plannedEntryPrice = 98.7654321,
            targetLeverage = 4.25,
            direction = TradeDirection.SHORT
        )

        assertEquals(123.456789, input.underlyingPrice, 0.0)
        assertEquals(98.7654321, input.plannedEntryPrice, 0.0)
        assertEquals(4.25, input.targetLeverage, 0.0)
        assertFalse(input.isLong)
        assertEquals(0.01, input.ratio, 0.0)
        assertEquals(
            CurrencyConversion.SameCurrency(currencyCode("XXX")),
            input.currencyConversion
        )
    }

    @Test
    fun scenario21ValidDomainResultMapsCompletelyAndWithoutRounding() {
        val result = validDomainResult(
            theoreticalProductValue = 1.234567891,
            knockoutPrice = 76.543219876,
            distanceAbsolute = 18.706780124,
            distancePercent = 19.6407136488189
        )

        assertEquals(
            TradePlannerUiResult.Success(
                relation = EntryPriceRelation.BELOW_CURRENT,
                theoreticalProductValue = 1.234567891,
                knockoutPrice = 76.543219876,
                distanceToKnockoutAbsolute = 18.706780124,
                distanceToKnockoutPercent = 19.6407136488189
            ),
            result.toTradePlannerUiResult(EntryPriceRelation.BELOW_CURRENT)
        )
    }

    @Test
    fun scenario22AllDomainErrorsAndInconsistentResultsMapCorrectly() {
        val mappings = listOf(
            TradeCalculationError.INVALID_PLANNED_ENTRY_PRICE to
                TradePlannerUiCalculationError.INVALID_PLANNED_ENTRY_PRICE,
            TradeCalculationError.INVALID_TARGET_LEVERAGE to
                TradePlannerUiCalculationError.INVALID_TARGET_LEVERAGE,
            TradeCalculationError.INVALID_RATIO to
                TradePlannerUiCalculationError.INVALID_RATIO,
            TradeCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE to
                TradePlannerUiCalculationError.INVALID_DERIVED_KNOCKOUT_PRICE,
            TradeCalculationError.INVALID_EXCHANGE_RATE to
                TradePlannerUiCalculationError.INVALID_EXCHANGE_RATE,
            TradeCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE to
                TradePlannerUiCalculationError.INVALID_THEORETICAL_PRODUCT_VALUE
        )

        mappings.forEach { (domainError, uiError) ->
            val result = invalidDomainResult(error = domainError)
                .toTradePlannerUiResult(EntryPriceRelation.AT_CURRENT)
            assertEquals(TradePlannerUiResult.Failure(uiError), result)
        }

        val inconsistentResults = listOf(
            validDomainResult().copy(error = TradeCalculationError.INVALID_TARGET_LEVERAGE),
            invalidDomainResult(error = null),
            invalidDomainResult(
                error = TradeCalculationError.INVALID_TARGET_LEVERAGE
            ).copy(theoreticalProductValue = 1.0)
        )
        inconsistentResults.forEach { result ->
            assertEquals(
                TradePlannerUiResult.Failure(
                    TradePlannerUiCalculationError.INCONSISTENT_CALCULATION_RESULT
                ),
                result.toTradePlannerUiResult(EntryPriceRelation.AT_CURRENT)
            )
        }
    }

    @Test
    fun scenario23DomainCurrencyTransparencyIsNotAddedToCurrentPresentationContract() {
        val first = validDomainResult(
            theoreticalUnderlyingValue = 1.5,
            underlyingCurrency = currencyCode("USD"),
            productCurrency = currencyCode("EUR")
        )
            .toTradePlannerUiResult(EntryPriceRelation.BELOW_CURRENT)
        val second = validDomainResult(
            theoreticalUnderlyingValue = 1.25,
            underlyingCurrency = currencyCode("EUR"),
            productCurrency = currencyCode("EUR")
        )
            .toTradePlannerUiResult(EntryPriceRelation.BELOW_CURRENT)

        assertEquals(first, second)
        assertTrue(
            presentationFields().none {
                it.name.contains("currency", ignoreCase = true) ||
                    it.name.contains("ratio", ignoreCase = true)
            }
        )
    }

    @Test
    fun scenario24ViewModelDoesNotRoundInputOrResult() {
        val input = createTradeCalculationInput(
            currentUnderlyingPrice = 123.456789123,
            plannedEntryPrice = 98.765432198,
            targetLeverage = 4.123456789,
            direction = TradeDirection.LONG
        )
        assertEquals(123.456789123, input.underlyingPrice, 0.0)
        assertEquals(98.765432198, input.plannedEntryPrice, 0.0)
        assertEquals(4.123456789, input.targetLeverage, 0.0)

        val mapped = validDomainResult(
            theoreticalProductValue = 1.000000009,
            knockoutPrice = 2.000000008,
            distanceAbsolute = 3.000000007,
            distancePercent = 4.000000006
        ).toTradePlannerUiResult(EntryPriceRelation.ABOVE_CURRENT)
        assertEquals(
            1.000000009,
            (mapped as TradePlannerUiResult.Success).theoreticalProductValue,
            0.0
        )
    }

    @Test
    fun scenario25PublicApiAndImplementationHaveNoSystemTimeDependency() {
        val typeNames = buildSet {
            TradePlannerViewModel::class.java.declaredFields.forEach { add(it.type.name) }
            TradePlannerViewModel::class.java.declaredMethods.forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { add(it.name) }
            }
        }
        val forbidden = listOf("java.time", "clock", "calendar", "date")

        assertTrue(typeNames.none { name -> forbidden.any { name.contains(it, true) } })
        assertTrue(
            TradePlannerViewModel::class.java.declaredMethods.none {
                it.name.contains("time", ignoreCase = true) ||
                    it.name.contains("now", ignoreCase = true)
            }
        )
    }

    @Test
    fun scenario26ConstructorAndPublicApiHaveNoRepositoryOrMarketDataDependency() {
        assertEquals(
            listOf(TradePlanningApplicationService::class.java),
            TradePlannerViewModel::class.java.constructors.single().parameterTypes.toList()
        )

        val forbidden = listOf("repository", "marketdata")
        assertTrue(
            publicApiTypeNames().none { name -> forbidden.any { name.contains(it, true) } }
        )
    }

    @Test
    fun scenario27SubmissionContractHasNoLoadingStateAndExactTypes() {
        assertEquals(
            setOf("Idle", "InvalidInput", "Completed"),
            TradePlannerUiSubmission::class.java.declaredClasses.map { it.simpleName }.toSet()
        )
        assertEquals(
            listOf(
                "CURRENT_PRICE_REQUIRED",
                "CURRENT_PRICE_INVALID",
                "PLANNED_ENTRY_PRICE_REQUIRED",
                "PLANNED_ENTRY_PRICE_INVALID",
                "TARGET_LEVERAGE_REQUIRED",
                "TARGET_LEVERAGE_INVALID"
            ),
            TradePlannerUiInputError.entries.map { it.name }
        )
    }

    @Test
    fun scenario28RepeatedIdenticalCalculationProducesIdenticalState() {
        val viewModel = viewModel()

        viewModel.onCalculateClicked()
        val first = viewModel.uiState.value
        viewModel.onCalculateClicked()
        val second = viewModel.uiState.value

        assertEquals(first, second)
    }

    @Test
    fun scenario29InputChangeAfterCompletedResetsSubmissionToIdle() {
        val viewModel = completedViewModel()
        assertTrue(viewModel.uiState.value.submission is TradePlannerUiSubmission.Completed)

        viewModel.onCurrentPriceChanged("101")

        assertEquals(TradePlannerUiSubmission.Idle, viewModel.uiState.value.submission)
    }

    @Test
    fun scenario30PublicContractHasExactMethodsAndNoContextResourceOrComposeDependency() {
        assertEquals(
            setOf(
                "onCurrentPriceChanged",
                "onPlannedEntryPriceChanged",
                "onTargetLeverageChanged",
                "onDirectionChanged",
                "onCalculateClicked"
            ),
            TradePlannerViewModel::class.java.declaredMethods
                .filter { Modifier.isPublic(it.modifiers) && it.name.startsWith("on") }
                .map { it.name }
                .toSet()
        )
        assertEquals(
            30,
            TradePlannerViewModelTest::class.java.declaredMethods
                .count { it.getAnnotation(Test::class.java) != null }
        )
        assertEquals(
            setOf("Success", "Failure"),
            TradePlannerUiResult::class.java.declaredClasses.map { it.simpleName }.toSet()
        )
        assertEquals(7, TradePlannerUiCalculationError.entries.size)

        val forbidden = listOf(
            "android.content.Context",
            "resource",
            "compose",
            "throwable",
            "message",
            "text"
        )
        assertTrue(
            publicApiTypeNames().none { name -> forbidden.any { name.contains(it, true) } }
        )
        assertTrue(
            presentationFields().none { field ->
                Throwable::class.java.isAssignableFrom(field.type)
            }
        )
    }

    private fun viewModel() = TradePlannerViewModel(
        TradePlanningApplicationService(TradeCalculationEngine)
    )

    private fun completedViewModel() = viewModel().apply { onCalculateClicked() }

    private fun calculatedRelation(
        currentPrice: String,
        plannedEntryPrice: String
    ): EntryPriceRelation {
        val viewModel = viewModel()
        viewModel.onCurrentPriceChanged(currentPrice)
        viewModel.onPlannedEntryPriceChanged(plannedEntryPrice)
        viewModel.onCalculateClicked()
        return successResult(viewModel).relation
    }

    private fun successResult(viewModel: TradePlannerViewModel) =
        ((viewModel.uiState.value.submission as TradePlannerUiSubmission.Completed).result as
            TradePlannerUiResult.Success)

    private fun invalidInputErrors(viewModel: TradePlannerViewModel) =
        (viewModel.uiState.value.submission as TradePlannerUiSubmission.InvalidInput).errors

    private fun validDomainResult(
        theoreticalUnderlyingValue: Double = 1.25,
        theoreticalProductValue: Double = 1.25,
        underlyingCurrency: CurrencyCode = currencyCode("EUR"),
        productCurrency: CurrencyCode = currencyCode("EUR"),
        knockoutPrice: Double = 75.0,
        distanceAbsolute: Double = 20.0,
        distancePercent: Double = 21.05263157894737
    ) = TradeCalculationResult(
        isValid = true,
        underlyingPrice = 95.0,
        knockoutPrice = knockoutPrice,
        theoreticalValueInUnderlyingCurrency = theoreticalUnderlyingValue,
        theoreticalProductValue = theoreticalProductValue,
        underlyingCurrency = underlyingCurrency,
        productCurrency = productCurrency,
        distanceToKnockoutAbsolute = distanceAbsolute,
        distanceToKnockoutPercent = distancePercent,
        error = null
    )

    private fun invalidDomainResult(
        error: TradeCalculationError?
    ) = TradeCalculationResult(
        isValid = false,
        underlyingPrice = null,
        knockoutPrice = null,
        theoreticalValueInUnderlyingCurrency = null,
        theoreticalProductValue = null,
        underlyingCurrency = null,
        productCurrency = null,
        distanceToKnockoutAbsolute = null,
        distanceToKnockoutPercent = null,
        error = error
    )

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid test currency: ${result.error}")
        }

    private fun publicApiTypeNames(): Set<String> = buildSet {
        TradePlannerViewModel::class.java.constructors.forEach { constructor ->
            constructor.parameterTypes.forEach { add(it.name) }
            constructor.genericParameterTypes.forEach { add(it.typeName) }
        }
        TradePlannerViewModel::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .forEach { method ->
                add(method.returnType.name)
                add(method.genericReturnType.typeName)
                method.parameterTypes.forEach { add(it.name) }
                method.genericParameterTypes.forEach { add(it.typeName) }
            }
    }

    private fun presentationFields() =
        (
            TradePlannerUiState::class.java.declaredClasses +
                TradePlannerUiSubmission::class.java.declaredClasses +
                TradePlannerUiResult::class.java.declaredClasses
        ).flatMap { type ->
            type.declaredFields.filterNot {
                Modifier.isStatic(it.modifiers) || it.isSynthetic
            }
        }
}
