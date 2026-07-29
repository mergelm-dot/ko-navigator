package de.konavigator.app.data.remote.provider.hsbc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HsbcKnockoutProductSpecificationResearchJsonParserTest {

    @Test
    fun completeSyntheticObjectParsesEveryValueExactly() {
        val record = parseSuccess(
            """{
                "productIsin":"DE000SYNTH01",
                "productWkn":"SYN001",
                "issuerId":"synthetic-provider",
                "underlyingId":"synthetic-underlying",
                "directionLabel":"Call",
                "basePrice":80.125,
                "knockoutBarrier":82.5,
                "ratio":0.1,
                "underlyingCurrency":"USD",
                "productCurrency":"EUR",
                "sourceTimestampEpochMillis":1700000000250
            }""".trimIndent()
        )

        assertEquals("DE000SYNTH01", record.productIsin)
        assertEquals("SYN001", record.productWkn)
        assertEquals("synthetic-provider", record.issuerId)
        assertEquals("synthetic-underlying", record.underlyingId)
        assertEquals("Call", record.directionLabel)
        assertEquals(80.125, record.basePrice)
        assertEquals(82.5, record.knockoutBarrier)
        assertEquals(0.1, record.ratio)
        assertEquals("USD", record.underlyingCurrency)
        assertEquals("EUR", record.productCurrency)
        assertEquals(1_700_000_000_250L, record.sourceTimestampEpochMillis)
    }

    @Test
    fun explicitNullValuesRemainNull() {
        val record = parseSuccess(
            """{
                "productIsin":null,"productWkn":null,"issuerId":null,
                "underlyingId":null,"directionLabel":null,"basePrice":null,
                "knockoutBarrier":null,"ratio":null,"underlyingCurrency":null,
                "productCurrency":null,"sourceTimestampEpochMillis":null
            }""".trimIndent()
        )

        assertAllFieldsNull(record)
    }

    @Test
    fun missingFieldsRemainNull() {
        assertAllFieldsNull(parseSuccess("{}"))
    }

    @Test
    fun whitespaceAndCaseRemainUnchanged() {
        val record = parseSuccess(
            """{
                "productIsin":" de000Synthetic01 ",
                "issuerId":" Synthetic-Provider ",
                "directionLabel":"cAlL",
                "underlyingCurrency":" usd ",
                "productCurrency":"eUr"
            }""".trimIndent()
        )

        assertEquals(" de000Synthetic01 ", record.productIsin)
        assertEquals(" Synthetic-Provider ", record.issuerId)
        assertEquals("cAlL", record.directionLabel)
        assertEquals(" usd ", record.underlyingCurrency)
        assertEquals("eUr", record.productCurrency)
    }

    @Test
    fun zeroAndNegativeNumbersRemainUnchanged() {
        val record = parseSuccess(
            """{
                "basePrice":0.0,
                "knockoutBarrier":-1.0,
                "ratio":0.0,
                "sourceTimestampEpochMillis":-1
            }""".trimIndent()
        )

        assertEquals(0.0, record.basePrice)
        assertEquals(-1.0, record.knockoutBarrier)
        assertEquals(0.0, record.ratio)
        assertEquals(-1L, record.sourceTimestampEpochMillis)
    }

    @Test
    fun malformedJsonReturnsInvalidJson() {
        assertFailure(
            "{\"productIsin\":",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_JSON
        )
    }

    @Test
    fun nonObjectRootsReturnRootNotObject() {
        assertFailure(
            "[]",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.ROOT_NOT_OBJECT
        )
    }

    @Test
    fun unexpectedFieldIsRejected() {
        assertFailure(
            """{"unexpectedField":"synthetic-value"}""",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.UNEXPECTED_FIELD
        )
    }

    @Test
    fun invalidStringFieldTypesAreReportedInFieldOrder() {
        assertFailure(
            """{
                "productIsin":1,
                "issuerId":true,
                "directionLabel":[],
                "productCurrency":{}
            }""".trimIndent(),
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_PRODUCT_ISIN_TYPE,
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_ISSUER_ID_TYPE,
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_DIRECTION_LABEL_TYPE,
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_PRODUCT_CURRENCY_TYPE
        )
    }

    @Test
    fun invalidNumericFieldTypesAreReportedInFieldOrder() {
        assertFailure(
            """{"basePrice":"80.0","knockoutBarrier":true,"ratio":{}}""",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_BASE_PRICE_TYPE,
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_KNOCKOUT_BARRIER_TYPE,
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_RATIO_TYPE
        )
    }

    @Test
    fun timestampRejectsStringDecimalAndExponentRepresentations() {
        val error = HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
            .INVALID_SOURCE_TIMESTAMP_EPOCH_MILLIS_TYPE

        assertFailure("""{"sourceTimestampEpochMillis":"1700000000250"}""", error)
        assertFailure("""{"sourceTimestampEpochMillis":1700000000250.5}""", error)
        assertFailure("""{"sourceTimestampEpochMillis":1e3}""", error)
    }

    @Test
    fun unexpectedFieldPrecedesTypedFieldErrors() {
        assertFailure(
            """{"unexpectedField":0,"productIsin":1,"ratio":"0.1"}""",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.UNEXPECTED_FIELD,
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_PRODUCT_ISIN_TYPE,
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_RATIO_TYPE
        )
    }

    private fun parseSuccess(json: String): HsbcKnockoutProductSpecificationRecord =
        (HsbcKnockoutProductSpecificationResearchJsonParser.parse(json) as
            HsbcKnockoutProductSpecificationResearchJsonParsingResult.Success).record

    private fun assertFailure(
        json: String,
        vararg expectedErrors: HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
    ) {
        val result = HsbcKnockoutProductSpecificationResearchJsonParser.parse(json) as
            HsbcKnockoutProductSpecificationResearchJsonParsingResult.Failure

        assertEquals(expectedErrors.toList(), result.errors)
    }

    private fun assertAllFieldsNull(record: HsbcKnockoutProductSpecificationRecord) {
        assertNull(record.productIsin)
        assertNull(record.productWkn)
        assertNull(record.issuerId)
        assertNull(record.underlyingId)
        assertNull(record.directionLabel)
        assertNull(record.basePrice)
        assertNull(record.knockoutBarrier)
        assertNull(record.ratio)
        assertNull(record.underlyingCurrency)
        assertNull(record.productCurrency)
        assertNull(record.sourceTimestampEpochMillis)
    }
}
