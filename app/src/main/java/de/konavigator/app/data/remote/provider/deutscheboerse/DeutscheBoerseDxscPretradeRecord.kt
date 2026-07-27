package de.konavigator.app.data.remote.provider.deutscheboerse

/**
 * UnverÃ¤nderte Rohdaten eines Datensatzes aus einer Deutsche-BÃ¶rse-DXSC-Pretrade-Datei.
 *
 * Die Werte sind noch nicht fachlich ausgewertet, validiert oder normalisiert.
 */
data class DeutscheBoerseDxscPretradeRecord(
    val messageId: String?,
    val instrumentIdentificationCode: String?,
    val bestBid: Double?,
    val bestBidQuantity: Double?,
    val bestAsk: Double?,
    val bestAskQuantity: Double?,
    val updateDateAndTime: String?
)
