package de.konavigator.app.application.repository

import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductRepositoryContractsTest {

    @Test
    fun repositoryResultContainsExactlyThreeSubtypes() {
        assertEquals(
            setOf("Success", "NotFound", "DataAccessFailure"),
            RepositoryResult::class.java.declaredClasses.map { it.simpleName }.toSet()
        )
    }

    @Test
    fun successContainsExactlyValueField() {
        assertEquals(
            listOf("value"),
            instanceFields(RepositoryResult.Success::class.java).map { it.name }
        )
    }

    @Test
    fun notFoundContainsNoFields() {
        assertTrue(instanceFields(RepositoryResult.NotFound::class.java).isEmpty())
    }

    @Test
    fun dataAccessFailureContainsNoFields() {
        assertTrue(instanceFields(RepositoryResult.DataAccessFailure::class.java).isEmpty())
    }

    @Test
    fun repositoryResultContainsNoMessageFields() {
        assertTrue(resultFieldNames().none { it.contains("message") })
    }

    @Test
    fun repositoryResultContainsNoThrowableFields() {
        assertTrue(
            resultTypes().flatMap(::instanceFields).none {
                Throwable::class.java.isAssignableFrom(it.type)
            }
        )
    }

    @Test
    fun repositoryResultContainsNoErrorLists() {
        assertTrue(
            resultTypes().flatMap(::instanceFields).none {
                Collection::class.java.isAssignableFrom(it.type)
            }
        )
    }

    @Test
    fun repositoryResultContainsNoUiTextFields() {
        assertTrue(resultFieldNames().none { it.contains("text") || it.contains("label") })
    }

    @Test
    fun repositoryResultApiContainsNoAndroidOrComposeTypes() {
        assertNoTypeName("android", "compose")
    }

    @Test
    fun bothRepositoryTypesAreInterfaces() {
        assertTrue(KnockoutProductSpecificationRepository::class.java.isInterface)
        assertTrue(KnockoutProductMarketDataRepository::class.java.isInterface)
    }

    @Test
    fun specificationRepositoryHasExactlyOnePublicFunction() {
        assertEquals(1, publicMethods(KnockoutProductSpecificationRepository::class.java).size)
    }

    @Test
    fun marketDataRepositoryHasExactlyOnePublicFunction() {
        assertEquals(1, publicMethods(KnockoutProductMarketDataRepository::class.java).size)
    }

    @Test
    fun bothRepositoryFunctionsAreNamedFindByProductIsin() {
        assertTrue(repositoryMethods().all { it.name == "findByProductIsin" })
    }

    @Test
    fun bothRepositoryFunctionsAreSuspendFunctions() {
        assertTrue(
            repositoryMethods().all {
                it.parameterTypes.lastOrNull() == Continuation::class.java
            }
        )
    }

    @Test
    fun bothRepositoryFunctionsAcceptExactlyOneStringValue() {
        assertTrue(
            repositoryMethods().all {
                it.parameterTypes.dropLast(1) == listOf(String::class.java)
            }
        )
    }

    @Test
    fun specificationRepositoryReturnsSpecificationResult() {
        val specification = specification()
        val result: RepositoryResult<KnockoutProductSpecification> = runSuspend {
            CapturingSpecificationRepository(RepositoryResult.Success(specification))
                .findByProductIsin(PRODUCT_ISIN)
        }

        assertSame(specification, (result as RepositoryResult.Success).value)
    }

    @Test
    fun marketDataRepositoryReturnsMarketDataResult() {
        val marketData = marketData()
        val result: RepositoryResult<KnockoutProductMarketData> = runSuspend {
            CapturingMarketDataRepository(RepositoryResult.Success(marketData))
                .findByProductIsin(PRODUCT_ISIN)
        }

        assertSame(marketData, (result as RepositoryResult.Success).value)
    }

    @Test
    fun repositoryFunctionsHaveNonNullableContractResults() {
        val specificationResult: RepositoryResult<KnockoutProductSpecification> = runSuspend {
            CapturingSpecificationRepository(RepositoryResult.NotFound)
                .findByProductIsin(PRODUCT_ISIN)
        }
        val marketDataResult: RepositoryResult<KnockoutProductMarketData> = runSuspend {
            CapturingMarketDataRepository(RepositoryResult.DataAccessFailure)
                .findByProductIsin(PRODUCT_ISIN)
        }

        assertSame(RepositoryResult.NotFound, specificationResult)
        assertSame(RepositoryResult.DataAccessFailure, marketDataResult)
    }

    @Test
    fun repositoryFunctionsHaveNoDefaultImplementations() {
        assertTrue(repositoryMethods().all { Modifier.isAbstract(it.modifiers) })
    }

    @Test
    fun repositoryInterfacesContainNoAdditionalMethods() {
        assertEquals(2, repositoryMethods().size)
    }

    @Test
    fun specificationRepositoryReceivesIsinExactly() {
        val repository = CapturingSpecificationRepository(RepositoryResult.NotFound)

        runSuspend { repository.findByProductIsin(PRODUCT_ISIN) }

        assertEquals(PRODUCT_ISIN, repository.receivedProductIsin)
    }

    @Test
    fun marketDataRepositoryReceivesIsinExactly() {
        val repository = CapturingMarketDataRepository(RepositoryResult.NotFound)

        runSuspend { repository.findByProductIsin(PRODUCT_ISIN) }

        assertEquals(PRODUCT_ISIN, repository.receivedProductIsin)
    }

    @Test
    fun leadingWhitespaceIsNotRemoved() {
        val repository = CapturingSpecificationRepository(RepositoryResult.NotFound)

        runSuspend { repository.findByProductIsin("  $PRODUCT_ISIN") }

        assertEquals("  $PRODUCT_ISIN", repository.receivedProductIsin)
    }

    @Test
    fun trailingWhitespaceIsNotRemoved() {
        val repository = CapturingMarketDataRepository(RepositoryResult.NotFound)

        runSuspend { repository.findByProductIsin("$PRODUCT_ISIN  ") }

        assertEquals("$PRODUCT_ISIN  ", repository.receivedProductIsin)
    }

    @Test
    fun characterCaseIsNotChanged() {
        val repository = CapturingSpecificationRepository(RepositoryResult.NotFound)
        val mixedCase = "de000Test001"

        runSuspend { repository.findByProductIsin(mixedCase) }

        assertEquals(mixedCase, repository.receivedProductIsin)
    }

    @Test
    fun repositoryPortsDoNotNormalizeProductIsin() {
        val repository = CapturingMarketDataRepository(RepositoryResult.NotFound)
        val unchanged = " de000Test001 "

        runSuspend { repository.findByProductIsin(unchanged) }

        assertEquals(unchanged, repository.receivedProductIsin)
    }

    @Test
    fun repositoryPortsDoNotImplicitlyValidateProductIsin() {
        val repository = CapturingSpecificationRepository(RepositoryResult.NotFound)

        val result = runSuspend { repository.findByProductIsin("") }

        assertSame(RepositoryResult.NotFound, result)
        assertEquals("", repository.receivedProductIsin)
    }

    @Test
    fun repositoryApisContainNoAndroidOrComposeDependencies() {
        assertNoTypeName("android", "compose")
    }

    @Test
    fun repositoryApisContainNoUiDependencies() {
        assertNoTypeName("presentation", ".ui.", "UiState", "UiEvent")
    }

    @Test
    fun repositoryApisContainNoNetworkLibraries() {
        assertNoTypeName("retrofit", "okhttp", "network")
    }

    @Test
    fun repositoryApisContainNoDtoDependencies() {
        assertNoTypeName("dto")
    }

    @Test
    fun productionRepositoryContractsAreNotImplementations() {
        assertTrue(KnockoutProductSpecificationRepository::class.java.isInterface)
        assertTrue(KnockoutProductMarketDataRepository::class.java.isInterface)
        assertTrue(repositoryMethods().all { Modifier.isAbstract(it.modifiers) })
    }

    @Test
    fun repositoryContractsContainNoApplicationServiceDependency() {
        assertNoTypeName("ApplicationService", "usecase")
    }

    @Test
    fun repositoryContractsContainNoOrchestratorDependency() {
        assertNoTypeName("orchestration", "Orchestrator")
    }

    @Test
    fun repositoryContractsContainNoDomainComponentDependencies() {
        assertNoTypeName("calculator", "validation", "freshness", "source", "policy")
    }

    @Test
    fun repositoryContractsContainNoUnderlyingRepositoryDependency() {
        assertNoTypeName("UnderlyingRepository", "UnderlyingAsset", "UnderlyingTestData")
    }

    @Test
    fun repositoryCallsDoNotMutateInput() {
        val original = "  de000Test001  "
        val repository = CapturingMarketDataRepository(RepositoryResult.NotFound)

        runSuspend { repository.findByProductIsin(original) }

        assertEquals("  de000Test001  ", original)
        assertEquals(original, repository.receivedProductIsin)
    }

    private fun assertNoTypeName(vararg forbiddenFragments: String) {
        assertTrue(
            repositoryApiTypeNames().none { typeName ->
                forbiddenFragments.any { typeName.contains(it, ignoreCase = true) }
            }
        )
    }

    private fun repositoryApiTypeNames(): Set<String> = buildSet {
        add(RepositoryResult::class.java.name)
        resultTypes().forEach { type ->
            add(type.name)
            instanceFields(type).forEach { add(it.type.name) }
        }
        repositoryTypes().forEach { type ->
            add(type.name)
            publicMethods(type).forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { add(it.name) }
                method.genericParameterTypes.forEach { add(it.typeName) }
            }
        }
    }

    private fun repositoryMethods(): List<Method> = repositoryTypes().flatMap(::publicMethods)

    private fun repositoryTypes(): List<Class<*>> = listOf(
        KnockoutProductSpecificationRepository::class.java,
        KnockoutProductMarketDataRepository::class.java
    )

    private fun publicMethods(type: Class<*>): List<Method> = type.declaredMethods
        .filter { Modifier.isPublic(it.modifiers) }

    private fun resultTypes(): List<Class<*>> =
        RepositoryResult::class.java.declaredClasses.toList()

    private fun resultFieldNames(): List<String> = resultTypes()
        .flatMap(::instanceFields)
        .map { it.name.lowercase() }

    private fun instanceFields(type: Class<*>) = type.declaredFields
        .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }

    private fun specification() = KnockoutProductSpecification(
        productIsin = PRODUCT_ISIN,
        productWkn = "ABC123",
        issuerId = "issuer-a",
        underlyingId = "underlying-a",
        direction = TradeDirection.LONG,
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = "EUR"
    )

    private fun marketData() = KnockoutProductMarketData(
        productIsin = PRODUCT_ISIN,
        bid = 1.8,
        ask = 2.0,
        bidTimestampEpochMillis = 1_000L,
        askTimestampEpochMillis = 1_000L,
        currency = "EUR",
        sourceId = "source-a"
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

        return (completed ?: error("Suspend contract did not complete synchronously")).getOrThrow()
    }

    private class CapturingSpecificationRepository(
        private val result: RepositoryResult<KnockoutProductSpecification>
    ) : KnockoutProductSpecificationRepository {

        var receivedProductIsin: String? = null
            private set

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductSpecification> {
            receivedProductIsin = productIsin
            return result
        }
    }

    private class CapturingMarketDataRepository(
        private val result: RepositoryResult<KnockoutProductMarketData>
    ) : KnockoutProductMarketDataRepository {

        var receivedProductIsin: String? = null
            private set

        override suspend fun findByProductIsin(
            productIsin: String
        ): RepositoryResult<KnockoutProductMarketData> {
            receivedProductIsin = productIsin
            return result
        }
    }

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
    }
}
