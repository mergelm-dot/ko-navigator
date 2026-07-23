# Projektspezifische Regeln für KO Navigator

- Projektziel: KO Navigator ist ein professionelles Planungstool für Knock-Out-Zertifikate.
- Keine Kauf- oder Verkaufsempfehlungen.
- Höchste Priorität hat jederzeit die mathematisch korrekte Berechnungsengine.
- Kleine, nachvollziehbare Änderungen.
- Bestehende Funktionen niemals verschlechtern.
- Verbindliche Projektdokumente sind `ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/FORMULAS.md`, `DEVLOG.md`, `AGENTS.md` und `docs/DECISIONS.md`.
- `ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/FORMULAS.md`, `DEVLOG.md` und `docs/DECISIONS.md` bei fachlich oder architektonisch betroffenen Änderungen konsistent aktuell halten.
- Wichtige Architekturentscheidungen vor Implementierung mit dem Projekteigner abstimmen und als nummerierten ADR mit Status, Datum, Problemstellung, Entscheidung, Begründung, Architekturauswirkungen und betroffenen Dokumenten in `docs/DECISIONS.md` erfassen.
- Akzeptierte Architekturentscheidungen niemals eigenständig ändern; Änderungen benötigen erneute Abstimmung und einen nachvollziehbaren ADR-Statuswechsel oder Folge-ADR.
- Keine Dateien ohne Rückfrage löschen.

## Verbindlicher Codex-Arbeitsablauf

### Auftragsumfang und Repository-Analyse

- Ein Codex-Auftrag behandelt genau eine kleine, fachlich abgeschlossene Änderung. Große Funktionen sind in getrennte Arbeitspakete zu zerlegen.
- Den Aufgabenbereich nicht eigenständig erweitern und keine zusätzlichen Verbesserungen „bei Gelegenheit“ umsetzen.
- Zuerst nur die im Auftrag genannten Dateien untersuchen. Weitere Dateien nur öffnen, wenn dies technisch erforderlich ist.
- Bei kleinen Änderungen weder das vollständige Repository noch unnötig den gesamten Git-Verlauf untersuchen.
- Projektdokumentation nur lesen, soweit sie für die konkrete Änderung relevant ist.
- Bei kleinen Unklarheiten die risikoärmste und engste Auslegung wählen. Keine weitreichenden Annahmen treffen.
- Bei grundlegenden Architektur- oder Formelunklarheiten keine Implementierung erzwingen, sondern die Unklarheit im Abschlussbericht benennen.
- Keine Änderungen außerhalb des ausdrücklich erlaubten Umfangs vornehmen.

### Architektur und Qualität

- Keine Fachlogik in der UI platzieren.
- Keine großen Refactorings ohne eigenen, vorherigen Auftrag.
- Bestehende öffentliche Schnittstellen möglichst erhalten.
- Berechnungsregeln müssen mit `docs/FORMULAS.md`, Architekturänderungen mit `docs/ARCHITECTURE.md` vereinbar sein.
- Vorhandene Entscheidungen in `docs/DECISIONS.md` beachten.
- Qualität, Wartbarkeit und Testbarkeit haben Vorrang vor Geschwindigkeit.

### Gestufte Teststrategie

Tests sind nach Umfang und Risiko der Änderung auszuführen:

1. **Stufe 1 – kleine lokale Änderung:** Nur direkt betroffene Unit-Tests ausführen; keine vollständige Testsuite ohne technischen Grund.
2. **Stufe 2 – Schnittstellen- oder komponentenübergreifende Änderung:** Direkt betroffene und angrenzende relevante Tests sowie die Kompilierungsprüfung des betroffenen Moduls ausführen.
3. **Stufe 3 – wichtiger Commit, Meilenstein oder Release:** Den vollständigen relevanten Testlauf und, sofern vorgesehen und technisch möglich, einen vollständigen Build ausführen.

- Nach jeder Code- oder Build-Änderung den betroffenen Code mindestens kompilieren. Bei reinen Dokumentationsänderungen sind ohne technischen Grund weder Kompilierung noch Tests erforderlich.
- Keine unnötig großen Testläufe ausführen, aber erforderliche Tests auch nicht aus Kosten- oder Zeitgründen auslassen.

### Trennung der Arbeitsschritte

- Analyse, Implementierung, Dokumentation sowie Commit und Push möglichst getrennt behandeln.
- Commit und Push nur nach ausdrücklicher Freigabe durchführen; keine neue Branch ohne Auftrag erstellen.
- Dokumentation nur ändern, wenn sie vom Auftrag tatsächlich betroffen ist.

### Dokumentationsregeln

- `docs/FORMULAS.md` nur bei Änderungen fachlicher Berechnungsregeln aktualisieren.
- `docs/ARCHITECTURE.md` nur bei Änderungen von Komponenten, Verantwortlichkeiten oder Datenflüssen aktualisieren.
- `docs/DECISIONS.md` nur bei einer echten dauerhaften Entscheidung aktualisieren.
- `ROADMAP.md` nur bei Änderungen der Planung oder des Projektstatus aktualisieren.
- `DEVLOG.md` nur nach einem abgeschlossenen und relevanten Arbeitspaket aktualisieren.
- Kleine Bugfixes lösen nicht automatisch Änderungen in allen Projektdokumenten aus.

### Abschlussbericht

Nach jeder Umsetzung kompakt angeben:

1. welche Dateien untersucht wurden,
2. welche Dateien geändert wurden,
3. was fachlich geändert wurde,
4. welche Tests ausgeführt wurden,
5. ob die Tests erfolgreich waren,
6. welche Risiken oder offenen Punkte bestehen und
7. wie der aktuelle Git-Status ist.
