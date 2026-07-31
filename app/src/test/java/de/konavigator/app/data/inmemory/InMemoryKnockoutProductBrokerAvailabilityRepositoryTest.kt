package de.konavigator.app.data.inmemory

import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductBrokerAvailabilityResult
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryKnockoutProductBrokerAvailabilityRepositoryTest {

    @Test
    fun implementsBrokerAvailabilityRepositoryPort() {
        assertTrue(
            KnockoutProductBrokerAvailabilityRepository::class.java.isAssignableFrom(
                InMemoryKnockoutProductBrokerAvailabilityRepository::class.java
            )
        )
    }

    @Test
    fun requestedTradableProductsForExactBrokerReturnSuccess() = runTest {
        val repository = repository(
            "synthetic-broker-a" to setOf(
                "DE000SYNTH01",
                "DE000SYNTH02",
                "DE000SYNTH03"
            )
        )

        val result = repository.findTradableProductIsins(
            query(
                brokerId = "synthetic-broker-a",
                productIsins = listOf("DE000SYNTH03", "DE000SYNTH01")
            )
        )

        assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
        assertEquals(
            setOf("DE000SYNTH03", "DE000SYNTH01"),
            successMembership(result)
        )
    }

    @Test
    fun configuredButUnrequestedProductIsExcluded() = runTest {
        val repository = repository(
            "synthetic-broker" to setOf(
                "DE000SYNTH01",
                "DE000SYNTH02",
                "DE000SYNTH03"
            )
        )

        val membership = successMembership(
            repository.findTradableProductIsins(
                query(productIsins = listOf("DE000SYNTH01", "DE000SYNTH03"))
            )
        )

        assertEquals(setOf("DE000SYNTH01", "DE000SYNTH03"), membership)
        assertFalse("DE000SYNTH02" in membership)
    }

    @Test
    fun requestedButUnavailableProductIsExcluded() = runTest {
        val repository = repository(
            "synthetic-broker" to setOf("DE000SYNTH01")
        )

        val membership = successMembership(
            repository.findTradableProductIsins(
                query(productIsins = listOf("DE000SYNTH01", "DE000SYNTH02"))
            )
        )

        assertEquals(setOf("DE000SYNTH01"), membership)
        assertFalse("DE000SYNTH02" in membership)
    }

    @Test
    fun availabilityIsSeparatedByBroker() = runTest {
        val repository = repository(
            "synthetic-broker-a" to setOf("DE000SYNTH01"),
            "synthetic-broker-b" to setOf("DE000SYNTH02")
        )
        val requested = listOf("DE000SYNTH01", "DE000SYNTH02")

        val brokerA = successMembership(
            repository.findTradableProductIsins(
                query(brokerId = "synthetic-broker-a", productIsins = requested)
            )
        )
        val brokerB = successMembership(
            repository.findTradableProductIsins(
                query(brokerId = "synthetic-broker-b", productIsins = requested)
            )
        )

        assertEquals(setOf("DE000SYNTH01"), brokerA)
        assertEquals(setOf("DE000SYNTH02"), brokerB)
    }

    @Test
    fun unknownBrokerReturnsEmptySuccess() = runTest {
        val result = repository(
            "synthetic-broker" to setOf("DE000SYNTH01")
        ).findTradableProductIsins(query(brokerId = "unknown-broker"))

        assertFalse(result is KnockoutProductBrokerAvailabilityResult.DataAccessFailure)
        assertFalse(result is KnockoutProductBrokerAvailabilityResult.InvalidData)
        assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun emptyProductQueryReturnsEmptySuccess() = runTest {
        val result = repository(
            "synthetic-broker" to setOf("DE000SYNTH01")
        ).findTradableProductIsins(query(productIsins = emptyList()))

        assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun emptyRepositoryReturnsEmptySuccess() = runTest {
        val repository = InMemoryKnockoutProductBrokerAvailabilityRepository(emptyMap())

        val result = repository.findTradableProductIsins(query())

        assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun brokerLookupIsCaseSensitive() = runTest {
        val repository = repository(
            "Synthetic-Broker" to setOf("DE000SYNTH01")
        )

        val result = repository.findTradableProductIsins(
            query(brokerId = "synthetic-broker")
        )

        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun brokerLookupIsWhitespaceSensitive() = runTest {
        val repository = repository(
            " Synthetic-Broker " to setOf("DE000SYNTH01")
        )

        val withoutWhitespace = repository.findTradableProductIsins(
            query(brokerId = "Synthetic-Broker")
        )
        val exact = repository.findTradableProductIsins(
            query(brokerId = " Synthetic-Broker ")
        )

        assertTrue(successMembership(withoutWhitespace).isEmpty())
        assertEquals(setOf("DE000SYNTH01"), successMembership(exact))
    }

    @Test
    fun productIsinLookupIsCaseSensitive() = runTest {
        val repository = repository(
            "synthetic-broker" to setOf("DE000SYNTH01")
        )

        val result = repository.findTradableProductIsins(
            query(productIsins = listOf("de000synth01"))
        )

        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun productIsinLookupIsWhitespaceSensitive() = runTest {
        val repository = repository(
            "synthetic-broker" to setOf(" DE000SYNTH01 ")
        )

        val withoutWhitespace = repository.findTradableProductIsins(
            query(productIsins = listOf("DE000SYNTH01"))
        )
        val exact = repository.findTradableProductIsins(
            query(productIsins = listOf(" DE000SYNTH01 "))
        )

        assertTrue(successMembership(withoutWhitespace).isEmpty())
        assertEquals(setOf(" DE000SYNTH01 "), successMembership(exact))
    }

    @Test
    fun duplicateRequestedIsinsProduceSingleResultMembership() = runTest {
        val repository = repository(
            "synthetic-broker" to setOf("DE000SYNTH01")
        )
        val requested = mutableListOf(
            "DE000SYNTH01",
            "DE000SYNTH01",
            "DE000SYNTH01"
        )
        val original = requested.toList()

        val result = repository.findTradableProductIsins(
            query(productIsins = requested)
        )

        assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
        assertEquals(setOf("DE000SYNTH01"), successMembership(result))
        assertEquals(1, successMembership(result).size)
        assertEquals(original, requested)
    }

    @Test
    fun laterChangesToOriginalMapDoNotAffectRepository() = runTest {
        val source = mutableMapOf<String, Set<String>>(
            "synthetic-broker-a" to setOf("DE000SYNTH01")
        )
        val repository = InMemoryKnockoutProductBrokerAvailabilityRepository(source)

        source.clear()
        source["synthetic-broker-b"] = setOf("DE000SYNTH02")

        val original = repository.findTradableProductIsins(
            query(
                brokerId = "synthetic-broker-a",
                productIsins = listOf("DE000SYNTH01", "DE000SYNTH02")
            )
        )
        val addedLater = repository.findTradableProductIsins(
            query(
                brokerId = "synthetic-broker-b",
                productIsins = listOf("DE000SYNTH01", "DE000SYNTH02")
            )
        )

        assertEquals(setOf("DE000SYNTH01"), successMembership(original))
        assertTrue(successMembership(addedLater).isEmpty())
    }

    @Test
    fun laterChangesToOriginalNestedSetDoNotAffectRepository() = runTest {
        val sourceSet = mutableSetOf("DE000SYNTH01")
        val repository = InMemoryKnockoutProductBrokerAvailabilityRepository(
            mapOf("synthetic-broker" to sourceSet)
        )

        sourceSet.clear()
        sourceSet += "DE000SYNTH02"

        val result = repository.findTradableProductIsins(
            query(productIsins = listOf("DE000SYNTH01", "DE000SYNTH02"))
        )

        assertEquals(setOf("DE000SYNTH01"), successMembership(result))
        assertFalse("DE000SYNTH02" in successMembership(result))
    }

    @Test
    fun repositoryQueryDoesNotMutateInputList() = runTest {
        val requested = mutableListOf(
            "DE000SYNTH02",
            "DE000SYNTH01",
            "DE000SYNTH01",
            " DE000SYNTH03 "
        )
        val original = requested.toList()
        val repository = repository(
            "synthetic-broker" to setOf(
                "DE000SYNTH01",
                "DE000SYNTH02",
                " DE000SYNTH03 "
            )
        )
        val query = query(productIsins = requested)

        repository.findTradableProductIsins(query)
        repository.findTradableProductIsins(query)
        repository.findTradableProductIsins(query)

        assertEquals(original, requested)
        assertEquals("DE000SYNTH02", requested[0])
        assertEquals("DE000SYNTH01", requested[1])
        assertEquals("DE000SYNTH01", requested[2])
        assertEquals(" DE000SYNTH03 ", requested[3])
    }

    @Test
    fun exactUnnormalizedValuesRemainSupported() = runTest {
        val repository = repository(
            " Broker-X " to setOf(" de000synthetic ")
        )

        val result = repository.findTradableProductIsins(
            query(
                brokerId = " Broker-X ",
                productIsins = listOf(" de000synthetic ")
            )
        )

        assertEquals(setOf(" de000synthetic "), successMembership(result))
    }

    @Test
    fun adapterNeverReturnsFailureModes() = runTest {
        val repository = repository(
            "synthetic-broker" to setOf("DE000SYNTH01")
        )
        val results = listOf(
            repository.findTradableProductIsins(
                query(productIsins = listOf("DE000SYNTH01"))
            ),
            repository.findTradableProductIsins(
                query(productIsins = listOf("DE000SYNTH02"))
            ),
            repository.findTradableProductIsins(query(brokerId = "unknown-broker")),
            repository.findTradableProductIsins(query(productIsins = emptyList())),
            InMemoryKnockoutProductBrokerAvailabilityRepository(emptyMap())
                .findTradableProductIsins(query())
        )

        results.forEach { result ->
            assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
            assertFalse(result is KnockoutProductBrokerAvailabilityResult.DataAccessFailure)
            assertFalse(result is KnockoutProductBrokerAvailabilityResult.InvalidData)
        }
    }

    @Test
    fun resultContainsNoUnrequestedAvailability() = runTest {
        val repository = repository(
            "synthetic-broker" to setOf(
                "DE000SYNTH01",
                "DE000SYNTH02",
                "DE000SYNTH03",
                "DE000SYNTH04"
            )
        )

        val result = repository.findTradableProductIsins(
            query(productIsins = listOf("DE000SYNTH03"))
        )

        assertEquals(setOf("DE000SYNTH03"), successMembership(result))
        assertEquals(1, successMembership(result).size)
    }

    @Test
    fun publicApiContainsNoAndroidComposeNetworkOrDatabaseTypes() {
        val forbidden = listOf("android", "compose", "retrofit", "okhttp", "room", "sqlite")

        assertTrue(apiTypeNames().none { name -> forbidden.any(name::contains) })
    }

    private fun repository(
        vararg availability: Pair<String, Set<String>>
    ) = InMemoryKnockoutProductBrokerAvailabilityRepository(mapOf(*availability))

    private fun query(
        brokerId: String = "synthetic-broker",
        productIsins: List<String> = listOf("DE000SYNTH01")
    ) = KnockoutProductBrokerAvailabilityQuery(
        brokerId = brokerId,
        productIsins = productIsins
    )

    private fun successMembership(
        result: KnockoutProductBrokerAvailabilityResult
    ): Set<String> =
        (result as KnockoutProductBrokerAvailabilityResult.Success).tradableProductIsins

    private fun apiTypeNames(): List<String> = buildList {
        val type = InMemoryKnockoutProductBrokerAvailabilityRepository::class.java
        add(type.name.lowercase())
        type.declaredFields.forEach { add(it.type.name.lowercase()) }
        type.constructors.forEach { constructor ->
            constructor.parameterTypes.forEach { add(it.name.lowercase()) }
        }
        type.declaredMethods.forEach { method ->
            add(method.returnType.name.lowercase())
            method.parameterTypes.forEach { add(it.name.lowercase()) }
        }
    }
}
