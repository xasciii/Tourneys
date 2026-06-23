# Tourneys

## Overview

Tourneys is a lightweight Paper tournament plugin for configurable team-based Minecraft events. It handles registration, team creation, invites, brackets, arenas, match flow, kits, spectators, byes, winners, and tournament cleanup.

The default configuration is suitable for a 2v2 NethPot event, but the plugin is not locked to that format. Team size, kits, arenas, active matches, messages, titles, bracket display, and most tournament behavior can be changed through `config.yml`.

Tourneys is intended for empty event servers or dedicated tournament worlds where staff control the full player experience.

## Table of Contents

- Supported Platforms & Versions
- Features
- Tournament Flow
- Commands
- Permission Nodes
- Installation
- Arena Setup
- Kit Setup
- Configuration
- Recommended Event Server Setup
- Credits
- License

## Supported Platforms & Versions

| Platform | Supported Versions |
| --- | --- |
| Paper and compatible forks | 1.21+ |
| Spigot | Should work where the Paper API features used by the plugin are available |

## Features

- Configurable Team Size - Supports 1v1, 2v2, 3v3, squads, and other team sizes.
- Configurable Kits - Admins can save their current inventory as the tournament kit using `/tourney kit save`.
- Arena System - Each arena has Team A, Team B, and spectator spawns.
- Randomized Brackets - Registration close generates randomized matchups.
- Byes - Odd team counts automatically advance one team.
- Parallel Matches - Run one match at a time by default, or multiple matches if enough arenas exist.
- Bracket GUI - Optional inventory GUI showing waiting, active, and finished matches.
- External Bracket URL - Optional broadcast after registration closes.
- Snapshot API File - Optional `snapshot.json` output for overlays or websites.
- PlaceholderAPI Support - Optional placeholders for TAB nametag prefixes.
- Spectator Handling - Dead or eliminated players can be moved to spectator mode and spectator spawn.
- Rejoin Support - Players can rejoin active matches if enabled.
- Configurable Messages - Messages, titles, and actionbars are configurable with MiniMessage styling.
- Runtime Reload - Reload config safely when no tournament is running.

## Tournament Flow

1. Admin opens registration.
2. Players create teams.
3. Captains invite teammates.
4. Players accept invites.
5. Players can leave or disband teams during registration.
6. Admin closes registration.
7. Tourneys generates a randomized bracket.
8. Admin starts the tournament.
9. Matches run using available arenas.
10. Winners advance each round.
11. Odd team counts receive byes.
12. Tournament ends when one team remains.
13. Players return to lobby after the configured delay.

## Commands

- `/tourney open` - Open registration.
- `/tourney close` - Close registration and generate the bracket.
- `/tourney start` - Start tournament matches.
- `/tourney cancel` - Cancel the active tournament.
- `/tourney status` - Show current tournament state.
- `/tourney reload` - Reload config when no tournament is running.
- `/tourney bracket` - Open the bracket GUI or print bracket info to console.
- `/tourney kit save` - Save the admin player's current inventory as the tournament kit.
- `/tourney team create` - Create a team during registration.
- `/tourney team create <name>` - Create a named team if custom names are enabled.
- `/tourney team invite <player>` - Invite a player to your team.
- `/tourney team accept <team>` - Accept a team invite.
- `/tourney team leave` - Leave or disband your team during registration.
- `/tourney team list` - List registered teams.
- `/tourney arena create <name>` - Create an arena.
- `/tourney arena setspawn <arena> teamA` - Set Team A spawn.
- `/tourney arena setspawn <arena> teamB` - Set Team B spawn.
- `/tourney arena setspawn <arena> spectator` - Set spectator spawn.
- `/tourney arena delete <name>` - Delete an arena.
- `/tourney arena list` - List arenas and readiness.

## Permission Nodes

Admin permissions are configurable in `config.yml`.

Operators have admin permissions by default through `plugin.yml`.

- `tourney.admin` - Full tournament administration.
- `tourney.admin.open` - Access to `/tourney open`.
- `tourney.admin.close` - Access to `/tourney close`.
- `tourney.admin.start` - Access to `/tourney start`.
- `tourney.admin.cancel` - Access to `/tourney cancel`.
- `tourney.admin.reload` - Access to `/tourney reload`.
- `tourney.admin.arena` - Access to arena management commands.
- `tourney.admin.kit` - Access to `/tourney kit save`.
- `tourney.player` - Base player access.
- `tourney.player.team` - Access to team commands.
- `tourney.player.bracket` - Access to `/tourney bracket`.

## Installation

1. Download or build the plugin jar.
2. Place the jar in your server's `plugins` directory.
3. Start the server once to generate the Tourneys config files.
4. Stop the server or use `/tourney reload` later after editing config.
5. Configure lobby spawn, fallback spectator spawn, team size, messages, kit, API, and match settings.
6. Start the server and create arenas in-game.

## Arena Setup

Each arena requires three spawns:

- Team A spawn
- Team B spawn
- Spectator spawn

Example setup:

```text
/tourney arena create arena1
/tourney arena setspawn arena1 teamA
/tourney arena setspawn arena1 teamB
/tourney arena setspawn arena1 spectator
/tourney arena list
```

An arena is only ready when all three spawns are set.

If `matches.max-active-matches` is `1`, you only need one ready arena. If you want multiple matches at the same time, create multiple ready arenas and increase `matches.max-active-matches`.

## Kit Setup

The recommended way to create a kit is in-game:

1. Join as an admin.
2. Fill your inventory, hotbar, armor slots, and offhand with the exact match kit.
3. Run `/tourney kit save`.
4. The plugin saves the inventory into `kit.yml`.
5. Start matches normally.

You can also edit `kit.yml` manually. The default file includes readable examples for materials, slots, enchantments, and potion effects.

## Configuration

Important config paths:

- `tournament.display-name` - Display name used in messages.
- `tournament.type-name` - Format name, such as `2v2 NethPot`, `1v1 Sword`, or `4v4 UHC`.
- `team.size` - Players per team.
- `team.minimum-teams` - Minimum teams required before registration can close.
- `team.maximum-teams` - Maximum number of teams.
- `team.allow-incomplete-teams-on-close` - Allow teams smaller than `team.size`.
- `matches.max-active-matches` - Maximum simultaneous matches.
- `matches.countdown-before-match-starts` - Delay before a match becomes active.
- `matches.delay-after-match-ends` - Delay after a match ends.
- `matches.tie-behavior` - `teamA`, `teamB`, `random`, or `rematch`.
- `matches.give-kit-on-match-start` - Give the configured kit at match start.
- `blocked-commands` - Commands blocked during running tournaments.
- `bracket.inventory.enabled` - Enable or disable the bracket GUI.
- `bracket.inventory.auto-rows` - Let the GUI grow by rows as matches are added, up to the configured max rows.
- `bracket.inventory.materials` - Configure waiting, countdown, active, and finished match items.

Extra files:

- `language.yml` - Prefix, messages, titles, and actionbars.
- `kit.yml` - Saved or manually configured tournament kit.
- `api.yml` - Snapshot output, external bracket URL, and PlaceholderAPI settings.
- `api.yml` `bracket-url.enabled` - Broadcast an external bracket URL.
- `api.yml` `snapshot.enabled` - Write `snapshot.json` for overlays or websites.
- `api.yml` `placeholders.enabled` - Enable PlaceholderAPI placeholders for TAB or other plugins.
- `api.yml` `placeholders.nametag-format` - Format returned by `%tourneys_nametag%`.

## PlaceholderAPI

If PlaceholderAPI is installed and `placeholders.enabled` is `true`, Tourneys registers these placeholders:

- `%tourneys_team%` - Team tag, for example `&a[43]`.
- `%tourneys_nametag%` - Team tag formatted for nametags, for example `&a[43] `.
- `%tourneys_team_plain%` - Team tag without color codes.
- `%tourneys_team_color%` - Team color code.
- `%tourneys_team_number%` - Random team number.
- `%tourneys_in_team%` - `true` or `false`.

For TAB, put `%tourneys_nametag%` next to the player name or in the group/player prefix where you want the team tag to appear.

## Recommended Event Server Setup

Tourneys works best on a clean event server.

Recommended setup:

- Use a dedicated tournament server or world.
- Disable unrelated gameplay systems during the event.
- Build a lobby for registration and waiting.
- Build one or more arenas.
- Build spectator areas that cannot interfere with active players.
- Set lobby and fallback spectator spawn in `config.yml`.
- Use `/tourney kit save` after preparing the exact event kit.
- Test a full mock bracket before the real event.

## Credits

Maintainer: ASC

Built as a standalone Kotlin Paper plugin for configurable Minecraft tournament events.

## License

No license has been specified yet.
