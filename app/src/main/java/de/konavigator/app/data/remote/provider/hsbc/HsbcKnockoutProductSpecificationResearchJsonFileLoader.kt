package de.konavigator.app.data.remote.provider.hsbc

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode {
    FILE_READING_FAILED
}

data class HsbcKnockoutProductSpecificationResearchJsonFileLoadingError(
    val productIsinKey: String,
    val code: HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode
)

sealed interface HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult {

    data class Success(
        val researchJsonByProductIsin: Map<String, String>
    ) : HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult

    data class Failure(
        val errors: List<HsbcKnockoutProductSpecificationResearchJsonFileLoadingError>
    ) : HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult
}

/**
 * Loader für lokale, bereits bereinigte und ausdrücklich freigegebene
 * HSBC-Forschungs-JSON-Dateien.
 *
 * Der Loader verarbeitet keine originale HSBC-Webseiten- oder Vaadin-Antwort. Dateien müssen
 * vorab kontrolliert und frei von Sitzungs- oder Sicherheitsdaten sein. Die Eingabe-Map wird vor
 * dem suspendierenden Dateizugriff defensiv kopiert; alle Dateizugriffe erfolgen ausschließlich
 * auf dem injizierten Dispatcher. Dateien werden vollständig als UTF-8 gelesen, und Schlüssel
 * sowie Inhalte werden unverändert übernommen.
 *
 * Lesefehler werden typisiert und in Eingabereihenfolge gesammelt. Bei mindestens einem Fehler
 * entsteht kein partielles Erfolgsergebnis. Der Loader besitzt keine JSON-, Parser-, Mapper-,
 * Snapshot-, Provider- oder Repository-Verantwortung und trifft keine Domainvalidierung,
 * Data-Quality-, Freshness- oder Berechnungsentscheidung. Er verwendet keine Netzwerkverbindung,
 * Systemzeit oder Zeitumrechnung.
 */
object HsbcKnockoutProductSpecificationResearchJsonFileLoader {

    suspend fun load(
        filesByProductIsin: Map<String, File>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult {
        val filesByProductIsinSnapshot = filesByProductIsin.toMap()
        return withContext(dispatcher) {
            val loadedJsonByProductIsin = linkedMapOf<String, String>()
            val errors =
                mutableListOf<HsbcKnockoutProductSpecificationResearchJsonFileLoadingError>()

            filesByProductIsinSnapshot.forEach { (productIsinKey, file) ->
                try {
                    loadedJsonByProductIsin[productIsinKey] = file.readText(Charsets.UTF_8)
                } catch (_: IOException) {
                    errors += loadingError(productIsinKey)
                } catch (_: SecurityException) {
                    errors += loadingError(productIsinKey)
                }
            }

            if (errors.isNotEmpty()) {
                HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Failure(errors)
            } else {
                HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Success(
                    researchJsonByProductIsin = loadedJsonByProductIsin.toMap()
                )
            }
        }
    }

    private fun loadingError(
        productIsinKey: String
    ) = HsbcKnockoutProductSpecificationResearchJsonFileLoadingError(
        productIsinKey = productIsinKey,
        code = HsbcKnockoutProductSpecificationResearchJsonFileLoadingErrorCode
            .FILE_READING_FAILED
    )
}
