package de.konavigator.app.data.inmemory

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class InMemoryKnockoutProductMarketDataRepositoryTest {

    @Test
    fun implementsMarketDataRepositoryPort() {
        assertTrue(
            KnockoutProductMarketDataRepository::class.java.isAssignableFrom(
                InMemoryKnockoutProductMarketDataRepository::class.java
            )
        )
    }

    @Test
    fun exposesExactlyOnePublicSuspendFunction() {
        val methods = publicDeclaredMethods()

        assertEquals(1, methods.size)
        assertEquals("findByProductIsin", methods.single().name)
        assertEquals(Continuation::class.java, methods.single().parameterTypes.last())
    }

    @Test
    fun publicApiContainsNoAndroidComposeNetworkOrDatabaseTypes() {
        val forbidden = listOf("android", "compose", "retrofit", "okhttp", "room", "sqlite")

        assertTrue(apiTypeNames().none { name -> forbidden.any(name::contains) })
    }

    @Test
    fun existingIsinReturnsSuccessWithExactStoredObject() {
        val marketData = marketData()
        val result = find(repository(marketData), PRODUCT_ISIN)

        assertTrue(result is RepositoryResult.Success)
        assertSame(marketData, (result as RepositoryResult.Success).value)
    }

    @Test
    fun unknownIsinReturnsNotFound() {
        assertSame(RepositoryResult.NotFound, find(repository(marketData()), "unknown"))
    }

    @Test
    fun emptyListReturnsNotFound() {
        assertSame(
            RepositoryResult.NotFound,
            find(InMemoryKnockoutProductMarketDataRepository(emptyList()), PRODUCT_ISIN)
        )
    }

    @Test
    fun lookupNeverUsesDataAccessFailureMode() {
        val repository = repository(marketData())

        assertFalse(find(repository, PRODUCT_ISIN) === RepositoryResult.DataAccessFailure)
        assertFalse(find(repository, "unknown") === RepositoryResult.DataAccessFailure)
    }

    @Test
    fun lookupIsCaseSensitiveAndDoesNotUppercase() {
        val repository = repository(marketData())

        assertSame(RepositoryResult.NotFound, find(repository, PRODUCT_ISIN.lowercase()))
    }

    @Test
    fun lookupIsWhitespaceSensitive() {
        val spaced = " $PRODUCT_ISIN "
        val repository = repository(marketData(productIsin = spaced))

        assertSame(RepositoryResult.NotFound, find(repository, PRODUCT_ISIN))
        assertTrue(find(repository, spaced) is RepositoryResult.Success)
    }

    @Test
    fun leadingWhitespaceIsNotRemoved() {
        assertSame(RepositoryResult.NotFound, find(repository(marketData()), " $PRODUCT_ISIN"))
    }

    @Test
    fun trailingWhitespaceIsNotRemoved() {
        assertSame(RepositoryResult.NotFound, find(repository(marketData()), "$PRODUCT_ISIN "))
    }

    @Test
    fun aliasesAndFallbackKeysAreNotResolved() {
        val repository = repository(marketData())

        assertSame(RepositoryResult.NotFound, find(repository, "product-alias"))
        assertSame(RepositoryResult.NotFound, find(repository, SOURCE_ID))
    }

    @Test
    fun exactDuplicateIsinIsRejectedWithoutLastWriteWins() {
        val first = marketData(bid = 1.8)
        val second = marketData(bid = 1.9)

        assertThrows(IllegalArgumentException::class.java) {
            InMemoryKnockoutProductMarketDataRepository(listOf(first, second))
        }
    }

    @Test
    fun differentlyCasedIsinsAreDistinctKeys() {
        val upper = marketData(productIsin = PRODUCT_ISIN)
        val lower = marketData(productIsin = PRODUCT_ISIN.lowercase())
        val repository = InMemoryKnockoutProductMarketDataRepository(listOf(upper, lower))

        assertSame(upper, successValue(find(repository, upper.productIsin)))
        assertSame(lower, successValue(find(repository, lower.productIsin)))
    }

    @Test
    fun differentlySpacedIsinsAreDistinctKeys() {
        val exact = marketData(productIsin = PRODUCT_ISIN)
        val spaced = marketData(productIsin = " $PRODUCT_ISIN")
        val repository = InMemoryKnockoutProductMarketDataRepository(listOf(exact, spaced))

        assertSame(exact, successValue(find(repository, exact.productIsin)))
        assertSame(spaced, successValue(find(repository, spaced.productIsin)))
    }

    @Test
    fun laterChangesToOriginalListDoNotAffectSnapshot() {
        val original = marketData()
        val addedLater = marketData(productIsin = "DE000TEST002")
        val values = mutableListOf(original)
        val repository = InMemoryKnockoutProductMarketDataRepository(values)

        values.clear()
        values += addedLater

        assertSame(original, successValue(find(repository, PRODUCT_ISIN)))
        assertSame(RepositoryResult.NotFound, find(repository, addedLater.productIsin))
    }

    @Test
    fun repositoryDoesNotExposeStoredCollectionAndIsReadOnly() {
        assertTrue(
            InMemoryKnockoutProductMarketDataRepository::class.java.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
                .all { Modifier.isPrivate(it.modifiers) }
        )
        assertEquals(setOf("findByProductIsin"), publicDeclaredMethods().map { it.name }.toSet())
        assertTrue(
            publicDeclaredMethods().none {
                it.name.contains("insert", true) ||
                    it.name.contains("update", true) ||
                    it.name.contains("delete", true)
            }
        )
    }

    @Test
    fun constructionAndLookupDoNotMutateInput() {
        val first = marketData()
        val second = marketData(productIsin = "DE000TEST002")
        val values = mutableListOf(first, second)
        val copy = values.toList()
        val repository = InMemoryKnockoutProductMarketDataRepository(values)

        find(repository, PRODUCT_ISIN)

        assertEquals(copy, values)
    }

    @Test
    fun repositoryDoesNotValidateOrCorrectDomainModel() {
        val unchanged = marketData(bid = -1.0, ask = null, currency = "eur")

        assertSame(unchanged, successValue(find(repository(unchanged), PRODUCT_ISIN)))
    }

    private fun repository(
        vararg marketData: KnockoutProductMarketData
    ) = InMemoryKnockoutProductMarketDataRepository(marketData.toList())

    private fun find(
        repository: InMemoryKnockoutProductMarketDataRepository,
        productIsin: String
    ): RepositoryResult<KnockoutProductMarketData> = runSuspend {
        repository.findByProductIsin(productIsin)
    }

    private fun successValue(
        result: RepositoryResult<KnockoutProductMarketData>
    ) = (result as RepositoryResult.Success).value

    private fun marketData(
        productIsin: String = PRODUCT_ISIN,
        bid: Double? = 1.8,
        ask: Double? = 2.0,
        currency: String = "EUR"
    ) = KnockoutProductMarketData(
        productIsin = productIsin,
        bid = bid,
        ask = ask,
        bidTimestampEpochMillis = NOW,
        askTimestampEpochMillis = NOW,
        currency = currency,
        sourceId = SOURCE_ID
    )

    private fun publicDeclaredMethods() =
        InMemoryKnockoutProductMarketDataRepository::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }

    private fun apiTypeNames(): List<String> = buildList {
        val type = InMemoryKnockoutProductMarketDataRepository::class.java
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

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val SOURCE_ID = "source-a"
        const val NOW = 1_000L
    }
}
