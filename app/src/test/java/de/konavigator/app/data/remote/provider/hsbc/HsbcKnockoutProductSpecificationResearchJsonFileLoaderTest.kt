package de.konavigator.app.data.remote.provider.hsbc

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HsbcKnockoutProductSpecificationResearchJsonFileLoaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun emptyInputProducesEmptySuccess() = runTest {
        assertTrue(loadSuccess(emptyMap()).isEmpty())
    }

    @Test
    fun singleFileContentIsReadExactly() = runTest {
        val content = "  {\r\n  \"productIsin\": \"DE000SYNTH01\"\r\n}\r\n  "
        val loaded = loadSuccess(mapOf(PRODUCT_ISIN_1 to writeFile("one.json", content)))

        assertEquals(setOf(PRODUCT_ISIN_1), loaded.keys)
        assertEquals(content, loaded.getValue(PRODUCT_ISIN_1))
    }

    @Test
    fun multipleFilesPreserveKeysContentsAndInputOrder() = runTest {
        val input = linkedMapOf(
            PRODUCT_ISIN_1 to writeFile("one.json", " first\n"),
            PRODUCT_ISIN_2 to writeFile("two.json", "SECOND\r\n"),
            PRODUCT_ISIN_3 to writeFile("three.json", "\nthird ")
        )

        val loaded = loadSuccess(input)

        assertEquals(listOf(PRODUCT_ISIN_1, PRODUCT_ISIN_2, PRODUCT_ISIN_3), loaded.keys.toList())
        assertEquals(" first\n", loaded.getValue(PRODUCT_ISIN_1))
        assertEquals("SECOND\r\n", loaded.getValue(PRODUCT_ISIN_2))
        assertEquals("\nthird ", loaded.getValue(PRODUCT_ISIN_3))
    }

    @Test
    fun productIsinKeyWhitespaceAndCaseRemainUnchanged() = runTest {
        val exactKey = " de000Synthetic01 "
        val loaded = loadSuccess(mapOf(exactKey to writeFile("exact.json", "{}")))

        assertTrue(exactKey in loaded)
        assertFalse("DE000SYNTHETIC01" in loaded)
        assertFalse("de000Synthetic01" in loaded)
    }

    @Test
    fun missingFileProducesTypedFailure() = runTest {
        assertEquals(
            loadingFailure(loadingError(PRODUCT_ISIN_1)),
            load(mapOf(PRODUCT_ISIN_1 to missingFile("missing.json")))
        )
    }

    @Test
    fun multipleReadingFailuresRemainInInputOrder() = runTest {
        val result = load(
            linkedMapOf(
                PRODUCT_ISIN_1 to missingFile("missing-one.json"),
                PRODUCT_ISIN_2 to missingFile("missing-two.json"),
                PRODUCT_ISIN_3 to missingFile("missing-three.json")
            )
        ) as HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Failure

        assertEquals(
            listOf(PRODUCT_ISIN_1, PRODUCT_ISIN_2, PRODUCT_ISIN_3),
            result.errors.map { it.productIsinKey }
        )
        assertTrue(
            result.errors.all {
                it.code == HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode
                    .FILE_READING_FAILED
            }
        )
    }

    @Test
    fun anyReadingFailurePreventsPartialSuccess() = runTest {
        val result = load(
            linkedMapOf(
                PRODUCT_ISIN_1 to writeFile("valid.json", "{}"),
                PRODUCT_ISIN_2 to missingFile("missing.json")
            )
        )

        assertTrue(result is HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Failure)
        assertFalse(result is HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Success)
    }

    @Test
    fun inputMapIsCopiedBeforeDispatcherResumes() = runTest {
        val originalContent = " original\n"
        val input = mutableMapOf(
            PRODUCT_ISIN_1 to writeFile("original.json", originalContent)
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            HsbcKnockoutProductSpecificationResearchJsonFileLoader.load(input, dispatcher)
        }

        input.clear()
        input[PRODUCT_ISIN_2] = writeFile("added.json", "added")
        advanceUntilIdle()

        val loaded = successMap(deferred.await())
        assertEquals(originalContent, loaded.getValue(PRODUCT_ISIN_1))
        assertFalse(PRODUCT_ISIN_2 in loaded)
    }

    @Test
    fun injectedDispatcherControlsFileAccess() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            HsbcKnockoutProductSpecificationResearchJsonFileLoader.load(
                mapOf(PRODUCT_ISIN_1 to writeFile("controlled.json", "{}")),
                dispatcher
            )
        }

        assertFalse(deferred.isCompleted)
        advanceUntilIdle()

        assertTrue(deferred.isCompleted)
        assertTrue(
            deferred.await() is
                HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Success
        )
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterLoading() = runTest {
        val file = writeFile("deletable.json", "{}")

        loadSuccess(mapOf(PRODUCT_ISIN_1 to file))

        assertTrue(file.delete())
    }

    private suspend fun TestScope.load(
        values: Map<String, File>
    ): HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult =
        HsbcKnockoutProductSpecificationResearchJsonFileLoader.load(
            filesByProductIsin = values,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

    private suspend fun TestScope.loadSuccess(values: Map<String, File>): Map<String, String> =
        successMap(load(values))

    private fun successMap(
        result: HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult
    ) = (result as HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Success)
        .researchJsonByProductIsin

    private fun loadingFailure(
        vararg errors: HsbcKnockoutProductSpecificationResearchJsonFileLoadingError
    ) = HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Failure(errors.toList())

    private fun loadingError(
        productIsinKey: String
    ) = HsbcKnockoutProductSpecificationResearchJsonFileLoadingError(
        productIsinKey = productIsinKey,
        code = HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode
            .FILE_READING_FAILED
    )

    private fun writeFile(name: String, content: String): File =
        temporaryFolder.newFile(name).also { file ->
            file.writeText(content, StandardCharsets.UTF_8)
        }

    private fun missingFile(name: String) = File(temporaryFolder.root, name)

    private companion object {
        const val PRODUCT_ISIN_1 = "DE000SYNTH01"
        const val PRODUCT_ISIN_2 = "DE000SYNTH02"
        const val PRODUCT_ISIN_3 = "DE000SYNTH03"
    }
}
