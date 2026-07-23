# KO Navigator Roadmap

## Aktueller Entwicklungsstand

Der aktive Release-Einstieg führt über `MainActivity`, `TradePlannerRoute` und
den state-gesteuerten `TradePlannerScreen` in die theoretische Trade-Planung.
Die Berechnungsengine ist für diesen abgegrenzten Modellpfad weit fortgeschritten
und durch 25 feste Szenarien abgesichert. Reale Zertifikats- und Kursprovider
sind noch nicht angebunden. Der vorhandene KO-Produktpfad arbeitet ausschließlich
mit Ports, In-Memory-Adaptern und einer Debug-Demo.

## Phase 1 – Fundament

- [x] Android-/Compose-Projektstruktur
- [x] Lokale Basiswert-Testdaten
- [x] `UnderlyingRepository` und lokale Suchmaschine
- [x] `UnderlyingSearchField`
- [x] Basiswertsuche in den aktiven `TradePlannerScreen` eingebunden
- [x] Aktiver Trade-Planner-Pfad über Composition, ViewModel, Route und Screen
- [x] Produktstammdaten und veränderliche Produktmarktdaten als getrennte Modelle
- [x] Repository-Ports und read-only In-Memory-Adapter für KO-Produktdaten
- [ ] Externe DTOs und Mapper
- [ ] Reale Produkt-, Marktpreis- und FX-Provider
- [ ] Automatischer Kursabruf

## Phase 2 – Theoretische Berechnungsengine

Für den derzeitigen theoretischen Planungsvertrag umgesetzt:

- [x] Long- und Short-Richtung typisiert bis zur UI
- [x] Theoretische KO-Barriere aus Einstieg, Zielhebel und Richtung
- [x] Absoluter und prozentualer KO-Abstand
- [x] Explizites Bezugsverhältnis
- [x] Typisierter Same-/Cross-Currency-Vertrag mit eindeutiger FX-Richtung
- [x] Ungerundeter theoretischer Produktwert
- [x] Berechneter theoretischer Hebel am geplanten Einstieg
- [x] Strukturierte Engine- und Presentation-Fehler
- [x] 25 feste Referenz-, Grenz- und Fehlerszenarien

Für einen realen Produktpfad weiterhin offen:

- [ ] Realen Produktpreis aus freigegebenen Produkt- und Marktdaten bestimmen
- [ ] Bid-/Ask-basierten Hebel berechnen
- [ ] Tatsächlichen Produkthebel fachlich vom theoretischen Hebel trennen
- [ ] KO-Status eines bestehenden Produkts bewerten
- [ ] Finanzierungskosten fachlich definieren
- [ ] Premium beziehungsweise Aufgeld fachlich definieren
- [ ] Vorhandene reine Spread-Berechnung in einen realen Produktablauf einordnen
- [ ] Tick Size und produktbezogene Rundung fachlich definieren
- [ ] Ziel-Einstiegspreis für ein konkretes Produkt
- [ ] Gewinn-/Verlustberechnung

## Phase 3 – Zertifikatssuche

- [ ] Reale Zertifikate laden
- [ ] Fachlich freigegebene Filter anwenden
- [ ] Produkte transparent bewerten
- [ ] Ergebnisliste

## Phase 4 – UI-Verfeinerung

### Asset-Info im TradingView-Stil

Status: Geplant

Darstellung nach Auswahl eines Basiswertes:

🟩 NVIDIA

NVDA • NASDAQ • USD

Ziele:

- kleines Firmenlogo links
- Firmenname groß
- Ticker, Börse und Währung kompakt darstellen
- professioneller TradingView-/Broker-Look
- Umsetzung erst nach Stabilisierung des realen Produkt- und Datenpfads

## Phase 5 – Verlässliche Markt- und Währungsdaten

Status: Teilbasis vorhanden, reale Anbindung offen

Priorität: **P0 – Voraussetzung für belastbare reale Produktberechnungen**

- [x] Basiswert- und Produktwährung im theoretischen Engine-Vertrag typisiert
- [x] Reiner, providerfreier Same-/Cross-Currency-Rechenvertrag
- [x] Marktdaten mit getrenntem Bid/Ask, Quelle und Zeitstempeln modelliert
- [x] Explizite blockierende Freshness- und Source-Policies als Teilbasis
- [ ] `CurrencyPolicy` als Freigabegrenze für reale FX-Daten vervollständigen
- [ ] Austauschbaren `FXRateProvider` anbinden
- [ ] FX-Paar, Richtung, Quelle und Zeitstempel realer Umrechnungen nachweisen
- [ ] Fachlich freigegebene Freshness-Warnstufen ergänzen
- [ ] Kontextabhängige Bid-/Ask-Policy für reale Produktabläufe ergänzen
- [ ] Konkrete Schwellenwerte und Ordertyp-Zuordnungen gesondert freigeben

Architekturgrundlage: [ADR-0001](docs/DECISIONS.md#adr-0001--currencypolicy-als-verbindliche-währungsgrenze), [ADR-0002](docs/DECISIONS.md#adr-0002--quote-freshness-mit-warn--und-blockierstufen), [ADR-0003](docs/DECISIONS.md#adr-0003--kontextabhängige-bid-ask-policy) und [ADR-0008](docs/DECISIONS.md#adr-0008--typisierter-fx-ratio-produktwertvertrag).

## Phase 6 – Datenqualität und Robustheit

Status: Struktureller V1-Vertrag im Marktdatenorchestrator aktiv;
Application-/Presentation-Weitergabe offen

Priorität: **P0 – nächster fachlich-technischer Schwerpunkt**

Vorhandene Teilbasis:

- [x] Strukturelle Produktspezifikationsvalidierung
- [x] Strukturelle Marktdatenvalidierung
- [x] Produkt-/Marktdaten-Kompatibilitätsprüfung
- [x] Anwendungsbezogene Quote-Verfügbarkeit
- [x] Blockierende Freshness-Prüfung mit explizitem Bewertungszeitpunkt
- [x] Fail-closed Source-Policy
- [x] Siebenstufige Fail-Fast-Marktdatenorchestrierung
- [x] Lokaler Application-Integrationspfad über In-Memory-Repositories

Nächste Schritte:

- [x] Einheitlichen, strukturellen Data-Quality-Vertrag mit Status, Findings und
  prüfbaren Evidenzen einführen
- [x] Bestehende Regeln über einen delegierenden Validator koordinieren, ohne sie
  zu verschieben oder zu duplizieren
- [x] Blocking Findings für die bereits vorhandenen strukturellen Regeln abbilden
- [x] `WARNING` nur als Vertragsoption vorbereiten; Schwellen und Regeln separat
  fachlich freigeben
- [x] Strukturelles Assessment als erste, fail-closed Freigabestufe in den
  bestehenden Orchestrator integrieren
- [ ] Integrationstests für fehlende Quotes, inkompatible Währungen, alte Quotes,
  Quellenfehler, Handelsaussetzung und ausgelösten Knock-out erweitern
- [ ] Warnungen, Blockierungen und Teilresultate später bis zur UI nachweisen

Der nächste technische Schritt ist:

**Orchestrator-`DataQualityAssessment` kontrolliert durch Application und
Presentation bis zur neutralen UI-Anzeige weiterreichen.**

Architekturgrundlage: [ADR-0004](docs/DECISIONS.md#adr-0004--realistische-robustheits-integrationstests) und [ADR-0009](docs/DECISIONS.md#adr-0009--einheitlicher-data-quality-vertrag-über-bestehenden-validatoren-und-policies).

## Phase 7 – Transparente Produktqualität

Status: Langfristig geplant

Priorität: **P1 – nach belastbarer Daten- und Berechnungsfreigabe**

- [ ] Zertifikats-Qualitätsscore aus fachlich freigegebenen Faktoren konzipieren
- [ ] Finanzierungskosten erst nach eigener fachlicher Definition ergänzen
- [ ] Explainable Engine mit Beiträgen, Ausschlussgründen, Datenbasis und Annahmen
  bereitstellen
- [ ] Confidence Score strikt von Data-Quality-Status, Produktqualität und
  Attraktivität trennen
- [ ] Gewichte, Normalisierung, Mindestdaten und Schwellenwerte vor
  Implementierung fachlich freigeben
- [ ] Sicherstellen, dass Scores weder Kauf- noch Verkaufsempfehlungen darstellen

Architekturgrundlage: [ADR-0005](docs/DECISIONS.md#adr-0005--mehrdimensionaler-zertifikats-qualitätsscore), [ADR-0006](docs/DECISIONS.md#adr-0006--explainable-engine-für-produktauswahl-und-berechnung) und [ADR-0007](docs/DECISIONS.md#adr-0007--confidence-score-für-die-berechnungszuverlässigkeit).

## Verbindliche Entwicklungsreihenfolge

1. **Theoretische Engine stabilisieren – weit fortgeschritten.**
2. **Mock-Daten-Szenario-Kit – mit 25 Szenarien abgeschlossen.**
3. **Einheitliche Data-Quality-Schicht – struktureller V1-Vertrag und
   kontrollierte Orchestrator-Integration aktiv;** Application- und
   Presentation-Weitergabe folgen separat.
4. **Externe DTOs, Mapper und API-Verträge – offen;** erst auf den stabilisierten
   Domain- und Data-Quality-Grenzen entwerfen.
5. **Live-Datenprovider – offen;** erst nach gesonderter fachlicher und
   architektonischer Freigabe anbinden.

Application und UI werden danach in kleinen, separat geprüften
Migrationsschritten auf den neuen Vertrag umgestellt.
