package de.konavigator.app.application.repository

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.FxRateQuote

/** Austauschbarer Port zur Beschaffung einer ausdrücklich gerichteten FX-Quote. */
interface FxRateProvider {
    suspend fun findRate(
        underlyingCurrency: CurrencyCode,
        productCurrency: CurrencyCode
    ): FxRateProviderResult
}

sealed interface FxRateProviderResult {
    data class Success(
        val quote: FxRateQuote
    ) : FxRateProviderResult

    data object NotFound : FxRateProviderResult

    data object DataAccessFailure : FxRateProviderResult

    data object InvalidData : FxRateProviderResult
}
