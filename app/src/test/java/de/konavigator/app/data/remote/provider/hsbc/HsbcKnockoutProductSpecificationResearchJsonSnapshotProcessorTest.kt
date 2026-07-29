package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessorTest {

    @Test
    fun completeCallJsonProducesExactSnapshot() {
        val snapshot = processSuccess(completeJson("Call"), RETRIEVED_AT)

        assertEquals("DE000SYNTH01", snapshot.specification.productIsin)
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
    fun completePutJsonMapsToShort() {
        assertEquals("SHORT", processSuccess(completeJson("Put")).specification.direction)
    }

    @Test
    fun malformedJsonPreservesParsingError() {
        assertEquals(
            processingFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Parsing(
                    listOf(
                        HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_JSON
                    )
                )
            ),
            process("{\"productIsin\":")
        )
    }

    @Test
    fun multipleParsingErrorsRemainInOriginalOrder() {
        assertEquals(
            processingFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Parsing(
                    listOf(
                        HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                            .UNEXPECTED_FIELD,
                        HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                            .INVALID_PRODUCT_ISIN_TYPE,
                        HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                            .INVALID_BASE_PRICE_TYPE,
                        HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                            .INVALID_PRODUCT_CURRENCY_TYPE
                    )
                )
            ),
            process(
                """{
                    "unexpectedField":"synthetic",
                    "productIsin":1,
                    "basePrice":"80.0",
                    "productCurrency":true
                }""".trimIndent()
            )
        )
    }

    @Test
    fun unsupportedDirectionPreservesMappingError() {
        assertMappingFailure("SyntheticDirection")
    }

    @Test
    fun directionCaseAndWhitespaceAreNotNormalized() {
        listOf("CALL", "call", " Call ", "PUT", "put").forEach(::assertMappingFailure)
    }

    @Test
    fun emptyObjectProducesSnapshotWithNullableSpecificationFields() {
        val snapshot = processSuccess("{}")

        assertNull(snapshot.specification.productIsin)
        assertNull(snapshot.specification.productWkn)
        assertNull(snapshot.specification.issuerId)
        assertNull(snapshot.specification.underlyingId)
        assertNull(snapshot.specification.direction)
        assertNull(snapshot.specification.basePrice)
        assertNull(snapshot.specification.knockoutBarrier)
        assertNull(snapshot.specification.ratio)
        assertNull(snapshot.specification.underlyingCurrency)
        assertNull(snapshot.specification.productCurrency)
        assertEquals("HSBC_RESEARCH_LOCAL", snapshot.sourceId)
        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun nullSourceTimestampIsNotReplaced() {
        val snapshot = processSuccess(
            """{"directionLabel":"Call","sourceTimestampEpochMillis":null}""",
            RETRIEVED_AT
        )

        assertEquals(RETRIEVED_AT, snapshot.retrievedAtEpochMillis)
        assertNull(snapshot.sourceTimestampEpochMillis)
    }

    @Test
    fun negativeRetrievedAtEpochMillisIsCopiedExactly() {
        assertEquals(-1L, processSuccess("{}", -1L).retrievedAtEpochMillis)
    }

    @Test
    fun textAndNumericValuesRemainUnchanged() {
        val snapshot = processSuccess(
            """{
                "productIsin":" de000Synthetic01 ",
                "issuerId":" Synthetic-Provider ",
                "underlyingId":" Synthetic-Underlying ",
                "directionLabel":"Call",
                "basePrice":0.0,
                "knockoutBarrier":-1.0,
                "ratio":0.0,
                "underlyingCurrency":" usd ",
                "productCurrency":"eUr"
            }""".trimIndent()
        )

        assertEquals(" de000Synthetic01 ", snapshot.specification.productIsin)
        assertEquals(" Synthetic-Provider ", snapshot.specification.issuerId)
        assertEquals(" Synthetic-Underlying ", snapshot.specification.underlyingId)
        assertEquals(0.0, snapshot.specification.basePrice)
        assertEquals(-1.0, snapshot.specification.knockoutBarrier)
        assertEquals(0.0, snapshot.specification.ratio)
        assertEquals(" usd ", snapshot.specification.underlyingCurrency)
        assertEquals("eUr", snapshot.specification.productCurrency)
    }

    @Test
    fun processingIsDeterministic() {
        val json = completeJson("Call")

        assertEquals(process(json), process(json))
    }

    private fun process(
        json: String,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ) = HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessor.process(
        json = json,
        retrievedAtEpochMillis = retrievedAtEpochMillis
    )

    private fun processSuccess(
        json: String,
        retrievedAtEpochMillis: Long = RETRIEVED_AT
    ): KnockoutProductSpecificationSnapshotDto =
        (process(json, retrievedAtEpochMillis) as
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult.Success)
            .snapshotDto

    private fun processingFailure(
        error: HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
    ) = HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult.Failure(error)

    private fun assertMappingFailure(directionLabel: String) {
        assertEquals(
            processingFailure(
                HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError.Mapping(
                    listOf(
                        HsbcKnockoutProductSpecificationRecordMappingErrorCode
                            .UNSUPPORTED_DIRECTION_LABEL
                    )
                )
            ),
            process("""{"directionLabel":"$directionLabel"}""")
        )
    }

    private fun completeJson(directionLabel: String) =
        """{
            "productIsin":"DE000SYNTH01",
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

    private companion object {
        const val RETRIEVED_AT = 1_700_000_000_500L
        const val SOURCE_TIMESTAMP = 1_700_000_000_250L
    }
}
