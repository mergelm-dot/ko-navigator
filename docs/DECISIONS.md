# KO Navigator – Architecture Decision Records

Dieses Dokument ist das verbindliche Register wichtiger Architekturentscheidungen. Es ergänzt `ROADMAP.md`, `docs/ARCHITECTURE.md`, `docs/FORMULAS.md`, `DEVLOG.md` und `AGENTS.md`.

`Accepted` bedeutet, dass die Architekturrichtung abgestimmt ist. Es bedeutet weder, dass sie bereits implementiert ist, noch dass noch offene Schwellenwerte, Gewichtungen oder Providerentscheidungen vorweggenommen wurden. Änderungen an einer akzeptierten Entscheidung erfolgen nicht stillschweigend, sondern nur nach erneuter Abstimmung durch einen neuen ADR oder den Status `Superseded`.

## Statusübersicht

| ADR | Titel | Status | Implementierung |
|---|---|---|---|
| ADR-0001 | CurrencyPolicy als verbindliche Währungsgrenze | Accepted | Nicht begonnen |
| ADR-0002 | Quote Freshness mit Warn- und Blockierstufen | Accepted | Teilbasis vorhanden; Erweiterung offen |
| ADR-0003 | Kontextabhängige Bid-/Ask-Policy | Accepted | Teilbasis vorhanden; Policy offen |
| ADR-0004 | Realistische Robustheits-Integrationstests | Accepted | Nicht begonnen |
| ADR-0005 | Mehrdimensionaler Zertifikats-Qualitätsscore | Accepted | Nicht begonnen |
| ADR-0006 | Explainable Engine für Produktauswahl und Berechnung | Accepted | Nicht begonnen |
| ADR-0007 | Confidence Score für die Berechnungszuverlässigkeit | Accepted | Nicht begonnen |
| ADR-0008 | Typisierter FX-/Ratio-Produktwertvertrag | Accepted | Aktiver Engine-Pfad mit theoretischem Hebel |

## ADR-0001 – CurrencyPolicy als verbindliche Währungsgrenze

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Basiswert, Produktquote und Rechenergebnis können auf unterschiedlichen Währungen lauten. Eine unbeschriftete FX-Zahl oder eine stille EUR-Annahme kann Richtung, Einheit und Aktualität einer Umrechnung verschleiern und dadurch mathematisch falsche Ergebnisse erzeugen.

### Entscheidung

Die Zielarchitektur erhält eine `CurrencyPolicy` als fachliche Freigabegrenze für alle währungsübergreifenden Berechnungen. Langfristig trennt sie einen reinen `CurrencyConverter` von einem austauschbaren `FXRateProvider`. Basiswert-Referenzwährung, Produktwährung, FX-Paar, Konvertierungsrichtung, Quelle und Zeitstempel werden ausdrücklich mitgeführt. Ohne kompatiblen und ausreichend aktuellen FX-Kurs wird keine währungsübergreifende Berechnung freigegeben.

Die bestehende Konvention aus `docs/FORMULAS.md` bleibt maßgeblich. Provider, Aktualitätsgrenzen, Fallbacks, Quanto-Behandlung und ein gemeinsamer Currency-Typ werden vor ihrer Implementierung gesondert abgestimmt.

### Begründung

Eine zentrale Policy verhindert verstreute Umrechnungsannahmen, macht Einheiten prüfbar und hält Providerzugriff von reiner Mathematik getrennt.

### Auswirkungen auf Architektur

- Domainmodelle müssen Basiswert- und Produktwährung eindeutig unterscheiden.
- FX-Beschaffung liegt hinter einem Port; reine Konvertierung bleibt deterministisch und providerfrei.
- Orchestrierung prüft Currency-Kompatibilität und FX-Freshness vor Calculator-Aufrufen.
- Fehler und verwendeter FX-Kurs werden Bestandteil strukturierter Ergebnisse und Erklärungen.

### Betroffene Dokumente

- `ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `AGENTS.md`

## ADR-0002 – Quote Freshness mit Warn- und Blockierstufen

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Ein numerisch gültiger Kurs kann für eine Planung fachlich unbrauchbar sein, wenn er zu alt ist oder Bid und Ask zeitlich nicht zusammenpassen. Die vorhandene isolierte `MarketDataFreshnessPolicy` unterscheidet bereits frische von blockierten Daten, bildet aber noch keine abgestuften Datenqualitätswarnungen für den späteren Produktfluss ab.

### Entscheidung

Jede Quote-Seite behält ihren eigenen Zeitstempel. Eine konfigurierbare Freshness-Policy bewertet das Alter relativ zu einem expliziten Bewertungszeitpunkt, ordnet Ergebnisse nachvollziehbaren Warnstufen zu und blockiert Berechnungen oberhalb einer maximal zulässigen Altersdifferenz. Für zweiseitige Berechnungen wird zusätzlich die Bid-/Ask-Zeitdifferenz geprüft.

Konkrete Schwellen, Bezeichnungen der Warnstufen, Handelszeitkalender und providerbezogene Abweichungen bleiben vor Implementierung der Erweiterung offen. Die bestehende blockierende Freshness-Prüfung wird nicht abgeschwächt.

### Begründung

Zeitliche Datenqualität ist eine fachliche Eingangsbedingung und darf weder der UI noch einem Provideradapter allein überlassen werden. Abgestufte Resultate ermöglichen transparente Warnungen, ohne blockierende Fälle als belastbar auszugeben.

### Auswirkungen auf Architektur

- Freshness bleibt eine eigene Domain-Policy ohne Systemzeitzugriff.
- Orchestrierung führt Freshness vor Berechnung, Scoring und Produktauswahl aus.
- Ergebnisse führen Quote-Zeitstempel, Bewertungszeitpunkt, Grenzwert und Stufe mit.
- Blockierte Quotes dürfen nicht durch Mid, Last, Cache oder einen stillen Default ersetzt werden.

### Betroffene Dokumente

- `ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `AGENTS.md`

## ADR-0003 – Kontextabhängige Bid-/Ask-Policy

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Mid, Bid und Ask haben unterschiedliche fachliche Bedeutungen. Ein einziger allgemeiner „Zertifikatspreis“ kann einen nicht handelbaren Referenzwert mit einem Kauf- oder Verkaufskurs vermischen und so Hebel, Szenarien und P&L verfälschen.

### Entscheidung

Die Zielarchitektur erhält eine explizite Bid-/Ask-Policy. Sie wählt eine Quote-Seite anhand eines typisierten Berechnungs- oder Orderkontexts aus und gibt zusätzlich die verwendete Preisart zurück. Ask bleibt die relevante Seite für eine Kaufpreisbetrachtung, Bid für eine Verkaufspreisbetrachtung und Mid ein nicht handelbarer analytischer Referenzwert. Fehlende erforderliche Seiten blockieren den jeweiligen Ablauf.

Die genaue Abbildung technischer Broker-Ordertypen, Trigger- und Limitpreise sowie Szenariofälle wird vor Implementierung der erweiterten Policy gesondert abgestimmt. Aus der Preisart wird keine Handlungsempfehlung abgeleitet.

### Begründung

Eine zentrale Auswahlregel verhindert widersprüchliche Preiswahl in UI, Engine und Produktsuche und macht jedes Ergebnis fachlich erklärbar.

### Auswirkungen auf Architektur

- Preisart und Verwendungskontext werden typisiert statt als Boolean oder Freitext geführt.
- Calculatoren erhalten bereits freigegebene Preise und entscheiden nicht selbst über den Orderkontext.
- Ergebnisse und Erklärungen nennen verwendete Seite und Quote-Zeitstempel.
- Mid wird nicht als ausführbarer Kurs oder Ersatz für fehlenden Bid beziehungsweise Ask verwendet.

### Betroffene Dokumente

- `ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `AGENTS.md`

## ADR-0004 – Realistische Robustheits-Integrationstests

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Unit-Tests einzelner Formeln beweisen nicht, dass Repository, Validierung, Policies, Orchestrierung und Ergebnisabbildung bei realistischen fehlerhaften Produkt- und Marktdaten gemeinsam korrekt reagieren.

### Entscheidung

Vor produktiver Nutzung externer Zertifikatsdaten wird eine Integrationstest-Suite für mindestens folgende Fälle verbindlich: fehlender Bid, fehlender Ask, inkompatible Währung, großer Spread, veraltete Daten, Handelsaussetzung, ungültige Produktdaten und bereits ausgelöster Knock-out. Jeder Test prüft den vollständigen fachlichen Pfad, den strukturierten Fehler beziehungsweise die Warnstufe und den Ausschluss scheinbar gültiger Ersatzresultate.

Konkrete Spread- und Freshness-Grenzen sowie die technische Abbildung einer Handelsaussetzung werden gemeinsam mit den jeweiligen Fachpolicies festgelegt.

### Begründung

Die größten Risiken entstehen an Komponentengrenzen und bei unvollständigen realen Daten. Die Suite schützt die mathematische Engine und die Nutzerkommunikation vor stillen Fallbacks.

### Auswirkungen auf Architektur

- Ports und Policies müssen deterministisch testbar und mit Fixtures konfigurierbar bleiben.
- Fehler-, Warn- und Blockierresultate müssen durch alle Schichten erhalten bleiben.
- Datenprovider werden in diesen Tests durch kontrollierte Adapter oder Fixtures ersetzt.
- Kein Fehlerfall darf ungeprüft zu `0`, Mid oder einer anderen Ersatzquote werden.

### Betroffene Dokumente

- `ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `AGENTS.md`

## ADR-0005 – Mehrdimensionaler Zertifikats-Qualitätsscore

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Ein einzelner Kennwert wie Hebel oder Spread beschreibt die Qualität eines KO-Zertifikats nicht ausreichend. Gleichzeitig darf eine verdichtete Kennzahl fehlende oder blockierende Daten nicht verdecken und nicht als Kauf- oder Verkaufsempfehlung erscheinen.

### Entscheidung

Langfristig wird ein Zertifikats-Qualitätsscore aus getrennt ausgewiesenen Teilfaktoren aufgebaut: Spread, Emittent, Datenqualität, Aktualität, Abstand zum Knock-out und Liquidität. Finanzierungskosten werden erst nach einer eigenen fachlichen Definition ergänzt. Ausschlussregeln und blockierende Datenprüfungen laufen vor dem Score; fehlende Pflichtfaktoren werden nicht still mit neutralen Werten ersetzt.

Skala, Normalisierung, Gewichtung, Emittenten- und Liquiditätsdaten, Mindestdaten sowie Qualitätsklassen bleiben vor Implementierung gesondert abzustimmen.

### Begründung

Ein mehrdimensionaler, zerlegbarer Score unterstützt nachvollziehbare Vergleiche besser als eine eindimensionale Sortierung, ohne die zugrunde liegenden Kriterien zu verbergen.

### Auswirkungen auf Architektur

- Scoring wird eine eigene reine Domainkomponente nach Validierung und Datenfreigabe.
- Teilwerte, Gewichte, Datenstand und ausgeschlossene Faktoren werden Bestandteil des Resultats.
- Harte Ausschlüsse bleiben von weichen Qualitätsbeiträgen getrennt.
- Die UI darf den Score nur zusammen mit Erklärung, Datenalter und Einschränkungen darstellen.

### Betroffene Dokumente

- `ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `AGENTS.md`

## ADR-0006 – Explainable Engine für Produktauswahl und Berechnung

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Eine Produktauswahl oder Rangfolge ist fachlich nicht überprüfbar, wenn nur das Endergebnis sichtbar ist. Nutzer müssen erkennen können, welche Daten, Regeln und Einschränkungen zu einem Resultat geführt haben.

### Entscheidung

Jede spätere Produktauswahl liefert zusätzlich ein strukturiertes Erklärungsmodell. Es enthält mindestens Hebel, Spread, KO-Abstand, Emittent, Datenalter und Qualitätsbewertung sowie verwendete Preisart, Währungskontext, Warnungen, Ausschlussgründe und fehlende Faktoren, soweit sie den Ablauf betreffen. Erklärungen werden aus denselben typisierten Zwischenergebnissen wie die Berechnung erzeugt und nicht aus UI-Freitext rekonstruiert.

Darstellungsform, Lokalisierung und Umfang einzelner Detailstufen bleiben Presentation-Entscheidungen. Die Erklärung beschreibt Kriterien und Datenbasis, aber keine Kauf- oder Verkaufsempfehlung.

### Begründung

Strukturierte Erklärungen machen Berechnungen auditierbar, verhindern widersprüchliche UI-Begründungen und ermöglichen Tests der fachlichen Herleitung.

### Auswirkungen auf Architektur

- Domain- und Applicationresultate führen Explainability-Daten neben numerischen Ergebnissen.
- Jeder Scorebeitrag referenziert Eingabewert, Regel, Wirkung und Datenstand.
- Presentation formatiert die Erklärung, verändert aber nicht ihre fachliche Aussage.
- Erklärungs- und Rechenergebnis müssen in Integrationstests konsistent geprüft werden.

### Betroffene Dokumente

- `ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `AGENTS.md`

## ADR-0007 – Confidence Score für die Berechnungszuverlässigkeit

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Mathematisch korrekt ausgeführte Berechnungen können je nach Vollständigkeit, Aktualität, Konsistenz und Herkunft der Eingangsdaten unterschiedlich belastbar sein. Diese Zuverlässigkeit darf nicht mit der Qualität oder Attraktivität eines Zertifikats vermischt werden.

### Entscheidung

Die Engine liefert langfristig einen getrennten Confidence Score für die Zuverlässigkeit einer konkreten Berechnung. Er bewertet ausschließlich die verfügbare Datenbasis, insbesondere Vollständigkeit, Freshness, Currency-/FX-Kompatibilität, Quellenqualität und Konsistenz. Blockierende Policy-Verletzungen erzeugen kein scheinbar belastbares Rechenergebnis mit niedrigem Score, sondern bleiben blockierend.

Skala, Aggregation, Gewichte, Mindestanforderungen und Klassenbezeichnungen werden vor Implementierung gesondert abgestimmt. Der Score ist weder eine statistisch kalibrierte Wahrscheinlichkeit noch eine Aussage über Kursentwicklung oder Produkterfolg, solange dies nicht ausdrücklich anders definiert und validiert wurde.

### Begründung

Die Trennung von Datenzuverlässigkeit und Produktqualität verhindert irreführende Rangfolgen und macht Einschränkungen einer Berechnung sichtbar.

### Auswirkungen auf Architektur

- Confidence wird aus strukturierten Datenqualitätsresultaten abgeleitet, nicht aus UI-Zustand.
- Confidence und Zertifikats-Qualitätsscore bleiben getrennte Typen und Felder.
- Ergebnisobjekte führen Beiträge, fehlende Daten, Datenstand und Blockierstatus mit.
- Explainability stellt die Gründe des Confidence Scores transparent dar.

### Betroffene Dokumente

- `ROADMAP.md`
- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `AGENTS.md`

## ADR-0008 – Typisierter FX-/Ratio-Produktwertvertrag

- **Status:** Accepted
- **Datum:** 2026-07-22

### Problemstellung

Der bestehende `TradeCalculationInput` führt Wechselkurs und Bezugsverhältnis
als unbeschriftete `Double`-Werte mit stillen Defaults. Der aktive Engine-Pfad
ignoriert den Wechselkurs, validiert das Bezugsverhältnis nicht und rundet
seinen Übergangswert frühzeitig. Eine direkte Migration würde Währungsrichtung,
Validierung, Formelkorrektur und bestehendes Laufzeitverhalten in einem großen
Schritt vermischen.

### Entscheidung

Als isolierte Teilbasis werden `CurrencyCode`, `CurrencyConversion` und der
reine `TheoreticalProductValueCalculator` eingeführt. `CurrencyCode`
normalisiert syntaktisch gültige dreistellige ASCII-Codes, ohne die Existenz
einer offiziellen Währung zu behaupten. `CurrencyConversion` unterscheidet
explizit zwischen identischer Währung ohne numerische Rate und Cross-Currency
mit verschiedenen Währungen.

Die Cross-Currency-Rate bedeutet verbindlich Einheiten der Basiswertwährung je
einer Einheit Produktwährung. Die Umrechnung in die Produktwährung erfolgt
durch Division. Das Bezugsverhältnis bedeutet Basiswerteinheiten je
Produktstück. Ratio und Cross-Currency-Rate müssen positiv und endlich sein.
Der neue Calculator berechnet:

```text
theoreticalValueInUnderlyingCurrency = knockoutDistanceAbsolute × ratio

theoreticalProductValue =
    theoreticalValueInUnderlyingCurrency
    / underlyingCurrencyPerProductCurrencyRate
```

Im Same-Currency-Fall bleibt der Wert numerisch unverändert. Der Calculator
rundet nicht. `TradeCalculationEngine`, ihre Input-/Resulttypen und der aktive
Trade-Planner-Pfad werden durch diese Entscheidung noch nicht migriert.

### Begründung

Der geschlossene Conversion-Vertrag verhindert unbeschriftete FX-Richtungen,
künstliche Same-Currency-Raten und ungültige Cross-Currency-Zustände. Die
isolierte reine Berechnung lässt sich vollständig testen, ohne das bestehende
Laufzeitverhalten oder seine Charakterisierungstests vorzeitig zu verändern.

### Auswirkungen auf Architektur

- Das neue Package `de.konavigator.app.domain.currency` enthält ausschließlich
  typisierte, providerfreie Currency-Verträge.
- FX-Quelle, Zeitstempel, Freshness und Providerzugriff bleiben Aufgabe der
  späteren `CurrencyPolicy` gemäß ADR-0001.
- Der neue Produktwertcalculator ist synchron, deterministisch, Android-frei,
  repositoryfrei und ungerundet.
- Die spätere Engine-Migration verwendet diesen Vertrag in einem gesondert
  freizugebenden Schritt.
- Der bestehende Engine-, Application-, Presentation-, UI- und MarketData-Pfad
  bleibt unverändert.

### Betroffene Dokumente

- `docs/ARCHITECTURE.md`
- `docs/FORMULAS.md`
- `DEVLOG.md`
- `docs/DECISIONS.md`

### Implementierungsstatus

Schritt 23D.1 führte die typisierten Currency-Verträge und den reinen
Produktwertcalculator zunächst isoliert ein. Seit Schritt 23D.2 verwendet der
aktive `TradeCalculationEngine`-Pfad `CurrencyConversion` und eine explizite
Ratio ohne Defaults. FX wird durch Division angewendet, der theoretische
Produktwert bleibt ungerundet und beide Ergebniswährungen werden mitgeführt.

Der aktuelle ViewModel-Pfad verwendet weiterhin ausdrücklich gekennzeichnete
Übergangsannahmen. FX-Provider, Quelle, Zeitstempel, Freshness, MarketData und
reale Produkthebel bleiben außerhalb dieser Aktivierung. Seit Schritt 23D.3
wird der berechnete theoretische Hebel am geplanten Einstieg aus dem
ungerundeten Produktwert und dem konsistent umgerechneten Basiswert-Exposure
rückgerechnet. Diese Statusaktualisierung ändert die Entscheidung nicht.
