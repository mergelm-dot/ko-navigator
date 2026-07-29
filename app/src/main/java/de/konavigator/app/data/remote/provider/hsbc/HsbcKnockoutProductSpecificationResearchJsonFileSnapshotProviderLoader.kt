package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationSnapshotProvider
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

sealed interface HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError {

    data class FileLoading(
        val errors: List<HsbcKnockoutProductSpecificationResearchJsonFileLoadingError>
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError

    data class ProviderCreation(
        val errors: List<HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationError>
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
}

sealed interface HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult {

    data class Success(
        val provider: InMemoryKnockoutProductSpecificationSnapshotProvider
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult

    data class Failure(
        val error: HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult
}

/**
 * Verbindet ausschließlich den bestehenden lokalen Datei-Loader mit der bestehenden
 * Forschungs-Snapshot-Provider-Factory.
 *
 * Der Loader verarbeitet nur vorab bereinigte und ausdrücklich freigegebene lokale
 * Forschungsdateien und keine originale HSBC-Webseiten- oder Vaadin-Antwort. Die Eingabe-Map
 * wird vor dem ersten suspendierenden Aufruf defensiv kopiert, und der Dispatcher wird
 * unverändert an den bestehenden Datei-Loader weitergereicht.
 *
 * Datei- und Provider-Erzeugungsfehler bleiben typisiert getrennt. Fehlerlisten werden
 * vollständig und in unveränderter Reihenfolge weitergegeben. Bei Dateiladefehlern wird die
 * Provider-Factory nicht ausgeführt; bei Fehlern entsteht kein partieller Provider. Der
 * Abrufzeitpunkt stammt ausschließlich vom Aufrufer, der Anbieterzeitpunkt ausschließlich aus
 * dem jeweiligen Forschungs-JSON. Zwischen beiden Zeitfeldern findet kein stiller Ersatz statt.
 *
 * Der Baustein enthält keine eigene Datei-, JSON-, Parser-, Mapper-, Processor- oder
 * Providerlogik, keine Domainvalidierung, Data-Quality-, Freshness- oder
 * Berechnungsentscheidung und keine Netzwerk-, Repository-, Systemzeit- oder
 * Zeitumrechnungsverantwortung.
 */
object HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoader {

    suspend fun load(
        filesByProductIsin: Map<String, File>,
        retrievedAtEpochMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult {
        val filesByProductIsinSnapshot = filesByProductIsin.toMap()
        return when (
            val fileLoadingResult =
                HsbcKnockoutProductSpecificationResearchJsonFileLoader.load(
                    filesByProductIsin = filesByProductIsinSnapshot,
                    dispatcher = dispatcher
                )
        ) {
            is HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Failure ->
                HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult
                    .Failure(
                        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                            .FileLoading(errors = fileLoadingResult.errors)
                    )

            is HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Success ->
                when (
                    val providerCreationResult =
                        HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderFactory.create(
                            researchJsonByProductIsin =
                                fileLoadingResult.researchJsonByProductIsin,
                            retrievedAtEpochMillis = retrievedAtEpochMillis
                        )
                ) {
                    is HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult
                        .Failure ->
                        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult
                            .Failure(
                                HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
                                    .ProviderCreation(errors = providerCreationResult.errors)
                            )

                    is HsbcKnockoutProductSpecificationResearchJsonSnapshotProviderCreationResult
                        .Success ->
                        HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult
                            .Success(provider = providerCreationResult.provider)
                }
        }
    }
}
