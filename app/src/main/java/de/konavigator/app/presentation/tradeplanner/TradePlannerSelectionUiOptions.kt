package de.konavigator.app.presentation.tradeplanner

/** Stable Broker-ID mit einem rein darstellenden Namen für die Selection-UI. */
data class TradePlannerBrokerUiOption(
    val id: String,
    val displayName: String
)

/** Stable Emittenten-ID mit einem rein darstellenden Namen für die Selection-UI. */
data class TradePlannerIssuerUiOption(
    val id: String,
    val displayName: String
)
