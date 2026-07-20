package de.konavigator.app.data.inmemory

import de.konavigator.app.application.repository.KnockoutProductSpecificationRepository
import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
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

class InMemoryKnockoutProductSpecificationRepositoryTest {

    @Test
    fun implementsSpecificationRepositoryPort() {
        assertTrue(
            KnockoutProductSpecificationRepository::class.java.isAssignableFrom(
                InMemoryKnockoutProductSpecificationRepository::class.java
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
        val specification = specification()
        val result = find(repository(specification), PRODUCT_ISIN)

        assertTrue(result is RepositoryResult.Success)
        assertSame(specification, (result as RepositoryResult.Success).value)
    }

    @Test
    fun unknownIsinReturnsNotFound() {
        assertSame(RepositoryResult.NotFound, find(repository(specification()), "unknown"))
    }

    @Test
    fun emptyListReturnsNotFound() {
        assertSame(
            RepositoryResult.NotFound,
            find(InMemoryKnockoutProductSpecificationRepository(emptyList()), PRODUCT_ISIN)
        )
    }

    @Test
    fun lookupNeverUsesDataAccessFailureMode() {
        val repository = repository(specification())

        assertFalse(find(repository, PRODUCT_ISIN) === RepositoryResult.DataAccessFailure)
        assertFalse(find(repository, "unknown") === RepositoryResult.DataAccessFailure)
    }

    @Test
    fun lookupIsCaseSensitiveAndDoesNotUppercase() {
        val repository = repository(specification())

        assertSame(RepositoryResult.NotFound, find(repository, PRODUCT_ISIN.lowercase()))
    }

    @Test
    fun lookupIsWhitespaceSensitive() {
        val spaced = " $PRODUCT_ISIN "
        val repository = repository(specification(productIsin = spaced))

        assertSame(RepositoryResult.NotFound, find(repository, PRODUCT_ISIN))
        assertTrue(find(repository, spaced) is RepositoryResult.Success)
    }

    @Test
    fun leadingWhitespaceIsNotRemoved() {
        assertSame(
            RepositoryResult.NotFound,
            find(repository(specification()), " $PRODUCT_ISIN")
        )
    }

    @Test
    fun trailingWhitespaceIsNotRemoved() {
        assertSame(
            RepositoryResult.NotFound,
            find(repository(specification()), "$PRODUCT_ISIN ")
        )
    }

    @Test
    fun wknAndAliasesAreNotFallbackKeys() {
        val repository = repository(specification(productWkn = "ABC123"))

        assertSame(RepositoryResult.NotFound, find(repository, "ABC123"))
        assertSame(RepositoryResult.NotFound, find(repository, "product-alias"))
    }

    @Test
    fun exactDuplicateIsinIsRejectedWithoutLastWriteWins() {
        val first = specification(basePrice = 80.0)
        val second = specification(basePrice = 90.0)

        assertThrows(IllegalArgumentException::class.java) {
            InMemoryKnockoutProductSpecificationRepository(listOf(first, second))
        }
    }

    @Test
    fun differentlyCasedIsinsAreDistinctKeys() {
        val upper = specification(productIsin = PRODUCT_ISIN)
        val lower = specification(productIsin = PRODUCT_ISIN.lowercase())
        val repository = InMemoryKnockoutProductSpecificationRepository(listOf(upper, lower))

        assertSame(upper, successValue(find(repository, upper.productIsin)))
        assertSame(lower, successValue(find(repository, lower.productIsin)))
    }

    @Test
    fun differentlySpacedIsinsAreDistinctKeys() {
        val exact = specification(productIsin = PRODUCT_ISIN)
        val spaced = specification(productIsin = " $PRODUCT_ISIN")
        val repository = InMemoryKnockoutProductSpecificationRepository(listOf(exact, spaced))

        assertSame(exact, successValue(find(repository, exact.productIsin)))
        assertSame(spaced, successValue(find(repository, spaced.productIsin)))
    }

    @Test
    fun laterChangesToOriginalListDoNotAffectSnapshot() {
        val original = specification()
        val addedLater = specification(productIsin = "DE000TEST002")
        val values = mutableListOf(original)
        val repository = InMemoryKnockoutProductSpecificationRepository(values)

        values.clear()
        values += addedLater

        assertSame(original, successValue(find(repository, PRODUCT_ISIN)))
        assertSame(RepositoryResult.NotFound, find(repository, addedLater.productIsin))
    }

    @Test
    fun repositoryDoesNotExposeStoredCollectionAndIsReadOnly() {
        assertTrue(
            InMemoryKnockoutProductSpecificationRepository::class.java.declaredFields
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
        val first = specification()
        val second = specification(productIsin = "DE000TEST002")
        val values = mutableListOf(first, second)
        val copy = values.toList()
        val repository = InMemoryKnockoutProductSpecificationRepository(values)

        find(repository, PRODUCT_ISIN)

        assertEquals(copy, values)
    }

    @Test
    fun repositoryDoesNotValidateOrCorrectDomainModel() {
        val unchanged = specification(
            issuerId = "",
            basePrice = -1.0,
            productCurrency = "eur"
        )

        assertSame(unchanged, successValue(find(repository(unchanged), PRODUCT_ISIN)))
    }

    private fun repository(
        vararg specifications: KnockoutProductSpecification
    ) = InMemoryKnockoutProductSpecificationRepository(specifications.toList())

    private fun find(
        repository: InMemoryKnockoutProductSpecificationRepository,
        productIsin: String
    ): RepositoryResult<KnockoutProductSpecification> = runSuspend {
        repository.findByProductIsin(productIsin)
    }

    private fun successValue(
        result: RepositoryResult<KnockoutProductSpecification>
    ) = (result as RepositoryResult.Success).value

    private fun specification(
        productIsin: String = PRODUCT_ISIN,
        productWkn: String? = "ABC123",
        issuerId: String = "issuer-a",
        basePrice: Double = 80.0,
        productCurrency: String = "EUR"
    ) = KnockoutProductSpecification(
        productIsin = productIsin,
        productWkn = productWkn,
        issuerId = issuerId,
        underlyingId = "underlying-a",
        direction = TradeDirection.LONG,
        basePrice = basePrice,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = productCurrency
    )

    private fun publicDeclaredMethods() =
        InMemoryKnockoutProductSpecificationRepository::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }

    private fun apiTypeNames(): List<String> = buildList {
        val type = InMemoryKnockoutProductSpecificationRepository::class.java
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
    }
}
