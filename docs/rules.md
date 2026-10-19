# Rules

**English** | [简体中文](rules.zh-CN.md)

Every rule is off by default and can be changed in game with `/carpet <rule> <value>`.
Use `/carpet setDefault <rule> <value>` to keep a value after restarts.

| Rule | Default | Options | Minecraft |
| --- | --- | --- | --- |
| [`reintroduceIllegalBlockEntity`](#reintroduceillegalblockentity) | `false` | `true`, `false` | 1.21+ |
| [`soundSuppressionRange`](#soundsuppressionrange) | `0` | `0` - `64` | 1.21+ |
| [`legacyThrowableMovement`](#legacythrowablemovement) | `off` | `off`, `pearl`, `all` | 1.21.2+ |
| [`commandPlayerPrefix`](#commandplayerprefix) | `false` | `true`, `false`, `ops`, `0` - `4` | 1.21+ |

---

## reintroduceIllegalBlockEntity

Reintroduces **illegal block entities** (a block entity sitting under a block of a different type),
which Minecraft 1.21.1 removed, so that **sound suppression** works again.

- Type: `boolean`
- Default: `false`
- Categories: `bill`, `experimental`, `survival`

```
/carpet reintroduceIllegalBlockEntity true
/carpet setDefault reintroduceIllegalBlockEntity true
```

### What it restores

| Change in vanilla | Version | With the rule on |
| --- | --- | --- |
| `BlockEntity` throws when it is created with, or given, the block state of another block, so illegal block entities are deleted when their chunk loads | 1.21.1 | Not validated, like 1.21 |
| `LevelChunk.setBlockState` deletes a block entity whose type does not match the new block | 1.21.1 | Kept and given the new block state, like 1.21 |
| Sculk sensors, lecterns, shulker boxes and chiseled bookshelves delete their block entity *before* sending the neighbor updates of their removal | 1.21.1 | Updates first, block entity deleted afterwards, like 1.21 |
| Every block entity is deleted before `affectNeighborsAfterRemoval` (the old `onRemove` was split up) | 1.21.5 | Neighbor updates of the removed block run first, the block entity is deleted afterwards |
| A removed lectern with a book only copies the book out instead of clearing itself, so the comparators next to it are no longer updated before its block entity is deleted | 1.21.5 | It clears itself again, updating those comparators first, like 1.21.4 |

The last two rows are what make illegal block entities buildable in survival: when the neighbor updates of a
removed block are update-suppressed, the exception aborts the replacement before the block entity is deleted,
so it stays behind under the new block.

### Lecterns and CCE boxes

A CCE box is a shulker box holding a lectern block entity: with Carpet TIS Addition's `shulkerBoxCCEReintroduced`,
a comparator reading it throws `ClassCastException`. To make one, break a lectern that has a book while the update
from its comparator is suppressed, for example through a comparator that breaks because nothing is under it, then
place a shulker box where the lectern was. Breaking the lectern updates that comparator before the lectern's block
entity is deleted, so the block entity stays behind and ends up in the shulker box. This works on every supported
version with the rule on, like in 1.21.

### How sound suppression works

With an illegal sculk sensor (or calibrated sculk sensor) block entity under, for example, a chest,
the sensor reads the `sculk_sensor_phase` property of the chest as soon as it hears a vibration,
which throws `IllegalArgumentException`. That exception is the sound suppressor: whatever caused the vibration
is cut off at that point.

### Notes

- Turn the rule on (`setDefault` is easiest) **before** loading chunks that contain old suppressors.
  With the rule off, 1.21.1+ deletes illegal block entities when their chunk loads, and that cannot be undone.
- On Minecraft 1.21 the rule does nothing because vanilla still has this behavior.
- Since 1.21.5 removal neighbor updates are only sent when the block is replaced with block updates
  (breaking it, pistons, explosions), not by `/setblock` in replace mode. Build suppressors the survival way.
- Suppression throws real exceptions, exactly as vanilla 1.21 does. Depending on what triggered it, the server
  can crash or kick the player. Use something like Carpet TIS Addition's `yeetUpdateSuppressionCrash` if you need protection.
- Back up your world.

---

## soundSuppressionRange

How far away sound suppressors hear vibrations. A sound suppressor is an illegal sculk sensor (or calibrated
sculk sensor) block entity, see [reintroduceIllegalBlockEntity](#reintroduceillegalblockentity).

- Type: `int`
- Default: `0`
- Options: `0`, `16`, `32`, `64`; any value from `0` to `64` is accepted
- Categories: `bill`, `experimental`, `survival`

```
/carpet soundSuppressionRange 64
```

| Value | Range of every sound suppressor |
| --- | --- |
| `0` | Vanilla: 8 blocks for sculk sensors, 16 for calibrated sculk sensors |
| `1` - `64` | Exactly this many blocks, bigger or smaller than vanilla |

Normal sculk sensors (a sculk sensor block entity under a sculk sensor) are never affected, so sculk redstone
keeps working as usual.

### How it works

Vanilla only offers a game event (vibration) to listeners in the chunk sections within the event's notification
radius, which is 16 blocks for most events, and then only to those within their own listener radius. Raising a
sensor's radius alone would therefore stop at about 16 blocks. The mod keeps a list of the sound suppressors in each
dimension instead:

- in the sections vanilla looks at, a suppressor's listener radius is replaced by the rule's value;
- suppressors in other sections get the event right after vanilla's pass, if they are within range.

The extra work per game event is a loop over the suppressors of that dimension, which is empty in most worlds,
so 64 costs no more than 16. Distances are measured like vanilla: block position to block position.

---

## legacyThrowableMovement

Makes throwable projectiles move exactly like in Minecraft 1.21.1, so vector ender pearl cannons and pearl
alignment built before 1.21.2 work again. Only on the 1.21.2+ branches; 1.21 - 1.21.1 already move this way.

- Type: `off` / `pearl` / `all`
- Default: `off`
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

### What changed in 1.21.2

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

### Verification

Positions and velocities are bit-identical to 1.21.1 on every branch in `scripts/movement_test.py`
(ticks 1 - 80 of 16 cases, from resting pearls to 2000 blocks/tick, walls, an entity, water and bubble columns).

### Notes

- Unchanged on purpose: the 1.21.2+ ender pearl chunk loading, owner tracking and teleport handling, and
  projectiles stopping at the world border (also new in 1.21.2, but not part of how they move).
- Clients without the mod predict the 1.21.2+ movement between position updates from the server, so a pearl
  can look slightly off on screen; the server-side result is what counts.
- Vector pearl cannons themselves have not been tested in game; the movement was verified tick by tick.

---

## commandPlayerPrefix

Enables `/playerprefix`, which shows a prefix before a player's name: `[prefix]name`, in chat, the player list
(tab) and above their head.

- Type: command permission, like Carpet's own command rules
- Default: `false`
- Options: `true` (everyone), `false` (nobody), `ops` (operators only), `0` - `4` (that permission level)
- Categories: `bill`, `command`

```
/carpet commandPlayerPrefix true
/playerprefix <player> <prefix> [<color>]
/playerprefix <player>
/playerprefix <player> ""
```

| Command | Effect |
| --- | --- |
| `/playerprefix Steve Builder` | Steve is shown as `[Builder]Steve` |
| `/playerprefix Steve Builder gold` | The same, with `[Builder]` in gold (any chat color: `red`, `aqua`, `dark_purple`, ...) |
| `/playerprefix Steve` | Shows Steve's current prefix and color |
| `/playerprefix Steve ""` | Removes Steve's prefix |

Use quotes for prefixes with spaces or non-English characters: `/playerprefix Steve "建筑师"`.
A prefix can be at most 32 characters.

### Who can set whose prefix

- Operators can set the prefix of anyone, including players who are offline.
- Everyone else who may use the command can only set their own prefix and the prefix of Carpet fake players.

### How it is shown

Vanilla clients only draw a team prefix above a player's head, so a player with a prefix is put into a team of their
own, `cba_prefix_<name>`, whose prefix is `[prefix]`. The prefix color only colors the prefix, not the name.
That team also shows the prefix in chat, the player list and `/list`.

If the player is already in another team, they stay in it: the prefix is then added to their name in chat and the
player list, but cannot be shown above their head. The command says so when that happens.

Prefixes are saved per world in `carpet-bill-addition/player_prefixes.json` and applied again whenever a player or
fake player logs in, so they survive restarts.
