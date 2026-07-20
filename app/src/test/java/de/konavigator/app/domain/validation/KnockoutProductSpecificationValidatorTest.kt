package de.konavigator.app.domain.validation

import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationValidatorTest {

    @Test
    fun validLongProductReturnsNoErrors() {
        assertNoErrors(createSpecification())
    }

    @Test
    fun validShortProductReturnsNoErrors() {
        assertNoErrors(createSpecification(direction = TradeDirection.SHORT))
    }

    @Test
    fun nullProductWknRemainsValid() {
        assertNoErrors(createSpecification(productWkn = null))
    }

    @Test
    fun differentBasePriceAndKnockoutBarrierRemainValid() {
        assertNoErrors(createSpecification(basePrice = 78.0, knockoutBarrier = 80.0))
    }

    @Test
    fun verySmallPositiveFiniteBasePriceRemainsValid() {
        assertNoErrors(createSpecification(basePrice = Double.MIN_VALUE))
    }

    @Test
    fun veryLargePositiveFiniteBasePriceRemainsValid() {
        assertNoErrors(createSpecification(basePrice = Double.MAX_VALUE))
    }

    @Test
    fun verySmallPositiveFiniteRatioRemainsValid() {
        assertNoErrors(createSpecification(ratio = Double.MIN_VALUE))
    }

    @Test
    fun veryLargePositiveFiniteRatioRemainsValid() {
        assertNoErrors(createSpecification(ratio = Double.MAX_VALUE))
    }

    @Test
    fun emptyProductIsinIsMissing() {
        assertSingleError(
            createSpecification(productIsin = ""),
            KnockoutProductValidationError.MISSING_PRODUCT_ISIN
        )
    }

    @Test
    fun blankProductIsinIsMissing() {
        assertSingleError(
            createSpecification(productIsin = "   "),
            KnockoutProductValidationError.MISSING_PRODUCT_ISIN
        )
    }

    @Test
    fun emptyPresentProductWknIsInvalid() {
        assertSingleError(
            createSpecification(productWkn = ""),
            KnockoutProductValidationError.INVALID_PRODUCT_WKN
        )
    }

    @Test
    fun blankPresentProductWknIsInvalid() {
        assertSingleError(
            createSpecification(productWkn = "   "),
            KnockoutProductValidationError.INVALID_PRODUCT_WKN
        )
    }

    @Test
    fun emptyIssuerIdIsMissing() {
        assertSingleError(
            createSpecification(issuerId = ""),
            KnockoutProductValidationError.MISSING_ISSUER_ID
        )
    }

    @Test
    fun blankIssuerIdIsMissing() {
        assertSingleError(
            createSpecification(issuerId = "   "),
            KnockoutProductValidationError.MISSING_ISSUER_ID
        )
    }

    @Test
    fun emptyUnderlyingIdIsMissing() {
        assertSingleError(
            createSpecification(underlyingId = ""),
            KnockoutProductValidationError.MISSING_UNDERLYING_ID
        )
    }

    @Test
    fun blankUnderlyingIdIsMissing() {
        assertSingleError(
            createSpecification(underlyingId = "   "),
            KnockoutProductValidationError.MISSING_UNDERLYING_ID
        )
    }

    @Test
    fun zeroBasePriceIsInvalid() {
        assertInvalidBasePrice(0.0)
    }

    @Test
    fun negativeBasePriceIsInvalid() {
        assertInvalidBasePrice(-1.0)
    }

    @Test
    fun nanBasePriceIsInvalid() {
        assertInvalidBasePrice(Double.NaN)
    }

    @Test
    fun positiveInfiniteBasePriceIsInvalid() {
        assertInvalidBasePrice(Double.POSITIVE_INFINITY)
    }

    @Test
    fun negativeInfiniteBasePriceIsInvalid() {
        assertInvalidBasePrice(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun zeroKnockoutBarrierIsInvalid() {
        assertInvalidKnockoutBarrier(0.0)
    }

    @Test
    fun negativeKnockoutBarrierIsInvalid() {
        assertInvalidKnockoutBarrier(-1.0)
    }

    @Test
    fun nanKnockoutBarrierIsInvalid() {
        assertInvalidKnockoutBarrier(Double.NaN)
    }

    @Test
    fun positiveInfiniteKnockoutBarrierIsInvalid() {
        assertInvalidKnockoutBarrier(Double.POSITIVE_INFINITY)
    }

    @Test
    fun negativeInfiniteKnockoutBarrierIsInvalid() {
        assertInvalidKnockoutBarrier(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun zeroRatioIsInvalid() {
        assertInvalidRatio(0.0)
    }

    @Test
    fun negativeRatioIsInvalid() {
        assertInvalidRatio(-1.0)
    }

    @Test
    fun nanRatioIsInvalid() {
        assertInvalidRatio(Double.NaN)
    }

    @Test
    fun positiveInfiniteRatioIsInvalid() {
        assertInvalidRatio(Double.POSITIVE_INFINITY)
    }

    @Test
    fun negativeInfiniteRatioIsInvalid() {
        assertInvalidRatio(Double.NEGATIVE_INFINITY)
    }

    @Test
    fun emptyUnderlyingCurrencyIsInvalid() {
        assertInvalidUnderlyingCurrency("")
    }

    @Test
    fun blankUnderlyingCurrencyIsInvalid() {
        assertInvalidUnderlyingCurrency("   ")
    }

    @Test
    fun shortUnderlyingCurrencyIsInvalid() {
        assertInvalidUnderlyingCurrency("EU")
    }

    @Test
    fun longUnderlyingCurrencyIsInvalid() {
        assertInvalidUnderlyingCurrency("EURO")
    }

    @Test
    fun lowercaseUnderlyingCurrencyIsInvalid() {
        assertInvalidUnderlyingCurrency("eur")
    }

    @Test
    fun underlyingCurrencyContainingDigitIsInvalid() {
        assertInvalidUnderlyingCurrency("E1R")
    }

    @Test
    fun underlyingCurrencyContainingSpecialCharacterIsInvalid() {
        assertInvalidUnderlyingCurrency("E-R")
    }

    @Test
    fun emptyProductCurrencyIsInvalid() {
        assertInvalidProductCurrency("")
    }

    @Test
    fun blankProductCurrencyIsInvalid() {
        assertInvalidProductCurrency("   ")
    }

    @Test
    fun shortProductCurrencyIsInvalid() {
        assertInvalidProductCurrency("EU")
    }

    @Test
    fun longProductCurrencyIsInvalid() {
        assertInvalidProductCurrency("EURO")
    }

    @Test
    fun lowercaseProductCurrencyIsInvalid() {
        assertInvalidProductCurrency("eur")
    }

    @Test
    fun productCurrencyContainingDigitIsInvalid() {
        assertInvalidProductCurrency("E1R")
    }

    @Test
    fun productCurrencyContainingSpecialCharacterIsInvalid() {
        assertInvalidProductCurrency("E-R")
    }

    @Test
    fun multipleIndependentErrorsAreCollectedCompletely() {
        val errors = validate(completelyInvalidSpecification())

        assertEquals(ALL_ERRORS_IN_STABLE_ORDER, errors)
    }

    @Test
    fun multipleErrorsUseDocumentedStableOrder() {
        val errors = validate(
            createSpecification(
                productIsin = "",
                productWkn = "",
                issuerId = "",
                underlyingId = "",
                basePrice = 0.0,
                knockoutBarrier = 0.0,
                ratio = 0.0,
                underlyingCurrency = "eur",
                productCurrency = "usd"
            )
        )

        assertEquals(ALL_ERRORS_IN_STABLE_ORDER, errors)
    }

    @Test
    fun eachFieldProducesAtMostOneError() {
        val errors = validate(completelyInvalidSpecification())

        assertEquals(9, errors.size)
        assertEquals(errors.size, errors.toSet().size)
    }

    @Test
    fun invalidProductCanBeConstructedWithoutException() {
        val specification = completelyInvalidSpecification()

        assertEquals("", specification.productIsin)
        assertEquals(Double.NaN, specification.basePrice, 0.0)
    }

    @Test
    fun validationDoesNotNormalizeInput() {
        val specification = createSpecification(
            productIsin = " DE000KO12345 ",
            underlyingCurrency = "eur"
        )

        validate(specification)

        assertEquals(" DE000KO12345 ", specification.productIsin)
        assertEquals("eur", specification.underlyingCurrency)
    }

    private fun assertInvalidBasePrice(value: Double) {
        assertSingleError(
            createSpecification(basePrice = value),
            KnockoutProductValidationError.INVALID_BASE_PRICE
        )
    }

    private fun assertInvalidKnockoutBarrier(value: Double) {
        assertSingleError(
            createSpecification(knockoutBarrier = value),
            KnockoutProductValidationError.INVALID_KNOCKOUT_BARRIER
        )
    }

    private fun assertInvalidRatio(value: Double) {
        assertSingleError(
            createSpecification(ratio = value),
            KnockoutProductValidationError.INVALID_RATIO
        )
    }

    private fun assertInvalidUnderlyingCurrency(value: String) {
        assertSingleError(
            createSpecification(underlyingCurrency = value),
            KnockoutProductValidationError.INVALID_UNDERLYING_CURRENCY
        )
    }

    private fun assertInvalidProductCurrency(value: String) {
        assertSingleError(
            createSpecification(productCurrency = value),
            KnockoutProductValidationError.INVALID_PRODUCT_CURRENCY
        )
    }

    private fun assertSingleError(
        specification: KnockoutProductSpecification,
        expectedError: KnockoutProductValidationError
    ) {
        assertEquals(listOf(expectedError), validate(specification))
    }

    private fun assertNoErrors(specification: KnockoutProductSpecification) {
        assertTrue(validate(specification).isEmpty())
    }

    private fun validate(
        specification: KnockoutProductSpecification
    ): List<KnockoutProductValidationError> {
        return KnockoutProductSpecificationValidator.validate(specification)
    }

    private fun completelyInvalidSpecification(): KnockoutProductSpecification {
        return createSpecification(
            productIsin = "",
            productWkn = "",
            issuerId = "",
            underlyingId = "",
            basePrice = Double.NaN,
            knockoutBarrier = Double.POSITIVE_INFINITY,
            ratio = -1.0,
            underlyingCurrency = "eur",
            productCurrency = "usd"
        )
    }

    private fun createSpecification(
        productIsin: String = "DE000KO12345",
        productWkn: String? = "KO1234",
        issuerId: String = "issuer-1",
        underlyingId: String = "nvidia",
        direction: TradeDirection = TradeDirection.LONG,
        basePrice: Double = 78.0,
        knockoutBarrier: Double = 80.0,
        ratio: Double = 0.1,
        underlyingCurrency: String = "USD",
        productCurrency: String = "EUR"
    ): KnockoutProductSpecification {
        return KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = productWkn,
            issuerId = issuerId,
            underlyingId = underlyingId,
            direction = direction,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = ratio,
            underlyingCurrency = underlyingCurrency,
            productCurrency = productCurrency
        )
    }

    private companion object {
        val ALL_ERRORS_IN_STABLE_ORDER = listOf(
            KnockoutProductValidationError.MISSING_PRODUCT_ISIN,
            KnockoutProductValidationError.INVALID_PRODUCT_WKN,
            KnockoutProductValidationError.MISSING_ISSUER_ID,
            KnockoutProductValidationError.MISSING_UNDERLYING_ID,
            KnockoutProductValidationError.INVALID_BASE_PRICE,
            KnockoutProductValidationError.INVALID_KNOCKOUT_BARRIER,
            KnockoutProductValidationError.INVALID_RATIO,
            KnockoutProductValidationError.INVALID_UNDERLYING_CURRENCY,
            KnockoutProductValidationError.INVALID_PRODUCT_CURRENCY
        )
    }
}
