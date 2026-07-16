package de.konavigator.app.calculator

object PriceConverter {
    fun calculateCertificatePrice(
        underlyingPrice: Double,
        knockoutPrice: Double,
        ratio: Double
    ): Double {

        return (underlyingPrice - knockoutPrice) * ratio
    }

}