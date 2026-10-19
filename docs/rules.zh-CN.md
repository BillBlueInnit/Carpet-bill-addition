# 规则

[English](rules.md) | **简体中文**

所有规则默认关闭，可在游戏中用 `/carpet <规则> <值>` 修改。
用 `/carpet setDefault <规则> <值>` 可让设置在重启后保留。

| 规则 | 默认值 | 可选值 | Minecraft |
| --- | --- | --- | --- |
| [`reintroduceIllegalBlockEntity`](#reintroduceillegalblockentity重新引入非法方块实体) | `false` | `true`、`false` | 1.21+ |
| [`soundSuppressionRange`](#soundsuppressionrange声音抑制范围) | `0` | `0` - `64` | 1.21+ |
| [`legacyThrowableMovement`](#legacythrowablemovement旧版投掷物运动) | `off` | `off`、`pearl`、`all` | 1.21.2+ |
| [`commandPlayerPrefix`](#commandplayerprefix玩家前缀命令) | `false` | `true`、`false`、`ops`、`0` - `4` | 1.21+ |

---

## reintroduceIllegalBlockEntity（重新引入非法方块实体）

重新引入 Minecraft 1.21.1 移除的**非法方块实体（illegal block entity）**，即方块实体（block entity）与所在方块的类型不一致，
从而让**声音抑制（sound suppression）**重新可用。

- 类型：`boolean`
- 默认值：`false`
- 分类：`bill`、`experimental`、`survival`

```
/carpet reintroduceIllegalBlockEntity true
/carpet setDefault reintroduceIllegalBlockEntity true
```

### 恢复的内容

| 原版改动 | 版本 | 开启规则后 |
| --- | --- | --- |
| 方块实体以其它方块的方块状态（block state）创建、或被赋予其它方块的状态时会抛出异常，因此区块（chunk）加载时非法方块实体会被删除 | 1.21.1 | 不再校验，与 1.21 相同 |
| `LevelChunk.setBlockState` 会删除类型与新方块不匹配的方块实体 | 1.21.1 | 保留该方块实体并更新它的方块状态，与 1.21 相同 |
| 幽匿感测体（sculk sensor）、讲台（lectern）、潜影盒（shulker box）、雕纹书架（chiseled bookshelf）被移除时，先删除方块实体，再发出邻居更新（neighbor update） | 1.21.1 | 先发出邻居更新，再删除方块实体，与 1.21 相同 |
| 所有方块实体都在 `affectNeighborsAfterRemoval` 之前被删除（原来的 `onRemove` 被拆分） | 1.21.5 | 先执行被移除方块的邻居更新，再删除方块实体 |
| 放有书的讲台被移除时只复制出书，不再清空自身，因此旁边的比较器（comparator）不会在其方块实体被删除之前收到更新 | 1.21.5 | 讲台重新清空自身，先更新这些比较器，与 1.21.4 相同 |

后两行让非法方块实体可以在生存中制造：被移除方块的邻居更新被更新抑制（update suppression）时，
异常会在删除方块实体之前中止这次替换，方块实体就留在了新方块下面。

### 讲台与 CCE 盒

CCE 盒（CCE box）是装着讲台方块实体的潜影盒：配合 Carpet TIS Addition 的 `shulkerBoxCCEReintroduced`，比较器读取它时会抛出
`ClassCastException`（类型转换异常）。制作方法：在比较器传来的更新被更新抑制时破坏放有书的讲台（例如通过一个下方没有方块、
收到更新就会掉落的比较器），然后在讲台原来的位置放置潜影盒。破坏讲台时会在其方块实体被删除之前更新这个比较器，
因此方块实体会留下来并进入潜影盒。开启规则后，在所有支持的版本上都与 1.21 一样可行。

### 声音抑制的原理

当非法的幽匿感测体（或校频幽匿感测体（calibrated sculk sensor））方块实体位于例如箱子下面时，
感测体一接收到振动（vibration）就会读取箱子并不存在的 `sculk_sensor_phase` 属性，从而抛出 `IllegalArgumentException`。
这个异常就是声音抑制器（sound suppressor）：引起振动的操作会在这里被中断。

### 注意

- 请在加载含有旧抑制器的区块**之前**开启规则（使用 `setDefault` 最方便）。规则关闭时，1.21.1+ 会在区块加载时删除非法方块实体，且无法恢复。
- 在 Minecraft 1.21 上原版本身就有该机制，规则不起作用。
- 从 1.21.5 起，只有带方块更新（block update）地替换方块（破坏、活塞、爆炸）才会发出移除时的邻居更新，替换模式的 `/setblock` 不会。请按生存的方式制作抑制器。
- 抑制会像原版 1.21 一样抛出真实的异常。视触发来源不同，可能导致服务器崩溃或玩家被踢出。如需防护，可使用 Carpet TIS Addition 的 `yeetUpdateSuppressionCrash` 等规则。
- 请备份存档。

---

## soundSuppressionRange（声音抑制范围）

声音抑制器能接收振动的距离。声音抑制器是指非法的幽匿感测体（或校频幽匿感测体）方块实体，
见 [reintroduceIllegalBlockEntity](#reintroduceillegalblockentity重新引入非法方块实体)。

- 类型：`int`
- 默认值：`0`
- 可选值：`0`、`16`、`32`、`64`；可输入 `0` 到 `64` 之间的任意值
- 分类：`bill`、`experimental`、`survival`

```
/carpet soundSuppressionRange 64
```

| 值 | 每个声音抑制器的范围 |
| --- | --- |
| `0` | 原版：幽匿感测体 8 格，校频幽匿感测体 16 格 |
| `1` - `64` | 正好这么多格，可以比原版大，也可以比原版小 |

正常的幽匿感测体（幽匿感测体方块下的幽匿感测体方块实体）永远不受影响，因此幽匿感测体红石照常工作。

### 实现方式

原版只会把游戏事件（game event，即振动）发给事件通知半径（notification radius，大多数事件为 16 格）内的区段（chunk section）中的监听器（listener），
并且只发给在自身监听半径（listener radius）内的监听器。因此单纯提高感测体的半径最多只能达到约 16 格。
本模组改为记录每个维度中的声音抑制器：

- 在原版会检查的区段中，把声音抑制器的监听半径替换为规则的值；
- 其它区段中位于范围内的声音抑制器，会在原版处理完之后立即收到该事件。

每个游戏事件额外的开销只是遍历该维度中的声音抑制器，大多数存档中这个列表是空的，因此 64 格并不比 16 格更卡。
距离按原版的方式计算：方块坐标到方块坐标。

---

## legacyThrowableMovement（旧版投掷物运动）

使投掷物（throwable projectile）的运动与 Minecraft 1.21.1 完全一致，以兼容 1.21.2 之前的矢量珍珠炮（vector pearl cannon）和珍珠对齐（pearl alignment）装置。
仅 1.21.2+ 的分支提供；1.21 - 1.21.1 本身就是旧版运动。

- 类型：`off` / `pearl` / `all`
- 默认值：`off`
- 分类：`bill`、`feature`、`survival`

```
/carpet legacyThrowableMovement pearl
```

| 模式 | 影响范围 |
| --- | --- |
| `off` | 不影响，原版运动 |
| `pearl` | 末影珍珠（ender pearl） |
| `all` | 所有 `ThrowableProjectile`：末影珍珠、雪球（snowball）、鸡蛋（egg）、投掷的药水（thrown potion）、附魔之瓶（bottle o' enchanting） |

箭（arrow）、三叉戟（trident）、火球（fireball）和风弹（wind charge）有各自的运动代码，永远不受影响。

### 1.21.2 的改动

1.21.2 重写了 `ThrowableProjectile.tick()`。每 tick（`v` 为速度（velocity））：

| | 1.21.1（恢复的行为） | 1.21.2+ |
| --- | --- | --- |
| 速度 | 移动之后 `v = v * 阻力（drag） - 重力（gravity）` | 移动之前 `v = (v - 重力) * 阻力` |
| 位置 | 按上一 tick 的 `v` 执行 `pos += v` | 按新的 `v` 执行 `pos += v`，或移到命中点（hit point） |
| 碰撞判定（collision search） | 沿上一 tick 的 `v` | 沿新的 `v` |
| 命中处理（hit handling） | 移动之前，在当前位置 | 移动到命中点之后 |
| 实体基础 tick（base entity tick） | 最先执行 | 最后执行 |

规则会让选中的投掷物按 1.21.1 的顺序执行 tick。从 1.21.6 起，投掷物的实体命中边距（entity hit margin）会在最初几 tick 内从 0 增长到 0.3；
开启规则后从第一 tick 起就使用 1.21.1 的 0.3，因此紧贴刚发射投掷物的实体会重新被命中。
末影珍珠把主人传送到的位置没有变化：两个版本都使用该 tick 开始时的位置。

### 验证

在每个分支上用 `scripts/movement_test.py` 对比，位置与速度都与 1.21.1 逐位相同（16 个用例的第 1 - 80 tick，
从静止的珍珠到每 tick 2000 格，以及撞墙、命中实体、水中和气泡柱（bubble column））。

### 注意

- 故意保持不变：1.21.2+ 的末影珍珠区块加载（chunk loading）、主人追踪（owner tracking）与传送处理，以及投掷物会停在世界边界（world border）处（同样是 1.21.2 的新改动，但不属于运动方式）。
- 未安装本模组的客户端会在服务器两次位置同步之间按 1.21.2+ 的方式预测运动，因此珍珠在屏幕上可能略有偏差；以服务端结果为准。
- 尚未在游戏内实际测试矢量珍珠炮；运动是逐 tick 验证的。

---

## commandPlayerPrefix（玩家前缀命令）

启用 `/playerprefix`，在玩家名称前显示前缀（prefix）：`[前缀]玩家名`，显示在聊天栏（chat）、玩家列表（player list，Tab 键）和头顶名称（name tag）上。

- 类型：命令权限（command permission），与 Carpet 自带的命令规则相同
- 默认值：`false`
- 可选值：`true`（所有人）、`false`（任何人都不能用）、`ops`（仅管理员（operator））、`0` - `4`（对应权限等级（permission level））
- 分类：`bill`、`command`

```
/carpet commandPlayerPrefix true
/playerprefix <玩家> <前缀> [<颜色>]
/playerprefix <玩家>
/playerprefix <玩家> ""
```

| 命令 | 效果 |
| --- | --- |
| `/playerprefix Steve Builder` | Steve 显示为 `[Builder]Steve` |
| `/playerprefix Steve Builder gold` | 同上，`[Builder]` 为金色（可用任何聊天颜色（chat color）：`red`、`aqua`、`dark_purple` 等） |
| `/playerprefix Steve` | 查看 Steve 当前的前缀和颜色 |
| `/playerprefix Steve ""` | 移除 Steve 的前缀 |

前缀含空格或中文时请加引号：`/playerprefix Steve "建筑师"`。前缀最多 32 个字符。

### 谁可以设置谁的前缀

- 管理员可以设置任何人的前缀，包括不在线的玩家。
- 其他有权使用该命令的玩家只能设置自己和 Carpet 假人（fake player）的前缀。

### 显示方式

原版客户端只会在玩家头顶显示队伍前缀（team prefix），因此设置了前缀的玩家会被加入一个专属队伍（team）`cba_prefix_<玩家名>`，
该队伍的前缀就是 `[前缀]`。颜色只作用于前缀，不改变名字的颜色。这个队伍也会让前缀显示在聊天栏、玩家列表和 `/list` 中。

如果玩家已经在别的队伍中，则保留原队伍：此时前缀会加在聊天栏和玩家列表中的名字前，但无法显示在头顶。命令会对此给出提示。

前缀按存档保存在 `carpet-bill-addition/player_prefixes.json` 中，并在玩家或假人每次登录时重新应用，因此重启后依然有效。
