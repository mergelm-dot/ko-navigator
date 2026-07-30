package de.konavigator.app.data.inmemory

import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogQuery
import de.konavigator.app.application.productdiscovery.KnockoutProductSpecificationCatalogResult
import de.konavigator.app.application.repository.KnockoutProductSpecificationCatalogRepository
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryKnockoutProductSpecificationCatalogRepositoryTest {

    @Test
    fun implementsCatalogRepositoryPort() {
        assertTrue(
            KnockoutProductSpecificationCatalogRepository::class.java.isAssignableFrom(
                InMemoryKnockoutProductSpecificationCatalogRepository::class.java
            )
        )
    }

    @Test
    fun exactUnderlyingAndLongDirectionReturnMatchingSnapshots() = runTest {
        val long1 = snapshot(productIsin = "DE000SYNTH01")
        val short = snapshot(productIsin = "DE000SYNTH02", direction = TradeDirection.SHORT)
        val long2 = snapshot(productIsin = "DE000SYNTH03")
        val otherUnderlying = snapshot(
            productIsin = "DE000SYNTH04",
            underlyingId = "synthetic-underlying-b"
        )
        val repository = repository(long1, short, long2, otherUnderlying)

        val result = repository.findCandidates(query(direction = TradeDirection.LONG))

        assertTrue(result is KnockoutProductSpecificationCatalogResult.Success)
        val candidates = successCandidates(result)
        assertEquals(2, candidates.size)
        assertSame(long1, candidates[0])
        assertSame(long2, candidates[1])
    }

    @Test
    fun exactUnderlyingAndShortDirectionReturnMatchingSnapshots() = runTest {
        val long = snapshot(productIsin = "DE000SYNTH01")
        val short = snapshot(productIsin = "DE000SYNTH02", direction = TradeDirection.SHORT)
        val repository = repository(long, short)

        val candidates = successCandidates(
            repository.findCandidates(query(direction = TradeDirection.SHORT))
        )

        assertEquals(1, candidates.size)
        assertSame(short, candidates.single())
    }

    @Test
    fun differentUnderlyingIsExcluded() = runTest {
        val matching = snapshot(productIsin = "DE000SYNTH01")
        val different = snapshot(
            productIsin = "DE000SYNTH02",
            underlyingId = "synthetic-underlying-b"
        )

        val candidates = successCandidates(
            repository(different, matching).findCandidates(query())
        )

        assertEquals(1, candidates.size)
        assertSame(matching, candidates.single())
    }

    @Test
    fun differentDirectionIsExcluded() = runTest {
        val matching = snapshot(productIsin = "DE000SYNTH01")
        val different = snapshot(
            productIsin = "DE000SYNTH02",
            direction = TradeDirection.SHORT
        )

        val candidates = successCandidates(
            repository(different, matching).findCandidates(query(direction = TradeDirection.LONG))
        )

        assertEquals(1, candidates.size)
        assertSame(matching, candidates.single())
    }

    @Test
    fun underlyingLookupIsCaseSensitive() = runTest {
        val repository = repository(snapshot(underlyingId = "Synthetic-Underlying"))

        val result = repository.findCandidates(query(underlyingId = "synthetic-underlying"))

        assertTrue(successCandidates(result).isEmpty())
    }

    @Test
    fun underlyingLookupIsWhitespaceSensitive() = runTest {
        val stored = snapshot(underlyingId = " Synthetic-Underlying ")
        val repository = repository(stored)

        val withoutWhitespace = repository.findCandidates(
            query(underlyingId = "Synthetic-Underlying")
        )
        val exact = repository.findCandidates(
            query(underlyingId = " Synthetic-Underlying ")
        )

        assertTrue(successCandidates(withoutWhitespace).isEmpty())
        assertSame(stored, successCandidates(exact).single())
    }

    @Test
    fun resultPreservesStoredOrder() = runTest {
        val first = snapshot(
            productIsin = "DE000SYNTH30",
            productWkn = "SYN030",
            issuerId = "synthetic-issuer-c",
            basePrice = 90.0,
            knockoutBarrier = 92.0
        )
        val second = snapshot(
            productIsin = "DE000SYNTH10",
            productWkn = "SYN010",
            issuerId = "synthetic-issuer-a",
            basePrice = 70.0,
            knockoutBarrier = 72.0
        )
        val third = snapshot(
            productIsin = "DE000SYNTH20",
            productWkn = "SYN020",
            issuerId = "synthetic-issuer-b",
            basePrice = 80.0,
            knockoutBarrier = 82.0
        )

        val candidates = successCandidates(
            repository(first, second, third).findCandidates(query())
        )

        assertEquals(3, candidates.size)
        assertSame(first, candidates[0])
        assertSame(second, candidates[1])
        assertSame(third, candidates[2])
    }

    @Test
    fun resultPreservesDuplicateSnapshotEntries() = runTest {
        val duplicate = snapshot()
        val repository = repository(duplicate, duplicate)

        val candidates = successCandidates(repository.findCandidates(query()))

        assertEquals(2, candidates.size)
        assertSame(duplicate, candidates[0])
        assertSame(duplicate, candidates[1])
    }

    @Test
    fun noMatchesReturnEmptySuccess() = runTest {
        val result = repository(
            snapshot(underlyingId = "synthetic-underlying-b")
        ).findCandidates(query())

        assertFalse(result is KnockoutProductSpecificationCatalogResult.DataAccessFailure)
        assertFalse(result is KnockoutProductSpecificationCatalogResult.InvalidData)
        assertTrue(result is KnockoutProductSpecificationCatalogResult.Success)
        assertTrue(successCandidates(result).isEmpty())
    }

    @Test
    fun emptyRepositoryReturnsEmptySuccess() = runTest {
        val repository = InMemoryKnockoutProductSpecificationCatalogRepository(emptyList())

        val longResult = repository.findCandidates(query(direction = TradeDirection.LONG))
        val shortResult = repository.findCandidates(query(direction = TradeDirection.SHORT))

        assertTrue(longResult is KnockoutProductSpecificationCatalogResult.Success)
        assertTrue(shortResult is KnockoutProductSpecificationCatalogResult.Success)
        assertTrue(successCandidates(longResult).isEmpty())
        assertTrue(successCandidates(shortResult).isEmpty())
    }

    @Test
    fun laterChangesToOriginalListDoNotAffectRepository() = runTest {
        val original = snapshot(productIsin = "DE000SYNTH01")
        val addedLater = snapshot(productIsin = "DE000SYNTH02")
        val source = mutableListOf(original)
        val repository = InMemoryKnockoutProductSpecificationCatalogRepository(source)

        source.clear()
        source += addedLater

        val candidates = successCandidates(repository.findCandidates(query()))
        assertEquals(1, candidates.size)
        assertSame(original, candidates.single())
        assertFalse(candidates.any { it === addedLater })
    }

    @Test
    fun queryDoesNotMutateOriginalSnapshotList() = runTest {
        val source = mutableListOf(
            snapshot(productIsin = "DE000SYNTH03"),
            snapshot(productIsin = "DE000SYNTH01", direction = TradeDirection.SHORT),
            snapshot(productIsin = "DE000SYNTH02")
        )
        val originalOrder = source.toList()
        val repository = InMemoryKnockoutProductSpecificationCatalogRepository(source)

        repository.findCandidates(query(direction = TradeDirection.LONG))
        repository.findCandidates(query(direction = TradeDirection.SHORT))
        repository.findCandidates(query(underlyingId = "unknown"))

        assertEquals(originalOrder, source)
        originalOrder.indices.forEach { index -> assertSame(originalOrder[index], source[index]) }
    }

    @Test
    fun snapshotObjectsAndMetadataRemainUnchanged() = runTest {
        val specification = KnockoutProductSpecification(
            productIsin = "DE000SYNTH01",
            productWkn = "SYN001",
            issuerId = "synthetic-issuer",
            underlyingId = "synthetic-underlying",
            direction = TradeDirection.LONG,
            basePrice = 80.125,
            knockoutBarrier = 82.5,
            ratio = 0.1,
            underlyingCurrency = "USD",
            productCurrency = "EUR"
        )
        val stored = KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "synthetic-source",
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = 1_700_000_000_250L
        )

        val returned = successCandidates(repository(stored).findCandidates(query())).single()

        assertSame(stored, returned)
        assertSame(specification, returned.specification)
        assertEquals("DE000SYNTH01", returned.specification.productIsin)
        assertEquals("SYN001", returned.specification.productWkn)
        assertEquals("synthetic-issuer", returned.specification.issuerId)
        assertEquals("synthetic-underlying", returned.specification.underlyingId)
        assertEquals(TradeDirection.LONG, returned.specification.direction)
        assertEquals(80.125, returned.specification.basePrice, 0.0)
        assertEquals(82.5, returned.specification.knockoutBarrier, 0.0)
        assertEquals(0.1, returned.specification.ratio, 0.0)
        assertEquals("USD", returned.specification.underlyingCurrency)
        assertEquals("EUR", returned.specification.productCurrency)
        assertEquals("synthetic-source", returned.sourceId)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertEquals(1_700_000_000_250L, returned.sourceTimestampEpochMillis)
    }

    @Test
    fun nullSourceTimestampRemainsNull() = runTest {
        val stored = snapshot(
            retrievedAtEpochMillis = 1_700_000_000_500L,
            sourceTimestampEpochMillis = null
        )

        val returned = successCandidates(repository(stored).findCandidates(query())).single()

        assertSame(stored, returned)
        assertEquals(1_700_000_000_500L, returned.retrievedAtEpochMillis)
        assertNull(returned.sourceTimestampEpochMillis)
    }

    @Test
    fun adapterNeverReturnsFailureModes() = runTest {
        val matchingRepository = repository(snapshot())
        val results = listOf(
            matchingRepository.findCandidates(query()),
            matchingRepository.findCandidates(query(underlyingId = "unknown")),
            InMemoryKnockoutProductSpecificationCatalogRepository(emptyList())
                .findCandidates(query())
        )

        results.forEach { result ->
            assertTrue(result is KnockoutProductSpecificationCatalogResult.Success)
            assertFalse(result is KnockoutProductSpecificationCatalogResult.DataAccessFailure)
            assertFalse(result is KnockoutProductSpecificationCatalogResult.InvalidData)
        }
    }

    @Test
    fun adapterDoesNotValidateOrCorrectSnapshots() = runTest {
        val stored = snapshot(
            issuerId = "",
            basePrice = -1.0,
            productCurrency = "eur"
        )

        val returned = successCandidates(repository(stored).findCandidates(query())).single()

        assertSame(stored, returned)
        assertEquals("", returned.specification.issuerId)
        assertEquals(-1.0, returned.specification.basePrice, 0.0)
        assertEquals("eur", returned.specification.productCurrency)
    }

    @Test
    fun resultIsNotLimitedToThreeCandidates() = runTest {
        val stored = (1..4).map { index ->
            snapshot(productIsin = "DE000SYNTH0$index")
        }

        val candidates = successCandidates(
            InMemoryKnockoutProductSpecificationCatalogRepository(stored)
                .findCandidates(query())
        )

        assertEquals(4, candidates.size)
        stored.indices.forEach { index -> assertSame(stored[index], candidates[index]) }
    }

    @Test
    fun productsFromSameIssuerRemainSeparateCandidates() = runTest {
        val first = snapshot(
            productIsin = "DE000SYNTH01",
            issuerId = "synthetic-shared-issuer"
        )
        val second = snapshot(
            productIsin = "DE000SYNTH02",
            issuerId = "synthetic-shared-issuer"
        )

        val candidates = successCandidates(repository(first, second).findCandidates(query()))

        assertEquals(2, candidates.size)
        assertSame(first, candidates[0])
        assertSame(second, candidates[1])
    }

    @Test
    fun publicApiContainsNoAndroidComposeNetworkOrDatabaseTypes() {
        val forbidden = listOf("android", "compose", "retrofit", "okhttp", "room", "sqlite")

        assertTrue(apiTypeNames().none { name -> forbidden.any(name::contains) })
    }

    private fun repository(
        vararg snapshots: KnockoutProductSpecificationSnapshot
    ) = InMemoryKnockoutProductSpecificationCatalogRepository(snapshots.toList())

    private fun query(
        underlyingId: String = "synthetic-underlying",
        direction: TradeDirection = TradeDirection.LONG
    ) = KnockoutProductSpecificationCatalogQuery(
        underlyingId = underlyingId,
        direction = direction
    )

    private fun snapshot(
        productIsin: String = "DE000SYNTH01",
        productWkn: String? = "SYN001",
        issuerId: String = "synthetic-issuer",
        underlyingId: String = "synthetic-underlying",
        direction: TradeDirection = TradeDirection.LONG,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        ratio: Double = 0.1,
        underlyingCurrency: String = "USD",
        productCurrency: String = "EUR",
        sourceId: String = "synthetic-source",
        retrievedAtEpochMillis: Long = 1_700_000_000_500L,
        sourceTimestampEpochMillis: Long? = 1_700_000_000_250L
    ) = KnockoutProductSpecificationSnapshot(
        specification = KnockoutProductSpecification(
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
        ),
        sourceId = sourceId,
        retrievedAtEpochMillis = retrievedAtEpochMillis,
        sourceTimestampEpochMillis = sourceTimestampEpochMillis
    )

    private fun successCandidates(
        result: KnockoutProductSpecificationCatalogResult
    ): List<KnockoutProductSpecificationSnapshot> =
        (result as KnockoutProductSpecificationCatalogResult.Success).candidates

    private fun apiTypeNames(): List<String> = buildList {
        val type = InMemoryKnockoutProductSpecificationCatalogRepository::class.java
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
