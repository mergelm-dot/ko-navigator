package de.konavigator.app.application.tradeplanning

import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationInput
import de.konavigator.app.calculator.TradeCalculationResult
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
            leverage = 4.25,
            isLong = true,
            exchangeRate = 1.123456,
            ratio = 0.0375
        )

        assertEquals(TradeCalculationEngine.calculateTrade(input), service().execute(input))
    }

    @Test
    fun completeShortInputIsForwardedUnchanged() {
        val input = TradeCalculationInput(
            underlyingPrice = 876.54321,
            plannedEntryPrice = 234.56789,
            leverage = 7.75,
            isLong = false,
            exchangeRate = 0.912345,
            ratio = 0.0625
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
        assertEquals(expected, actual)
    }

    @Test
    fun invalidTradeCalculationResultAndStructuredErrorAreReturnedUnchanged() {
        val input = validInput().copy(leverage = 1.0)
        val expected = TradeCalculationEngine.calculateTrade(input)
        val actual = service().execute(input)

        assertFalse(actual.isValid)
        assertEquals(TradeCalculationError.INVALID_TARGET_LEVERAGE, actual.error)
        assertEquals(expected, actual)
    }

    @Test
    fun ratioAndExchangeRateAreForwardedUnchanged() {
        val input = validInput().copy(
            exchangeRate = 1.987654321,
            ratio = 0.123456789
        )

        assertEquals(TradeCalculationEngine.calculateTrade(input), service().execute(input))
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
        leverage = 5.0,
        isLong = true,
        exchangeRate = 1.0,
        ratio = 0.01
    )

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
