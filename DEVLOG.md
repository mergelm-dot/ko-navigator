# KO Navigator – Development Log

Dieses Dokument hält wichtige technische und fachliche Entscheidungen des Projekts fest.

---

## Entwicklungsprinzip

Der KO Navigator wird in kleinen, stabilen Schritten entwickelt.

Für jeden neuen Baustein gilt:

1. Ziel definieren
2. Betroffene Dateien festlegen
3. Eine abgeschlossene Funktion umsetzen
4. Projekt bauen und testen
5. Funktionierenden Stand committen
6. Erst danach den nächsten Baustein beginnen

Jeder Commit soll einen lauffähigen Projektstand enthalten.

---

## Architektur der Berechnungsengine

Die Berechnungslogik wird in mehrere Ebenen getrennt.

### KoCalculator

Verantwortlich für reine mathematische Berechnungen rund um Knock-out-Produkte.

Beispiele:

- theoretischer Zertifikatspreis
- KO-Barriere
- Hebel
- Abstand zur KO-Barriere

### PriceConverter

Verantwortlich für Preis- und Währungsumrechnungen.

### TradeCalculationEngine

Verbindet die einzelnen Berechnungen und prüft die Eingaben.

Die Benutzeroberfläche soll keine eigene Finanzmathematik enthalten.

### CertificateMatcher

Später geplante Komponente.

Sie soll echte Emittentenprodukte mit dem geplanten Trade vergleichen und passende Zertifikate auswählen.

### Konsolidierungsschritt 1 – Verantwortlichkeiten verbindlich festgelegt

Für die schrittweise Konsolidierung der Berechnungsengine gelten verbindlich folgende Zielverantwortlichkeiten:

- `KoCalculator` enthält ausschließlich reine mathematische KO-Produktformeln. Er übernimmt keine Orchestrierung, Benutzertexte, Anzeigeformatierung, Empfehlungen oder Zugriffe auf UI, Repositories, Netzwerk, Android und Compose.
- `TradeCalculationEngine` nimmt vollständige Berechnungseingaben entgegen, validiert fachliche Zusammenhänge, legt Rechenschritte fest, ruft reine Calculator-Komponenten auf und führt Ergebnisse, Status, Warnungen und strukturierte Fehler zusammen. Sie dupliziert langfristig keine mathematischen Fachformeln.
- `PriceConverter` ist ausschließlich für eindeutig definierte Währungs-, Einheiten- und preisbezogene Konvertierungen vorgesehen. Eigene Zertifikatspreis-, KO-Abstands- oder Hebelformeln gehören nicht zu seiner Zielverantwortung.

Dieser Schritt ändert ausschließlich KDoc und Architekturdokumentation. Die vorhandenen Formeln, Funktionssignaturen und das Laufzeitverhalten bleiben unverändert. Insbesondere bleiben die aktuelle Rundung in `KoCalculator`, die direkt in `TradeCalculationEngine` enthaltenen Formeln und die Zertifikatspreisformel in `PriceConverter` als dokumentierte Ist-Abweichungen bestehen, bis ihre jeweilige Änderung gesondert freigegeben und durch Tests abgesichert wird.

---

## Fachliche Annahmen

Die ersten Berechnungen sind theoretische Modelle.

Noch nicht berücksichtigt werden:

- Spread
- Finanzierungskosten
- Aufgeld
- Emittentenanpassungen
- Wechselkursschwankungen
- unterschiedliche KO-Barriere und Basispreis

Diese Faktoren werden später getrennt ergänzt, damit die Berechnung nachvollziehbar und transparent bleibt.

---

## Verschobene UI-Idee

Nach Auswahl eines Basiswertes soll später eine kompakte Asset-Karte angezeigt werden.

Beispiel:

NVIDIA  
NVDA • NASDAQ • USD

Die Karte soll ein kleines Firmenlogo, den Firmennamen, Ticker, Börse und Währung enthalten.

Die Umsetzung erfolgt erst nach Fertigstellung der Berechnungsengine und der Zertifikatssuche.
