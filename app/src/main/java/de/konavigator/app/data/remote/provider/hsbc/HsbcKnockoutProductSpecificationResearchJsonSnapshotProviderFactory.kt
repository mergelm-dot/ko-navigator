package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider

sealed interface HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError {

    data class ProcessingFailure(
        val productIsinKey: String,
        val error: HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError

    data class ProductIsinMismatch(
        val productIsinKey: String,
        val snapshotProductIsin: String?
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
}

sealed interface HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult {

    data class Success(
        val provider: InMemoryKnockoutProductSpecificationSnapshotProvider
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult

    data class Failure(
        val errors: List<HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError>
    ) : HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult
}

/**
 * Factory für bereits bereinigte und ausdrücklich freigegebene lokale
 * HSBC-Forschungsdaten.
 *
 * Die Factory verarbeitet keine originale HSBC-Webseiten- oder Vaadin-Antwort und verwendet
 * ausschließlich den bestehenden Snapshot-Processor und den bestehenden In-Memory-Snapshot-
 * Provider. Die Eingabe-Map wird defensiv kopiert. Map-Schlüssel und eingebettete Produkt-ISIN
 * müssen exakt übereinstimmen; es erfolgen keine Normalisierung, kein `trim()` und keine
 * Änderung der Schreibweise.
 *
 * Processing-Fehler bleiben vollständig typisiert erhalten. Alle Einträge werden verarbeitet
 * und Fehler in Eingabereihenfolge gesammelt; bei mindestens einem Fehler entsteht kein
 * partieller Provider. Der Abrufzeitpunkt stammt ausschließlich vom Aufrufer, der
 * Anbieterzeitpunkt ausschließlich aus dem jeweiligen Forschungs-JSON. Zwischen beiden
 * Zeitfeldern findet kein stiller Ersatz statt.
 *
 * Die Factory enthält keine Domainvalidierung, Data-Quality-, Freshness- oder
 * Berechnungsentscheidung und besitzt keine Datei-, Netzwerk- oder Repository-Verantwortung.
 */
object HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderFactory {

    fun create(
        researchJsonByProductIsin: Map<String, String>,
        retrievedAtEpochMillis: Long
    ): HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult {
        val researchJsonByProductIsinSnapshot = researchJsonByProductIsin.toMap()
        val snapshotsByProductIsin =
            linkedMapOf<String, KnockoutProductSpecificationSnapshotDto>()
        val errors =
            mutableListOf<HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError>()

        researchJsonByProductIsinSnapshot.forEach { (productIsinKey, json) ->
            when (
                val processingResult =
                    HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessor.process(
                        json = json,
                        retrievedAtEpochMillis = retrievedAtEpochMillis
                    )
            ) {
                is HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult.Failure ->
                    errors +=
                        HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                            .ProcessingFailure(
                                productIsinKey = productIsinKey,
                                error = processingResult.error
                            )

                is HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult.Success -> {
                    val snapshotProductIsin =
                        processingResult.snapshotDto.specification.productIsin
                    if (snapshotProductIsin != productIsinKey) {
                        errors +=
                            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError
                                .ProductIsinMismatch(
                                    productIsinKey = productIsinKey,
                                    snapshotProductIsin = snapshotProductIsin
                                )
                    } else {
                        snapshotsByProductIsin[productIsinKey] = processingResult.snapshotDto
                    }
                }
            }
        }

        return if (errors.isNotEmpty()) {
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult.Failure(
                errors = errors
            )
        } else {
            HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult.Success(
                provider = InMemoryKnockoutProductSpecificationSnapshotProvider(
                    snapshotsByProductIsin
                )
            )
        }
    }
}
