package de.konavigator.app.domain.calculator

/**
 * Berechnet reine, unformatierte Marktdatenkennzahlen aus Bid und Ask.
 *
 * Der Calculator kennt kein vollständiges Marktdatenmodell. Er prüft nur die
 * gemeinsamen mathematischen Vorbedingungen und liefert erwartbare Fehler
 * strukturiert statt als Exception. Er rundet und formatiert nicht, normalisiert
 * oder korrigiert keine Werte, vertauscht Bid und Ask nicht und verwendet
 * keinen Absolutbetrag zur Korrektur eines gekreuzten Quotes.
 *
 * Aktualität, Quelle, Währung, Zeitstempel und Produktspezifikation gehören
 * nicht zu seiner Verantwortung. Der Aufrufer muss Verfügbarkeit, vollständige
 * Modellvalidierung, Kompatibilität und später die Aktualität sicherstellen.
 * Ein erfolgreiches Ergebnis ist daher keine Berechnungsfreigabe für einen
 * vollständigen Anwendungsfall.
 */
object MarketDataCalculator {

    fun calculateAbsoluteSpread(
        bid: Double,
        ask: Double
    ): MarketDataCalculationResult {
        validateInputs(bid = bid, ask = ask)?.let { return it }

        return MarketDataCalculationResult.Success(
            value = ask - bid
        )
    }

    fun calculateRelativeSpreadToAskPercent(
        bid: Double,
        ask: Double
    ): MarketDataCalculationResult {
        validateInputs(bid = bid, ask = ask)?.let { return it }

        return MarketDataCalculationResult.Success(
            value = (ask - bid) / ask * 100.0
        )
    }

    /**
     * Berechnet den nicht handelbaren Referenzwert zwischen Bid und Ask.
     *
     * `bid + (ask - bid) / 2.0` ist algebraisch gleichwertig zu
     * `(bid + ask) / 2.0`, vermeidet aber den möglichen Überlauf der
     * Zwischensumme bei sehr großen endlichen Preisen.
     */
    fun calculateMidPrice(
        bid: Double,
        ask: Double
    ): MarketDataCalculationResult {
        validateInputs(bid = bid, ask = ask)?.let { return it }

        return MarketDataCalculationResult.Success(
            value = bid + (ask - bid) / 2.0
        )
    }

    private fun validateInputs(
        bid: Double,
        ask: Double
    ): MarketDataCalculationResult.Failure? {
        if (!bid.isFinite() || bid < 0.0) {
            return MarketDataCalculationResult.Failure(
                error = MarketDataCalculationError.INVALID_BID
            )
        }

        if (!ask.isFinite() || ask <= 0.0) {
            return MarketDataCalculationResult.Failure(
                error = MarketDataCalculationError.INVALID_ASK
            )
        }

        if (bid > ask) {
            return MarketDataCalculationResult.Failure(
                error = MarketDataCalculationError.BID_ABOVE_ASK
            )
        }

        return null
    }
}
