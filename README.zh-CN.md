# Carpet Bill Addition

<img src="src/main/resources/assets/carpet-bill-addition/icon.png" width="96" align="right" alt="icon">

[English](README.md) | **简体中文**

一个面向技术生存的 [Fabric Carpet](https://github.com/gnembon/fabric-carpet) 附属模组，支持 Minecraft 1.21
至最新正式版。所有功能都是默认关闭的 Carpet 规则，可用 `/carpet` 开关。

## 安装

1. 为你的 Minecraft 版本安装 [Fabric Loader](https://fabricmc.net/use/) 和 [Fabric Carpet](https://github.com/gnembon/fabric-carpet/releases)。
2. 把对应版本的 Carpet Bill Addition jar（见[支持的版本](#支持的版本)）放进 `mods` 文件夹。

只需安装在服务端（单人游戏装在客户端即可）。不需要 Fabric API。

## 规则

| 规则 | 默认值 | 可选值 | Minecraft |
| --- | --- | --- | --- |
| [`reintroduceIllegalBlockEntity`](#reintroduceillegalblockentity重新引入非法方块实体) | `false` | `true`、`false` | 1.21+ |
| [`legacyThrowableMovement`](#legacythrowablemovement旧版投掷物运动) | `off` | `off`、`pearl`、`all` | 1.21.2+ |

### reintroduceIllegalBlockEntity（重新引入非法方块实体）

重新引入 Minecraft 1.21.1 移除的**非法方块实体**（方块实体与所在方块类型不一致），使**声音抑制**重新可用。

- 分类：`bill`、`experimental`、`survival`

```
/carpet reintroduceIllegalBlockEntity true
/carpet setDefault reintroduceIllegalBlockEntity true
```

恢复的内容：

| 原版改动 | 版本 | 开启规则后 |
| --- | --- | --- |
| 方块实体以其它方块的状态创建、或被赋予其它方块的状态时会抛出异常，因此区块加载时非法方块实体会被删除 | 1.21.1 | 不再校验，与 1.21 相同 |
| `LevelChunk.setBlockState` 会删除类型与新方块不匹配的方块实体 | 1.21.1 | 保留该方块实体并更新它的方块状态，与 1.21 相同 |
| 幽匿感测体、讲台、潜影盒、雕纹书架被移除时，先删除方块实体再发出邻居更新 | 1.21.1 | 先发出更新，再删除方块实体，与 1.21 相同 |
| 所有方块实体都在 `affectNeighborsAfterRemoval` 之前被删除（原来的 `onRemove` 被拆分） | 1.21.5 | 先执行被移除方块的邻居更新，再删除方块实体 |

后两行让非法方块实体可以在生存中制造：被移除方块的邻居更新被更新抑制时，异常会在删除方块实体之前中止替换，
方块实体就留在了新方块下面。

当非法的幽匿感测体（或校频幽匿感测体）方块实体位于例如箱子下面时，感测体一接收到振动就会读取箱子并不存在的
`sculk_sensor_phase` 属性，从而抛出 `IllegalArgumentException`，这就是声音抑制器。

注意：

- 请在加载含有旧抑制器的区块**之前**开启规则（使用 `setDefault` 最方便）。规则关闭时，1.21.1+ 会在区块加载时删除非法方块实体，且无法恢复。
- 在 Minecraft 1.21 上原版本身就有该机制，规则不起作用。
- 从 1.21.5 起，只有带方块更新地替换方块（破坏、活塞、爆炸）才会发出移除时的邻居更新，替换模式的 `/setblock` 不会。请按生存的方式制作抑制器。
- 抑制会像原版 1.21 一样抛出真实的异常。视触发来源不同，可能导致服务器崩溃或玩家被踢出。如需防护，可使用 Carpet TIS Addition 的 `yeetUpdateSuppressionCrash` 等规则。
- 请备份存档。

### legacyThrowableMovement（旧版投掷物运动）

使投掷物的运动与 Minecraft 1.21.1 完全一致，以兼容 1.21.2 之前的矢量珍珠炮和珍珠对齐装置。仅 1.21.2+ 的分支提供；
1.21 - 1.21.1 本身就是旧版运动。

- 分类：`bill`、`feature`、`survival`

```
/carpet legacyThrowableMovement pearl
```

| 模式 | 影响范围 |
| --- | --- |
| `off` | 不影响，原版运动 |
| `pearl` | 末影珍珠 |
| `all` | 所有 `ThrowableProjectile`：末影珍珠、雪球、鸡蛋、投掷的药水、附魔之瓶 |

箭、三叉戟、火球和风弹有各自的运动代码，永远不受影响。

1.21.2 重写了 `ThrowableProjectile.tick()`。每 tick（`v` 为速度）：

| | 1.21.1（恢复的行为） | 1.21.2+ |
| --- | --- | --- |
| 速度 | 移动之后 `v = v * 阻力 - 重力` | 移动之前 `v = (v - 重力) * 阻力` |
| 位置 | 按上一 tick 的 `v` 执行 `pos += v` | 按新的 `v` 执行 `pos += v`，或移到命中点 |
| 碰撞判定 | 沿上一 tick 的 `v` | 沿新的 `v` |
| 命中处理 | 移动之前，在当前位置 | 移动到命中点之后 |
| 实体基础 tick | 最先执行 | 最后执行 |

规则会让选中的投掷物按 1.21.1 的顺序执行 tick。从 1.21.6 起，投掷物的实体命中边距会在最初几 tick 内从 0 增长到 0.3；
开启规则后从第一 tick 起就使用 1.21.1 的 0.3，因此紧贴刚发射投掷物的实体会重新被命中。末影珍珠把主人传送到的位置没有变化：
两个版本都使用该 tick 开始时的位置。

在每个分支上用 `scripts/movement_test.py` 对比，位置与速度都与 1.21.1 逐位相同（16 个用例的第 1 - 80 tick，
从静止的珍珠到每 tick 2000 格，以及撞墙、命中实体、水中和气泡柱）。

注意：

- 故意保持不变：1.21.2+ 的末影珍珠区块加载、主人追踪与传送处理，以及投掷物会停在世界边界处（同样是 1.21.2 的新改动，但不属于运动方式）。
- 未安装本模组的客户端会在服务器两次位置同步之间按 1.21.2+ 的方式预测运动，因此珍珠在屏幕上可能略有偏差；以服务端结果为准。
- 尚未在游戏内实际测试矢量珍珠炮；运动是逐 tick 验证的。

## 支持的版本

每个 Minecraft 版本系列位于各自的 git 分支。`main` 跟随最新正式版。

| 分支 | Minecraft | Carpet | Java |
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

修复提交到适用的最旧分支，再向后合并。

## 构建

需要 JDK 21+（26.x 分支需要 JDK 25）。

```
./gradlew build
```

jar 输出在 `build/libs/`。

## 测试

两个脚本都通过 RCON 控制真实的专用服务器。`./gradlew runTestServer` 会启动一个附带仅供开发使用的辅助模组
（`src/testmod`，不会发布）的服务器：它把放在标靶方块上的屏障变成更新抑制器，并每 tick 输出投掷物的精确位置。

`scripts/server_test.py` 检查 `reintroduceIllegalBlockEntity`：在激活的幽匿感测体的移除更新被抑制时破坏它，
检查其方块实体是否留在之后放置的箱子下面，触发振动，并重启服务器检查区块加载后方块实体是否完好，规则开启和关闭各测一次。

```
./gradlew runTestServer
python scripts/server_test.py setup
# 重启服务器
python scripts/server_test.py reload
```

`scripts/movement_test.py` 把投掷物运动与在原版 1.21.1 上录制的轨迹（`scripts/reference/movement-1.21.1.json`）逐 tick 对比：

```
./gradlew runTestServer
python scripts/movement_test.py record run/test-server/logs/latest.log traces.json off pearl all
python scripts/movement_test.py compare scripts/reference/movement-1.21.1.json traces.json
```

## 许可证

MIT

图标基于 gnembon 的 [Fabric Carpet](https://github.com/gnembon/fabric-carpet) 图标（MIT）。
