package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryKnockoutProductSpecificationSnapshotProviderTest {

    @Test
    fun existingProductIsinReturnsSuccessWithExactSnapshot() {
        val snapshot = snapshot()

        val result = find(provider(mapOf(PRODUCT_ISIN to snapshot)), PRODUCT_ISIN)

        assertTrue(result is ProviderResult.Success)
        assertSame(snapshot, (result as ProviderResult.Success).value)
    }

    @Test
    fun unknownProductIsinReturnsNotFound() {
        assertSame(
            ProviderResult.NotFound,
            find(provider(mapOf(PRODUCT_ISIN to snapshot())), "unknown")
        )
    }

    @Test
    fun whitespaceInProductIsinIsSearchedExactly() {
        val spacedProductIsin = " $PRODUCT_ISIN "
        val snapshot = snapshot(productIsin = spacedProductIsin)
        val provider = provider(mapOf(spacedProductIsin to snapshot))

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN))
        assertSame(snapshot, successValue(find(provider, spacedProductIsin)))
    }

    @Test
    fun productIsinCaseIsNotChanged() {
        val lowerProductIsin = PRODUCT_ISIN.lowercase()
        val snapshot = snapshot(productIsin = lowerProductIsin)
        val provider = provider(mapOf(lowerProductIsin to snapshot))

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN))
        assertSame(snapshot, successValue(find(provider, lowerProductIsin)))
    }

    @Test
    fun multipleSnapshotsRemainDistinctByMapKey() {
        val first = snapshot(productIsin = PRODUCT_ISIN, sourceId = "source-a")
        val secondProductIsin = "DE000TEST002"
        val second = snapshot(productIsin = secondProductIsin, sourceId = "source-b")
        val provider = provider(
            mapOf(
                PRODUCT_ISIN to first,
                secondProductIsin to second
            )
        )

        assertSame(first, successValue(find(provider, PRODUCT_ISIN)))
        assertSame(second, successValue(find(provider, secondProductIsin)))
    }

    @Test
    fun inputMapIsDefensivelyCopiedBeforeEntriesAreDeletedOrReplaced() {
        val original = snapshot(sourceId = "original")
        val replacement = snapshot(sourceId = "replacement")
        val input = mutableMapOf(PRODUCT_ISIN to original)
        val provider = provider(input)

        input.clear()
        input[PRODUCT_ISIN] = replacement

        assertSame(original, successValue(find(provider, PRODUCT_ISIN)))
    }

    @Test
    fun repeatedLookupsDeterministicallyReturnSameSnapshot() {
        val snapshot = snapshot()
        val provider = provider(mapOf(PRODUCT_ISIN to snapshot))

        val first = successValue(find(provider, PRODUCT_ISIN))
        val second = successValue(find(provider, PRODUCT_ISIN))

        assertSame(snapshot, first)
        assertSame(first, second)
    }

    @Test
    fun allSnapshotValuesRemainExactlyUnchanged() {
        val specification = specification(PRODUCT_ISIN)
        val snapshot = KnockoutProductSpecificationSnapshotDto(
            specification = specification,
            sourceId = " Source-Id ",
            retrievedAtEpochMillis = 123L,
            sourceTimestampEpochMillis = null
        )

        val transported = successValue(
            find(provider(mapOf(PRODUCT_ISIN to snapshot)), PRODUCT_ISIN)
        )

        assertSame(snapshot, transported)
        assertSame(specification, transported.specification)
        assertEquals(" Source-Id ", transported.sourceId)
        assertEquals(123L, transported.retrievedAtEpochMillis)
        assertNull(transported.sourceTimestampEpochMillis)
    }

    @Test
    fun providerDoesNotValidateSnapshotFieldsOrMapKeyConsistency() {
        val mismatchingSpecification = specification("DIFFERENT-ISIN")
        val snapshot = KnockoutProductSpecificationSnapshotDto(
            specification = mismatchingSpecification,
            sourceId = "source",
            retrievedAtEpochMillis = -1L,
            sourceTimestampEpochMillis = null
        )

        val transported = successValue(
            find(provider(mapOf(PRODUCT_ISIN to snapshot)), PRODUCT_ISIN)
        )

        assertSame(snapshot, transported)
        assertEquals("DIFFERENT-ISIN", transported.specification.productIsin)
        assertEquals(-1L, transported.retrievedAtEpochMillis)
        assertNull(transported.sourceTimestampEpochMillis)
    }

    private fun provider(
        values: Map<String, KnockoutProductSpecificationSnapshotDto>
    ) = InMemoryKnockoutProductSpecificationSnapshotProvider(values)

    private fun find(
        provider: InMemoryKnockoutProductSpecificationSnapshotProvider,
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationSnapshotDto> = runSuspend {
        provider.findByProductIsin(productIsin)
    }

    private fun successValue(
        result: ProviderResult<KnockoutProductSpecificationSnapshotDto>
    ) = (result as ProviderResult.Success).value

    private fun snapshot(
        productIsin: String = PRODUCT_ISIN,
        sourceId: String = "source"
    ) = KnockoutProductSpecificationSnapshotDto(
        specification = specification(productIsin),
        sourceId = sourceId,
        retrievedAtEpochMillis = 100L,
        sourceTimestampEpochMillis = 90L
    )

    private fun specification(productIsin: String) = KnockoutProductSpecificationDto(
        productIsin = productIsin,
        productWkn = "ABC123",
        issuerId = "issuer-a",
        underlyingId = "underlying-a",
        direction = "LONG",
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )

    private fun <T> runSuspend(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<T>) {
                    completed = result
                }
            }
        )
        return (completed ?: error("Suspend provider call did not complete synchronously"))
            .getOrThrow()
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
    }
}
