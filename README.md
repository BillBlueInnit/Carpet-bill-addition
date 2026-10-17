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

## License

MIT
