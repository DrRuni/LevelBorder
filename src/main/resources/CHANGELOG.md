# Changelog

## v1.0

### Neu

- Erste Veröffentlichung von LevelBorder
- Unterstützung für Paper 1.21.10 hinzugefügt

---

## v1.1

### Neu

- Eigenes Mob-Spawning-System für die LevelBorder Challenge hinzugefügt
- Konfigurierbares Mob-Spawning über die Plugin-Konfiguration hinzugefügt
- Border-bewusstes Mob-Spawning-Verhalten hinzugefügt
- Unterstützung für Mob-Spawning hinter bzw. außerhalb der aktuellen Challenge-Border hinzugefügt

### Geändert

- Projekt auf die neue Paper-API-Version 26.1.2 aktualisiert
- Projekt auf Java 25 aktualisiert
- Mob-Spawning-Verhalten für kleine WorldBorder-Challenges verbessert

### Hinweis

In Minecraft können Entities normalerweise nicht natürlich außerhalb der WorldBorder spawnen.  
Da dies bei kleinen LevelBorder-Challenges das Mob-Spawning stark einschränken kann, fügt Version 1.1 ein eigenes Mob-Spawning-System hinzu, damit die Challenge spielbar bleibt und sich trotzdem möglichst nah am Vanilla-Verhalten orientiert.

---

## v1.1.1

- Code bereinigt und API-Warnungen reduziert

---

## v1.2

- Paper-API auf Version 26.2-alpha aktualisiert
- Portallogik deutlich verbessert
- Reset- und Start-Ablauf verbessert
- Scoreboard-Spielerliste korrigiert
- Optionen-GUI hinzugefügt
- Neuer Befehl /optionen hinzugefügt
- /levelborder optionen als zusätzlicher Zugriff auf das Optionen-Menü hinzugefügt
- Optionen-Menü auf OP-Spieler beschränkt
- KeepInventory kann nun über das Optionen-Menü umgeschaltet werden
- LevelBorder kann über das Optionen-Menü gestartet, gestoppt, zurückgesetzt und zentriert werden
- Bordergröße kann im Optionen-Menü per Linksklick verkleinert und per Rechtsklick vergrößert werden
- Border-Mitte wird nun direkt nach dem Setzen aktualisiert
- Kleinere Codebereinigungen und API-Anpassungen