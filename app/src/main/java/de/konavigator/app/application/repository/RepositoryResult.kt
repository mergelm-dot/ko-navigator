package de.konavigator.app.application.repository

/**
 * Strukturierter Application-Layer-Vertrag für Repository-Zugriffe.
 *
 * Der Vertrag trennt Erfolg, nicht gefundene Daten, technische
 * Datenzugriffsfehler und ungültige externe Daten ohne UI-Texte oder
 * Infrastrukturdetails. Spätere Implementierungen im Data-Layer bilden
 * konkrete technische Ursachen auf diese vier Zustände ab.
 */
sealed interface RepositoryResult<out T> {

    data class Success<T>(
        val value: T
    ) : RepositoryResult<T>

    data object NotFound : RepositoryResult<Nothing>

    data object DataAccessFailure : RepositoryResult<Nothing>

    data object InvalidData : RepositoryResult<Nothing>
}
