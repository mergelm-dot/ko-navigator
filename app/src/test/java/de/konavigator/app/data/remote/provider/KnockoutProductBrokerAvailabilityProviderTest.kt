package de.konavigator.app.data.remote.provider

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductBrokerAvailabilityProviderTest {

    @Test
    fun searchTransportsBrokerIdAndExactProductIsinList() {
        val productIsins = listOf(
            " DE000AAA1111 ",
            "DE000BBB2222",
            " DE000AAA1111 "
        )
        val provider = RecordingProvider(
            KnockoutProductBrokerAvailabilityProviderResult.Success(emptySet())
        )

        find(provider, " Broker CaseSensitive ", productIsins)

        assertEquals(" Broker CaseSensitive ", provider.receivedBrokerId)
        assertSame(productIsins, provider.receivedProductIsins)
        assertEquals(productIsins, provider.receivedProductIsins)
    }

    @Test
    fun successTransportsExactMembershipIncludingEmptySet() {
        val membership = linkedSetOf("DE000AAA1111", "DE000BBB2222")
        val success = KnockoutProductBrokerAvailabilityProviderResult.Success(membership)
        val emptySuccess = KnockoutProductBrokerAvailabilityProviderResult.Success(emptySet())

        assertSame(membership, success.tradableProductIsins)
        assertTrue(emptySuccess.tradableProductIsins.isEmpty())
    }

    @Test
    fun dataAccessFailureAndInvalidDataRemainDistinctWithoutNotFoundState() {
        assertNotEquals(
            KnockoutProductBrokerAvailabilityProviderResult.DataAccessFailure::class,
            KnockoutProductBrokerAvailabilityProviderResult.InvalidData::class
        )
    }

    private class RecordingProvider(
        private val result: KnockoutProductBrokerAvailabilityProviderResult
    ) : KnockoutProductBrokerAvailabilityProvider {

        var receivedBrokerId: String? = null
            private set

        var receivedProductIsins: List<String>? = null
            private set

        override suspend fun findTradableProductIsins(
            brokerId: String,
            productIsins: List<String>
        ): KnockoutProductBrokerAvailabilityProviderResult {
            receivedBrokerId = brokerId
            receivedProductIsins = productIsins
            return result
        }
    }

    private fun find(
        provider: KnockoutProductBrokerAvailabilityProvider,
        brokerId: String,
        productIsins: List<String>
    ): KnockoutProductBrokerAvailabilityProviderResult = runSuspend {
        provider.findTradableProductIsins(brokerId, productIsins)
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
}
