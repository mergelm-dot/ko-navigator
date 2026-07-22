package de.konavigator.app.application.tradeplanning

import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradePlanningApplicationServiceTest {

    @Test
    fun constructorDependsOnlyOnTradeCalculationEngine() {
        assertEquals(
            listOf(TradeCalculationEngine::class.java),
            TradePlanningApplicationService::class.java.constructors
                .single()
                .parameterTypes
                .toList()
        )
    }

    @Test
    fun executeIsTheOnlyPublicBusinessMethod() {
        assertEquals(
            listOf("execute"),
            publicBusinessMethods().map { it.name }
        )
    }

    @Test
    fun executeIsSynchronousAndNotSuspend() {
        val execute = publicBusinessMethods().single()

        assertEquals(listOf(TradeCalculationInput::class.java), execute.parameterTypes.toList())
        assertEquals(TradeCalculationResult::class.java, execute.returnType)
        assertFalse(execute.parameterTypes.any { it.name == "kotlin.coroutines.Continuation" })
    }

    @Test
    fun completeLongInputIsForwardedUnchanged() {
        val input = TradeCalculationInput(
            underlyingPrice = 123.456789,
            plannedEntryPrice = 98.7654321,
            targetLeverage = 4.25,
            isLong = true,
            ratio = 0.0375,
            currencyConversion = crossCurrency("USD", "EUR", 1.123456)
        )

        assertEquals(TradeCalculationEngine.calculateTrade(input), service().execute(input))
    }

    @Test
    fun completeShortInputIsForwardedUnchanged() {
        val input = TradeCalculationInput(
            underlyingPrice = 876.54321,
            plannedEntryPrice = 234.56789,
            targetLeverage = 7.75,
            isLong = false,
            ratio = 0.0625,
            currencyConversion = crossCurrency("EUR", "USD", 0.912345)
        )

        assertEquals(TradeCalculationEngine.calculateTrade(input), service().execute(input))
    }

    @Test
    fun validTradeCalculationResultIsReturnedUnchanged() {
        val input = validInput()
        val expected = TradeCalculationEngine.calculateTrade(input)
        val actual = service().execute(input)

        assertTrue(actual.isValid)
        assertNull(actual.error)
        assertEquals(5.0, actual.targetLeverage!!, 0.0)
        assertEquals(1.0, actual.underlyingExposureInProductCurrency!!, 1e-12)
        assertEquals(5.0, actual.calculatedTheoreticalLeverageAtEntry!!, 1e-12)
        assertEquals(expected, actual)
    }

    @Test
    fun invalidTradeCalculationResultAndStructuredErrorAreReturnedUnchanged() {
        val input = validInput().copy(targetLeverage = 1.0)
        val expected = TradeCalculationEngine.calculateTrade(input)
        val actual = service().execute(input)

        assertFalse(actual.isValid)
        assertEquals(TradeCalculationError.INVALID_TARGET_LEVERAGE, actual.error)
        assertEquals(expected, actual)
    }

    @Test
    fun ratioAndCurrencyConversionAreForwardedUnchanged() {
        val conversion = crossCurrency("USD", "EUR", 1.987654321)
        val input = validInput().copy(
            ratio = 0.123456789,
            currencyConversion = conversion
        )

        assertEquals(TradeCalculationEngine.calculateTrade(input), service().execute(input))
        assertEquals(0.123456789, input.ratio, 0.0)
        assertEquals(conversion, input.currencyConversion)
    }

    @Test
    fun publicApiContainsNoRepositoryMarketDataAndroidComposeContextResourceOrUiTextDependency() {
        val forbiddenFragments = listOf(
            "repository",
            "marketdata",
            "android",
            "compose",
            "context",
            "resource",
            "java.lang.String"
        )

        assertTrue(
            publicApiTypeNames().none { typeName ->
                forbiddenFragments.any { fragment ->
                    typeName.contains(fragment, ignoreCase = true)
                }
            }
        )
    }

    private fun service() = TradePlanningApplicationService(TradeCalculationEngine)

    private fun validInput() = TradeCalculationInput(
        underlyingPrice = 110.0,
        plannedEntryPrice = 100.0,
        targetLeverage = 5.0,
        isLong = true,
        ratio = 0.01,
        currencyConversion = CurrencyConversion.SameCurrency(currencyCode("EUR"))
    )

    private fun crossCurrency(
        underlyingCurrency: String,
        productCurrency: String,
        rate: Double
    ): CurrencyConversion.CrossCurrency =
        when (
            val result = CurrencyConversion.CrossCurrency.create(
                currencyCode(underlyingCurrency),
                currencyCode(productCurrency),
                rate
            )
        ) {
            is CurrencyConversionCreationResult.Success -> result.conversion
            is CurrencyConversionCreationResult.Failure ->
                error("Unexpected invalid test conversion: ${result.error}")
        }

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid test currency: ${result.error}")
        }

    private fun publicBusinessMethods() =
        TradePlanningApplicationService::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }

    private fun publicApiTypeNames(): Set<String> = buildSet {
        TradePlanningApplicationService::class.java.constructors.forEach { constructor ->
            constructor.parameterTypes.forEach { add(it.name) }
            constructor.genericParameterTypes.forEach { add(it.typeName) }
        }
        publicBusinessMethods().forEach { method ->
            add(method.returnType.name)
            add(method.genericReturnType.typeName)
            method.parameterTypes.forEach { add(it.name) }
            method.genericParameterTypes.forEach { add(it.typeName) }
        }
    }
}
