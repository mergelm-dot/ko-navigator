package de.konavigator.app.calculator

import kotlin.math.round

object KoCalculator {

    /**
     * Berechnet den theoretischen Preis eines Long-KO-Zertifikats.
     */
    fun calculateCertificatePrice(
        underlyingPrice: Double,
        knockoutPrice: Double,
        ratio: Double,
        isLong: Boolean
    ): Double {

        val price =
            if (isLong) {

                if (underlyingPrice <= knockoutPrice) {
                    return 0.0
                }

                (underlyingPrice - knockoutPrice) * ratio

            } else {

                if (underlyingPrice >= knockoutPrice) {
                    return 0.0
                }

                (knockoutPrice - underlyingPrice) * ratio
            }

        return round(price * 100) / 100
    }
}