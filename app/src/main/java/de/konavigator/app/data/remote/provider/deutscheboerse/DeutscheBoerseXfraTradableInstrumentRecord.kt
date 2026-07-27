package de.konavigator.app.data.remote.provider.deutscheboerse

/**
 * UnverÃ¤nderte Rohdaten eines Datensatzes aus einer Deutsche-BÃ¶rse-XFRA-Referenzdatei.
 *
 * Die Werte sind noch nicht fachlich ausgewertet, validiert oder normalisiert.
 */
data class DeutscheBoerseXfraTradableInstrumentRecord(
    val productStatus: String?,
    val instrumentStatus: String?,
    val instrumentName: String?,
    val isin: String?,
    val wkn: String?,
    val micCode: String?,
    val instrumentType: String?,
    val settlementCurrency: String?,
    val currency: String?,
    val warrantType: String?,
    val quotingPeriodStart: String?,
    val quotingPeriodEnd: String?
)
