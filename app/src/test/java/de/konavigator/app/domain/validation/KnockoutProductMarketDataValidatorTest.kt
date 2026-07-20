package de.konavigator.app.domain.validation

import de.konavigator.app.domain.model.KnockoutProductMarketData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductMarketDataValidatorTest {

    @Test
    fun completeValidQuoteReturnsNoErrors() = assertNoErrors(createMarketData())

    @Test
    fun validBidOnlyQuoteReturnsNoErrors() = assertNoErrors(
        createMarketData(ask = null, askTimestampEpochMillis = null)
    )

    @Test
    fun validAskOnlyQuoteReturnsNoErrors() = assertNoErrors(
        createMarketData(bid = null, bidTimestampEpochMillis = null)
    )

    @Test
    fun completelyEmptyQuoteReturnsNoErrors() = assertNoErrors(emptyQuote())

    @Test
    fun zeroBidWithTimestampIsStructurallyValid() = assertNoErrors(createMarketData(bid = 0.0))

    @Test
    fun equalBidAndAskAreValid() = assertNoErrors(createMarketData(bid = 2.0, ask = 2.0))

    @Test
    fun bidBelowAskIsValid() = assertNoErrors(createMarketData(bid = 1.99, ask = 2.0))

    @Test
    fun negativeBidTimestampIsStructurallyValid() = assertNoErrors(
        createMarketData(bidTimestampEpochMillis = -1L)
    )

    @Test
    fun zeroBidTimestampIsStructurallyValid() = assertNoErrors(
        createMarketData(bidTimestampEpochMillis = 0L)
    )

    @Test
    fun veryLargeBidTimestampIsStructurallyValid() = assertNoErrors(
        createMarketData(bidTimestampEpochMillis = Long.MAX_VALUE)
    )

    @Test
    fun negativeAskTimestampIsStructurallyValid() = assertNoErrors(
        createMarketData(askTimestampEpochMillis = -1L)
    )

    @Test
    fun zeroAskTimestampIsStructurallyValid() = assertNoErrors(
        createMarketData(askTimestampEpochMillis = 0L)
    )

    @Test
    fun veryLargeAskTimestampIsStructurallyValid() = assertNoErrors(
        createMarketData(askTimestampEpochMillis = Long.MAX_VALUE)
    )

    @Test
    fun emptyProductIsinIsMissing() = assertSingleError(
        createMarketData(productIsin = ""),
        KnockoutProductMarketDataValidationError.MISSING_PRODUCT_ISIN
    )

    @Test
    fun blankProductIsinIsMissing() = assertSingleError(
        createMarketData(productIsin = "   "),
        KnockoutProductMarketDataValidationError.MISSING_PRODUCT_ISIN
    )

    @Test
    fun emptySourceIdIsMissing() = assertSingleError(
        createMarketData(sourceId = ""),
        KnockoutProductMarketDataValidationError.MISSING_SOURCE_ID
    )

    @Test
    fun blankSourceIdIsMissing() = assertSingleError(
        createMarketData(sourceId = "   "),
        KnockoutProductMarketDataValidationError.MISSING_SOURCE_ID
    )

    @Test
    fun emptyCurrencyIsInvalid() = assertInvalidCurrency("")

    @Test
    fun blankCurrencyIsInvalid() = assertInvalidCurrency("   ")

    @Test
    fun shortCurrencyIsInvalid() = assertInvalidCurrency("EU")

    @Test
    fun longCurrencyIsInvalid() = assertInvalidCurrency("EURO")

    @Test
    fun lowercaseCurrencyIsInvalid() = assertInvalidCurrency("eur")

    @Test
    fun currencyContainingDigitIsInvalid() = assertInvalidCurrency("E1R")

    @Test
    fun currencyContainingSpecialCharacterIsInvalid() = assertInvalidCurrency("E-R")

    @Test
    fun positiveFiniteBidIsValid() = assertNoErrors(createMarketData(bid = Double.MIN_VALUE))

    @Test
    fun negativeBidIsInvalid() = assertInvalidBid(-1.0)

    @Test
    fun nanBidIsInvalid() = assertInvalidBid(Double.NaN)

    @Test
    fun positiveInfiniteBidIsInvalid() = assertInvalidBid(Double.POSITIVE_INFINITY)

    @Test
    fun negativeInfiniteBidIsInvalid() = assertInvalidBid(Double.NEGATIVE_INFINITY)

    @Test
    fun positiveFiniteAskIsValid() = assertNoErrors(
        createMarketData(
            bid = null,
            ask = Double.MIN_VALUE,
            bidTimestampEpochMillis = null
        )
    )

    @Test
    fun zeroAskIsInvalid() = assertInvalidAsk(0.0)

    @Test
    fun negativeAskIsInvalid() = assertInvalidAsk(-1.0)

    @Test
    fun nanAskIsInvalid() = assertInvalidAsk(Double.NaN)

    @Test
    fun positiveInfiniteAskIsInvalid() = assertInvalidAsk(Double.POSITIVE_INFINITY)

    @Test
    fun negativeInfiniteAskIsInvalid() = assertInvalidAsk(Double.NEGATIVE_INFINITY)

    @Test
    fun bidWithTimestampIsCoherent() = assertNoErrors(
        createMarketData(ask = null, askTimestampEpochMillis = null)
    )

    @Test
    fun bidWithoutTimestampIsInvalid() = assertSingleError(
        createMarketData(ask = null, bidTimestampEpochMillis = null, askTimestampEpochMillis = null),
        KnockoutProductMarketDataValidationError.MISSING_BID_TIMESTAMP
    )

    @Test
    fun bidTimestampWithoutBidIsInvalid() = assertSingleError(
        createMarketData(bid = null, ask = null, askTimestampEpochMillis = null),
        KnockoutProductMarketDataValidationError.ORPHAN_BID_TIMESTAMP
    )

    @Test
    fun missingBidAndTimestampAreCoherent() = assertNoErrors(
        createMarketData(bid = null, ask = null, bidTimestampEpochMillis = null, askTimestampEpochMillis = null)
    )

    @Test
    fun askWithTimestampIsCoherent() = assertNoErrors(
        createMarketData(bid = null, bidTimestampEpochMillis = null)
    )

    @Test
    fun askWithoutTimestampIsInvalid() = assertSingleError(
        createMarketData(bid = null, bidTimestampEpochMillis = null, askTimestampEpochMillis = null),
        KnockoutProductMarketDataValidationError.MISSING_ASK_TIMESTAMP
    )

    @Test
    fun askTimestampWithoutAskIsInvalid() = assertSingleError(
        createMarketData(bid = null, ask = null, bidTimestampEpochMillis = null),
        KnockoutProductMarketDataValidationError.ORPHAN_ASK_TIMESTAMP
    )

    @Test
    fun missingAskAndTimestampAreCoherent() = assertNoErrors(
        createMarketData(ask = null, askTimestampEpochMillis = null)
    )

    @Test
    fun relationWithBidBelowAskIsValid() = assertNoErrors(createMarketData(bid = 1.0, ask = 2.0))

    @Test
    fun relationWithEqualBidAndAskIsValid() = assertNoErrors(createMarketData(bid = 2.0, ask = 2.0))

    @Test
    fun bidAboveAskProducesDataQualityError() = assertSingleError(
        createMarketData(bid = 2.01, ask = 2.0),
        KnockoutProductMarketDataValidationError.BID_ABOVE_ASK
    )

    @Test
    fun bidAboveAskDoesNotSwapValues() {
        val marketData = createMarketData(bid = 2.01, ask = 2.0)

        validate(marketData)

        assertEquals(2.01, marketData.bid!!, TOLERANCE)
        assertEquals(2.0, marketData.ask!!, TOLERANCE)
    }

    @Test
    fun invalidBidDoesNotAlsoProduceBidAboveAsk() {
        assertEquals(
            listOf(KnockoutProductMarketDataValidationError.INVALID_BID),
            validate(createMarketData(bid = Double.POSITIVE_INFINITY))
        )
    }

    @Test
    fun invalidAskDoesNotAlsoProduceBidAboveAsk() {
        assertEquals(
            listOf(KnockoutProductMarketDataValidationError.INVALID_ASK),
            validate(createMarketData(bid = 2.0, ask = 0.0))
        )
    }

    @Test
    fun missingTimestampsDoNotPreventBidAboveAskCheck() {
        assertEquals(
            listOf(
                KnockoutProductMarketDataValidationError.MISSING_BID_TIMESTAMP,
                KnockoutProductMarketDataValidationError.MISSING_ASK_TIMESTAMP,
                KnockoutProductMarketDataValidationError.BID_ABOVE_ASK
            ),
            validate(
                createMarketData(
                    bid = 2.01,
                    ask = 2.0,
                    bidTimestampEpochMillis = null,
                    askTimestampEpochMillis = null
                )
            )
        )
    }

    @Test
    fun multipleIndependentErrorsAreCollectedCompletely() {
        assertEquals(ALL_INDEPENDENT_ERRORS, validate(completelyInvalidMarketData()))
    }

    @Test
    fun multipleErrorsUseDocumentedStableOrder() {
        val errors = validate(completelyInvalidMarketData())

        assertEquals(ALL_INDEPENDENT_ERRORS, errors)
    }

    @Test
    fun eachValidationAspectProducesAtMostOneError() {
        val errors = validate(completelyInvalidMarketData())

        assertEquals(errors.size, errors.toSet().size)
    }

    @Test
    fun missingBidDoesNotProduceAnError() = assertNoErrors(
        createMarketData(bid = null, bidTimestampEpochMillis = null)
    )

    @Test
    fun missingAskDoesNotProduceAnError() = assertNoErrors(
        createMarketData(ask = null, askTimestampEpochMillis = null)
    )

    @Test
    fun emptyErrorListDoesNotRequireACompleteQuote() {
        val marketData = emptyQuote()

        assertNoErrors(marketData)
        assertNull(marketData.bid)
        assertNull(marketData.ask)
    }

    @Test
    fun validationDoesNotNormalizeCurrency() {
        val marketData = createMarketData(currency = "eur")

        validate(marketData)

        assertEquals("eur", marketData.currency)
    }

    @Test
    fun validationDoesNotNormalizeProductIsin() {
        val marketData = createMarketData(productIsin = " DE000TEST001 ")

        validate(marketData)

        assertEquals(" DE000TEST001 ", marketData.productIsin)
    }

    @Test
    fun validationDoesNotNormalizeSourceId() {
        val marketData = createMarketData(sourceId = " source-1 ")

        validate(marketData)

        assertEquals(" source-1 ", marketData.sourceId)
    }

    @Test
    fun invalidMarketDataCanBeConstructedWithoutException() {
        val marketData = completelyInvalidMarketData()

        assertEquals("", marketData.productIsin)
        assertTrue(marketData.bid!!.isNaN())
    }

    private fun assertInvalidCurrency(value: String) {
        assertSingleError(
            createMarketData(currency = value),
            KnockoutProductMarketDataValidationError.INVALID_CURRENCY
        )
    }

    private fun assertInvalidBid(value: Double) {
        assertSingleError(
            createMarketData(bid = value),
            KnockoutProductMarketDataValidationError.INVALID_BID
        )
    }

    private fun assertInvalidAsk(value: Double) {
        assertSingleError(
            createMarketData(ask = value),
            KnockoutProductMarketDataValidationError.INVALID_ASK
        )
    }

    private fun assertSingleError(
        marketData: KnockoutProductMarketData,
        expectedError: KnockoutProductMarketDataValidationError
    ) {
        assertEquals(listOf(expectedError), validate(marketData))
    }

    private fun assertNoErrors(marketData: KnockoutProductMarketData) {
        assertTrue(validate(marketData).isEmpty())
    }

    private fun validate(
        marketData: KnockoutProductMarketData
    ): List<KnockoutProductMarketDataValidationError> {
        return KnockoutProductMarketDataValidator.validate(marketData)
    }

    private fun emptyQuote(): KnockoutProductMarketData {
        return createMarketData(
            bid = null,
            ask = null,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )
    }

    private fun completelyInvalidMarketData(): KnockoutProductMarketData {
        return createMarketData(
            productIsin = "",
            sourceId = "",
            currency = "eur",
            bid = Double.NaN,
            ask = 0.0,
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
        sourceId: String = "issuer-demo"
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

    private companion object {
        const val TOLERANCE = 1e-9

        val ALL_INDEPENDENT_ERRORS = listOf(
            KnockoutProductMarketDataValidationError.MISSING_PRODUCT_ISIN,
            KnockoutProductMarketDataValidationError.MISSING_SOURCE_ID,
            KnockoutProductMarketDataValidationError.INVALID_CURRENCY,
            KnockoutProductMarketDataValidationError.INVALID_BID,
            KnockoutProductMarketDataValidationError.MISSING_BID_TIMESTAMP,
            KnockoutProductMarketDataValidationError.INVALID_ASK,
            KnockoutProductMarketDataValidationError.MISSING_ASK_TIMESTAMP
        )
    }
}
