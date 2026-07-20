package de.konavigator.app.domain.validation

import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductMarketDataCompatibilityValidatorTest {

    @Test
    fun equalProductIsinAndCurrencyReturnNoErrors() {
        assertNoErrors(createSpecification(), createMarketData())
    }

    @Test
    fun differentUnderlyingCurrencyRemainsCompatible() {
        assertNoErrors(
            createSpecification(underlyingCurrency = "JPY", productCurrency = "EUR"),
            createMarketData(currency = "EUR")
        )
    }

    @Test
    fun differentIssuerIdAndSourceIdRemainCompatible() {
        assertNoErrors(
            createSpecification(issuerId = "issuer-a"),
            createMarketData(sourceId = "broker-b")
        )
    }

    @Test
    fun unrelatedSpecificationFieldsDoNotAffectCompatibility() {
        assertNoErrors(
            createSpecification(
                productWkn = "OTHER1",
                underlyingId = "other-underlying",
                direction = TradeDirection.SHORT,
                basePrice = 120.0,
                knockoutBarrier = 118.0,
                ratio = 0.01
            ),
            createMarketData()
        )
    }

    @Test
    fun differentProductIsinProducesMismatch() {
        assertSingleError(
            createSpecification(productIsin = "DE000SPEC001"),
            createMarketData(productIsin = "DE000MARKET1"),
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH
        )
    }

    @Test
    fun productIsinComparisonIsCaseSensitive() {
        assertSingleError(
            createSpecification(productIsin = "DE000TEST001"),
            createMarketData(productIsin = "de000test001"),
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH
        )
    }

    @Test
    fun productIsinIsNotTrimmed() {
        assertSingleError(
            createSpecification(productIsin = "DE000TEST001"),
            createMarketData(productIsin = " DE000TEST001 "),
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH
        )
    }

    @Test
    fun productWknIsNotUsedAsIsinFallback() {
        assertSingleError(
            createSpecification(productIsin = "DE000SPEC001", productWkn = "MARKET"),
            createMarketData(productIsin = "MARKET"),
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH
        )
    }

    @Test
    fun equalBlankProductIsinsProduceNoMismatch() {
        assertNoErrors(
            createSpecification(productIsin = ""),
            createMarketData(productIsin = "")
        )
    }

    @Test
    fun differentInvalidProductIsinsAreOnlyComparedRelationally() {
        assertSingleError(
            createSpecification(productIsin = ""),
            createMarketData(productIsin = " "),
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH
        )
    }

    @Test
    fun differentProductAndQuoteCurrencyProduceMismatch() {
        assertSingleError(
            createSpecification(productCurrency = "EUR"),
            createMarketData(currency = "USD"),
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH
        )
    }

    @Test
    fun currencyComparisonIsCaseSensitive() {
        assertSingleError(
            createSpecification(productCurrency = "EUR"),
            createMarketData(currency = "eur"),
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH
        )
    }

    @Test
    fun currencyIsNotTrimmed() {
        assertSingleError(
            createSpecification(productCurrency = "EUR"),
            createMarketData(currency = " EUR "),
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH
        )
    }

    @Test
    fun missingQuoteCurrencyIsNotDefaultedToEur() {
        assertSingleError(
            createSpecification(productCurrency = "EUR"),
            createMarketData(currency = ""),
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH
        )
    }

    @Test
    fun underlyingCurrencyIsNotComparedWithQuoteCurrency() {
        assertNoErrors(
            createSpecification(underlyingCurrency = "USD", productCurrency = "EUR"),
            createMarketData(currency = "EUR")
        )
    }

    @Test
    fun equalInvalidCurrenciesProduceNoMismatch() {
        assertNoErrors(
            createSpecification(productCurrency = "eur"),
            createMarketData(currency = "eur")
        )
    }

    @Test
    fun differentInvalidCurrenciesProduceOnlyCurrencyMismatch() {
        assertSingleError(
            createSpecification(productCurrency = "eur"),
            createMarketData(currency = "usd"),
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH
        )
    }

    @Test
    fun isinAndCurrencyMismatchAreCollectedCompletely() {
        assertEquals(
            ALL_ERRORS_IN_STABLE_ORDER,
            validate(
                createSpecification(productIsin = "DE000SPEC001", productCurrency = "EUR"),
                createMarketData(productIsin = "DE000MARKET1", currency = "USD")
            )
        )
    }

    @Test
    fun multipleErrorsUseDocumentedStableOrder() {
        val errors = validate(
            createSpecification(productIsin = "DE000SPEC001", productCurrency = "EUR"),
            createMarketData(productIsin = "DE000MARKET1", currency = "USD")
        )

        assertEquals(ALL_ERRORS_IN_STABLE_ORDER, errors)
    }

    @Test
    fun eachRelationshipProducesAtMostOneError() {
        val errors = validate(
            createSpecification(productIsin = "DE000SPEC001", productCurrency = "EUR"),
            createMarketData(productIsin = "DE000MARKET1", currency = "USD")
        )

        assertEquals(2, errors.size)
        assertEquals(errors.size, errors.toSet().size)
    }

    @Test
    fun validationDoesNotMutateSpecification() {
        val specification = createSpecification()
        val original = specification.copy()

        validate(specification, createMarketData())

        assertEquals(original, specification)
    }

    @Test
    fun validationDoesNotMutateMarketData() {
        val marketData = createMarketData()
        val original = marketData.copy()

        validate(createSpecification(), marketData)

        assertEquals(original, marketData)
    }

    @Test
    fun relationallyIncompatibleValuesDoNotThrowExceptions() {
        val result = runCatching {
            validate(
                createSpecification(productIsin = "DE000SPEC001", productCurrency = "EUR"),
                createMarketData(productIsin = "DE000MARKET1", currency = "USD")
            )
        }

        assertTrue(result.isSuccess)
    }

    @Test
    fun internallyInvalidButRelationallyEqualModelsProduceNoCompatibilityErrors() {
        assertNoErrors(
            createSpecification(
                productIsin = "",
                issuerId = "",
                basePrice = Double.NaN,
                productCurrency = ""
            ),
            createMarketData(
                productIsin = "",
                bid = Double.NaN,
                ask = 0.0,
                currency = "",
                sourceId = ""
            )
        )
    }

    @Test
    fun compatibilityErrorsContainNoMissingOrInvalidCodes() {
        assertTrue(
            KnockoutProductCompatibilityError.entries.none {
                it.name.startsWith("MISSING_") || it.name.startsWith("INVALID_")
            }
        )
    }

    @Test
    fun compatibilityErrorEnumContainsExactlyTwoCodes() {
        assertEquals(ALL_ERRORS_IN_STABLE_ORDER, KnockoutProductCompatibilityError.entries)
    }

    @Test
    fun compatibilityErrorsRemainMachineReadableCodesWithoutMessages() {
        assertEquals(
            listOf("PRODUCT_ISIN_MISMATCH", "PRODUCT_CURRENCY_MISMATCH"),
            KnockoutProductCompatibilityError.entries.map { it.name }
        )
    }

    @Test
    fun validatorRunsAsPlainDomainUnitTest() {
        assertNoErrors(createSpecification(), createMarketData())
    }

    @Test
    fun invalidPricesDoNotTriggerCalculatorErrorsOrCalls() {
        assertNoErrors(
            createSpecification(),
            createMarketData(bid = Double.NaN, ask = 0.0)
        )
    }

    @Test
    fun timestampsAreNotUsedForCompatibility() {
        assertNoErrors(
            createSpecification(),
            createMarketData(
                bidTimestampEpochMillis = Long.MIN_VALUE,
                askTimestampEpochMillis = Long.MAX_VALUE
            )
        )
    }

    private fun assertSingleError(
        specification: KnockoutProductSpecification,
        marketData: KnockoutProductMarketData,
        expectedError: KnockoutProductCompatibilityError
    ) {
        assertEquals(listOf(expectedError), validate(specification, marketData))
    }

    private fun assertNoErrors(
        specification: KnockoutProductSpecification,
        marketData: KnockoutProductMarketData
    ) {
        assertTrue(validate(specification, marketData).isEmpty())
    }

    private fun validate(
        specification: KnockoutProductSpecification,
        marketData: KnockoutProductMarketData
    ): List<KnockoutProductCompatibilityError> {
        return KnockoutProductMarketDataCompatibilityValidator.validate(
            specification = specification,
            marketData = marketData
        )
    }

    private fun createSpecification(
        productIsin: String = "DE000TEST001",
        productWkn: String? = "TEST01",
        issuerId: String = "issuer-1",
        underlyingId: String = "underlying-1",
        direction: TradeDirection = TradeDirection.LONG,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        ratio: Double = 0.1,
        underlyingCurrency: String = "USD",
        productCurrency: String = "EUR"
    ): KnockoutProductSpecification {
        return KnockoutProductSpecification(
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

    private fun createMarketData(
        productIsin: String = "DE000TEST001",
        bid: Double? = 1.95,
        ask: Double? = 2.0,
        bidTimestampEpochMillis: Long? = 1_700_000_000_000L,
        askTimestampEpochMillis: Long? = 1_700_000_000_100L,
        currency: String = "EUR",
        sourceId: String = "source-1"
    ): KnockoutProductMarketData {
        return KnockoutProductMarketData(
            productIsin = productIsin,
            bid = bid,
            ask = ask,
            bidTimestampEpochMillis = bidTimestampEpochMillis,
            askTimestampEpochMillis = askTimestampEpochMillis,
            currency = currency,
            sourceId = sourceId
        )
    }

    private companion object {
        val ALL_ERRORS_IN_STABLE_ORDER = listOf(
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH,
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH
        )
    }
}
