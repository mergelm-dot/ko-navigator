package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductBrokerAvailabilityContractsTest {

    @Test
    fun queryPreservesBrokerIdExactly() {
        val query = KnockoutProductBrokerAvailabilityQuery(
            brokerId = " Synthetic-Broker ",
            productIsins = listOf("DE000SYNTH01")
        )

        assertEquals(" Synthetic-Broker ", query.brokerId)
    }

    @Test
    fun queryPreservesProductIsinOrderDuplicatesAndExactValues() {
        val productIsins = listOf(
            "DE000SYNTH02",
            " DE000SYNTH01 ",
            "DE000SYNTH02",
            "de000synth03"
        )

        val query = KnockoutProductBrokerAvailabilityQuery(
            brokerId = "synthetic-broker",
            productIsins = productIsins
        )

        assertSame(productIsins, query.productIsins)
        assertEquals(
            listOf(
                "DE000SYNTH02",
                " DE000SYNTH01 ",
                "DE000SYNTH02",
                "de000synth03"
            ),
            query.productIsins
        )
    }

    @Test
    fun queryAllowsEmptyProductList() {
        val productIsins = emptyList<String>()

        val query = KnockoutProductBrokerAvailabilityQuery(
            brokerId = "synthetic-broker",
            productIsins = productIsins
        )

        assertSame(productIsins, query.productIsins)
        assertTrue(query.productIsins.isEmpty())
    }

    @Test
    fun successPreservesExactTradableMembership() {
        val tradableProductIsins = linkedSetOf(
            "DE000SYNTH02",
            " DE000SYNTH01 ",
            "de000synth03"
        )

        val result = KnockoutProductBrokerAvailabilityResult.Success(tradableProductIsins)

        assertSame(tradableProductIsins, result.tradableProductIsins)
        assertEquals(
            setOf("DE000SYNTH02", " DE000SYNTH01 ", "de000synth03"),
            result.tradableProductIsins
        )
        assertFalse("DE000SYNTH01" in result.tradableProductIsins)
        assertFalse("DE000SYNTH03" in result.tradableProductIsins)
    }

    @Test
    fun emptySuccessRepresentsSuccessfulCheckWithoutTradableProducts() {
        val result: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.Success(tradableProductIsins = emptySet())

        assertFalse(result is KnockoutProductBrokerAvailabilityResult.DataAccessFailure)
        assertFalse(result is KnockoutProductBrokerAvailabilityResult.InvalidData)
        assertTrue(result is KnockoutProductBrokerAvailabilityResult.Success)
        assertTrue(successMembership(result).isEmpty())
    }

    @Test
    fun dataAccessFailureIsDistinctFromEmptySuccess() {
        val emptySuccess: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.Success(tradableProductIsins = emptySet())
        val failure: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.DataAccessFailure

        assertFalse(emptySuccess == failure)
        assertFalse(emptySuccess === failure)
    }

    @Test
    fun invalidDataIsDistinctFromEmptySuccessAndDataAccessFailure() {
        val emptySuccess: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.Success(tradableProductIsins = emptySet())
        val dataAccessFailure: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.DataAccessFailure
        val invalidData: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.InvalidData

        assertFalse(emptySuccess == dataAccessFailure)
        assertFalse(emptySuccess == invalidData)
        assertFalse(dataAccessFailure == invalidData)
    }

    @Test
    fun repositoryReceivesExactQueryAndReturnsExactResult() = runTest {
        val productIsins = listOf(
            "DE000SYNTH02",
            " DE000SYNTH01 ",
            "DE000SYNTH02",
            "de000synth03"
        )
        val query = KnockoutProductBrokerAvailabilityQuery(
            brokerId = " Synthetic-Broker ",
            productIsins = productIsins
        )
        val expectedResult: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.Success(
                tradableProductIsins = setOf(" DE000SYNTH01 ")
            )
        val repository = FakeBrokerAvailabilityRepository(expectedResult)

        val actualResult = repository.findTradableProductIsins(query)

        assertSame(query, repository.receivedQuery)
        assertEquals(" Synthetic-Broker ", repository.receivedQuery?.brokerId)
        assertSame(productIsins, repository.receivedQuery?.productIsins)
        assertEquals(productIsins, repository.receivedQuery?.productIsins)
        assertSame(expectedResult, actualResult)
    }

    @Test
    fun differentBrokerIdsRemainDistinct() {
        val lowerCase = KnockoutProductBrokerAvailabilityQuery(
            brokerId = "synthetic-broker",
            productIsins = emptyList()
        )
        val mixedCase = KnockoutProductBrokerAvailabilityQuery(
            brokerId = "Synthetic-Broker",
            productIsins = emptyList()
        )

        assertEquals("synthetic-broker", lowerCase.brokerId)
        assertEquals("Synthetic-Broker", mixedCase.brokerId)
        assertFalse(lowerCase.brokerId == mixedCase.brokerId)
        assertFalse(lowerCase == mixedCase)
    }

    @Test
    fun availabilityContractRequiresNoCatalogSnapshotsOrMarketData() {
        val productIsins = listOf("DE000SYNTH01")
        val query = KnockoutProductBrokerAvailabilityQuery(
            brokerId = "synthetic-broker",
            productIsins = productIsins
        )
        val tradableProductIsins = setOf("DE000SYNTH01")
        val result = KnockoutProductBrokerAvailabilityResult.Success(tradableProductIsins)

        assertEquals("synthetic-broker", query.brokerId)
        assertSame(productIsins, query.productIsins)
        assertSame(tradableProductIsins, result.tradableProductIsins)
    }

    @Test
    fun resultMembershipCanFilterCatalogWithoutReorderingIt() {
        val catalogProductIsins = listOf(
            "DE000SYNTH03",
            "DE000SYNTH01",
            "DE000SYNTH02",
            "DE000SYNTH01"
        )
        val availability = setOf("DE000SYNTH01", "DE000SYNTH03")
        val result = KnockoutProductBrokerAvailabilityResult.Success(availability)

        val filtered = catalogProductIsins.filter(result.tradableProductIsins::contains)

        assertEquals(
            listOf("DE000SYNTH03", "DE000SYNTH01", "DE000SYNTH01"),
            filtered
        )
        assertSame(availability, result.tradableProductIsins)
    }

    @Test
    fun failureModesMustNotBeTreatedAsNoAvailability() {
        val emptySuccess: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.Success(tradableProductIsins = emptySet())
        val dataAccessFailure: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.DataAccessFailure
        val invalidData: KnockoutProductBrokerAvailabilityResult =
            KnockoutProductBrokerAvailabilityResult.InvalidData

        assertFalse(dataAccessFailure == emptySuccess)
        assertFalse(invalidData == emptySuccess)
        assertFalse(dataAccessFailure == invalidData)
    }

    private fun successMembership(
        result: KnockoutProductBrokerAvailabilityResult
    ): Set<String> =
        (result as KnockoutProductBrokerAvailabilityResult.Success).tradableProductIsins

    private class FakeBrokerAvailabilityRepository(
        private val result: KnockoutProductBrokerAvailabilityResult
    ) : KnockoutProductBrokerAvailabilityRepository {

        var receivedQuery: KnockoutProductBrokerAvailabilityQuery? = null
            private set

        override suspend fun findTradableProductIsins(
            query: KnockoutProductBrokerAvailabilityQuery
        ): KnockoutProductBrokerAvailabilityResult {
            receivedQuery = query
            return result
        }
    }
}
