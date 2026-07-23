package de.konavigator.app.data.mapper

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductSpecificationMapperTest {

    @Test
    fun completeLongDtoMapsEveryFieldExactly() {
        val result = success(dto())

        assertEquals(
            KnockoutProductSpecification(
                productIsin = "DE000TEST001",
                productWkn = "ABC123",
                issuerId = "issuer-a",
                underlyingId = "underlying-a",
                direction = TradeDirection.LONG,
                basePrice = 80.123456789,
                knockoutBarrier = 82.987654321,
                ratio = 0.123456789,
                underlyingCurrency = "USD",
                productCurrency = "EUR"
            ),
            result
        )
    }

    @Test
    fun completeShortDtoMapsDirectionAndAllOtherFieldsExactly() {
        val input = dto(
            productIsin = "DE000TEST002",
            productWkn = "DEF456",
            issuerId = "issuer-b",
            underlyingId = "underlying-b",
            direction = "SHORT",
            basePrice = 120.25,
            knockoutBarrier = 125.75,
            ratio = 0.01,
            underlyingCurrency = "CHF",
            productCurrency = "USD"
        )

        val result = success(input)

        assertEquals("DE000TEST002", result.productIsin)
        assertEquals("DEF456", result.productWkn)
        assertEquals("issuer-b", result.issuerId)
        assertEquals("underlying-b", result.underlyingId)
        assertEquals(TradeDirection.SHORT, result.direction)
        assertEquals(120.25, result.basePrice, 0.0)
        assertEquals(125.75, result.knockoutBarrier, 0.0)
        assertEquals(0.01, result.ratio, 0.0)
        assertEquals("CHF", result.underlyingCurrency)
        assertEquals("USD", result.productCurrency)
    }

    @Test
    fun nullProductWknRemainsNull() {
        assertNull(success(dto(productWkn = null)).productWkn)
    }

    @Test
    fun everyMissingRequiredFieldProducesItsTypedError() {
        val cases = listOf(
            KnockoutProductSpecificationDtoField.PRODUCT_ISIN to
                dto(productIsin = null),
            KnockoutProductSpecificationDtoField.ISSUER_ID to
                dto(issuerId = null),
            KnockoutProductSpecificationDtoField.UNDERLYING_ID to
                dto(underlyingId = null),
            KnockoutProductSpecificationDtoField.DIRECTION to
                dto(direction = null),
            KnockoutProductSpecificationDtoField.BASE_PRICE to
                dto(basePrice = null),
            KnockoutProductSpecificationDtoField.KNOCKOUT_BARRIER to
                dto(knockoutBarrier = null),
            KnockoutProductSpecificationDtoField.RATIO to
                dto(ratio = null),
            KnockoutProductSpecificationDtoField.UNDERLYING_CURRENCY to
                dto(underlyingCurrency = null),
            KnockoutProductSpecificationDtoField.PRODUCT_CURRENCY to
                dto(productCurrency = null)
        )

        cases.forEach { (field, input) ->
            assertEquals(
                listOf(missing(field)),
                failure(input).errors
            )
        }
    }

    @Test
    fun multipleMissingRequiredFieldsRemainCompleteAndOrdered() {
        val result = failure(
            dto(
                productIsin = null,
                direction = null,
                ratio = null,
                productCurrency = null
            )
        )

        assertEquals(
            listOf(
                missing(KnockoutProductSpecificationDtoField.PRODUCT_ISIN),
                missing(KnockoutProductSpecificationDtoField.DIRECTION),
                missing(KnockoutProductSpecificationDtoField.RATIO),
                missing(KnockoutProductSpecificationDtoField.PRODUCT_CURRENCY)
            ),
            result.errors
        )
    }

    @Test
    fun unknownDirectionProducesTypedDirectionError() {
        assertEquals(
            listOf(unsupportedDirection()),
            failure(dto(direction = "SIDEWAYS")).errors
        )
    }

    @Test
    fun directionCaseAndWhitespaceVariantsAreNotAccepted() {
        listOf("long", "Long", " LONG", "LONG ", " SHORT ").forEach { direction ->
            assertEquals(
                listOf(unsupportedDirection()),
                failure(dto(direction = direction)).errors
            )
        }
    }

    @Test
    fun emptyStringsAreForwardedWithoutCorrection() {
        val result = success(
            dto(
                productIsin = "",
                productWkn = "",
                issuerId = "",
                underlyingId = "",
                underlyingCurrency = "",
                productCurrency = ""
            )
        )

        assertEquals("", result.productIsin)
        assertEquals("", result.productWkn)
        assertEquals("", result.issuerId)
        assertEquals("", result.underlyingId)
        assertEquals("", result.underlyingCurrency)
        assertEquals("", result.productCurrency)
    }

    @Test
    fun currencyCodesAreNotNormalized() {
        val result = success(
            dto(
                underlyingCurrency = " usd ",
                productCurrency = "eur"
            )
        )

        assertEquals(" usd ", result.underlyingCurrency)
        assertEquals("eur", result.productCurrency)
    }

    @Test
    fun negativeNanAndInfinityValuesAreNotCorrectedOrRejected() {
        val result = success(
            dto(
                basePrice = -1.0,
                knockoutBarrier = Double.NaN,
                ratio = Double.POSITIVE_INFINITY
            )
        )

        assertEquals(-1.0, result.basePrice, 0.0)
        assertTrue(result.knockoutBarrier.isNaN())
        assertEquals(Double.POSITIVE_INFINITY, result.ratio, 0.0)
    }

    @Test
    fun equalInputsProduceEqualResults() {
        val input = dto()

        assertEquals(
            KnockoutProductSpecificationMapper.map(input),
            KnockoutProductSpecificationMapper.map(input)
        )
    }

    @Test
    fun mappingDoesNotChangeDto() {
        val input = dto(
            productIsin = " de000test001 ",
            productWkn = null,
            issuerId = "",
            underlyingCurrency = " usd ",
            productCurrency = "eur"
        )
        val snapshot = input.copy()

        KnockoutProductSpecificationMapper.map(input)

        assertEquals(snapshot, input)
    }

    private fun success(
        input: KnockoutProductSpecificationDto
    ): KnockoutProductSpecification {
        val result = KnockoutProductSpecificationMapper.map(input)

        assertTrue(result is KnockoutProductSpecificationMappingResult.Success)
        return (result as KnockoutProductSpecificationMappingResult.Success).specification
    }

    private fun failure(
        input: KnockoutProductSpecificationDto
    ): KnockoutProductSpecificationMappingResult.Failure {
        val result = KnockoutProductSpecificationMapper.map(input)

        assertTrue(result is KnockoutProductSpecificationMappingResult.Failure)
        return result as KnockoutProductSpecificationMappingResult.Failure
    }

    private fun missing(
        field: KnockoutProductSpecificationDtoField
    ) = KnockoutProductSpecificationMappingError(
        field = field,
        reason = KnockoutProductSpecificationMappingErrorReason.MISSING_REQUIRED_VALUE
    )

    private fun unsupportedDirection() = KnockoutProductSpecificationMappingError(
        field = KnockoutProductSpecificationDtoField.DIRECTION,
        reason = KnockoutProductSpecificationMappingErrorReason.UNSUPPORTED_VALUE
    )

    private fun dto(
        productIsin: String? = "DE000TEST001",
        productWkn: String? = "ABC123",
        issuerId: String? = "issuer-a",
        underlyingId: String? = "underlying-a",
        direction: String? = "LONG",
        basePrice: Double? = 80.123456789,
        knockoutBarrier: Double? = 82.987654321,
        ratio: Double? = 0.123456789,
        underlyingCurrency: String? = "USD",
        productCurrency: String? = "EUR"
    ) = KnockoutProductSpecificationDto(
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
    )
}
