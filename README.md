# QMBorderGuard

World borders made of a polygon you walk out yourself. Place at least three nodes around an area and
the region becomes active: players get stopped at the edge with a puff of smoke, and staff who turned
the preview on see the whole outline drawn in particles. Regions are per world, so the nether and the
end can have completely different shapes.

No dependencies. Paper / Spigot 1.12.2, Java 8.

## Quick start

```
/border wand
```

| Action | Effect |
| --- | --- |
| right click | add a node at your feet |
| left click | remove the last node |
| sneak + right click | toggle the border preview |

Walk around the area you want to enclose, clockwise or counter-clockwise, and the region switches on
once the third node is placed. Concave shapes work: the polygon is the node order, so do not zigzag
back across yourself.

Without the wand: `/border add` and `/border undo`.

## Commands

| Command | Does |
| --- | --- |
| `/border help` | list everything |
| `/border add` / `undo` | node at your feet, undo the last one |
| `/border remove <n>` | delete node n, counting from 1 |
| `/border insert <n>` | insert the node at your feet as node n |
| `/border list [world]` | nodes with their coordinates |
| `/border clear` | drop every node of this world |
| `/border tp <n>` | teleport to a node |
| `/border on` / `off` | enable or disable the border of this world |
| `/border min <n>` | nodes required before the region activates |
| `/border y <low> <high>` | only intercept inside this height range |
| `/border y full` | no height limit (default) |
| `/border spawn` | set the rescue point at your feet |
| `/border toggle` | your own preview, on or off |
| `/border bypass` | ignore the border yourself, for building |
| `/border info [world]` | node count, y range, rescue mode |
| `/border reload` | reload config, messages and region files |

Aliases: `/bg`, `/boundary`, `/bianjie`, `/边界`.

Permissions: `borderguard.admin` (op), `borderguard.bypass` (nobody by default).

## Files

```
plugins/BorderGuard/
├── config.yml            settings, wand, rendering, effects
├── messages.yml          every message, with {0} {1} placeholders
├── debug.yml             who has the preview on, maintained by the plugin
└── regions/
    ├── world.yml
    ├── world_nether.yml
    └── world_the_end.yml
```

Each region file holds the node list, the y range and whether the border is enabled. Nodes are stored
as plain coordinates rather than world references, so a world that is not loaded never loses its
data.

## How the interception works

`settings.rescue` decides what happens to a player who is already outside:

- `PUSH` pulls them back towards the nearest inside point, `rescue-speed` blocks per tick. 0.9 holds
  up against a sprint.
- `SPAWN` teleports them to the rescue point set with `/border spawn`.
- `NONE` only blocks crossings from inside to outside, which is useful when the border is decorative.

Movement is handled per axis, so a player running into the border slides along it instead of stopping
dead. Ender pearls and similar teleports are checked as well (`teleport-guard`), and players who log
in outside the region are sent back (`rescue-on-join`).

Creative flight and spectators are not exempt by default; `settings.ignore-gamemodes` is where to add
them, and `ignore-ops` decides whether operators are stopped too. Turning that on is handy while
building, which is what `/border bypass` is for.

## Rendering

The line is only sent to players with the preview on, refreshed every `render.interval-ticks` and
only within `render.radius` blocks. `render.step` is the distance between particles, so lowering it
thickens the line at the cost of packets. Node markers are drawn as pillars of `node-pillar` blocks.

Effects on touch live under `effects`: a list of particle groups with count, offset and speed, plus a
sound. Nothing there is hardcoded, so replacing the smoke with flame or portal particles is a config
edit.

## Build

```bash
mkdir -p lib   # spigot-api.jar (the patched 1.12.2 server jar works too)
bash build.sh
# or: MC_SERVER=/path/to/server bash build.sh install
```

Output: `build/BorderGuard.jar`.
