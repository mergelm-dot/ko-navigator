package de.konavigator.app.data

import de.konavigator.app.models.UnderlyingAsset

object UnderlyingTestData {

    val assets = listOf(
        UnderlyingAsset(
            id = "nvidia",
            name = "NVIDIA Corporation",
            ticker = "NVDA",
            wkn = "918422",
            isin = "US67066G1040",
            referenceExchange = "NASDAQ",
            currency = "USD",
            currentPrice = 100.0,
            priceTimestamp = null
        ),
        UnderlyingAsset(
            id = "amazon",
            name = "Amazon.com Inc.",
            ticker = "AMZN",
            wkn = "906866",
            isin = "US0231351067",
            referenceExchange = "NASDAQ",
            currency = "USD",
            currentPrice = 200.0,
            priceTimestamp = null
        ),
        UnderlyingAsset(
            id = "sap",
            name = "SAP SE",
            ticker = "SAP",
            wkn = "716460",
            isin = "DE0007164600",
            referenceExchange = "XETRA",
            currency = "EUR",
            currentPrice = 250.0,
            priceTimestamp = null
        ),
        UnderlyingAsset(
            id = "allianz",
            name = "Allianz SE",
            ticker = "ALV",
            wkn = "840400",
            isin = "DE0008404005",
            referenceExchange = "XETRA",
            currency = "EUR",
            currentPrice = 300.0,
            priceTimestamp = null
        )
    )
}