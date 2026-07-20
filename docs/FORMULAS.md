# KO Navigator – Mathematisches Berechnungshandbuch

## 1. Zweck und Geltungsbereich

Dieses Dokument ist die verbindliche fachliche Grundlage der Berechnungsengine des KO Navigators. Formeln, Konventionen und Validierungsregeln dürfen nur geändert werden, wenn gleichzeitig dieses Dokument und die zugehörigen Tests angepasst werden. Abweichungen zwischen Implementierung und diesem Handbuch sind als Fehler zu behandeln.

Der KO Navigator unterstützt die Handelsplanung und stellt Berechnungen als Informationshilfe bereit. Er gibt keine Kauf- oder Verkaufsempfehlungen und keine Anlageberatung.

Theoretische Modellwerte sind stets klar von realen Emittentenpreisen zu unterscheiden. Modellwerte beruhen auf den hier beschriebenen Annahmen; reale Geld- und Briefkurse können unter anderem durch Produktbedingungen, Finanzierungskosten, Aufgeld, Spread, Marktlage und Emittentenanpassungen abweichen.

## 2. Allgemeine Berechnungsgrundsätze

- Intern wird nicht vorzeitig gerundet.
- Eine Rundung erfolgt erst für die Anzeige oder nach einer ausdrücklich definierten produktspezifischen Tick-Size.
- Basispreis `B` und Knock-out-Barriere `KO` werden getrennt modelliert.
- Aktueller Basiswertkurs `S_current` und geplanter Einstiegskurs `S_entry` werden getrennt modelliert.
- Basiswertwährung und Produktwährung werden getrennt modelliert.
- Fehlende Werte dürfen nicht stillschweigend durch `0` ersetzt werden.
- `NaN`, positive oder negative Unendlichkeit, Nullwerte und negative Werte werden ausdrücklich und kontextbezogen validiert.
- Long und Short werden immer separat betrachtet und getestet.
- Der KO-Status wird als eigener fachlicher Status modelliert. Ein Preis von `0` ist kein Ersatz für diesen Status.
- Jede Berechnung benennt, auf welchen Kurszeitpunkt und welche Preisart sie sich bezieht.
- Vereinfachte Modelle werden als solche gekennzeichnet und dürfen nicht als reale Emittentenbewertung ausgegeben werden.

## 3. Begriffe und Variablen

„Pflicht“ bezieht sich auf die Berechnung, in der die Variable verwendet wird. „Kontextabhängig“ bedeutet, dass der Wert nur für bestimmte Berechnungen erforderlich ist und andernfalls fehlen darf. Alle Zahlen müssen endlich sein, sofern nicht ausdrücklich ein fehlender Wert als Zustand zugelassen ist.

| Variable | Bedeutung | Einheit | Zulässiger Wertebereich | Pflichtfeld oder optional |
|---|---|---|---|---|
| `S_current` | Aktueller Kurs des Basiswerts | Basiswertwährung je Basiswerteinheit | `> 0` | Kontextabhängig; Pflicht für aktuelle Bewertung |
| `S_entry` | Geplanter Einstiegskurs des Basiswerts | Basiswertwährung je Basiswerteinheit | `> 0` | Kontextabhängig; Pflicht für Einstiegsplanung |
| `S_target` | Zielkurs des Basiswerts | Basiswertwährung je Basiswerteinheit | `> 0` | Optional; Pflicht im Zielkursszenario |
| `B` | Basispreis (Strike) des Produkts | Basiswertwährung je Basiswerteinheit | `> 0` | Pflicht für inneren Wert und Produktbewertung |
| `KO` | Knock-out-Barriere des Produkts | Basiswertwährung je Basiswerteinheit | `> 0` | Pflicht für KO-Abstand und KO-Status |
| `R` | Bezugsverhältnis; vom Produkt abgebildeter Anteil des Basiswerts | Basiswerteinheiten je Produktstück | `> 0` | Pflicht |
| `FX` | Einheiten der Basiswertwährung je `1` Einheit Produktwährung | Basiswertwährung/Produktwährung | `> 0` | Pflicht bei Währungsumrechnung; bei gleicher Währung `1` |
| `P_model` | Theoretischer Modellpreis des Produkts | Produktwährung je Stück | Im aktiven Modell grundsätzlich `>= 0`; für Hebelberechnung `> 0` | Ergebnisgröße |
| `P_bid` | Geldkurs des Produkts | Produktwährung je Stück | `>= 0`; für reguläre Bewertung `> 0` | Optional; kontextabhängig |
| `P_ask` | Briefkurs des Produkts | Produktwährung je Stück | Falls vorhanden: `> 0` | Optional; Pflicht für Kaufplanung |
| `L_target` | Gewünschter Zielhebel | dimensionslos | `> 1`; verbindliche Obergrenze noch offen | Kontextabhängig; Pflicht zur Barrierenäherung oder Produktsuche |
| `L_actual` | Aus Kurs und relevantem Produktpreis berechneter tatsächlicher Hebel | dimensionslos | `> 0`, sofern berechenbar | Ergebnisgröße |
| `D_abs` | Gerichteter absoluter Abstand zwischen Basiswertkurs und KO-Barriere | Basiswertwährung je Basiswerteinheit | Kann positiv, `0` oder negativ sein | Ergebnisgröße |
| `D_pct` | Gerichteter KO-Abstand relativ zum verwendeten Basiswertkurs | Prozent | Kann positiv, `0` oder negativ sein | Ergebnisgröße |
| `Investment` | Geplanter maximaler Kapitaleinsatz | Produktwährung | `> 0` | Kontextabhängig; Pflicht für Positionsplanung |
| `Quantity` | Handelbare Stückzahl | Stück | Ganzzahlig und `>= 0` im Zielmodell | Ergebnisgröße |
| `Fees` | Kauf- oder Verkaufsgebühren; je Vorgang separat zu führen | Produktwährung | `>= 0` | Optional; ohne Gebühren ausdrücklich `0` |
| `Premium` | Separat ausgewiesenes Aufgeld | Produktwährung je Stück | Endlich; Vorzeichen und fachliche Ermittlung noch offen | Optional |
| `Financing` | Separat ausgewiesene Finanzierungskomponente | Produktwährung je Stück | Endlich; Vorzeichen und fachliche Ermittlung noch offen | Optional |

Zusätzlich werden `Intrinsic` als innerer Wert, `InvestedAmount` als tatsächlich eingesetzter Gesamtbetrag, `ExitValue` als Nettoerlös und `PnL_abs` beziehungsweise `PnL_pct` als Gewinn oder Verlust verwendet. Währungen sind mit einem eindeutigen Währungscode, beispielsweise `EUR` oder `USD`, zu führen.

### Handelbare Produktmarktdaten

`P_bid` ist der aktuell verfügbare Verkaufspreis des KO-Produkts, `P_ask`
der aktuell verfügbare Kaufpreis. Ein Last-Preis ist nur eine ergänzende
Marktinformation und kein Ersatz für Bid oder Ask. Die Produkt- oder
Quote-Währung darf nicht pauschal als EUR angenommen werden.

Fehlende Bid- oder Ask-Seiten werden als `null` modelliert und nicht durch
`0.0` ersetzt. Beide Seiten besitzen getrennte Zeitstempel, die in Version 1
als UTC Epoch Milliseconds geführt werden. Quote-Währung und Datenquelle
werden ausdrücklich gespeichert.

Für die allgemeine Version-1-Validierung sind fehlende Bid- und Ask-Seiten
nicht automatisch ungültig. Ein vorhandener Bid muss endlich und `>= 0` sein;
`0.0` ist dabei ein zulässiger ausdrücklicher Rohwert und kein Ersatz für einen
fehlenden Bid. Ein vorhandener Ask muss endlich und `> 0` sein; `0.0` ist als
Ask ungültig. Preis und Zeitstempel müssen je Quote-Seite gemeinsam vorhanden
oder gemeinsam abwesend sein.

Wenn beide numerisch gültigen Seiten vorhanden sind, ist `bid > ask` ein
blockierender Datenqualitätsfehler. Die Werte werden weder vertauscht noch wird
der Spread durch einen Absolutbetrag scheinbar korrigiert. Der Validator
normalisiert keine Eingaben und prüft keine Aktualität. Eine leere Fehlerliste
bestätigt ausschließlich die interne Konsistenz nach den Version-1-Regeln und
ist keine Freigabe für Bid-, Ask-, Spread-, Mid- oder andere Berechnungen.

Konkrete Aktualitätsregeln, der Abgleich von Produktreferenz und Währung mit
einer Produktspezifikation sowie anwendungsbezogene Berechnungsfreigaben bleiben
**OFFEN** und werden in späteren Schritten getrennt festgelegt.

Spread, relativer Spread und Mid-Preis werden nicht gespeichert, sondern
später aus validierten, fachlich zusammengehörigen Quotes berechnet:

```text
absoluteSpread = ask - bid
relativeSpreadToAskPercent = (ask - bid) / ask * 100
midPrice = (bid + ask) / 2
```

Für alle drei Berechnungen gelten die mathematischen Vorbedingungen
`0 <= bid <= ask` und `ask > 0`; beide Preise müssen endlich sein. Der absolute
Spread besitzt die Einheit Produktwährung je Zertifikat. Der relative Spread
ist ausdrücklich auf Ask bezogen, liegt unter diesen Vorbedingungen zwischen
`0` und `100` Prozent, ergibt bei Bid `0` genau `100` Prozent und bei gleichem
Bid und Ask `0` Prozent. Er wird nicht geklemmt.

Zur Verringerung des Overflow-Risikos wird der Mid-Preis algebraisch
gleichwertig als `bid + (ask - bid) / 2` implementiert. Der Mid-Preis ist ein
Referenzwert und kein handelbarer Preis. Keine der drei Berechnungen rundet,
formatiert, vertauscht oder korrigiert Werte; ein negativer Spread wird nicht
durch einen Absolutbetrag verborgen.

Der `MarketDataCalculator` prüft ausschließlich diese mathematischen
Vorbedingungen. Die vollständige Modellvalidierung bleibt Aufgabe des
`KnockoutProductMarketDataValidator`. Verfügbarkeit, Produktspezifikations-
kompatibilität und Aktualität müssen durch spätere Orchestrierung sichergestellt
werden; eine Engine- oder UI-Anbindung besteht noch nicht. Last, Bid- und
Ask-Stückzahlen sowie die konkrete Aktualitätspolitik bleiben **OFFEN**.

Für die Version-1-Kompatibilität einer bereits intern validierten
`KnockoutProductSpecification` mit bereits intern validierten
`KnockoutProductMarketData` gelten genau zwei Cross-Model-Regeln: Die
Produktreferenz wird case-sensitiv und ohne Normalisierung über `productIsin`
verglichen, und die Quote-Währung muss exakt `productCurrency` entsprechen.
`underlyingCurrency` ist nicht die Vergleichswährung des Produktquotes;
`issuerId` und `sourceId` beschreiben unterschiedliche Rollen und werden nicht
gleichgesetzt. Es gibt weder WKN-Fallback noch Währungsumrechnung.

Eine abweichende ISIN oder Produktwährung blockiert jede produktbezogene
Verwendung dieses Modellpaars. Eine leere Kompatibilitätsfehlerliste bestätigt
jedoch weder interne Modellvalidität, Bid-/Ask-Verfügbarkeit, Aktualität,
Quellenqualität noch eine vollständige Berechnungsfreigabe. FX, Quanto,
Provider-Mapping und eine spätere interne Produkt-ID bleiben **OFFEN**.

### Strukturelle Berechnungsverfügbarkeit

Strukturelle Availability ist keine mathematische Berechnung. Der
`MarketDataCalculationAvailabilityEvaluator` setzt eine intern gültige
Produktspezifikation, intern gültige Marktdaten und erfolgreiche
Cross-Model-Kompatibilität voraus und prüft ausschließlich die für einen
angefragten Berechnungstyp benötigten Quote-Seiten:

- `PURCHASE_PRICE` benötigt Ask; Bid ist dafür irrelevant.
- `SALE_PRICE` benötigt einen vorhandenen Bid größer als `0.0`; Ask ist dafür
  irrelevant.
- `SPREAD` benötigt Bid und Ask; Bid `0.0` ist zulässig.
- `MID` benötigt Bid und Ask; Bid `0.0` ist zulässig und Mid bleibt ein nicht
  handelbarer Referenzwert.

Die Preis-/Zeitstempel-Kohärenz wird bereits durch den vorgelagerten
`KnockoutProductMarketDataValidator` garantiert. Der AvailabilityEvaluator
prüft deshalb keine Zeitstempel und wiederholt weder numerische Preisregeln
noch ISIN-, Währungs- oder Kompatibilitätsprüfungen. Er ruft keine Validatoren
und keinen Calculator auf.

`StructurallyAvailable` bestätigt nur die strukturelle Quote-Verfügbarkeit und
bei `SALE_PRICE` einen positiven Bid. Das Ergebnis bestätigt weder Aktualität,
Quellenqualität, Handelbarkeit noch eine vollständige fachliche Freigabe oder
erfolgreiche Berechnung. Freshness und Quellenpolicy bleiben **OFFEN**. Nach
erfolgreicher vorgelagerter Orchestrierung wird der `MarketDataCalculator`
weiterhin separat aufgerufen; keine mathematische Formel wird durch die
Availability-Prüfung verändert.

### Zeitliche Marktdatenfrische

Freshness bewertet ausschließlich die Zeit der für einen Berechnungstyp
relevanten Quote-Seiten. Der Bewertungszeitpunkt wird als UTC Epoch Milliseconds
explizit je Aufruf übergeben; die Policy liest keine Systemzeit. Die spätere
Application- oder Composition-Schicht liefert vier explizite, nichtnegative
Schwellen ohne Produktionsdefaults:

- `maxBidAgeMillis`
- `maxAskAgeMillis`
- `maxBidAskDifferenceMillis`
- `allowedFutureSkewMillis`

Alle Grenzen sind inklusiv. Ein Timestamp ist erst stale, wenn seine Distanz
zum späteren Bewertungszeitpunkt die jeweilige maximale Altersgrenze
überschreitet. Ein zukünftiger Timestamp ist erst unzulässig, wenn seine
Distanz zum früheren Bewertungszeitpunkt `allowedFutureSkewMillis`
überschreitet. `PURCHASE_PRICE` prüft nur Ask, `SALE_PRICE` nur Bid. `SPREAD`
und `MID` prüfen beide Seiten sowie deren maximale Zeitdifferenz.

Ein Zeitstempel kann nicht zugleich als zukünftig und stale gelten. Liegt eine
relevante Seite unzulässig in der Zukunft, wird kein zusätzlicher
Bid-/Ask-Zeitdifferenzfehler erzeugt. Eine stale Seite unterdrückt diese
Differenzprüfung dagegen nicht. Negative Epoch-Millis sowie `Long.MIN_VALUE`
und `Long.MAX_VALUE` sind zulässig und werden nur relativ zum expliziten
Bewertungszeitpunkt beurteilt. Distanzen werden ohne überlaufgefährdete direkte
Subtraktion und ohne `abs()` durch einen geordneten, additionsgesicherten
Grenzvergleich geprüft.

`Fresh` bestätigt ausschließlich die zeitliche Policy und ist keine
vollständige Berechnungsfreigabe oder Handelbarkeitsaussage. Intern gültige und
kompatible Marktdaten sowie strukturelle Availability sind Vorbedingungen.
SourcePolicy, Handelszeiten, Marktstatus, providerbezogene Regeln und konkrete
Produktionsschwellen bleiben **OFFEN**. Keine bestehende Preisformel wird durch
diese zeitliche Policy verändert.

### Konfigurierte Quellenfreigabe

Die `MarketDataSourcePolicy` bewertet ausschließlich, ob ein exakter
`sourceId` für einen angefragten `MarketDataCalculationType` konfiguriert ist.
Der Quellenbezeichner wird case-sensitiv und ohne `trim()`, Großschreibung,
Aliasauflösung oder andere Normalisierung verglichen. Aus seinem Text wird
keine Provider- oder Quellentypbedeutung abgeleitet.

Jede Regel enthält ein explizites Set unterstützter CalculationTypes. Nur die
unmittelbare Set-Mitgliedschaft führt zu `Allowed`; es findet keine
Capability-Ableitung zwischen `PURCHASE_PRICE`, `SALE_PRICE`, `SPREAD` und
`MID` statt. Nicht konfigurierte Quellen werden blockiert. Es gibt keine
Default-Zulässigkeit, Wildcard oder Fallback-Regel. Eine leere Konfiguration
blockiert alle Quellen; ein leeres Capability-Set blockiert alle Typen der
bekannten Quelle.

Die SourcePolicy bewertet keine Preise, Quote-Seiten, Zeitstempel oder
Freshness. Umgekehrt bewertet die `MarketDataFreshnessPolicy` keine Quellen.
`Allowed` ist deshalb keine vollständige Berechnungsfreigabe. Interne
Validierung, Cross-Model-Kompatibilität, Availability, Freshness,
Quellenfreigabe und Calculator-Aufruf werden erst in einer späteren
Orchestrierung getrennt zusammengeführt.

Quellentypen, Latenzklassen, Vertrauensstufen, Provider-Metadaten,
Provider-Mapping, konkrete Produktionsprovider und die serverseitige
Konfigurationsbereitstellung bleiben **OFFEN**. Keine Preisformel wird durch
die Quellenfreigabe verändert.

### Zentrale Marktdatenorchestrierung

Der `MarketDataCalculationOrchestrator` verbindet die vorhandenen Komponenten
für genau einen `MarketDataCalculationType` in der festen Fail-Fast-Reihenfolge
Produktspezifikationsvalidierung, Marktdatenvalidierung, Cross-Model-
Kompatibilität, strukturelle Availability, Freshness, Quellenfreigabe und
Berechnung beziehungsweise Quote-Auswahl. Sobald eine Stufe blockiert, werden
keine späteren Stufen ausgewertet. Mehrere Fehler werden nur innerhalb der
bereits bestehenden Validatoren gesammelt.

Die vorhandenen Validatoren, der AvailabilityEvaluator sowie Freshness- und
SourcePolicy bleiben die alleinigen Quellen ihrer Regeln. Ihre bestehenden
Fehlercodes werden unverändert in stufenspezifischen, maschinenlesbaren
Result-Untertypen weitergegeben. Calculator-Fehler bleiben ebenfalls als
strukturierte Calculator-Fehler erhalten. Die Orchestrierung führt keine
zusätzliche Validierung, Normalisierung oder Fehlerkorrektur durch.

Nach allen erfolgreichen Freigaben gilt für die beiden handelbaren Quote-
Seiten:

```text
PurchasePrice = ask
SalePrice = bid, mit bid > 0
```

Dies ist eine Auswahl bereits vorhandener Quotes und keine neue Preisformel.
Für `SPREAD` werden sowohl `absoluteSpread` als auch
`relativeSpreadToAskPercent` gemeinsam ausgegeben; beide Werte stammen aus den
vorhandenen Funktionen des `MarketDataCalculator`. `MID` verwendet dessen
vorhandene Mid-Funktion. Keine dieser Ergebniszusammenführungen rundet
zusätzlich.

Der Bewertungszeitpunkt wird als UTC Epoch Milliseconds explizit übergeben und
unverändert an die FreshnessPolicy weitergereicht; die Orchestrierung liest
keine Systemzeit. Ein erfolgreiches Result bestätigt ausschließlich den
dokumentierten Prüf- und Berechnungsablauf. Es ist weder eine Kauf- oder
Verkaufsempfehlung noch eine allgemeine Zusage der Handelbarkeit.

## 4. Produktrichtung

### Long

Ein Long-KO-Produkt steigt grundsätzlich im Wert, wenn der Basiswert steigt. Der Basispreis und die KO-Barriere liegen normalerweise unter dem relevanten Basiswertkurs.

### Short

Ein Short-KO-Produkt steigt grundsätzlich im Wert, wenn der Basiswert fällt. Der Basispreis und die KO-Barriere liegen normalerweise über dem relevanten Basiswertkurs.

Diese Aussagen beschreiben die grundsätzliche Wirkungsrichtung. Maßgeblich bleiben immer die produktspezifischen Bedingungen des Emittenten, insbesondere die Definition und Beobachtung der Barriere sowie mögliche Anpassungsmechanismen.

## 5. Ableitung einer theoretischen KO-Barriere aus Zielhebel

Für eine erste Näherung gilt das folgende **vereinfachte Modell**.

Long:

```text
KO = S_entry × (1 − 1 / L_target)
```

Short:

```text
KO = S_entry × (1 + 1 / L_target)
```

Die Näherung behandelt Basispreis und KO-Barriere für diesen Zweck gleich. Sie berücksichtigt weder Finanzierung noch Aufgeld, Spread oder Wechselkurs. Das Ergebnis ist eine theoretische Barriere zur Planung und nicht automatisch die Barriere eines realen, handelbaren Produkts.

**Beispiel Long:** Bei `S_entry = 100,00 EUR` und `L_target = 5` ergibt sich:

```text
KO = 100,00 × (1 − 1 / 5) = 80,00 EUR
```

**Beispiel Short:** Bei `S_entry = 100,00 EUR` und `L_target = 5` ergibt sich:

```text
KO = 100,00 × (1 + 1 / 5) = 120,00 EUR
```

## 6. KO-Abstand

Der KO-Abstand ist gerichtet zu berechnen. In den folgenden Formeln steht `S` jeweils eindeutig für `S_current`, `S_entry` oder `S_target`.

Long:

```text
D_abs = S − KO
```

Short:

```text
D_abs = KO − S
```

Für beide Richtungen:

```text
D_pct = D_abs / S × 100
```

Der Abstand wird getrennt für den aktuellen Kurs und den geplanten Einstieg berechnet. Negative Werte dürfen nicht durch einen Absolutwert verborgen werden. Bei einer negativen oder null großen Distanz ist der KO-Status gesondert zu prüfen.

**Beispiel Long:** `S_current = 100,00 EUR`, `KO = 80,00 EUR`:

```text
D_abs = 100,00 − 80,00 = 20,00 EUR
D_pct = 20,00 / 100,00 × 100 = 20,00 %
```

**Beispiel Short:** `S_entry = 100,00 EUR`, `KO = 120,00 EUR`:

```text
D_abs = 120,00 − 100,00 = 20,00 EUR
D_pct = 20,00 / 100,00 × 100 = 20,00 %
```

Ein Long mit `S = 79,00 EUR` und `KO = 80,00 EUR` hat folglich `D_abs = -1,00 EUR`; dieser negative Wert darf nicht als positiver Abstand angezeigt werden.

## 7. Vereinfachter innerer Wert

Long:

```text
Intrinsic = max(S − B, 0) × R
```

Short:

```text
Intrinsic = max(B − S, 0) × R
```

Für den inneren Wert ist der Basispreis `B` zu verwenden, nicht automatisch die KO-Barriere. Das Ergebnis lautet zunächst auf die Basiswertwährung. Es enthält noch keine Währungsumrechnung, Finanzierungskosten, kein Aufgeld und keinen Spread. Der innere Wert ist daher nicht automatisch der reale Kauf- oder Verkaufspreis.

**Beispiel Long:** `S = 100,00 USD`, `B = 80,00 USD`, `R = 0,1`:

```text
Intrinsic = max(100,00 − 80,00, 0) × 0,1 = 2,00 USD
```

**Beispiel Short:** `S = 100,00 EUR`, `B = 120,00 EUR`, `R = 0,1`:

```text
Intrinsic = max(120,00 − 100,00, 0) × 0,1 = 2,00 EUR
```

## 8. Währungsumrechnung

Die verbindliche Konvention lautet:

```text
FX = Einheiten der Basiswertwährung je 1 Einheit Produktwährung
```

Damit gilt:

```text
Preis in Produktwährung = Preis in Basiswertwährung / FX
```

Beispiel: Der Basiswert lautet auf USD, das Produkt auf EUR. Bei `EUR/USD = 1,10` gilt `1 EUR = 1,10 USD`. Ein innerer Wert von `2,20 USD` wird daher wie folgt umgerechnet:

```text
2,20 USD / 1,10 (USD/EUR) = 2,00 EUR
```

`FX` muss größer als `0` und endlich sein. Bei gleicher Basiswert- und Produktwährung gilt `FX = 1`. Währungspaar und Zeitstempel des verwendeten Wechselkurses müssen mit der späteren Datenquellenintegration mitgeführt werden; eine Richtung darf nie allein aus einer unbeschrifteten Zahl abgeleitet werden.

## 9. Theoretischer Modellpreis

Das vorläufige Zielmodell lautet:

```text
P_model = Intrinsic in Produktwährung + Financing + Premium
```

Dabei gilt:

```text
Intrinsic in Produktwährung = Intrinsic in Basiswertwährung / FX
```

`Financing` und `Premium` sind zunächst optionale und separat auszuweisende Komponenten. Falls sie im vereinfachten Modell nicht berücksichtigt werden, ist dies ausdrücklich mit `0` und einer entsprechenden Modellkennzeichnung zu dokumentieren. Der Spread darf nicht unbemerkt in `P_model` einfließen. Reale Geld- und Briefkurse (`P_bid` und `P_ask`) werden getrennt dargestellt.

Die genaue Ermittlung, zeitliche Fortschreibung und Vorzeichenkonvention der Finanzierungskosten bleibt bis zur Datenquellenintegration eine offene Fachentscheidung.

## 10. Tatsächlicher Hebel

Der tatsächliche Hebel ergibt sich aus:

```text
L_actual = (S × R / FX) / P_relevant
```

`P_relevant` hängt vom Anwendungsfall ab:

- Für eine Kaufplanung ist grundsätzlich `P_ask` zu verwenden.
- Für die Bewertung einer bestehenden Position kann `P_bid` relevant sein.
- Für eine reine Modellrechnung ist `P_model` zu verwenden.

`P_relevant` muss größer als `0` und endlich sein. Bei einem KO-Status oder einem Preis von `0` wird kein normaler Hebel ausgegeben. Der Hebel muss sowohl für `S_current` als auch für `S_entry` berechnet werden können; Kurs und Produktpreis müssen dabei zum selben Szenario gehören.

**Beispiel:** `S = 100,00 EUR`, `R = 0,1`, `FX = 1` und `P_ask = 2,00 EUR`:

```text
L_actual = (100,00 × 0,1 / 1) / 2,00 = 5,00
```

Das Ergebnis ist der auf dem Briefkurs basierende Hebel für eine Kaufplanung.

## 11. Zukünftiger Hebel bei geplantem Einstieg

Ein bereits existierendes Produkt besitzt einen festen Basispreis `B`, eine feste KO-Barriere `KO`, ein festes Bezugsverhältnis `R` und eine definierte Produktwährung. Für den geplanten Basiswertkurs `S_entry` werden berechnet:

- der theoretische Produktpreis am Einstieg,
- der KO-Abstand am Einstieg und
- der tatsächliche Hebel am Einstieg.

Dabei wird nicht zwangsläufig eine neue KO-Barriere erzeugt. Bei der Produktsuche ist stattdessen zu prüfen, welches vorhandene Zertifikat den gewünschten Zielhebel am geplanten Einstieg möglichst gut trifft.

**Beispiel eines bestehenden Long-Produkts:**

```text
B = 78,00 EUR
KO = 80,00 EUR
R = 0,1
FX = 1
S_entry = 90,00 EUR
Financing = 0,00 EUR (vereinfachtes Modell)
Premium = 0,00 EUR (vereinfachtes Modell)
```

Dann gilt:

```text
Intrinsic_entry = (90,00 − 78,00) × 0,1 = 1,20 EUR
P_model_entry = 1,20 / 1 + 0,00 + 0,00 = 1,20 EUR
D_abs_entry = 90,00 − 80,00 = 10,00 EUR
D_pct_entry = 10,00 / 90,00 × 100 = 11,111… %
L_actual_entry = (90,00 × 0,1 / 1) / 1,20 = 7,50
```

Die feste Barriere bleibt `80,00 EUR`; sie wird nicht aus `L_target = 7,50` neu abgeleitet.

## 12. KO-Status

Folgende fachliche Zustände sind zu verwenden:

| Status | Bedeutung |
|---|---|
| `ACTIVE` | Das Produkt ist nach den verfügbaren Daten nicht ausgeknockt und der aktuelle Kurs liegt auf der aktiven Seite der Barriere. |
| `AT_KNOCKOUT` | Der betrachtete Kurs liegt innerhalb der definierten Toleranz an der Barriere, ohne dass eine bereits erfolgte Barrierenberührung bekannt ist. |
| `KNOCKED_OUT` | Die Barriere ist nach den verfügbaren Daten verletzt oder bereits berührt worden. |
| `INVALID_CONFIGURATION` | Eingaben oder Produktparameter sind fachlich beziehungsweise technisch ungültig. |
| `DATA_UNAVAILABLE` | Für eine belastbare Statusbestimmung fehlen erforderliche Daten. |

Für eine reine Kursmomentaufnahme gilt vorbehaltlich der Toleranz:

| Richtung | `ACTIVE` | `AT_KNOCKOUT` | `KNOCKED_OUT` |
|---|---|---|---|
| Long | `S > KO` | `S = KO` innerhalb definierter Toleranz | `S < KO` oder Barriere bereits berührt |
| Short | `S < KO` | `S = KO` innerhalb definierter Toleranz | `S > KO` oder Barriere bereits berührt |

Ist eine frühere Barrierenberührung bekannt, hat `KNOCKED_OUT` Vorrang vor der Momentaufnahme. Die reale Emittentenregel und Beobachtungszeit sind maßgeblich. Der Status ist pfadabhängig; aus einem Endkurs-Szenario allein kann ein zwischenzeitlicher KO weder sicher ausgeschlossen noch rückgängig gemacht werden.

## 13. Gewinn- und Verlustrechnung

Das Zielmodell verwendet getrennte Kauf- und Verkaufsgebühren:

```text
Quantity = floor((Investment − Kaufgebühren) / P_ask)

InvestedAmount = Quantity × P_ask + Kaufgebühren

ExitValue = Quantity × P_bid_target − Verkaufsgebühren

PnL_abs = ExitValue − InvestedAmount

PnL_pct = PnL_abs / InvestedAmount × 100
```

Zunächst werden nur ganzzahlige Stückzahlen unterstützt. Vor der Berechnung müssen `Investment > 0`, `P_ask > 0`, nichtnegative Gebühren und `Investment >= Kaufgebühren` gelten. `PnL_pct` darf nur bei `InvestedAmount > 0` berechnet werden. Geld- und Briefkurs sowie Kauf- und Verkaufsgebühren sind getrennt zu berücksichtigen.

Bei KO ist ein möglicher produktspezifischer Restwert zu berücksichtigen. Solange die Restwertregel nicht verbindlich definiert und verfügbar ist, darf nicht pauschal ein sicherer Restwert von `0` unterstellt werden; der P&L ist entsprechend als unvollständig oder nicht verfügbar zu kennzeichnen.

## 14. Zielkursszenario

Ein vollständiges Zielkursszenario umfasst:

- den Zielkurs `S_target` des Basiswerts,
- den theoretischen Produktpreis am Ziel,
- die Produktänderung absolut und prozentual gegenüber einem eindeutig benannten Ausgangspreis,
- den Hebel am Ziel,
- den P&L der Position und
- eine KO-Warnung.

Für die prozentuale Produktänderung muss der Ausgangspreis größer als `0` sein. Alle Zielwerte sind als Szenariowerte und nicht als Kursprognose zu kennzeichnen.

**Ein Zielkurs oberhalb der KO-Barriere beweist bei einem Long-Produkt nicht, dass das Produkt auf dem Weg dorthin nicht ausgeknockt wurde.** Entsprechend beweist ein Zielkurs unterhalb der Barriere bei einem Short-Produkt keine ununterbrochene Aktivität. Ohne Pfaddaten ist die KO-Aussage für den Weg zum Ziel unvollständig.

## 15. Rundungsregeln

- Zwischenwerte werden nicht gerundet.
- Der Basiswert wird abhängig von der Präzision des jeweiligen Marktes angezeigt.
- Zertifikatspreise werden zunächst mit mindestens vier Nachkommastellen angezeigt, solange keine produktspezifische Tick-Size vorliegt.
- Prozentwerte werden nur in der Darstellung gerundet.
- Die Stückzahl wird bei ganzzahlig handelbaren Produkten mit `floor` abgerundet.
- Spätere Emittenten-Tick-Sizes haben Vorrang vor pauschalen Rundungsregeln.
- Vergleichstoleranzen für die KO-Barriere sind keine Anzeigerundung und müssen separat definiert werden.

## 16. Validierungsregeln

Validierungsfehler sind strukturiert über einen Fehlercode auszugeben. Mehrere gleichzeitig vorliegende Fehler dürfen nicht durch einen einzelnen Ersatzwert verdeckt werden.

### Allgemeine Version-1-Regeln für Produktspezifikationen

Für `KnockoutProductSpecification` gelten zunächst ausschließlich folgende
allgemeine Regeln:

- `productIsin`, `issuerId` und `underlyingId` dürfen nicht blank sein.
- `productWkn` ist optional; ein vorhandener Wert darf nicht blank sein.
- `basePrice`, `knockoutBarrier` und `ratio` müssen endlich und größer als
  `0` sein.
- Basiswert- und Produktwährung werden vorläufig rein syntaktisch mit
  `[A-Z]{3}` geprüft.
- Alle erkennbaren Fehler werden als stabile Mehrfachfehlerliste in der
  Feldreihenfolge ISIN, WKN, Emittent, Basiswert, Basispreis, KO-Barriere,
  Ratio, Basiswertwährung und Produktwährung ausgegeben.
- Der Validator normalisiert oder korrigiert keine Eingaben.

Diese Validierungsregeln ersetzen keine mathematischen Formeln. Insbesondere
gilt weder `basePrice == knockoutBarrier` noch eine feste Größenrelation
zwischen Basispreis und KO-Barriere. Formale ISIN- und WKN-Prüfungen sowie die
Prüfung gegen eine echte ISO-4217-Liste bleiben **OFFEN**. Ebenso bleiben
emittenten- und produktspezifische Regeln ausdrücklich späteren fachlichen
Entscheidungen vorbehalten.

| Fehlercode | Bedingung | Bedeutung |
|---|---|---|
| `NON_FINITE_NUMBER` | Ein numerischer Eingang ist `NaN`, `+Infinity` oder `-Infinity`. | Die Berechnung ist technisch nicht definiert. |
| `PRICE_NOT_POSITIVE` | Ein für die jeweilige Berechnung erforderlicher Kurs oder Preis ist `<= 0`. | Ein notwendiger positiver Preis fehlt oder ist ungültig. |
| `RATIO_NOT_POSITIVE` | `R <= 0`. | Das Bezugsverhältnis ist ungültig. |
| `FX_NOT_POSITIVE` | `FX <= 0`. | Die Währungsumrechnung wäre nicht definiert. |
| `TARGET_LEVERAGE_OUT_OF_RANGE` | `L_target <= 1` oder oberhalb der noch festzulegenden Obergrenze. | Der Zielhebel liegt außerhalb des erlaubten Bereichs. |
| `INVALID_LONG_BARRIER` | Für eine aktive Long-Konfiguration gilt `KO >= S` am maßgeblichen Prüfkurs. | Die Barriere liegt nicht auf der aktiven Long-Seite; KO-Status zusätzlich prüfen. |
| `INVALID_SHORT_BARRIER` | Für eine aktive Short-Konfiguration gilt `KO <= S` am maßgeblichen Prüfkurs. | Die Barriere liegt nicht auf der aktiven Short-Seite; KO-Status zusätzlich prüfen. |
| `PRODUCT_ALREADY_KNOCKED_OUT` | Eine Barrierenberührung oder ein bereits erfolgter KO ist bekannt. | Das Produkt darf nicht als aktiv bewertet werden. |
| `CURRENT_PRICE_MISSING` | `S_current` fehlt in einer aktuellen Bewertung. | Aktuelle Berechnung nicht möglich. |
| `ENTRY_PRICE_MISSING` | `S_entry` fehlt in einer Einstiegsplanung. | Einstiegsberechnung nicht möglich. |
| `CURRENCY_MISSING` | Basiswert- oder Produktwährung fehlt. | Währungsbezug und gegebenenfalls Umrechnung sind unklar. |
| `BASIS_PRICE_MISSING` | `B` fehlt für inneren Wert oder Modellpreis. | Produktwert kann nicht fachlich korrekt berechnet werden. |
| `KNOCKOUT_BARRIER_MISSING` | `KO` fehlt für Abstand oder Status. | KO-Risiko kann nicht bewertet werden. |

Ein Nullwert ist nur dort zulässig, wo er fachlich eine echte Größe beschreibt, beispielsweise nichtnegative Gebühren oder ein berechneter innerer Wert. Er darf niemals als Ersatz für einen fehlenden Pflichtwert dienen.

## 17. Offene fachliche Entscheidungen

Jeder folgende Punkt ist vor seiner Implementierung fachlich zu spezifizieren und durch Tests abzusichern:

- **OFFEN – vor Implementierung verbindlich entscheiden:** Restwert und Abrechnungsverfahren nach einem KO.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Beobachtungszeiten, Beobachtungsart und relevante Handelsplätze für die KO-Barriere.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Ermittlung, Fortschreibung und Vorzeichen der Finanzierungskosten.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Definition, Ermittlung und Darstellung des Aufgelds.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Modellierung und Zuordnung des Spreads zu Bid und Ask.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Produktspezifische Tick-Sizes und Rundungsverfahren.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Gebührenmodelle für Kauf, Verkauf, Mindestentgelt und variable Gebühren.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Numerische und fachliche Toleranz bei Barrieregleichheit.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Zulässiger maximaler Zielhebel und Verhalten an der Obergrenze.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Verwendung von `Double` oder dezimalgenauen Typen je Berechnungs- und Geldwert.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Risikoklassifizierung und zugehörige Schwellenwerte beziehungsweise Warntexte.
- **OFFEN – vor Implementierung verbindlich entscheiden:** Verbindliche Datenquellen, Aktualitätsgrenzen, Währungspaare und Zeitstempel.

Keine dieser offenen Entscheidungen darf durch eine unmarkierte Annahme in der Implementierung vorweggenommen werden.

## 18. Verbindliche Testfälle

Die Zahlen in dieser Tabelle sind fachliche Sollwerte ohne vorzeitige Rundung. „Vereinfachtes Modell“ bedeutet jeweils `Financing = 0` und `Premium = 0`. Kotlin-Testcode ist in diesem Dokument noch nicht erforderlich.

| Testfall | Eingaben | Erwarteter Status | Erwartete Kernaussage |
|---|---|---|---|
| Long Standardfall | Long; `S = 100`; `B = 80`; `KO = 80`; `R = 0,1`; `FX = 1` | `ACTIVE` | `D_abs = 20`, `D_pct = 20 %`, `Intrinsic = P_model = 2` |
| Short Standardfall | Short; `S = 100`; `B = 120`; `KO = 120`; `R = 0,1`; `FX = 1` | `ACTIVE` | `D_abs = 20`, `D_pct = 20 %`, `Intrinsic = P_model = 2` |
| Long exakt an KO | Long; `S = KO = 80`; keine frühere Berührung bekannt | `AT_KNOCKOUT` | Momentaufnahme liegt innerhalb der festzulegenden Toleranz an der Barriere; kein normaler Hebel |
| Short exakt an KO | Short; `S = KO = 120`; keine frühere Berührung bekannt | `AT_KNOCKOUT` | Momentaufnahme liegt innerhalb der festzulegenden Toleranz an der Barriere; kein normaler Hebel |
| Long unter KO | Long; `S = 79`; `KO = 80` | `KNOCKED_OUT` | `D_abs = -1`; negativer Abstand wird nicht verborgen |
| Short über KO | Short; `S = 121`; `KO = 120` | `KNOCKED_OUT` | `D_abs = -1`; negativer Abstand wird nicht verborgen |
| Gleiche Währung | Long; Basiswert EUR; Produkt EUR; `FX = 1`; Basiswährungswert `2` | `ACTIVE` | Produktwährungswert bleibt `2 EUR` |
| USD-Basiswert und EUR-Produkt | Basiswert USD; Produkt EUR; `FX = 1,10 USD/EUR`; Basiswährungswert `2,20 USD` | `ACTIVE` | Produktwährungswert ist `2,00 EUR` |
| Ratio `0,01` | Long; `S = 100`; `B = 80`; `KO = 80`; `R = 0,01`; `FX = 1` | `ACTIVE` | `Intrinsic = P_model = 0,20`; Ratio wird genau einmal angewendet |
| Sehr hoher Hebel | Long; `S_entry = 100`; `L_target = 1.000` | Abhängig von offener Obergrenze | Theoretisch `KO = 99,90`; Grenzregel darf nicht stillschweigend angenommen werden |
| Ungültiger Hebel | `L_target = 1` | `INVALID_CONFIGURATION` | Fehler `TARGET_LEVERAGE_OUT_OF_RANGE`; keine Ersatzbarriere `0` |
| `NaN` | Ein erforderlicher numerischer Eingang ist `NaN` | `INVALID_CONFIGURATION` | Fehler `NON_FINITE_NUMBER`; keine Berechnung |
| `Infinity` | Ein erforderlicher numerischer Eingang ist `+Infinity` oder `-Infinity` | `INVALID_CONFIGURATION` | Fehler `NON_FINITE_NUMBER`; keine Berechnung |
| Fehlender aktueller Kurs | Aktuelle Bewertung ohne `S_current` | `DATA_UNAVAILABLE` | Fehler `CURRENT_PRICE_MISSING`; fehlender Wert wird nicht `0` |
| Geplanter Limit-Einstieg | Long; `S_current = 100`; `S_entry = 90`; bestehend `B = 78`, `KO = 80`, `R = 0,1`, `FX = 1` | `ACTIVE` am Einstieg | Aktuelle und geplante Werte bleiben getrennt; am Einstieg `D_abs = 10`, `P_model = 1,20` |
| Zukünftiger Hebel eines bestehenden Produkts | Daten wie Limit-Einstieg; `P_model_entry = 1,20` | `ACTIVE` am Einstieg | `L_actual_entry = 7,50`; feste Barriere bleibt `80` |
| Zielkurs mit möglichem KO auf dem Weg | Long; Start `S = 100`; `KO = 80`; Ziel `S_target = 110`; keine Pfaddaten | `DATA_UNAVAILABLE` für Pfadaussage | Ziel-Endwert kann aktiv sein, schließt einen zwischenzeitlichen KO aber nicht aus; KO-Warnung erforderlich |

Zusätzliche Tests sind für negative Werte, `0`, fehlende Währungen, getrennte Basispreise und Barrieren, Bid/Ask, Gebühren sowie alle später festgelegten offenen Fachentscheidungen erforderlich.

## 19. Implementierungsreihenfolge

1. Fachkonventionen bestätigen
2. Typsichere Modelle
3. Validierung
4. KO-Status
5. KO-Abstand
6. Innerer Wert
7. Währungsumrechnung
8. Modellpreis
9. Tatsächlicher Hebel
10. Zukünftiger Hebel am Limit-Einstieg
11. P&L
12. Zielkursszenarien
13. UI-Anbindung
14. Bereinigung alter Rechenpfade

Jeder Schritt ist mit Long- und Short-Tests sowie Grenz- und Fehlerfällen abzuschließen, bevor der nächste Rechenpfad verbindlich darauf aufbaut.

## 20. Rechtlicher und fachlicher Hinweis

Alle Berechnungen sind Planungs- und Informationshilfen. Sie sind keine Anlageberatung und keine Kauf- oder Verkaufsempfehlung.

Reale Produktbedingungen, Emittentenpreise, Handelszeiten, Finanzierungskosten, Spreads, Wechselkurse und KO-Regeln können von den vereinfachten Modellen abweichen. Vor einer Order müssen die offiziellen und aktuellen Produktunterlagen des Emittenten geprüft werden.
