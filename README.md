# BlueArcade - Run From The Beast

This resource is a **BlueArcade 3 module** and requires the core plugin to run.
Get BlueArcade 3 here: https://store.blueva.net/resources/resource/1-blue-arcade/

## Description
Escape the Beast, gear up from chests, and fight back together.

## Game type notes
This is a **Minigame**: it is designed for standalone arenas, but it can also be used inside party rotations. Minigames usually provide longer, feature-rich rounds.

## What you get with BlueArcade 3 + this module
- Party system (lobbies, queues, and shared party flow).
- Store-ready menu integration and vote menus.
- Victory effects and end-game celebrations.
- Scoreboards, timers, and game lifecycle management.
- Player stats tracking and placeholders.
- XP system, leaderboards, and achievements.
- Arena management tools and setup commands.

## Features
- Beast spawn point and beast zone setup.
- Loot-driven pacing from arena chests.
- Asymmetrical hunter vs. runners gameplay.

## Arena setup
### Common steps
Use these steps to register the arena and attach the module:

- `/baa create [id] <standalone|party>` — Create a new arena in standalone or party mode.
- `/baa arena [id] setname [name]` — Give the arena a friendly display name.
- `/baa arena [id] setlobby` — Set the lobby spawn for the arena.
- `/baa arena [id] minplayers [amount]` — Define the minimum players required to start.
- `/baa arena [id] maxplayers [amount]` — Define the maximum players allowed.
- `/baa game [arena_id] add [minigame]` — Attach this minigame module to the arena.
- `/baa stick` — Get the setup tool to select regions.
- `/baa game [arena_id] [minigame] bounds set` — Save the game bounds for this arena.
- `/baa game [arena_id] [minigame] spawn add` — Add spawn points for players.
- `/baa game [arena_id] [minigame] time [minutes]` — Set the match duration.

### Module-specific steps
Finish the setup with the commands below:
- `/baa game [arena_id] run_from_the_beast beastspawn set` — Set the Beast spawn point.
- `/baa game [arena_id] run_from_the_beast beastzone set` — Select and save the Beast zone.

## Technical details
- **Minigame ID:** `run_from_the_beast`
- **Module Type:** `MINIGAME`

## Hytale edition notes
- The Hytale edition requires downloading HytaleServer from https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual. It is not included because it requires authentication.

## Building individual editions
If you only need one edition, you can build it on its own:
- `mvn clean package -pl runfromthebeast-minecraft -am`
- `mvn clean package -pl runfromthebeast-hytale -am`

## Links & Support
- Website: https://www.blueva.net
- Documentation: https://docs.blueva.net/books/blue-arcade
- Support: https://discord.com/invite/CRFJ32NdcK
