package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.provider.KnockoutProductSpecificationCatalogProviderResult
import de.konavigator.app.domain.model.TradeDirection
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HsbcResearchKnockoutProductSpecificationCatalogProviderFactoryTest {

    @Test
    fun validResearchDocumentsCreateSearchableCatalogInInputOrder() {
        val provider = createSuccess(
            linkedMapOf(
                PRODUCT_A_LONG_1 to researchJson(
                    PRODUCT_A_LONG_1,
                    "Underlying-A",
                    "Call",
                    basePrice = 88.0,
                    knockoutBarrier = 91.25,
                    sourceTimestampEpochMillis = null
                ),
                PRODUCT_A_SHORT to researchJson(
                    PRODUCT_A_SHORT,
                    "Underlying-A",
                    "Put"
                ),
                PRODUCT_A_LONG_2 to researchJson(
                    PRODUCT_A_LONG_2,
                    "Underlying-A",
                    "Call"
                ),
                PRODUCT_B_LONG to researchJson(
                    PRODUCT_B_LONG,
                    "Underlying-B",
                    "Call"
                )
            )
        )

        val longA = candidates(provider, "Underlying-A", TradeDirection.LONG)
        val shortA = candidates(provider, "Underlying-A", TradeDirection.SHORT)
        val longB = candidates(provider, "Underlying-B", TradeDirection.LONG)

        assertEquals(listOf(PRODUCT_A_LONG_1, PRODUCT_A_LONG_2), longA.map(::productIsin))
        assertEquals(listOf(PRODUCT_A_SHORT), shortA.map(::productIsin))
        assertEquals(listOf(PRODUCT_B_LONG), longB.map(::productIsin))

        val first = longA.first()
        assertEquals("SYN001", first.specification.productWkn)
        assertEquals("synthetic-hsbc", first.specification.issuerId)
        assertEquals(88.0, first.specification.basePrice)
        assertEquals(91.25, first.specification.knockoutBarrier)
        assertEquals("HSBC_RESEARCH_LOCAL", first.sourceId)
        assertEquals(RETRIEVED_AT, first.retrievedAtEpochMillis)
        assertNull(first.sourceTimestampEpochMillis)
    }

    @Test
    fun absentUnderlyingOrDirectionCombinationReturnsSuccessEmptyList() {
        val provider = createSuccess(
            mapOf(
                PRODUCT_A_LONG_1 to researchJson(
                    PRODUCT_A_LONG_1,
                    "Underlying-A",
                    "Call"
                )
            )
        )

        assertTrue(candidates(provider, "Missing", TradeDirection.LONG).isEmpty())
        assertTrue(candidates(provider, "Underlying-A", TradeDirection.SHORT).isEmpty())
    }

    @Test
    fun malformedJsonFailsClosedWithoutProvider() {
        val result = create(
            linkedMapOf(
                PRODUCT_A_LONG_1 to researchJson(
                    PRODUCT_A_LONG_1,
                    "Underlying-A",
                    "Call"
                ),
                PRODUCT_A_SHORT to "{\"productIsin\":"
            )
        )

        assertFailure(result)
        val failure = result as
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Failure
        assertEquals(1, failure.errors.size)
        assertTrue(
            failure.errors.single() is
                HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                    .ProcessingFailure
        )
    }

    @Test
    fun unsupportedDirectionFailsClosedWithoutProvider() {
        val result = create(
            mapOf(
                PRODUCT_A_LONG_1 to researchJson(
                    PRODUCT_A_LONG_1,
                    "Underlying-A",
                    "Bull"
                )
            )
        )

        assertFailure(result)
        val failure = result as
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Failure
        val processingFailure = failure.errors.single() as
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                .ProcessingFailure
        assertEquals(
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Mapping(
                listOf(
                    HsbcKnockoutProductSpecificationRecordMappingErrorCode
                        .UNSUPPORTED_DIRECTION_LABEL
                )
            ),
            processingFailure.error
        )
    }

    @Test
    fun mismatchingMapKeyAndSnapshotIsinFailsClosed() {
        val result = create(
            mapOf(
                PRODUCT_A_LONG_1 to researchJson(
                    PRODUCT_A_LONG_2,
                    "Underlying-A",
                    "Call"
                )
            )
        )

        assertEquals(
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Failure(
                listOf(
                    HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                        .ProductIsinMismatch(PRODUCT_A_LONG_1, PRODUCT_A_LONG_2)
                )
            ),
            result
        )
    }

    @Test
    fun allFailuresAreCollectedInInputOrderAndPreventPartialProvider() {
        val result = create(
            linkedMapOf(
                PRODUCT_A_LONG_1 to "{\"productIsin\":",
                PRODUCT_A_SHORT to researchJson(
                    PRODUCT_A_LONG_2,
                    "Underlying-A",
                    "Put"
                ),
                PRODUCT_A_LONG_2 to researchJson(
                    PRODUCT_A_LONG_2,
                    "Underlying-A",
                    "Bull"
                )
            )
        )

        assertFailure(result)
        val failure = result as
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Failure
        assertEquals(
            listOf(PRODUCT_A_LONG_1, PRODUCT_A_SHORT, PRODUCT_A_LONG_2),
            failure.errors.map { error ->
                when (error) {
                    is HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                        .ProcessingFailure -> error.productIsinKey
                    is HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                        .ProductIsinMismatch -> error.productIsinKey
                }
            }
        )
    }

    @Test
    fun factoryDefensivelyCopiesInputMapBeforeBuildingProvider() {
        val input = linkedMapOf(
            PRODUCT_A_LONG_1 to researchJson(
                PRODUCT_A_LONG_1,
                "Underlying-A",
                "Call"
            )
        )
        val provider = createSuccess(input)

        input.clear()
        input[PRODUCT_B_LONG] = researchJson(PRODUCT_B_LONG, "Underlying-B", "Call")

        assertEquals(
            listOf(PRODUCT_A_LONG_1),
            candidates(provider, "Underlying-A", TradeDirection.LONG).map(::productIsin)
        )
        assertTrue(candidates(provider, "Underlying-B", TradeDirection.LONG).isEmpty())
    }

    private fun create(
        values: Map<String, String>
    ) = HsbcResearchKnockoutProductSpecificationCatalogProviderFactory.create(
        researchJsonByProductIsin = values,
        retrievedAtEpochMillis = RETRIEVED_AT
    )

    private fun createSuccess(
        values: Map<String, String>
    ): HsbcResearchKnockoutProductSpecificationCatalogProvider =
        (create(values) as
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Success)
            .provider

    private fun candidates(
        provider: HsbcResearchKnockoutProductSpecificationCatalogProvider,
        underlyingId: String,
        direction: TradeDirection
    ) = (runSuspend { provider.findCandidates(underlyingId, direction) } as
        KnockoutProductSpecificationCatalogProviderResult.Success).candidates

    private fun productIsin(
        snapshot: de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
    ) = snapshot.specification.productIsin

    private fun assertFailure(
        result: HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult
    ) {
        assertTrue(
            result is HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Failure
        )
        assertFalse(
            result is HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Success
        )
    }

    private fun researchJson(
        productIsin: String,
        underlyingId: String,
        directionLabel: String,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        sourceTimestampEpochMillis: Long? = SOURCE_TIMESTAMP
    ): String {
        val sourceTimestamp = sourceTimestampEpochMillis?.toString() ?: "null"
        return """{
            "productIsin":"$productIsin",
            "productWkn":"SYN001",
            "issuerId":"synthetic-hsbc",
            "underlyingId":"$underlyingId",
            "directionLabel":"$directionLabel",
            "basePrice":$basePrice,
            "knockoutBarrier":$knockoutBarrier,
            "ratio":0.1,
            "underlyingCurrency":"USD",
            "productCurrency":"EUR",
            "sourceTimestampEpochMillis":$sourceTimestamp
        }""".trimIndent()
    }

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
        const val PRODUCT_A_LONG_1 = "DE000AAA1111"
        const val PRODUCT_A_SHORT = "DE000AAA2222"
        const val PRODUCT_A_LONG_2 = "DE000AAA3333"
        const val PRODUCT_B_LONG = "DE000BBB1111"
        const val RETRIEVED_AT = 1_700_000_000_500L
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L
    }
}
