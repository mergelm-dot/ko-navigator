package de.konavigator.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnockoutProductSpecificationTest {

    @Test
    fun longProductSpecificationStoresAllRequiredFields() {
        val specification = createSpecification()

        assertEquals("DE000KO12345", specification.productIsin)
        assertEquals("KO1234", specification.productWkn)
        assertEquals("issuer-1", specification.issuerId)
        assertEquals("nvidia", specification.underlyingId)
        assertEquals(TradeDirection.LONG, specification.direction)
        assertEquals(78.0, specification.basePrice, TOLERANCE)
        assertEquals(80.0, specification.knockoutBarrier, TOLERANCE)
        assertEquals(0.1, specification.ratio, TOLERANCE)
        assertEquals("USD", specification.underlyingCurrency)
        assertEquals("EUR", specification.productCurrency)
    }

    @Test
    fun shortProductSpecificationUsesShortDirection() {
        val specification = createSpecification(
            direction = TradeDirection.SHORT
        )

        assertEquals(TradeDirection.SHORT, specification.direction)
    }

    @Test
    fun basePriceAndKnockoutBarrierRemainSeparate() {
        val specification = createSpecification(
            basePrice = 78.0,
            knockoutBarrier = 80.0
        )

        assertEquals(78.0, specification.basePrice, TOLERANCE)
        assertEquals(80.0, specification.knockoutBarrier, TOLERANCE)
        assertNotEquals(specification.basePrice, specification.knockoutBarrier, TOLERANCE)
    }

    @Test
    fun productIsinAndUnderlyingIdRemainSeparate() {
        val specification = createSpecification(
            productIsin = "DE000KO12345",
            underlyingId = "nvidia"
        )

        assertEquals("DE000KO12345", specification.productIsin)
        assertEquals("nvidia", specification.underlyingId)
        assertNotEquals(specification.productIsin, specification.underlyingId)
    }

    @Test
    fun productAndUnderlyingCurrencyCanDiffer() {
        val specification = createSpecification(
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        )

        assertEquals("USD", specification.underlyingCurrency)
        assertEquals("EUR", specification.productCurrency)
        assertNotEquals(specification.underlyingCurrency, specification.productCurrency)
    }

    @Test
    fun optionalProductWknCanBeNull() {
        val specification = createSpecification(
            productWkn = null
        )

        assertNull(specification.productWkn)
    }

    @Test
    fun identicalSpecificationsAreEqual() {
        val first = createSpecification()
        val second = createSpecification()

        assertEquals(first, second)
    }

    @Test
    fun copyCanChangeKnockoutBarrierWithoutChangingOtherFields() {
        val original = createSpecification()
        val updated = original.copy(knockoutBarrier = 85.0)

        assertEquals(85.0, updated.knockoutBarrier, TOLERANCE)
        assertEquals(original.productIsin, updated.productIsin)
        assertEquals(original.productWkn, updated.productWkn)
        assertEquals(original.issuerId, updated.issuerId)
        assertEquals(original.underlyingId, updated.underlyingId)
        assertEquals(original.direction, updated.direction)
        assertEquals(original.basePrice, updated.basePrice, TOLERANCE)
        assertEquals(original.ratio, updated.ratio, TOLERANCE)
        assertEquals(original.underlyingCurrency, updated.underlyingCurrency)
        assertEquals(original.productCurrency, updated.productCurrency)
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
        const val TOLERANCE = 1e-9
    }
}
