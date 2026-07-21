package de.konavigator.app.domain.tradeplanning

import de.konavigator.app.domain.model.TradeDirection
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryPriceRelationEvaluatorTest {

    @Test
    fun plannedEntryBelowCurrentPriceReturnsBelowCurrent() {
        assertSuccess(90.0, 100.0, EntryPriceRelation.BELOW_CURRENT)
    }

    @Test
    fun plannedEntryAboveCurrentPriceReturnsAboveCurrent() {
        assertSuccess(110.0, 100.0, EntryPriceRelation.ABOVE_CURRENT)
    }

    @Test
    fun exactlyEqualPricesReturnAtCurrent() {
        assertSuccess(100.0, 100.0, EntryPriceRelation.AT_CURRENT)
    }

    @Test
    fun nextDownFromCurrentPriceReturnsBelowCurrent() {
        val currentPrice = 100.0

        assertSuccess(
            plannedEntryPrice = Math.nextDown(currentPrice),
            currentPrice = currentPrice,
            expected = EntryPriceRelation.BELOW_CURRENT
        )
    }

    @Test
    fun nextUpFromCurrentPriceReturnsAboveCurrent() {
        val currentPrice = 100.0

        assertSuccess(
            plannedEntryPrice = Math.nextUp(currentPrice),
            currentPrice = currentPrice,
            expected = EntryPriceRelation.ABOVE_CURRENT
        )
    }

    @Test
    fun publicEvaluateSignatureContainsNoTradeDirectionOrBoolean() {
        val method = publicEvaluateMethod()

        assertEquals(
            listOf(java.lang.Double.TYPE, java.lang.Double.TYPE),
            method.parameterTypes.toList()
        )
        assertTrue(method.parameterTypes.none { it == TradeDirection::class.java })
        assertTrue(
            method.parameterTypes.none {
                it == java.lang.Boolean.TYPE || it == java.lang.Boolean::class.java
            }
        )
    }

    @Test
    fun zeroCurrentPriceReturnsInvalidCurrentPrice() {
        assertFailure(0.0, 100.0, EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE)
    }

    @Test
    fun negativeCurrentPriceReturnsInvalidCurrentPrice() {
        assertFailure(-1.0, 100.0, EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE)
    }

    @Test
    fun nanCurrentPriceReturnsInvalidCurrentPrice() {
        assertFailure(Double.NaN, 100.0, EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE)
    }

    @Test
    fun positiveInfiniteCurrentPriceReturnsInvalidCurrentPrice() {
        assertFailure(
            Double.POSITIVE_INFINITY,
            100.0,
            EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE
        )
    }

    @Test
    fun negativeInfiniteCurrentPriceReturnsInvalidCurrentPrice() {
        assertFailure(
            Double.NEGATIVE_INFINITY,
            100.0,
            EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE
        )
    }

    @Test
    fun zeroPlannedEntryPriceReturnsInvalidPlannedEntryPrice() {
        assertFailure(100.0, 0.0, EntryPriceRelationEvaluationError.INVALID_PLANNED_ENTRY_PRICE)
    }

    @Test
    fun negativePlannedEntryPriceReturnsInvalidPlannedEntryPrice() {
        assertFailure(100.0, -1.0, EntryPriceRelationEvaluationError.INVALID_PLANNED_ENTRY_PRICE)
    }

    @Test
    fun nanPlannedEntryPriceReturnsInvalidPlannedEntryPrice() {
        assertFailure(
            100.0,
            Double.NaN,
            EntryPriceRelationEvaluationError.INVALID_PLANNED_ENTRY_PRICE
        )
    }

    @Test
    fun positiveInfinitePlannedEntryPriceReturnsInvalidPlannedEntryPrice() {
        assertFailure(
            100.0,
            Double.POSITIVE_INFINITY,
            EntryPriceRelationEvaluationError.INVALID_PLANNED_ENTRY_PRICE
        )
    }

    @Test
    fun negativeInfinitePlannedEntryPriceReturnsInvalidPlannedEntryPrice() {
        assertFailure(
            100.0,
            Double.NEGATIVE_INFINITY,
            EntryPriceRelationEvaluationError.INVALID_PLANNED_ENTRY_PRICE
        )
    }

    @Test
    fun invalidCurrentPriceHasPriorityWhenBothPricesAreInvalid() {
        assertFailure(
            currentPrice = Double.NaN,
            plannedEntryPrice = Double.NEGATIVE_INFINITY,
            expected = EntryPriceRelationEvaluationError.INVALID_CURRENT_PRICE
        )
    }

    @Test
    fun repeatedIdenticalEvaluationReturnsIdenticalResult() {
        val first = EntryPriceRelationEvaluator.evaluate(100.0, 91.23456789)
        val second = EntryPriceRelationEvaluator.evaluate(100.0, 91.23456789)

        assertEquals(first, second)
    }

    @Test
    fun publicApiAndResultTypesContainNoAndroidComposeUiTextOrThrowableDependency() {
        assertEquals(
            listOf("BELOW_CURRENT", "AT_CURRENT", "ABOVE_CURRENT"),
            EntryPriceRelation.entries.map { it.name }
        )
        assertEquals(
            listOf("INVALID_CURRENT_PRICE", "INVALID_PLANNED_ENTRY_PRICE"),
            EntryPriceRelationEvaluationError.entries.map { it.name }
        )
        assertEquals(
            setOf("Success", "Failure"),
            EntryPriceRelationEvaluationResult::class.java.declaredClasses
                .map { it.simpleName }
                .toSet()
        )

        val forbiddenFragments = listOf("android", "compose", "ui", "text", "message")
        assertTrue(
            publicApiTypeNames().none { typeName ->
                forbiddenFragments.any { fragment ->
                    typeName.contains(fragment, ignoreCase = true)
                }
            }
        )
        assertFalse(
            resultFields().any { field ->
                Throwable::class.java.isAssignableFrom(field.type)
            }
        )
    }

    private fun assertSuccess(
        plannedEntryPrice: Double,
        currentPrice: Double,
        expected: EntryPriceRelation
    ) {
        assertEquals(
            EntryPriceRelationEvaluationResult.Success(expected),
            EntryPriceRelationEvaluator.evaluate(currentPrice, plannedEntryPrice)
        )
    }

    private fun assertFailure(
        currentPrice: Double,
        plannedEntryPrice: Double,
        expected: EntryPriceRelationEvaluationError
    ) {
        assertEquals(
            EntryPriceRelationEvaluationResult.Failure(expected),
            EntryPriceRelationEvaluator.evaluate(currentPrice, plannedEntryPrice)
        )
    }

    private fun publicEvaluateMethod() =
        EntryPriceRelationEvaluator::class.java.declaredMethods.single { method ->
            method.name == "evaluate" && Modifier.isPublic(method.modifiers)
        }

    private fun publicApiTypeNames(): Set<String> = buildSet {
        add(EntryPriceRelation::class.java.name)
        add(EntryPriceRelationEvaluationError::class.java.name)
        add(EntryPriceRelationEvaluationResult::class.java.name)
        publicEvaluateMethod().let { method ->
            add(method.returnType.name)
            add(method.genericReturnType.typeName)
            method.parameterTypes.forEach { add(it.name) }
            method.genericParameterTypes.forEach { add(it.typeName) }
        }
        resultFields().forEach { field -> add(field.type.name) }
    }

    private fun resultFields() =
        EntryPriceRelationEvaluationResult::class.java.declaredClasses
            .flatMap { resultType ->
                resultType.declaredFields.filterNot { field ->
                    Modifier.isStatic(field.modifiers) || field.isSynthetic
                }
            }
}
