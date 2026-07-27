package de.konavigator.app.presentation.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationError
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MarketDataCalculationUiResultTest {

    @Test
    fun invalidProductSpecificationMapsToInvalidSpecification() {
        val result = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_SPECIFICATION
        )

        assertEquals(
            MarketDataCalculationUiResult.Failure(
                MarketDataCalculationUiError.INVALID_SPECIFICATION
            ),
            result
        )
    }

    @Test
    fun invalidProductMarketDataMapsToInvalidMarketData() {
        val result = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_MARKET_DATA
        )

        assertEquals(
            MarketDataCalculationUiResult.Failure(
                MarketDataCalculationUiError.INVALID_MARKET_DATA
            ),
            result
        )
    }

    @Test
    fun invalidProductErrorsMapToDistinctUiErrors() {
        val specificationResult = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_SPECIFICATION
        )
        val marketDataResult = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_MARKET_DATA
        )

        assertNotEquals(specificationResult, marketDataResult)
    }

    private fun toUiResult(
        error: MarketDataCalculationApplicationError
    ): MarketDataCalculationUiResult =
        MarketDataCalculationApplicationResult.DataUnavailable(error).toUiResult()
}
