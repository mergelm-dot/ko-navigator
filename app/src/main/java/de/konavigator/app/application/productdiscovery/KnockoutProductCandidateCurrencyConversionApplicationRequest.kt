package de.konavigator.app.application.productdiscovery

/** Providerneutraler Auftrag zur Währungsauflösung berechneter Kandidaten. */
data class KnockoutProductCandidateCurrencyConversionApplicationRequest(
    val candidates: List<KnockoutProductCandidateWithCalculation>,
    val evaluationTimeEpochMillis: Long,
    val maxFxAgeMillis: Long
)
