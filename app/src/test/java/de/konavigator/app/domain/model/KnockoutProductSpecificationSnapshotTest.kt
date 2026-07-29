package de.konavigator.app.domain.model

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationSnapshotTest {

    @Test
    fun specificationIsStoredAsExactSameInstance() {
        val specification = specification()

        assertSame(specification, snapshot(specification = specification).specification)
    }

    @Test
    fun sourceIdPreservesWhitespaceAndCase() {
        val sourceId = "  Provider-Source Id  "

        assertEquals(sourceId, snapshot(sourceId = sourceId).sourceId)
    }

    @Test
    fun retrievedAtEpochMillisRemainsExact() {
        val retrievedAt = 9_223_372_036_854_775_000L

        assertEquals(retrievedAt, snapshot(retrievedAtEpochMillis = retrievedAt).retrievedAtEpochMillis)
    }

    @Test
    fun sourceTimestampEpochMillisMayBeNull() {
        assertNull(snapshot(sourceTimestampEpochMillis = null).sourceTimestampEpochMillis)
    }

    @Test
    fun retrievedAndSourceTimestampsRemainSeparate() {
        val snapshot = snapshot(
            retrievedAtEpochMillis = 123L,
            sourceTimestampEpochMillis = 456L
        )

        assertEquals(123L, snapshot.retrievedAtEpochMillis)
        assertEquals(456L, snapshot.sourceTimestampEpochMillis)
        assertNotEquals(snapshot.retrievedAtEpochMillis, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun negativeNearZeroAndLargeTimestampsAreNotChangedOrValidated() {
        listOf(
            Long.MIN_VALUE to -1L,
            -1L to 0L,
            0L to 1L,
            1L to Long.MAX_VALUE
        ).forEach { (retrievedAt, sourceTimestamp) ->
            val snapshot = snapshot(
                retrievedAtEpochMillis = retrievedAt,
                sourceTimestampEpochMillis = sourceTimestamp
            )

            assertEquals(retrievedAt, snapshot.retrievedAtEpochMillis)
            assertEquals(sourceTimestamp, snapshot.sourceTimestampEpochMillis)
        }
    }

    @Test
    fun completeSpecificationRemainsFullyUnchanged() {
        val specification = specification()
        val stored = snapshot(specification = specification).specification

        assertSame(specification, stored)
        assertEquals("DE000KO12345", stored.productIsin)
        assertEquals("KO1234", stored.productWkn)
        assertEquals("issuer-1", stored.issuerId)
        assertEquals("nvidia", stored.underlyingId)
        assertEquals(TradeDirection.LONG, stored.direction)
        assertEquals(78.0, stored.basePrice, TOLERANCE)
        assertEquals(80.0, stored.knockoutBarrier, TOLERANCE)
        assertEquals(0.1, stored.ratio, TOLERANCE)
        assertEquals("USD", stored.underlyingCurrency)
        assertEquals("EUR", stored.productCurrency)
    }

    @Test
    fun publicContractHasNoDataRemoteProviderAndroidOrComposeDependency() {
        val typeNames = apiTypeNames(KnockoutProductSpecificationSnapshot::class.java)

        assertTrue(
            typeNames.none {
                it.contains(".data.") ||
                    it.contains(".remote.") ||
                    it.contains(".provider.") ||
                    it.startsWith("android.") ||
                    it.startsWith("androidx.") ||
                    it.contains("compose", ignoreCase = true)
            }
        )
    }

    private fun snapshot(
        specification: KnockoutProductSpecification = specification(),
        sourceId: String = "test-source",
        retrievedAtEpochMillis: Long = 100L,
        sourceTimestampEpochMillis: Long? = 90L
    ) = KnockoutProductSpecificationSnapshot(
        specification = specification,
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )

    private fun specification() = KnockoutProductSpecification(
        productIsin = "DE000KO12345",
        productWkn = "KO1234",
        issuerId = "issuer-1",
        underlyingId = "nvidia",
        direction = TradeDirection.LONG,
        basePrice = 78.0,
        knockoutBarrier = 80.0,
        ratio = 0.1,
        underlyingCurrency = "USD",
        productCurrency = "EUR"
    )

    private fun apiTypeNames(type: Class<*>): Set<String> = buildSet {
        add(type.name)
        type.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
            .forEach { add(it.type.name) }
        type.declaredConstructors
            .filterNot { it.isSynthetic }
            .forEach { constructor ->
                constructor.parameterTypes.forEach { add(it.name) }
            }
        type.declaredMethods.forEach { method ->
            add(method.returnType.name)
            method.parameterTypes.forEach { add(it.name) }
        }
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}
