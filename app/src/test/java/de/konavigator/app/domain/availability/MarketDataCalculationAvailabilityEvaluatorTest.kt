package de.konavigator.app.domain.availability

import de.konavigator.app.domain.model.KnockoutProductMarketData
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculationAvailabilityEvaluatorTest {

    @Test
    fun calculationTypeEnumContainsExactlyFourCodes() {
        assertEquals(
            listOf(PURCHASE_PRICE, SALE_PRICE, SPREAD, MID),
            MarketDataCalculationType.entries
        )
    }

    @Test
    fun availabilityErrorEnumContainsExactlyThreeCodes() {
        assertEquals(
            listOf(MISSING_BID, MISSING_ASK, BID_NOT_POSITIVE_FOR_SALE),
            MarketDataCalculationAvailabilityError.entries
        )
    }

    @Test
    fun evaluatorIsAnObject() {
        assertSame(
            MarketDataCalculationAvailabilityEvaluator,
            MarketDataCalculationAvailabilityEvaluator
        )
    }

    @Test
    fun evaluatorExposesExactlyOnePublicEvaluateFunction() {
        val publicMethods = MarketDataCalculationAvailabilityEvaluator::class.java
            .declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }

        assertEquals(listOf("evaluate"), publicMethods.map { it.name })
    }

    @Test
    fun resultTypeContainsNoNumericValues() {
        val fieldTypes = resultFieldTypes()

        assertTrue(fieldTypes.none(::isNumericType))
    }

    @Test
    fun resultTypeContainsNoMessagesOrUserTexts() {
        val fieldNames = resultClasses().flatMap { type ->
            type.declaredFields.map { it.name.lowercase() }
        }

        assertTrue(fieldNames.none { it.contains("message") || it.contains("text") })
    }

    @Test
    fun publicApiContainsNoAndroidOrComposeTypes() {
        val publicApiTypeNames = MarketDataCalculationAvailabilityEvaluator::class.java
            .declaredMethods
            .flatMap { method -> method.parameterTypes.toList() + method.returnType }
            .map { it.name }

        assertTrue(publicApiTypeNames.none { it.startsWith("android.") || it.contains("compose") })
    }

    @Test
    fun purchaseWithCompleteQuoteIsAvailable() {
        assertAvailable(PURCHASE_PRICE, createMarketData())
    }

    @Test
    fun purchaseWithAskOnlyQuoteIsAvailable() {
        assertAvailable(PURCHASE_PRICE, askOnlyQuote())
    }

    @Test
    fun purchaseDoesNotRequireBid() {
        assertAvailable(PURCHASE_PRICE, createMarketData(bid = null, bidTimestampEpochMillis = null))
    }

    @Test
    fun purchaseWithoutAskReturnsOnlyMissingAsk() {
        assertErrors(PURCHASE_PRICE, bidOnlyQuote(), MISSING_ASK)
    }

    @Test
    fun zeroBidDoesNotAffectPurchase() {
        assertAvailable(PURCHASE_PRICE, createMarketData(bid = 0.0))
    }

    @Test
    fun positiveBidDoesNotAffectPurchase() {
        assertAvailable(PURCHASE_PRICE, createMarketData(bid = 1.0, ask = 2.0))
        assertAvailable(PURCHASE_PRICE, createMarketData(bid = 2.0, ask = 2.0))
    }

    @Test
    fun purchaseRemainsAvailableWhenAskExistsAndBidIsMissing() {
        assertAvailable(PURCHASE_PRICE, askOnlyQuote(ask = 1.0))
    }

    @Test
    fun saleWithCompleteQuoteAndPositiveBidIsAvailable() {
        assertAvailable(SALE_PRICE, createMarketData())
    }

    @Test
    fun saleWithPositiveBidOnlyQuoteIsAvailable() {
        assertAvailable(SALE_PRICE, bidOnlyQuote())
    }

    @Test
    fun saleDoesNotRequireAsk() {
        assertAvailable(SALE_PRICE, createMarketData(ask = null, askTimestampEpochMillis = null))
    }

    @Test
    fun saleWithoutBidReturnsOnlyMissingBid() {
        assertErrors(SALE_PRICE, askOnlyQuote(), MISSING_BID)
    }

    @Test
    fun saleWithZeroBidReturnsOnlyBidNotPositiveForSale() {
        assertErrors(SALE_PRICE, createMarketData(bid = 0.0), BID_NOT_POSITIVE_FOR_SALE)
    }

    @Test
    fun missingBidDoesNotAlsoReturnBidNotPositiveForSale() {
        val errors = errorsOf(SALE_PRICE, askOnlyQuote())

        assertEquals(listOf(MISSING_BID), errors)
        assertTrue(BID_NOT_POSITIVE_FOR_SALE !in errors)
    }

    @Test
    fun askValueDoesNotAffectSale() {
        assertAvailable(SALE_PRICE, createMarketData(bid = 1.0, ask = 1.0))
        assertAvailable(SALE_PRICE, createMarketData(bid = 1.0, ask = 100.0))
    }

    @Test
    fun spreadWithCompleteQuoteIsAvailable() {
        assertAvailable(SPREAD, createMarketData())
    }

    @Test
    fun spreadAllowsZeroBidWithAsk() {
        assertAvailable(SPREAD, createMarketData(bid = 0.0))
    }

    @Test
    fun spreadWithBidOnlyReturnsOnlyMissingAsk() {
        assertErrors(SPREAD, bidOnlyQuote(), MISSING_ASK)
    }

    @Test
    fun spreadWithAskOnlyReturnsOnlyMissingBid() {
        assertErrors(SPREAD, askOnlyQuote(), MISSING_BID)
    }

    @Test
    fun spreadWithEmptyQuoteReturnsBothMissingSides() {
        assertErrors(SPREAD, emptyQuote(), MISSING_BID, MISSING_ASK)
    }

    @Test
    fun spreadErrorsUseStableOrder() {
        assertEquals(listOf(MISSING_BID, MISSING_ASK), errorsOf(SPREAD, emptyQuote()))
    }

    @Test
    fun midWithCompleteQuoteIsAvailable() {
        assertAvailable(MID, createMarketData())
    }

    @Test
    fun midAllowsZeroBidWithAsk() {
        assertAvailable(MID, createMarketData(bid = 0.0))
    }

    @Test
    fun midWithBidOnlyReturnsOnlyMissingAsk() {
        assertErrors(MID, bidOnlyQuote(), MISSING_ASK)
    }

    @Test
    fun midWithAskOnlyReturnsOnlyMissingBid() {
        assertErrors(MID, askOnlyQuote(), MISSING_BID)
    }

    @Test
    fun midWithEmptyQuoteReturnsBothMissingSides() {
        assertErrors(MID, emptyQuote(), MISSING_BID, MISSING_ASK)
    }

    @Test
    fun midErrorsUseStableOrder() {
        assertEquals(listOf(MISSING_BID, MISSING_ASK), errorsOf(MID, emptyQuote()))
    }

    @Test
    fun evaluatorDoesNotModifyMarketData() {
        val marketData = createMarketData()
        val original = marketData.copy()

        evaluate(SPREAD, marketData)

        assertEquals(original, marketData)
    }

    @Test
    fun evaluatorProducesNoNumericCalculationResult() {
        val result = evaluate(SPREAD, createMarketData())

        assertTrue(result !is Number)
        assertTrue(resultFieldTypes().none(::isNumericType))
    }

    @Test
    fun structurallyAvailableContainsNoValue() {
        val instanceFields = MarketDataCalculationAvailabilityResult.StructurallyAvailable::class.java
            .declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }

        assertTrue(instanceFields.isEmpty())
    }

    @Test
    fun structurallyUnavailableContainsOnlyAvailabilityErrors() {
        val result = evaluate(SPREAD, emptyQuote())
        val errorsField =
            MarketDataCalculationAvailabilityResult.StructurallyUnavailable::class.java
                .getDeclaredField("errors")

        assertTrue(result is MarketDataCalculationAvailabilityResult.StructurallyUnavailable)
        assertEquals(List::class.java, errorsField.type)
        assertEquals(
            "java.util.List<de.konavigator.app.domain.availability." +
                "MarketDataCalculationAvailabilityError>",
            errorsField.genericType.typeName
        )
    }

    @Test
    fun evaluatorApiHasNoValidatorDependency() {
        val apiTypeNames = evaluatorApiTypeNames()

        assertTrue(apiTypeNames.none { it.contains("Validator") || it.contains("Validation") })
    }

    @Test
    fun evaluatorApiHasNoCalculatorDependency() {
        val apiTypeNames = evaluatorApiTypeNames()

        assertTrue(apiTypeNames.none { it.contains("Calculator") })
    }

    @Test
    fun evaluatorApiRequiresNoSystemTimeInput() {
        val evaluateMethod = publicEvaluateMethod()

        assertTrue(evaluateMethod.parameterTypes.none { it == Long::class.javaPrimitiveType })
    }

    @Test
    fun resultTypeContainsNoFreshnessState() {
        val fieldNames = resultClasses().flatMap { type -> type.declaredFields.map { it.name } }

        assertTrue(fieldNames.none { it.contains("fresh", ignoreCase = true) || it.contains("stale", ignoreCase = true) })
    }

    @Test
    fun sourceDoesNotAffectAvailability() {
        val first = evaluate(SPREAD, createMarketData(sourceId = "source-a"))
        val second = evaluate(SPREAD, createMarketData(sourceId = "source-b"))

        assertEquals(first, second)
    }

    @Test
    fun productReferenceAndCurrencyDoNotAffectAvailability() {
        val first = evaluate(
            SPREAD,
            createMarketData(productIsin = "DE000TEST001", currency = "EUR")
        )
        val second = evaluate(
            SPREAD,
            createMarketData(productIsin = "DE000TEST002", currency = "USD")
        )

        assertEquals(first, second)
    }

    private fun assertAvailable(
        calculationType: MarketDataCalculationType,
        marketData: KnockoutProductMarketData
    ) {
        assertSame(
            MarketDataCalculationAvailabilityResult.StructurallyAvailable,
            evaluate(calculationType, marketData)
        )
    }

    private fun assertErrors(
        calculationType: MarketDataCalculationType,
        marketData: KnockoutProductMarketData,
        vararg expectedErrors: MarketDataCalculationAvailabilityError
    ) {
        assertEquals(expectedErrors.toList(), errorsOf(calculationType, marketData))
    }

    private fun errorsOf(
        calculationType: MarketDataCalculationType,
        marketData: KnockoutProductMarketData
    ): List<MarketDataCalculationAvailabilityError> {
        val result = evaluate(calculationType, marketData)
        assertTrue(result is MarketDataCalculationAvailabilityResult.StructurallyUnavailable)
        return (result as MarketDataCalculationAvailabilityResult.StructurallyUnavailable).errors
    }

    private fun evaluate(
        calculationType: MarketDataCalculationType,
        marketData: KnockoutProductMarketData
    ): MarketDataCalculationAvailabilityResult {
        return MarketDataCalculationAvailabilityEvaluator.evaluate(calculationType, marketData)
    }

    private fun bidOnlyQuote(bid: Double = 1.95): KnockoutProductMarketData {
        return createMarketData(ask = null, askTimestampEpochMillis = null, bid = bid)
    }

    private fun askOnlyQuote(ask: Double = 2.0): KnockoutProductMarketData {
        return createMarketData(bid = null, bidTimestampEpochMillis = null, ask = ask)
    }

    private fun emptyQuote(): KnockoutProductMarketData {
        return createMarketData(
            bid = null,
            ask = null,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )
    }

    private fun createMarketData(
        productIsin: String = "DE000TEST001",
        bid: Double? = 1.95,
        ask: Double? = 2.0,
        bidTimestampEpochMillis: Long? = 1_700_000_000_000L,
        askTimestampEpochMillis: Long? = 1_700_000_000_100L,
        currency: String = "EUR",
        sourceId: String = "source-1"
    ): KnockoutProductMarketData {
        return KnockoutProductMarketData(
            productIsin = productIsin,
            bid = bid,
            ask = ask,
            bidTimestampEpochMillis = bidTimestampEpochMillis,
            askTimestampEpochMillis = askTimestampEpochMillis,
            currency = currency,
            sourceId = sourceId
        )
    }

    private fun resultClasses(): List<Class<*>> {
        return listOf(
            MarketDataCalculationAvailabilityResult.StructurallyAvailable::class.java,
            MarketDataCalculationAvailabilityResult.StructurallyUnavailable::class.java
        )
    }

    private fun resultFieldTypes(): List<Class<*>> {
        return resultClasses().flatMap { type ->
            type.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) }
                .map { it.type }
        }
    }

    private fun evaluatorApiTypeNames(): List<String> {
        val method = publicEvaluateMethod()
        return method.parameterTypes.map { it.name } + method.returnType.name
    }

    private fun publicEvaluateMethod() =
        MarketDataCalculationAvailabilityEvaluator::class.java
            .declaredMethods
            .single { Modifier.isPublic(it.modifiers) && it.name == "evaluate" }

    private fun isNumericType(type: Class<*>): Boolean {
        return Number::class.java.isAssignableFrom(type) ||
            type in listOf<Class<*>>(
                java.lang.Byte.TYPE,
                java.lang.Short.TYPE,
                Integer.TYPE,
                java.lang.Long.TYPE,
                java.lang.Float.TYPE,
                java.lang.Double.TYPE
            )
    }

    private companion object {
        val PURCHASE_PRICE = MarketDataCalculationType.PURCHASE_PRICE
        val SALE_PRICE = MarketDataCalculationType.SALE_PRICE
        val SPREAD = MarketDataCalculationType.SPREAD
        val MID = MarketDataCalculationType.MID

        val MISSING_BID = MarketDataCalculationAvailabilityError.MISSING_BID
        val MISSING_ASK = MarketDataCalculationAvailabilityError.MISSING_ASK
        val BID_NOT_POSITIVE_FOR_SALE =
            MarketDataCalculationAvailabilityError.BID_NOT_POSITIVE_FOR_SALE
    }
}
