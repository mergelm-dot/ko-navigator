package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.application.repository.RepositoryResult
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationSnapshotRepository
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import java.io.File
import java.nio.charset.StandardCharsets
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
class HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyInputCreatesQueryableEmptyRepository() = runTest {
        val repository = loadSuccess(emptyMap())

        assertSame(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN_1))
    }

    @Test
    fun validCallFileCreatesExactDomainSnapshot() = runTest {
        val snapshot = findSuccess(
            loadSuccess(mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call"))),
            PRODUCT_ISIN_1
        )

        assertEquals(PRODUCT_ISIN_1, snapshot.specification.productIsin)
        assertEquals("SYN001", snapshot.specification.productWkn)
        assertEquals("synthetic-provider", snapshot.specification.issuerId)
        assertEquals("synthetic-underlying", snapshot.specification.underlyingId)
        assertEquals(TradeDirection.LONG, snapshot.specification.direction)
        assertEquals(80.125, snapshot.specification.basePrice, 0.0)
        assertEquals(82.5, snapshot.specification.knockoutBarrier, 0.0)
        assertEquals(0.1, snapshot.specification.ratio, 0.0)
        assertEquals("USD", snapshot.specification.underlyingCurrency)
        assertEquals("EUR", snapshot.specification.productCurrency)
        assertEquals("HSBC_RESEARCH_LOCAL", snapshot.sourceId)
        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertEquals(SOURCE_TIMESTAMP, snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun validPutFileMapsToShortDomainDirection() = runTest {
        val snapshot = findSuccess(
            loadSuccess(mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Put"))),
            PRODUCT_ISIN_1
        )

        assertEquals(TradeDirection.SHORT, snapshot.specification.direction)
    }

    @Test
    fun multipleValidFilesAreQueryableByExactKey() = runTest {
        val repository = loadSuccess(
            linkedMapOf(
                PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call"),
                PRODUCT_ISIN_2 to writeJson(PRODUCT_ISIN_2, "Put")
            )
        )

        assertTrue(repository.findByProductIsin(PRODUCT_ISIN_1) is RepositoryResult.Success)
        assertTrue(repository.findByProductIsin(PRODUCT_ISIN_2) is RepositoryResult.Success)
        assertSame(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN_3))
    }

    @Test
    fun missingFilePreservesFileLoadingFailureExactly() = runTest {
        val expected =
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                .FileLoading(
                    listOf(
                        HsbcKnockoutProductSpecificationResearchJsonFileLoadingError(
                            productIsinKey = PRODUCT_ISIN_1,
                            code = fileReadingFailed
                        )
                    )
                )

        assertEquals(
            failure(expected),
            load(mapOf(PRODUCT_ISIN_1 to missingFile("missing.json")))
        )
    }

    @Test
    fun malformedJsonPreservesProviderCreationFailureExactly() = runTest {
        val result = load(
            mapOf(PRODUCT_ISIN_1 to writeFile("malformed.json", "{\"productIsin\":"))
        ) as HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
            .Failure
        val error = result.error as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                .ProviderCreation
        val processingFailure = error.errors.single() as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                .ProcessingFailure
        val parsing = processingFailure.error as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Parsing

        assertEquals(PRODUCT_ISIN_1, processingFailure.productIsinKey)
        assertEquals(
            listOf(HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_JSON),
            parsing.errors
        )
    }

    @Test
    fun unsupportedDirectionPreservesMappingFailureExactly() = runTest {
        val result = load(
            mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "SyntheticDirection"))
        ) as HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
            .Failure
        val error = result.error as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                .ProviderCreation
        val processingFailure = error.errors.single() as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                .ProcessingFailure
        val mapping = processingFailure.error as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Mapping

        assertEquals(
            listOf(
                HsbcKnockoutProductSpecificationRecordMappingErrorCode
                    .UNSUPPORTED_DIRECTION_LABEL
            ),
            mapping.errors
        )
    }

    @Test
    fun mismatchingEmbeddedProductIsinIsPreservedExactly() = runTest {
        val expected =
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                .ProviderCreation(
                    listOf(
                        HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                            .ProductIsinMismatch(PRODUCT_ISIN_1, PRODUCT_ISIN_2)
                    )
                )

        assertEquals(
            failure(expected),
            load(mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_2, "Call")))
        )
    }

    @Test
    fun missingRequiredDomainValuesProduceRepositoryInvalidData() = runTest {
        val file = writeFile(
            "missing-domain-values.json",
            """{"productIsin":"$PRODUCT_ISIN_1"}"""
        )
        val repository = loadSuccess(mapOf(PRODUCT_ISIN_1 to file))

        assertSame(RepositoryResult.InvalidData, repository.findByProductIsin(PRODUCT_ISIN_1))
    }

    @Test
    fun exactWhitespaceAndCaseRemainRequiredForRepositoryLookup() = runTest {
        val exactKey = " $PRODUCT_ISIN_1 "
        val repository = loadSuccess(mapOf(exactKey to writeJson(exactKey, "Call")))

        assertTrue(repository.findByProductIsin(exactKey) is RepositoryResult.Success)
        assertSame(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN_1))
    }

    @Test
    fun inputMapIsCopiedBeforeProviderLoadingSuspends() = runTest {
        val input = mutableMapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call"))
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoader.load(
                filesByProductIsin = input,
                retrievedAtEpochMillis = RETRIEVED_AT,
                dispatcher = dispatcher
            )
        }

        input.clear()
        input[PRODUCT_ISIN_2] = writeJson(PRODUCT_ISIN_2, "Put")
        advanceUntilIdle()

        val repository = (deferred.await() as
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
                .Success).repository
        assertTrue(repository.findByProductIsin(PRODUCT_ISIN_1) is RepositoryResult.Success)
        assertSame(RepositoryResult.NotFound, repository.findByProductIsin(PRODUCT_ISIN_2))
    }

    @Test
    fun injectedDispatcherControlsUnderlyingFileLoading() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoader.load(
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
            HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
                .Success)
    }

    @Test
    fun negativeRetrievedAtEpochMillisIsPreservedInDomainSnapshot() = runTest {
        val repository = loadSuccess(
            mapOf(PRODUCT_ISIN_1 to writeJson(PRODUCT_ISIN_1, "Call")),
            retrievedAtEpochMillis = -1L
        )

        assertEquals(
            -1L,
            findSuccess(repository, PRODUCT_ISIN_1).retrievedAtEpochMillis
        )
    }

    @Test
    fun nullSourceTimestampIsNotReplaced() = runTest {
        val json =
            """{"productIsin":"$PRODUCT_ISIN_1","productWkn":"SYN001","issuerId":"synthetic-provider","underlyingId":"synthetic-underlying","directionLabel":"Call","basePrice":80.125,"knockoutBarrier":82.5,"ratio":0.1,"underlyingCurrency":"USD","productCurrency":"EUR","sourceTimestampEpochMillis":null}"""
        val snapshot = findSuccess(
            loadSuccess(mapOf(PRODUCT_ISIN_1 to writeFile("null-time.json", json))),
            PRODUCT_ISIN_1
        )

        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterRepositoryCreationAndQuery() = runTest {
        val file = writeJson(PRODUCT_ISIN_1, "Call")
        val repository = loadSuccess(mapOf(PRODUCT_ISIN_1 to file))

        findSuccess(repository, PRODUCT_ISIN_1)

        assertTrue(file.delete())
    }

    @Test
    fun repeatedLoadingProducesEquivalentRepositoryResults() = runTest {
        val file = writeJson(PRODUCT_ISIN_1, "Call")

        val first = loadSuccess(mapOf(PRODUCT_ISIN_1 to file))
            .findByProductIsin(PRODUCT_ISIN_1)
        val second = loadSuccess(mapOf(PRODUCT_ISIN_1 to file))
            .findByProductIsin(PRODUCT_ISIN_1)

        assertEquals(first, second)
    }

    private suspend fun TestScope.load(
        files: Map<String, File>,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ): HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult =
        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoader.load(
            filesByProductIsin = files,
            retrievedAtEpochMillis = retrievedAtEpochMillis,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

    private suspend fun TestScope.loadSuccess(
        files: Map<String, File>,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ) = (load(files, retrievedAtEpochMillis) as
        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult.Success)
        .repository

    private fun failure(
        error: HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
    ) = HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
        .Failure(error)

    private suspend fun findSuccess(
        repository: RemoteKnockoutProductSpecificationSnapshotRepository,
        productIsin: String
    ): KnockoutProductSpecificationSnapshot =
        (repository.findByProductIsin(productIsin) as RepositoryResult.Success).value

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
