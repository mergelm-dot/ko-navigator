package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderFactoryTest {

    @Test
    fun emptyInputCreatesEmptyProvider() {
        val provider = createSuccess(emptyMap())

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN_1))
    }

    @Test
    fun singleValidCallJsonCreatesQueryableProvider() {
        val snapshot = findSuccess(
            createSuccess(mapOf(PRODUCT_ISIN_1 to researchJson(PRODUCT_ISIN_1, "Call"))),
            PRODUCT_ISIN_1
        )

        assertEquals(PRODUCT_ISIN_1, snapshot.specification.productIsin)
        assertEquals("SYN001", snapshot.specification.productWkn)
        assertEquals("synthetic-provider", snapshot.specification.issuerId)
        assertEquals("synthetic-underlying", snapshot.specification.underlyingId)
        assertEquals("LONG", snapshot.specification.direction)
        assertEquals(80.125, snapshot.specification.basePrice)
        assertEquals(82.5, snapshot.specification.knockoutBarrier)
        assertEquals(0.1, snapshot.specification.ratio)
        assertEquals("USD", snapshot.specification.underlyingCurrency)
        assertEquals("EUR", snapshot.specification.productCurrency)
        assertEquals("HSBC_RESEARCH_LOCAL", snapshot.sourceId)
        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertEquals(SOURCE_TIMESTAMP, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun multipleValidDocumentsAreAvailableByExactKey() {
        val provider = createSuccess(
            linkedMapOf(
                PRODUCT_ISIN_1 to researchJson(PRODUCT_ISIN_1, "Call"),
                PRODUCT_ISIN_2 to researchJson(PRODUCT_ISIN_2, "Put")
            )
        )

        assertEquals("LONG", findSuccess(provider, PRODUCT_ISIN_1).specification.direction)
        assertEquals("SHORT", findSuccess(provider, PRODUCT_ISIN_2).specification.direction)
    }

    @Test
    fun malformedJsonPreservesTypedProcessingFailure() {
        assertEquals(
            creationFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                    .ProcessingFailure(
                        productIsinKey = PRODUCT_ISIN_1,
                        error =
                            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
                                .Parsing(
                                    listOf(
                                        HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                                            .INVALID_JSON
                                    )
                                )
                    )
            ),
            create(mapOf(PRODUCT_ISIN_1 to "{\"productIsin\":"))
        )
    }

    @Test
    fun unsupportedDirectionPreservesTypedProcessingFailure() {
        assertEquals(
            creationFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                    .ProcessingFailure(
                        productIsinKey = PRODUCT_ISIN_1,
                        error =
                            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
                                .Mapping(
                                    listOf(
                                        HsbcKnockoutProductSpecificationRecordMappingErrorCode
                                            .UNSUPPORTED_DIRECTION_LABEL
                                    )
                                )
                    )
            ),
            create(
                mapOf(
                    PRODUCT_ISIN_1 to researchJson(
                        PRODUCT_ISIN_1,
                        "SyntheticDirection"
                    )
                )
            )
        )
    }

    @Test
    fun mismatchingEmbeddedProductIsinIsRejected() {
        assertEquals(
            creationFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                    .ProductIsinMismatch(PRODUCT_ISIN_1, PRODUCT_ISIN_2)
            ),
            create(mapOf(PRODUCT_ISIN_1 to researchJson(PRODUCT_ISIN_2, "Call")))
        )
    }

    @Test
    fun nullEmbeddedProductIsinIsRejected() {
        assertEquals(
            creationFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                    .ProductIsinMismatch(PRODUCT_ISIN_1, null)
            ),
            create(mapOf(PRODUCT_ISIN_1 to """{"directionLabel":"Call"}"""))
        )
    }

    @Test
    fun productIsinWhitespaceAndCaseAreNotNormalized() {
        val mismatching = " de000synth01 "
        assertEquals(
            creationFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                    .ProductIsinMismatch(PRODUCT_ISIN_1, mismatching)
            ),
            create(mapOf(PRODUCT_ISIN_1 to researchJson(mismatching, "Call")))
        )

        val spacedKey = " $PRODUCT_ISIN_1 "
        val provider = createSuccess(mapOf(spacedKey to researchJson(spacedKey, "Call")))
        assertTrue(find(provider, spacedKey) is ProviderResult.Success)
        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN_1))
    }

    @Test
    fun multipleErrorsAreCollectedInInputOrder() {
        val result = create(
            linkedMapOf(
                PRODUCT_ISIN_1 to "{\"productIsin\":",
                PRODUCT_ISIN_2 to researchJson(PRODUCT_ISIN_1, "Call"),
                PRODUCT_ISIN_3 to researchJson(PRODUCT_ISIN_3, "SyntheticDirection")
            )
        ) as HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult.Failure

        assertEquals(3, result.errors.size)
        assertEquals(
            listOf(PRODUCT_ISIN_1, PRODUCT_ISIN_2, PRODUCT_ISIN_3),
            result.errors.map { error ->
                when (error) {
                    is HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                        .ProcessingFailure -> error.productIsinKey
                    is HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                        .ProductIsinMismatch -> error.productIsinKey
                }
            }
        )
        assertTrue(result.errors[0] is
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                .ProcessingFailure)
        assertTrue(result.errors[1] is
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                .ProductIsinMismatch)
        assertTrue(result.errors[2] is
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                .ProcessingFailure)
    }

    @Test
    fun anyErrorPreventsPartialProvider() {
        val result = create(
            linkedMapOf(
                PRODUCT_ISIN_1 to researchJson(PRODUCT_ISIN_1, "Call"),
                PRODUCT_ISIN_2 to "{\"productIsin\":"
            )
        )

        assertTrue(result is
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult.Failure)
        assertFalse(result is
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult.Success)
    }

    @Test
    fun negativeRetrievedAtEpochMillisIsCopiedToEverySnapshot() {
        val provider = createSuccess(
            linkedMapOf(
                PRODUCT_ISIN_1 to researchJson(PRODUCT_ISIN_1, "Call"),
                PRODUCT_ISIN_2 to researchJson(PRODUCT_ISIN_2, "Put")
            ),
            retrievedAtEpochMillis = -1L
        )

        assertEquals(-1L, findSuccess(provider, PRODUCT_ISIN_1).retrievedAtEpochMillis)
        assertEquals(-1L, findSuccess(provider, PRODUCT_ISIN_2).retrievedAtEpochMillis)
    }

    @Test
    fun nullSourceTimestampIsNotReplaced() {
        val json =
            """{"productIsin":"$PRODUCT_ISIN_1","directionLabel":"Call","sourceTimestampEpochMillis":null}"""
        val snapshot = findSuccess(
            createSuccess(mapOf(PRODUCT_ISIN_1 to json)),
            PRODUCT_ISIN_1
        )

        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun mutableInputChangesAfterCreationDoNotAffectProvider() {
        val input = mutableMapOf(PRODUCT_ISIN_1 to researchJson(PRODUCT_ISIN_1, "Call"))
        val provider = createSuccess(input)

        input.clear()
        input[PRODUCT_ISIN_2] = researchJson(PRODUCT_ISIN_2, "Put")

        assertTrue(find(provider, PRODUCT_ISIN_1) is ProviderResult.Success)
        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN_2))
    }

    private fun create(
        values: Map<String, String>,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ) = HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderFactory.create(
        researchJsonByProductIsin = values,
        retrievedAtEpochMillis = retrievedAtEpochMillis
    )

    private fun createSuccess(
        values: Map<String, String>,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ): InMemoryKnockoutProductSpecificationSnapshotProvider =
        (create(values, retrievedAtEpochMillis) as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult.Success)
            .provider

    private fun creationFailure(
        vararg errors: HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
    ) = HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult.Failure(
        errors.toList()
    )

    private fun find(
        provider: InMemoryKnockoutProductSpecificationSnapshotProvider,
        productIsin: String
    ): ProviderResult<KnockoutProductSpecificationSnapshotDto> = runSuspend {
        provider.findByProductIsin(productIsin)
    }

    private fun findSuccess(
        provider: InMemoryKnockoutProductSpecificationSnapshotProvider,
        productIsin: String
    ) = (find(provider, productIsin) as ProviderResult.Success).value

    private fun researchJson(productIsin: String, directionLabel: String) =
        """{
            "productIsin":"$productIsin",
            "productWkn":"SYN001",
            "issuerId":"synthetic-provider",
            "underlyingId":"synthetic-underlying",
            "directionLabel":"$directionLabel",
            "basePrice":80.125,
            "knockoutBarrier":82.5,
            "ratio":0.1,
            "underlyingCurrency":"USD",
            "productCurrency":"EUR",
            "sourceTimestampEpochMillis":1700000000250
        }""".trimIndent()

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
        const val PRODUCT_ISIN_1 = "DE000SYNTH01"
        const val PRODUCT_ISIN_2 = "DE000SYNTH02"
        const val PRODUCT_ISIN_3 = "DE000SYNTH03"
        const val RETRIEVED_AT = 1_700_000_000_500L
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L
    }
}
