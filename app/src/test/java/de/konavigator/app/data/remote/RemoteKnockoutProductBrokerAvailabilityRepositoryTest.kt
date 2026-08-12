package de.konavigator.app.data.remote

import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityResult
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProvider
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProviderResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKnockoutProductBrokerAvailabilityRepositoryTest {

    @Test
    fun implementsBrokerAvailabilityRepositoryWithOnlyProviderDependency() {
        assertTrue(
            KnockoutProductBrokerAvailabilityRepository::class.java.isAssignableFrom(
                RemoteKnockoutProductBrokerAvailabilityRepository::class.java
            )
        )
        assertEquals(
            listOf(KnockoutProductBrokerAvailabilityProvider::class.java),
            RemoteKnockoutProductBrokerAvailabilityRepository::class.java
                .declaredConstructors.single().parameterTypes.toList()
        )
    }

    @Test
    fun queryReachesProviderExactlyIncludingOrderWhitespaceCaseAndDuplicates() {
        val productIsins = mutableListOf(
            " DE000AAA1111 ",
            "DE000BBB2222",
            " DE000AAA1111 "
        )
        val original = productIsins.toList()
        val provider = RecordingProvider(success(emptySet()))

        find(
            provider,
            brokerId = " Broker CaseSensitive ",
            productIsins = productIsins
        )

        assertEquals(" Broker CaseSensitive ", provider.receivedBrokerId)
        assertSame(productIsins, provider.receivedProductIsins)
        assertEquals(original, provider.receivedProductIsins)
        assertEquals(original, productIsins)
    }

    @Test
    fun emptyQueryIsDelegatedExactlyAndProviderSuccessRemainsEmptySuccess() {
        val productIsins = emptyList<String>()
        val provider = RecordingProvider(success(emptySet()))

        val result = find(provider, productIsins = productIsins)

        assertSame(productIsins, provider.receivedProductIsins)
        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun validSubsetRemainsSuccessfulMembership() {
        assertEquals(
            setOf("A", "C"),
            successMembership(
                find(
                    RecordingProvider(success(linkedSetOf("A", "C"))),
                    productIsins = listOf("A", "B", "C")
                )
            )
        )
    }

    @Test
    fun completeProviderMembershipRemainsSuccessful() {
        assertEquals(
            setOf("A", "B", "C"),
            successMembership(
                find(
                    RecordingProvider(success(linkedSetOf("A", "B", "C"))),
                    productIsins = listOf("A", "B", "C")
                )
            )
        )
    }

    @Test
    fun providerEmptySuccessRemainsEmptyApplicationSuccess() {
        val result = find(
            RecordingProvider(success(emptySet())),
            productIsins = listOf("A", "B", "C")
        )

        assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun providerDataAccessFailureRemainsApplicationDataAccessFailure() {
        assertSame(
            KnockoutProductBrokerAvailabilityResult.DataAccessFailure,
            find(
                RecordingProvider(
                    KnockoutProductBrokerAvailabilityProviderResult.DataAccessFailure
                )
            )
        )
    }

    @Test
    fun providerInvalidDataRemainsApplicationInvalidData() {
        assertSame(
            KnockoutProductBrokerAvailabilityResult.InvalidData,
            find(
                RecordingProvider(KnockoutProductBrokerAvailabilityProviderResult.InvalidData)
            )
        )
    }

    @Test
    fun unrequestedProviderIsinMakesEntireResultInvalidData() {
        assertSame(
            KnockoutProductBrokerAvailabilityResult.InvalidData,
            find(
                RecordingProvider(success(setOf("A", "X"))),
                productIsins = listOf("A", "B")
            )
        )
    }

    @Test
    fun providerIsinCaseMismatchMakesEntireResultInvalidData() {
        assertSame(
            KnockoutProductBrokerAvailabilityResult.InvalidData,
            find(
                RecordingProvider(success(setOf("de000abc1234"))),
                productIsins = listOf("DE000ABC1234")
            )
        )
    }

    @Test
    fun providerIsinWhitespaceMismatchMakesEntireResultInvalidData() {
        assertSame(
            KnockoutProductBrokerAvailabilityResult.InvalidData,
            find(
                RecordingProvider(success(setOf(" DE000ABC1234 "))),
                productIsins = listOf("DE000ABC1234")
            )
        )
    }

    @Test
    fun duplicateQueryValuesAreNotRemovedBeforeProviderCall() {
        val requested = listOf("A", "B", "A", "C")
        val provider = RecordingProvider(success(setOf("A", "C")))

        val result = find(provider, productIsins = requested)

        assertEquals(requested, provider.receivedProductIsins)
        assertEquals(setOf("A", "C"), successMembership(result))
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
        brokerId: String = "synthetic-broker",
        productIsins: List<String> = listOf("DE000SYNTH01")
    ): KnockoutProductBrokerAvailabilityResult = runSuspend {
        RemoteKnockoutProductBrokerAvailabilityRepository(provider)
            .findTradableProductIsins(
                KnockoutProductBrokerAvailabilityQuery(
                    brokerId = brokerId,
                    productIsins = productIsins
                )
            )
    }

    private fun success(
        tradableProductIsins: Set<String>
    ) = KnockoutProductBrokerAvailabilityProviderResult.Success(tradableProductIsins)

    private fun successMembership(
        result: KnockoutProductBrokerAvailabilityResult
    ) = (result as KnockoutProductBrokerAvailabilityResult.Success).tradableProductIsins

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
        return (completed ?: error("Suspend repository call did not complete synchronously"))
            .getOrThrow()
    }
}
