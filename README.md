# Carpet Bill Addition

<img src="src/main/resources/assets/carpet-bill-addition/icon.png" width="96" align="right" alt="icon">

A [Fabric Carpet](https://github.com/gnembon/fabric-carpet) extension for technical Minecraft.
Every feature is a Carpet rule, off by default, and can be toggled with `/carpet`.

[中文说明](#中文说明)

## Rules

### reintroduceIllegalBlockEntity

Reintroduces **illegal block entities** (a block entity sitting under a block of a different type),
which Minecraft 1.21.1 removed, so that **sound suppression** works again.

- Type: `boolean`
- Default: `false`
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
- Suppression throws real exceptions, exactly as vanilla 1.21 does. Depending on what triggered it, the server
  can crash or kick the player. Use something like Carpet TIS Addition's `yeetUpdateSuppressionCrash` if you need protection.
- Back up your world.

### legacyThrowableMovement

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

1.21.2 rewrote `ThrowableProjectile.tick()`. Each tick, with `v` the velocity:

| | 1.21.1 (restored) | 1.21.2+ |
| --- | --- | --- |
| Velocity | `v = v * drag - gravity`, after moving | `v = (v - gravity) * drag`, before moving |
| Position | `pos += v` with last tick's `v` | `pos += v` with the new `v`, or the hit point |
| Collision search | along last tick's `v` | along the new `v` |
| Hit handled | before moving, at the current position | after moving to the hit point |
| Base entity tick | first | last |

The rule runs the 1.21.1 tick order for the chosen projectiles. Positions and velocities are bit-identical to
1.21.1 in `scripts/movement_test.py` (ticks 1 - 80 of 15 cases, from resting pearls to 2000 blocks/tick,
walls, water and bubble columns). Where a pearl teleports its owner did not change: both versions use
the position at the start of the tick.

Notes:

- Unchanged on purpose: the 1.21.2+ ender pearl chunk loading, owner tracking and teleport handling.
- Clients without the mod predict the 1.21.2+ movement between position updates from the server, so a pearl
  can look slightly off on screen; the server-side result is what counts.
- Vector pearl cannons themselves have not been tested in game; the movement was verified tick by tick.

## Supported versions

Each Minecraft version family lives on its own git branch. `main` tracks the newest release.

| Branch | Minecraft | Carpet |
| --- | --- | --- |
| `main` / `26.3` | 26.3 | 26.3 |
| `26.2` | 26.2 | 26.2 |
| `26.1` | 26.1 - 26.1.2 | 26.1 |
| `1.21.11` | 1.21.11 | 1.21.11 |
| `1.21.10` | 1.21.9 - 1.21.10 | 1.21.10 |
| `1.21.8` | 1.21.6 - 1.21.8 | 1.21.7 |
| `1.21.5` | 1.21.5 | 1.21.5 |
| `1.21.4` | 1.21.4 | 1.21.4 |
| `1.21.3` | 1.21.2 - 1.21.3 | 1.21.2 |
| `1.21.1` | 1.21 - 1.21.1 | 1.21 |

The mod is only needed on the server (or on the client for singleplayer). Fabric API is not required.

## Movement test

`scripts/movement_test.py` compares projectile movement tick by tick with a trace recorded on vanilla 1.21.1
(`scripts/reference/movement-1.21.1.json`):

```
./gradlew runTestServer
python scripts/movement_test.py record run/test-server/logs/latest.log traces.json off pearl all
python scripts/movement_test.py compare scripts/reference/movement-1.21.1.json traces.json
```

## Building

Requires JDK 21+ (JDK 25 for 26.x branches).

```
./gradlew build
```

The jar is written to `build/libs/`.

### Testing

`scripts/server_test.py` runs an end-to-end check against a real dedicated server:

```
./gradlew runTestServer
python scripts/server_test.py
```

`runTestServer` also loads a development-only helper mod (`src/testmod`, never shipped) that turns a barrier
standing on a target block into an update suppressor. The script then, over RCON, swaps an active sculk sensor
for a chest while its removal update is suppressed, checks the block entity type, triggers a vibration,
and reloads the chunk, with the rule both on and off.

## 中文说明

一个 Fabric Carpet 附属模组。所有功能都是默认关闭的 Carpet 规则，用 `/carpet` 开关。

### reintroduceIllegalBlockEntity（重新引入非法方块实体）

重新引入 1.21.1 移除的**非法方块实体**（方块实体与所在方块类型不一致），从而让**声音抑制**重新可用。

```
/carpet reintroduceIllegalBlockEntity true
/carpet setDefault reintroduceIllegalBlockEntity true
```

开启后：

- 方块实体以其它方块的状态创建、或被赋予其它方块的状态时不再抛出异常，区块加载时也不再删除非法方块实体（撤销 1.21.1 的改动）；
- 放置不匹配的方块时不再删除原有方块实体，而是更新它的方块状态（撤销 1.21.1 的改动）；
- 幽匿感测体、讲台、潜影盒、雕纹书架被移除时重新先发出邻居更新、再删除方块实体（撤销 1.21.1 的改动）；
- 1.21.5+：所有方块被移除时都先执行 `affectNeighborsAfterRemoval` 邻居更新，再删除方块实体。

后两点让非法方块实体可以在生存中制造：被移除方块的邻居更新被更新抑制时，异常会在删除方块实体之前中止替换，方块实体就留在了新方块下面。

幽匿感测体方块实体位于例如箱子下面时，一旦接收到振动就会读取箱子并不存在的 `sculk_sensor_phase` 属性并抛出 `IllegalArgumentException`，这就是声音抑制器。

注意：请在加载含有旧抑制器的区块**之前**开启规则（建议 `setDefault`），否则 1.21.1+ 会在区块加载时删除非法方块实体且无法恢复；在 1.21 上原版本身就有该机制，规则不起作用；抑制会像原版 1.21 一样抛出真实异常，可能导致崩服或玩家掉线；请备份存档。

### legacyThrowableMovement（旧版投掷物运动）

使投掷物的运动与 1.21.1 完全一致，以兼容 1.21.2 之前的矢量珍珠炮和珍珠对齐装置。仅 1.21.2+ 分支提供；1.21 - 1.21.1 本身就是旧版运动。

```
/carpet legacyThrowableMovement pearl
```

- `off`（默认）：原版运动；`pearl`：仅末影珍珠；`all`：所有 `ThrowableProjectile`（末影珍珠、雪球、鸡蛋、投掷的药水、附魔之瓶）。
- 箭、三叉戟、火球和风弹不受影响。
- 恢复的是 1.21.1 的完整 tick 顺序：先执行实体基础 tick；沿上一 tick 的速度判定碰撞并在移动前处理命中；再按上一 tick 的速度移动；最后计算阻力与重力（`v = v * 阻力 - 重力`）。
- 经 `scripts/movement_test.py` 逐 tick 对比，位置与速度和 1.21.1 逐位相同；尚未在游戏内实际测试矢量珍珠炮。

## License

MIT
