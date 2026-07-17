package de.konavigator.app.data

import de.konavigator.app.models.UnderlyingAsset
import de.konavigator.app.R

object UnderlyingTestData {

    val assets = listOf(

    UnderlyingAsset(
    id = "nvidia",
    displayName = "NVIDIA",
    name = "NVIDIA Corporation",
    ticker = "NVDA",
    wkn = "918422",
    isin = "US67066G1040",
    referenceExchange = "NASDAQ",
    currency = "USD",
    currentPrice = 100.0,
    priceTimestamp = null,
    logoResId = R.drawable.logo_nvidia
    ),
        UnderlyingAsset(
            id = "amazon",
    displayName = "Amazon",
            name = "Amazon.com Inc.",
            ticker = "AMZN",
            wkn = "906866",
            isin = "US0231351067",
            referenceExchange = "NASDAQ",
            currency = "USD",
            currentPrice = 200.0,
            priceTimestamp = null,
    logoResId = null
        ),
        UnderlyingAsset(
            id = "sap",
            displayName = "SAP",
            name = "SAP SE",
            ticker = "SAP",
            wkn = "716460",
            isin = "DE0007164600",
            referenceExchange = "XETRA",
            currency = "EUR",
            currentPrice = 250.0,
            priceTimestamp = null,
    logoResId = null
        ),
        UnderlyingAsset(
            id = "allianz",
            displayName = "Allianz",
            name = "Allianz SE",
            ticker = "ALV",
            wkn = "840400",
            isin = "DE0008404005",
            referenceExchange = "XETRA",
            currency = "EUR",
            currentPrice = 300.0,
            priceTimestamp = null,
    logoResId = null
        )
    )

}