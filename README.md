<div align="center">
  <h1>Tourneys</h1>
  <img alt="License" src="https://img.shields.io/github/license/xasciii/Tourneys">
  <img alt="GitHub Release" src="https://img.shields.io/github/release/xasciii/Tourneys.svg">
  <br><br>
  <a href="https://github.com/xasciii/Tourneys/releases/latest"><img alt="Download" src="https://img.shields.io/badge/-Download-blue?style=for-the-badge&logo=github"></a>
  <a href="https://discord.gg/ZmdNWv8vW6"><img alt="Discord" src="https://img.shields.io/badge/-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white"></a>
</div>

## Overview

Tourneys is a lightweight Paper tournament plugin for configurable team-based Minecraft events. It handles registration, team creation, invites, brackets, arenas, match flow, kits, spectators, byes, and cleanup.

The default configuration targets a 2v2 NethPot event, but the plugin is not locked to that format. Team size, kits, arenas, messages, and most tournament behaviour can be changed through `config.yml`.

Tourneys is intended for empty event servers or dedicated tournament worlds where staff control the full player experience.

## Table of Contents

- [Showcase](#showcase)
- [Supported Platforms & Versions](#supported-platforms--versions)
- [Features](#features)
- [Commands](#commands)
- [Permission Nodes](#permission-nodes)
- [Installation](#installation)
- [Arena Setup](#arena-setup)
- [Kit Setup](#kit-setup)
- [Configuration](#configuration)
- [PlaceholderAPI](#placeholderapi)
- [Credits](#credits)
- [License](#license)

## Showcase
<img width="1810" height="661" alt="image" src="https://github.com/user-attachments/assets/7025a13e-1bc0-4045-8929-670c916986b2" />
<img width="560" height="95" alt="image" src="https://github.com/user-attachments/assets/5f013e5b-e4c9-4c7f-b0a9-0254aad9ae0f" />



## Supported Platforms & Versions

| Platform | Supported Versions |
|---|---|
| Paper and compatible forks | 1.21+ |
| Spigot | Where Paper API features are available |

## Features

- **Configurable Team Size** - Supports 1v1, 2v2, 3v3, squads, and other team sizes.
- **Configurable Kits** - Save your current inventory as the tournament kit with `/tourney kit save`.
- **Arena System** - Each arena has Team A, Team B, and spectator spawns.
- **Randomised Brackets** - Closing registration generates randomised matchups automatically.
- **Byes** - Odd team counts automatically advance one team.
- **Parallel Matches** - Run one match at a time, or multiple if enough arenas exist.
- **Bracket GUI** - Optional inventory GUI showing waiting, active, and finished matches.
- **External Bracket URL** - Optional broadcast after registration closes.
- **Snapshot API** - Optional `snapshot.json` output for overlays or websites.
- **PlaceholderAPI Support** - Team tag placeholders for TAB and other plugins.
- **Spectator Handling** - Eliminated players moved to a spectator spawn.
- **Rejoin Support** - Players can rejoin active matches if enabled.
- **Configurable Messages** - Full MiniMessage support for messages, titles, and actionbars.
- **Runtime Reload** - Safely reload config when no tournament is running.

## Commands

- `/tourney open` - Open registration.
- `/tourney close` - Close registration and generate the bracket.
- `/tourney start` - Start tournament matches.
- `/tourney cancel` - Cancel the active tournament.
- `/tourney restart` - Reset the current event and reopen registration.
- `/tourney status` - Show current tournament state.
- `/tourney reload` - Reload config when no tournament is running.
- `/tourney bracket` - Open the bracket GUI or print bracket info to console.
- `/tourney kit save` - Save your current inventory as the tournament kit.
- `/tourney team create [name]` - Create a team during registration.
- `/tourney team invite <player>` - Invite a player to your team.
- `/tourney team accept <team>` - Accept a team invite.
- `/tourney team leave` - Leave or disband your team during registration.
- `/tourney team list` - List all registered teams.
- `/tourney arena create <name>` - Create an arena.
- `/tourney arena setspawn <arena> teamA|teamB|spectator` - Set an arena spawn.
- `/tourney arena delete <name>` - Delete an arena.
- `/tourney arena list` - List arenas and their readiness.

## Permission Nodes

Operators have admin permissions by default. All nodes are configurable in `config.yml`.

- `tourney.admin` - Full tournament administration.
- `tourney.admin.open` - Access to `/tourney open`.
- `tourney.admin.close` - Access to `/tourney close`.
- `tourney.admin.start` - Access to `/tourney start`.
- `tourney.admin.cancel` - Access to `/tourney cancel`.
- `tourney.admin.restart` - Access to `/tourney restart`.
- `tourney.admin.reload` - Access to `/tourney reload`.
- `tourney.admin.arena` - Access to arena management commands.
- `tourney.admin.kit` - Access to `/tourney kit save`.
- `tourney.player` - Base player access.
- `tourney.player.team` - Access to team commands.
- `tourney.player.bracket` - Access to `/tourney bracket`.

## Installation

1. **Download**: Get the latest release from the [GitHub releases page](https://github.com/xasciii/Tourneys/releases/latest).
2. **Install**: Place the plugin JAR in your server's `plugins/` directory.
3. **Start**: Boot the server to generate config files.
4. **Configure**: Edit `config.yml` — set lobby spawn, spectator spawn, team size, and match settings.
5. **Reload**: Apply changes with `/tourney reload`.
6. **Arenas**: Create arenas in-game before running your first event.

## Arena Setup

Each arena requires three spawns before it is considered ready:

```
/tourney arena create arena1
/tourney arena setspawn arena1 teamA
/tourney arena setspawn arena1 teamB
/tourney arena setspawn arena1 spectator
/tourney arena list
```

If `matches.max-active-matches` is `1`, one ready arena is sufficient. For parallel matches, create additional arenas and raise `max-active-matches` accordingly.

## Kit Setup

<!-- Add an image of a filled-out kit inventory here if you have one -->
<!-- Recommended: docs/kit-example.png -->

1. Fill your inventory, hotbar, armor slots, and offhand with the exact match kit.
2. Run `/tourney kit save`.
3. The kit is saved to `kit.yml`.

You can also edit `kit.yml` manually. The default file includes examples for materials, slots, enchantments, and potion effects.

## Configuration

Key config paths:

- `tournament.display-name` - Display name used in messages.
- `tournament.type-name` - Format label, e.g. `2v2 NethPot` or `1v1 Sword`.
- `team.size` - Players per team.
- `team.minimum-teams` - Minimum teams required to close registration.
- `team.maximum-teams` - Maximum number of teams.
- `team.allow-incomplete-teams-on-close` - Allow teams smaller than `team.size`.
- `matches.max-active-matches` - Maximum simultaneous matches.
- `matches.countdown-before-match-starts` - Delay before a match becomes active.
- `matches.delay-after-match-ends` - Delay after a match ends.
- `matches.tie-behavior` - `teamA`, `teamB`, `random`, or `rematch`.
- `matches.give-kit-on-match-start` - Give the configured kit at match start.
- `blocked-commands` - Commands blocked during active tournaments.
- `bracket.inventory.enabled` - Enable or disable the bracket GUI.
- `bracket.inventory.auto-rows` - Grow GUI rows dynamically up to the configured max.
- `bracket.inventory.materials` - Items for each match state.

Extra files:

- `language.yml` - Prefix, messages, titles, and actionbars.
- `kit.yml` - Saved or manually configured tournament kit.
- `api.yml` - Snapshot output, external bracket URL, and PlaceholderAPI settings.

## PlaceholderAPI

If PlaceholderAPI is installed and `placeholders.enabled` is `true` in `api.yml`, Tourneys registers these placeholders:

- `%tourneys_team%` - Team tag, e.g. `&a[43]`.
- `%tourneys_nametag%` - Tag formatted for nametags, e.g. `&a[43] `.
- `%tourneys_team_plain%` - Team tag without colour codes.
- `%tourneys_team_color%` - Team colour code.
- `%tourneys_team_number%` - Random team number.
- `%tourneys_in_team%` - `true` or `false`.

For TAB, place `%tourneys_nametag%` in the player name or group prefix where you want the tag to appear.

## Credits

**Maintainer: [asc](https://github.com/xasciii)**

Built as a standalone Kotlin Paper plugin for configurable Minecraft tournament events.

## License

This project is licensed under the [GPL3 License](LICENSE).
