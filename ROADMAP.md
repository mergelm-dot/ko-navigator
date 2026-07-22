# KO Navigator Roadmap

## Phase 1 – Fundament

- [x] Projektstruktur
- [x] Datenmodelle
- [x] Testdaten
- [x] Repository
- [x] Suchmaschine mit Repository verbunden
- [x] UnderlyingSearchField erstellt
- [ ] Suchfeld in Oberfläche
- [ ] Automatischer Kursabruf

## Phase 2 – Berechnungsengine

- [ ] Preisberechnung
- [ ] Hebelberechnung
- [ ] KO-Abstand
- [ ] Ziel-Einstiegspreis
- [ ] Gewinn-/Verlustberechnung

## Phase 3 – Zertifikatssuche

- [ ] Zertifikate laden
- [ ] Filtern
- [ ] Bewerten
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
- Ticker • Börse • Währung in einer Zeile
- kompakte Darstellung
- professioneller TradingView-/Broker-Look
- Umsetzung erst nach Fertigstellung der Berechnungs-Engine

## Phase 5 – Verlässliche Markt- und Währungsdaten

Status: Langfristig geplant

Priorität: **P0 – Voraussetzung für belastbare reale Produktberechnungen**

- [ ] `CurrencyPolicy` als verbindliche fachliche Grenze einführen
- [ ] Referenzwährung des Basiswerts und Produktwährung typisiert führen
- [ ] `CurrencyConverter` und austauschbaren `FXRateProvider` anbinden
- [ ] FX-Paar, Richtung, Quelle und Zeitstempel jeder Umrechnung nachweisbar machen
- [ ] bestehende Quote-Freshness um konfigurierbare Warnstufen und eine blockierende Stufe erweitern
- [ ] Bid, Ask und Mid über eine kontextabhängige Bid-/Ask-Policy auswählen
- [ ] Mid ausschließlich als analytischen Referenzwert behandeln
- [ ] konkrete Schwellenwerte und Ordertyp-Zuordnungen vor Implementierung fachlich freigeben

Architekturgrundlage: [ADR-0001](docs/DECISIONS.md#adr-0001--currencypolicy-als-verbindliche-währungsgrenze), [ADR-0002](docs/DECISIONS.md#adr-0002--quote-freshness-mit-warn--und-blockierstufen) und [ADR-0003](docs/DECISIONS.md#adr-0003--kontextabhängige-bid-ask-policy).

## Phase 6 – Robustheit und Datenqualitätsfreigabe

Status: Langfristig geplant

Priorität: **P0 – vor produktiver Nutzung realer Zertifikatsdaten**

- [ ] Integrationstests für fehlenden Bid und fehlenden Ask
- [ ] Integrationstests für falsche beziehungsweise inkompatible Währungen
- [ ] Integrationstests für große Spreads und veraltete Quotes
- [ ] Integrationstests für Handelsaussetzung und ungültige Produktdaten
- [ ] Integrationstests für bereits ausgelösten Knock-out
- [ ] Fail-closed-Verhalten für blockierende Datenqualitätsfehler nachweisen
- [ ] Warnungen, Blockierungen und Teilresultate durchgängig bis zur UI testen

Architekturgrundlage: [ADR-0004](docs/DECISIONS.md#adr-0004--realistische-robustheits-integrationstests).

## Phase 7 – Transparente Produktqualität

Status: Langfristig geplant

Priorität: **P1 – nach belastbarer Daten- und Berechnungsfreigabe**

- [ ] Zertifikats-Qualitätsscore aus Spread, Emittent, Datenqualität, Aktualität, KO-Abstand und Liquidität konzipieren
- [ ] Finanzierungskosten erst nach eigener fachlicher Definition als Score-Faktor ergänzen
- [ ] Explainable Engine mit Beiträgen, Ausschlussgründen, Datenbasis und Annahmen bereitstellen
- [ ] Confidence Score getrennt von Produktqualität und Attraktivität modellieren
- [ ] Score-Gewichte, Normalisierung, Mindestdaten und Schwellenwerte vor Implementierung fachlich freigeben
- [ ] sicherstellen, dass Scores weder Kauf- noch Verkaufsempfehlungen darstellen

Architekturgrundlage: [ADR-0005](docs/DECISIONS.md#adr-0005--mehrdimensionaler-zertifikats-qualitätsscore), [ADR-0006](docs/DECISIONS.md#adr-0006--explainable-engine-für-produktauswahl-und-berechnung) und [ADR-0007](docs/DECISIONS.md#adr-0007--confidence-score-für-die-berechnungszuverlässigkeit).
