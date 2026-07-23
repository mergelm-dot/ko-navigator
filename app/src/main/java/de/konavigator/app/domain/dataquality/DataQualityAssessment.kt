package de.konavigator.app.domain.dataquality

import java.util.Collections

enum class DataQualityStatus {
    PASSED,
    WARNING,
    BLOCKED
}

/**
 * Unveränderliches Ergebnis einer ausdrücklich begrenzten Data-Quality-Prüfung.
 *
 * Die Factory-Funktionen sichern die Statusinvarianten ab und übernehmen einen
 * defensiven Snapshot der Findings. Schritt 25A erzeugt über den delegierenden
 * Validator ausschließlich [DataQualityStatus.PASSED] oder
 * [DataQualityStatus.BLOCKED].
 */
class DataQualityAssessment private constructor(
    val status: DataQualityStatus,
    findings: List<DataQualityFinding>
) {
    val findings: List<DataQualityFinding> = Collections.unmodifiableList(findings.toList())

    init {
        when (status) {
            DataQualityStatus.PASSED -> require(this.findings.isEmpty()) {
                "PASSED assessment must not contain findings"
            }

            DataQualityStatus.WARNING -> require(
                this.findings.isNotEmpty() &&
                    this.findings.all { it.severity == DataQualitySeverity.WARNING }
            ) {
                "WARNING assessment requires warning findings only"
            }

            DataQualityStatus.BLOCKED -> require(
                this.findings.any { it.severity == DataQualitySeverity.BLOCKING }
            ) {
                "BLOCKED assessment requires at least one blocking finding"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataQualityAssessment) return false
        return status == other.status && findings == other.findings
    }

    override fun hashCode(): Int = 31 * status.hashCode() + findings.hashCode()

    override fun toString(): String {
        return "DataQualityAssessment(status=$status, findings=$findings)"
    }

    companion object {
        fun passed(): DataQualityAssessment {
            return DataQualityAssessment(
                status = DataQualityStatus.PASSED,
                findings = emptyList()
            )
        }

        fun warning(findings: List<DataQualityFinding>): DataQualityAssessment {
            return DataQualityAssessment(
                status = DataQualityStatus.WARNING,
                findings = findings
            )
        }

        fun blocked(findings: List<DataQualityFinding>): DataQualityAssessment {
            return DataQualityAssessment(
                status = DataQualityStatus.BLOCKED,
                findings = findings
            )
        }
    }
}
