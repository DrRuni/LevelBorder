# 📏 LevelBorder Challenge – WorldBorder Plugin

**LevelBorder Challenge** is a server-side challenge plugin for **Paper-based servers**, inspired by the popular **Level = Border** challenge known from BastiGHG.

The challenge is simple:  
the shared WorldBorder grows based on the total amount of collected player levels.

All players contribute to one global level count.  
The more experience levels the players collect, the bigger the border becomes.

This makes progression, exploration and survival directly connected to player XP.

---

## ⭐ Features

- Global WorldBorder shared across Overworld, Nether and End
- Dynamic level-based border expansion
- Smooth border growth on level progress
- Multiplayer-ready challenge system
- Separate challenge timer
- Toggleable scoreboard system
- Persistent data storage across server restarts
- Admin controls for starting, stopping and resetting the challenge
- Configurable border center
- OP-only options GUI
- KeepInventory toggle through the options GUI
- Border size control through the options GUI
- 1:1 Nether border handling
- Custom mob spawning system for small WorldBorder challenges
- Border-aware monster spawning outside the current challenge border
- Paper-optimized implementation

---

## 🌍 Dimension Handling

- Overworld, Nether and End share one global border size
- Nether coordinate scaling is adjusted from the usual **1:8** behavior to a **1:1** style setup
- The challenge feels consistent across all dimensions while staying close to vanilla behavior

---

## ⚙️ Compatibility

- Requires **Paper 26.2-alpha**
- Tested with **Paper 26.2-alpha**
- Requires **Java 25**
- Paper-based servers only
- Not compatible with pure Spigot/Bukkit
- Not compatible with Vanilla servers

---

## ⌨️ Commands

### Admin / OP commands

```text
/optionen
/levelborder optionen
/levelborder start
/levelborder stop
/levelborder set <size>
/levelborder reset
/levelborder center
```

### Player commands

```text
/lbscore hide
/lbscore reload
```

### General commands

```text
/levelborder
/levelborder info
/levelborder score
```

---

## 📥 Installation

1. Put the `.jar` file into your server's `/plugins` folder.
2. Start or restart the server.
3. Run `/levelborder start`.
4. Begin the challenge.

---

## ⚠️ Disclaimer

This is an unofficial fan project inspired by the Level = Border challenge known from BastiGHG.

LevelBorder Challenge was developed independently and is not affiliated with, endorsed by, partnered with or officially connected to BastiGHG.

---

## 💬 Notes

This plugin was created as part of my journey as a developer.  
Feedback, bug reports and suggestions are always welcome.

---

## 📄 License

All rights reserved.

This plugin may be downloaded and used on private or public Minecraft servers.

You may not modify, decompile, redistribute, reupload, resell or publish modified versions of this plugin without explicit permission from the author.