package de.konavigator.app.scenarios

import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.calculator.TradeCalculationResult
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TradeCalculationScenarioTest(
    private val displayName: String,
    private val scenario: TradeCalculationScenario
) {

    @Test
    fun realEngineMatchesFixedScenarioExpectation() {
        val context = "${scenario.name} [${scenario.group}] via $displayName"
        assertEquals(
            "$context | scenario metadata",
            "${scenario.group}: ${scenario.name}",
            displayName
        )
        val result = TradeCalculationEngine.calculateTrade(scenario.input)

        when (val expectation = scenario.expectation) {
            is TradeCalculationExpectation.Success ->
                assertSuccess(context, result, expectation)

            is TradeCalculationExpectation.Failure ->
                assertFailure(context, result, expectation)
        }
    }

    private fun assertSuccess(
        context: String,
        result: TradeCalculationResult,
        expectation: TradeCalculationExpectation.Success
    ) {
        assertTrue("$context | isValid", result.isValid)
        assertNull("$context | error", result.error)
        assertDoubleField(context, "underlyingPrice", expectation.expectedUnderlyingPrice, result.underlyingPrice)
        assertDoubleField(context, "knockoutPrice", expectation.expectedKnockoutPrice, result.knockoutPrice)
        assertDoubleField(context, "targetLeverage", expectation.expectedTargetLeverage, result.targetLeverage)
        assertDoubleField(
            context,
            "theoreticalValueInUnderlyingCurrency",
            expectation.expectedTheoreticalValueInUnderlyingCurrency,
            result.theoreticalValueInUnderlyingCurrency
        )
        assertDoubleField(
            context,
            "theoreticalProductValue",
            expectation.expectedTheoreticalProductValue,
            result.theoreticalProductValue
        )
        assertDoubleField(
            context,
            "underlyingExposureInProductCurrency",
            expectation.expectedUnderlyingExposureInProductCurrency,
            result.underlyingExposureInProductCurrency
        )
        assertDoubleField(
            context,
            "calculatedTheoreticalLeverageAtEntry",
            expectation.expectedCalculatedTheoreticalLeverageAtEntry,
            result.calculatedTheoreticalLeverageAtEntry
        )
        assertEquals(
            "$context | underlyingCurrency",
            expectation.expectedUnderlyingCurrency,
            result.underlyingCurrency
        )
        assertEquals(
            "$context | productCurrency",
            expectation.expectedProductCurrency,
            result.productCurrency
        )
        assertDoubleField(
            context,
            "distanceToKnockoutAbsolute",
            expectation.expectedDistanceToKnockoutAbsolute,
            result.distanceToKnockoutAbsolute
        )
        assertDoubleField(
            context,
            "distanceToKnockoutPercent",
            expectation.expectedDistanceToKnockoutPercent,
            result.distanceToKnockoutPercent
        )
    }

    private fun assertFailure(
        context: String,
        result: TradeCalculationResult,
        expectation: TradeCalculationExpectation.Failure
    ) {
        assertFalse("$context | isValid", result.isValid)
        assertEquals("$context | error", expectation.expectedError, result.error)
        assertNull("$context | underlyingPrice", result.underlyingPrice)
        assertNull("$context | knockoutPrice", result.knockoutPrice)
        assertNull("$context | targetLeverage", result.targetLeverage)
        assertNull(
            "$context | theoreticalValueInUnderlyingCurrency",
            result.theoreticalValueInUnderlyingCurrency
        )
        assertNull("$context | theoreticalProductValue", result.theoreticalProductValue)
        assertNull(
            "$context | underlyingExposureInProductCurrency",
            result.underlyingExposureInProductCurrency
        )
        assertNull(
            "$context | calculatedTheoreticalLeverageAtEntry",
            result.calculatedTheoreticalLeverageAtEntry
        )
        assertNull("$context | underlyingCurrency", result.underlyingCurrency)
        assertNull("$context | productCurrency", result.productCurrency)
        assertNull(
            "$context | distanceToKnockoutAbsolute",
            result.distanceToKnockoutAbsolute
        )
        assertNull(
            "$context | distanceToKnockoutPercent",
            result.distanceToKnockoutPercent
        )
    }

    private fun assertDoubleField(
        context: String,
        fieldName: String,
        expected: Double,
        actual: Double?
    ) {
        assertNotNull("$context | $fieldName", actual)
        val nonNullActual = requireNotNull(actual)
        val tolerance = max(ABSOLUTE_TOLERANCE, abs(expected) * RELATIVE_TOLERANCE)
        assertTrue(
            "$context | $fieldName | expected $expected, actual $nonNullActual, " +
                "tolerance $tolerance",
            abs(nonNullActual - expected) <= tolerance
        )
    }

    companion object {
        private const val ABSOLUTE_TOLERANCE = 1e-12
        private const val RELATIVE_TOLERANCE = 1e-12

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun scenarios(): List<Array<Any>> =
            TradeCalculationScenarioFixtures.scenarios.map { scenario ->
                arrayOf("${scenario.group}: ${scenario.name}", scenario)
            }
    }
}
