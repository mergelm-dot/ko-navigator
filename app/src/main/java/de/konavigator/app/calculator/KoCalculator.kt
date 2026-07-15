package de.konavigator.app.calculator

import kotlin.math.round

object KoCalculator {

    /**
     * Berechnet den theoretischen Preis eines Long-KO-Zertifikats.
     */
    fun calculateCertificatePrice(
        underlyingPrice: Double,
        knockoutPrice: Double,
        ratio: Double
    ): Double {

        if (underlyingPrice <= knockoutPrice) {
            return 0.0
        }

        val price = (underlyingPrice - knockoutPrice) * ratio

        return round(price * 100) / 100
    }
}