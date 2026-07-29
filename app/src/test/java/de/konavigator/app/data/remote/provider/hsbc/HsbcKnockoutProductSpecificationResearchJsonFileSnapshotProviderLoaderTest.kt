package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider
import de.konavigator.app.data.remote.provider.ProviderResult
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyInputCreatesEmptyQueryableProvider() = runTest {
        val provider = loadSuccess(emptyMap())

        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN_1))
    }

    @Test
    fun validCallFileCreatesExactQueryableSnapshot() = runTest {
        val snapshot = findSuccess(
            loadSuccess(mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call"))),
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
    fun multipleValidFilesCreateExactProviderEntries() = runTest {
        val provider = loadSuccess(
            linkedMapOf(
                PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call"),
                PRODUCT_ISIN_2 to writeJson(PRODUCT_ISIN_2, "Put")
            )
        )

        assertEquals("LONG", findSuccess(provider, PRODUCT_ISIN_1).specification.direction)
        assertEquals("SHORT", findSuccess(provider, PRODUCT_ISIN_2).specification.direction)
    }

    @Test
    fun missingFilePreservesTypedFileLoadingFailure() = runTest {
        assertEquals(
            fileLoadingFailure(loadingError(PRODUCT_ISIN_1)),
            load(mapOf(PRODUCT_ISIN_1 to missingFile("missing.json")))
        )
    }

    @Test
    fun multipleFileLoadingErrorsRemainInInputOrder() = runTest {
        val result = load(
            linkedMapOf(
                PRODUCT_ISIN_1 to missingFile("missing-one.json"),
                PRODUCT_ISIN_2 to missingFile("missing-two.json"),
                PRODUCT_ISIN_3 to missingFile("missing-three.json")
            )
        ) as HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult.Failure
        val error = result.error as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError.FileLoading

        assertEquals(
            listOf(PRODUCT_ISIN_1, PRODUCT_ISIN_2, PRODUCT_ISIN_3),
            error.errors.map { it.productIsinKey }
        )
        assertTrue(error.errors.all { it.code == fileReadingFailed })
    }

    @Test
    fun fileLoadingFailureStopsBeforeProviderCreation() = runTest {
        val result = load(
            linkedMapOf(
                PRODUCT_ISIN_1 to writeFile("invalid.json", "{\"productIsin\":"),
                PRODUCT_ISIN_2 to missingFile("missing.json")
            )
        ) as HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult.Failure

        assertTrue(result.error is
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError.FileLoading)
        assertEquals(
            listOf(PRODUCT_ISIN_2),
            (result.error as
                HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                    .FileLoading).errors.map { it.productIsinKey }
        )
    }

    @Test
    fun malformedJsonPreservesTypedProviderCreationFailure() = runTest {
        assertEquals(
            providerCreationFailure(
                processingFailure(
                    PRODUCT_ISIN_1,
                    HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Parsing(
                        listOf(
                            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                                .INVALID_JSON
                        )
                    )
                )
            ),
            load(mapOf(PRODUCT_ISIN_1 to writeFile("invalid.json", "{\"x\":")))
        )
    }

    @Test
    fun unsupportedDirectionPreservesMappingFailure() = runTest {
        assertEquals(
            providerCreationFailure(
                processingFailure(
                    PRODUCT_ISIN_1,
                    HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Mapping(
                        listOf(
                            HsbcKnockoutProductSpecificationRecordMappingErrorCode
                                .UNSUPPORTED_DIRECTION_LABEL
                        )
                    )
                )
            ),
            load(
                mapOf(
                    PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "SyntheticDirection")
                )
            )
        )
    }

    @Test
    fun mismatchingEmbeddedProductIsinIsPreserved() = runTest {
        assertEquals(
            providerCreationFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                    .ProductIsinMismatch(PRODUCT_ISIN_1, PRODUCT_ISIN_2)
            ),
            load(mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_2, "Call")))
        )
    }

    @Test
    fun productIsinWhitespaceAndCaseAreNotNormalized() = runTest {
        val exactKey = " $PRODUCT_ISIN_1 "
        val provider = loadSuccess(mapOf(exactKey to writeJson(exactKey, "Call")))

        assertTrue(find(provider, exactKey) is ProviderResult.Success)
        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN_1))
    }

    @Test
    fun inputMapIsCopiedBeforeFileLoadingSuspends() = runTest {
        val input = mutableMapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call"))
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoader.load(
                filesByProductIsin = input,
                retrievedAtEpochMillis = RETRIEVED_AT,
                dispatcher = dispatcher
            )
        }

        input.clear()
        input[PRODUCT_ISIN_2] = writeJson(PRODUCT_ISIN_2, "Put")
        advanceUntilIdle()

        val provider = (deferred.await() as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult.Success)
            .provider
        assertTrue(find(provider, PRODUCT_ISIN_1) is ProviderResult.Success)
        assertSame(ProviderResult.NotFound, find(provider, PRODUCT_ISIN_2))
    }

    @Test
    fun injectedDispatcherControlsFileLoading() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoader.load(
                filesByProductIsin = mapOf(
                    PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call")
                ),
                retrievedAtEpochMillis = RETRIEVED_AT,
                dispatcher = dispatcher
            )
        }

        assertFalse(deferred.isCompleted)
        advanceUntilIdle()
        assertTrue(deferred.isCompleted)
        assertTrue(deferred.await() is
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult.Success)
    }

    @Test
    fun negativeRetrievedAtEpochMillisIsCopiedExactly() = runTest {
        val provider = loadSuccess(
            mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call")),
            retrievedAtEpochMillis = -1L
        )

        assertEquals(-1L, findSuccess(provider, PRODUCT_ISIN_1).retrievedAtEpochMillis)
    }

    @Test
    fun nullSourceTimestampIsNotReplaced() = runTest {
        val json =
            """{"productIsin":"$PRODUCT_ISIN_1","directionLabel":"Call","sourceTimestampEpochMillis":null}"""
        val snapshot = findSuccess(
            loadSuccess(mapOf(PRODUCT_ISIN_1 to writeFile("null-time.json", json))),
            PRODUCT_ISIN_1
        )

        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterSuccessfulLoading() = runTest {
        val file = writeJson(PRODUCT_ISIN_1, "Call")

        loadSuccess(mapOf(PRODUCT_ISIN_1 to file))

        assertTrue(file.delete())
    }

    @Test
    fun repeatedLoadingIsDeterministic() = runTest {
        val file = writeJson(PRODUCT_ISIN_1, "Call")

        val first = findSuccess(loadSuccess(mapOf(PRODUCT_ISIN_1 to file)), PRODUCT_ISIN_1)
        val second = findSuccess(loadSuccess(mapOf(PRODUCT_ISIN_1 to file)), PRODUCT_ISIN_1)

        assertEquals(first, second)
    }

    private suspend fun TestScope.load(
        files: Map<String, File>,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ): HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult =
        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoader.load(
            filesByProductIsin = files,
            retrievedAtEpochMillis = retrievedAtEpochMillis,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

    private suspend fun TestScope.loadSuccess(
        files: Map<String, File>,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ) = (load(files, retrievedAtEpochMillis) as
        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult.Success)
        .provider

    private fun fileLoadingFailure(
        vararg errors: HsbcKnockoutProductSpecificationResearchJsonFileLoadingError
    ) = HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult.Failure(
        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError.FileLoading(
            errors.toList()
        )
    )

    private fun providerCreationFailure(
        vararg errors: HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
    ) = HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult.Failure(
        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
            .ProviderCreation(errors.toList())
    )

    private fun processingFailure(
        productIsinKey: String,
        error: HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
    ) = HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
        .ProcessingFailure(productIsinKey, error)

    private fun loadingError(
        productIsinKey: String
    ) = HsbcKnockoutProductSpecificationResearchJsonFileLoadingError(
        productIsinKey,
        fileReadingFailed
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

    private fun writeJson(productIsin: String, directionLabel: String): File =
        writeFile(
            "research-${fileCounter++}.json",
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
        )

    private fun writeFile(name: String, content: String): File =
        temporaryFolder.newFile(name).also { it.writeText(content, StandardCharsets.UTF_8) }

    private fun missingFile(name: String) = File(temporaryFolder.root, name)

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

    private var fileCounter = 0

    private companion object {
        const val PRODUCT_ISIN_1 = "DE000SYNTH01"
        const val PRODUCT_ISIN_2 = "DE000SYNTH02"
        const val PRODUCT_ISIN_3 = "DE000SYNTH03"
        const val RETRIEVED_AT = 1_700_000_000_500L
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L

        val fileReadingFailed =
            HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode.FILE_READING_FAILED
    }
}
