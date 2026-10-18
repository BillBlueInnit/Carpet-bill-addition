# Carpet Bill Addition

<img src="src/main/resources/assets/carpet-bill-addition/icon.png" width="96" align="right" alt="icon">

**English** | [简体中文](README.zh-CN.md)

A [Fabric Carpet](https://github.com/gnembon/fabric-carpet) extension for technical Minecraft, for
Minecraft 1.21 up to the latest release. Every feature is a Carpet rule, off by default, and can be
toggled with `/carpet`.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric Carpet](https://github.com/gnembon/fabric-carpet/releases)
   for your Minecraft version.
2. Put the Carpet Bill Addition jar for your version (see [Supported versions](#supported-versions)) into `mods`.

The mod is only needed on the server (or on the client for singleplayer). Fabric API is not required.

## Rules

| Rule | Default | Options | Minecraft |
| --- | --- | --- | --- |
| [`reintroduceIllegalBlockEntity`](#reintroduceillegalblockentity) | `false` | `true`, `false` | 1.21+ |
| [`legacyThrowableMovement`](#legacythrowablemovement) | `off` | `off`, `pearl`, `all` | 1.21.2+ |

### reintroduceIllegalBlockEntity

Reintroduces **illegal block entities** (a block entity sitting under a block of a different type),
which Minecraft 1.21.1 removed, so that **sound suppression** works again.

- Categories: `bill`, `experimental`, `survival`

```
/carpet reintroduceIllegalBlockEntity true
/carpet setDefault reintroduceIllegalBlockEntity true
```

What it restores:

| Change in vanilla | Version | With the rule on |
| --- | --- | --- |
| `BlockEntity` throws when it is created with, or given, the block state of another block, so illegal block entities are deleted when their chunk loads | 1.21.1 | Not validated, like 1.21 |
| `LevelChunk.setBlockState` deletes a block entity whose type does not match the new block | 1.21.1 | Kept and given the new block state, like 1.21 |
| Sculk sensors, lecterns, shulker boxes and chiseled bookshelves delete their block entity *before* sending the neighbor updates of their removal | 1.21.1 | Updates first, block entity deleted afterwards, like 1.21 |
| Every block entity is deleted before `affectNeighborsAfterRemoval` (the old `onRemove` was split up) | 1.21.5 | Neighbor updates of the removed block run first, the block entity is deleted afterwards |

The last two rows are what make illegal block entities buildable in survival: when the neighbor updates of a
removed block are update-suppressed, the exception aborts the replacement before the block entity is deleted,
so it stays behind under the new block.

With an illegal sculk sensor (or calibrated sculk sensor) block entity under, for example, a chest,
the sensor reads the `sculk_sensor_phase` property of the chest as soon as it hears a vibration,
which throws `IllegalArgumentException`. That exception is the sound suppressor.

Notes:

- Turn the rule on (`setDefault` is easiest) **before** loading chunks that contain old suppressors.
  With the rule off, 1.21.1+ deletes illegal block entities when their chunk loads, and that cannot be undone.
- On Minecraft 1.21 the rule does nothing because vanilla still has this behavior.
- Since 1.21.5 removal neighbor updates are only sent when the block is replaced with block updates
  (breaking it, pistons, explosions), not by `/setblock` in replace mode. Build suppressors the survival way.
- Suppression throws real exceptions, exactly as vanilla 1.21 does. Depending on what triggered it, the server
  can crash or kick the player. Use something like Carpet TIS Addition's `yeetUpdateSuppressionCrash` if you need protection.
- Back up your world.

### legacyThrowableMovement

Makes throwable projectiles move exactly like in Minecraft 1.21.1, so vector ender pearl cannons and pearl
alignment built before 1.21.2 work again. Only on the 1.21.2+ branches; 1.21 - 1.21.1 already move this way.

- Categories: `bill`, `feature`, `survival`

```
/carpet legacyThrowableMovement pearl
```

| Mode | Affected |
| --- | --- |
| `off` | Nothing, vanilla movement |
| `pearl` | Ender pearls |
| `all` | Every `ThrowableProjectile`: ender pearls, snowballs, eggs, thrown potions, bottles o' enchanting |

Arrows, tridents, fireballs and wind charges have their own movement code and are never affected.

1.21.2 rewrote `ThrowableProjectile.tick()`. Each tick, with `v` the velocity:

| | 1.21.1 (restored) | 1.21.2+ |
| --- | --- | --- |
| Velocity | `v = v * drag - gravity`, after moving | `v = (v - gravity) * drag`, before moving |
| Position | `pos += v` with last tick's `v` | `pos += v` with the new `v`, or the hit point |
| Collision search | along last tick's `v` | along the new `v` |
| Hit handled | before moving, at the current position | after moving to the hit point |
| Base entity tick | first | last |

The rule runs the 1.21.1 tick order for the chosen projectiles. Since 1.21.6 the entity hit margin of a
projectile grows from 0 to 0.3 over its first ticks; with the rule the 1.21.1 margin of 0.3 is used from the
first tick, so entities right next to a fresh projectile are hit again. Where a pearl teleports its owner did
not change: both versions use the position at the start of the tick.

Positions and velocities are bit-identical to 1.21.1 on every branch in `scripts/movement_test.py`
(ticks 1 - 80 of 16 cases, from resting pearls to 2000 blocks/tick, walls, an entity, water and bubble columns).

Since 1.21.6 the entity hit margin of a projectile grows from 0 to 0.3 over its first ticks; with the rule
the 1.21.1 margin of 0.3 is used from the first tick, so entities right next to a fresh projectile are hit again.

Notes:

- Unchanged on purpose: the 1.21.2+ ender pearl chunk loading, owner tracking and teleport handling, and
  projectiles stopping at the world border (also new in 1.21.2, but not part of how they move).
- Clients without the mod predict the 1.21.2+ movement between position updates from the server, so a pearl
  can look slightly off on screen; the server-side result is what counts.
- Vector pearl cannons themselves have not been tested in game; the movement was verified tick by tick.

## Supported versions

Each Minecraft version family lives on its own git branch. `main` tracks the newest release.

| Branch | Minecraft | Carpet | Java |
| --- | --- | --- | --- |
| `main` / `26.3` | 26.3 | 26.3 | 25 |
| `26.2` | 26.2 | 26.2 | 25 |
| `26.1` | 26.1 - 26.1.2 | 26.1 | 25 |
| `1.21.11` | 1.21.11 | 1.21.11 | 21 |
| `1.21.10` | 1.21.9 - 1.21.10 | 1.21.10 | 21 |
| `1.21.8` | 1.21.6 - 1.21.8 | 1.21.7 | 21 |
| `1.21.5` | 1.21.5 | 1.21.5 | 21 |
| `1.21.4` | 1.21.4 | 1.21.4 | 21 |
| `1.21.3` | 1.21.2 - 1.21.3 | 1.21.2 | 21 |
| `1.21.1` | 1.21 - 1.21.1 | 1.21 | 21 |

Fixes go to the oldest branch they apply to and are merged forward.

## Building

Requires JDK 21+ (JDK 25 for the 26.x branches).

```
./gradlew build
```

The jar is written to `build/libs/`.

## Testing

Both scripts drive a real dedicated server over RCON. `./gradlew runTestServer` starts one with a
development-only helper mod (`src/testmod`, never shipped) that turns a barrier standing on a target block into
an update suppressor and prints exact projectile positions every tick.

`scripts/server_test.py` checks `reintroduceIllegalBlockEntity`: it breaks an active sculk sensor while its
removal update is suppressed, checks that its block entity stays under the chests placed there, triggers a
vibration, and restarts the server to check the chunk loads with the block entity intact, with the rule on and off.

```
./gradlew runTestServer
python scripts/server_test.py setup
# restart the server
python scripts/server_test.py reload
```

`scripts/movement_test.py` compares projectile movement tick by tick with a trace recorded on vanilla 1.21.1
(`scripts/reference/movement-1.21.1.json`):

```
./gradlew runTestServer
python scripts/movement_test.py record run/test-server/logs/latest.log traces.json off pearl all
python scripts/movement_test.py compare scripts/reference/movement-1.21.1.json traces.json
```

## License

MIT

The icon is based on the icon of [Fabric Carpet](https://github.com/gnembon/fabric-carpet) (MIT) by gnembon.
