# Carpet Bill Addition

<img src="src/main/resources/assets/carpet-bill-addition/icon.png" width="96" align="right" alt="icon">

[English](README.md) | **简体中文**

一个面向技术生存的 [Fabric Carpet](https://github.com/gnembon/fabric-carpet) 附属模组，支持 Minecraft 1.21
至最新正式版。所有功能都是默认关闭的 Carpet 规则，可用 `/carpet` 开关。

## 安装

1. 为你的 Minecraft 版本安装 [Fabric Loader](https://fabricmc.net/use/) 和 [Fabric Carpet](https://github.com/gnembon/fabric-carpet/releases)。
2. 从 [Releases](https://github.com/BillBlueInnit/Carpet-bill-addition/releases) 下载对应版本的 Carpet Bill Addition jar，放进 `mods` 文件夹。

只需安装在服务端（单人游戏装在客户端即可）。不需要 Fabric API。

## 规则

完整说明见[规则页面](docs/rules.zh-CN.md)。

| 规则 | 默认值 | 作用 |
| --- | --- | --- |
| [`reintroduceIllegalBlockEntity`](docs/rules.zh-CN.md#reintroduceillegalblockentity重新引入非法方块实体) | `false` | 重新引入 1.21.1 移除的非法方块实体（illegal block entity），使声音抑制（sound suppression）重新可用 |
| [`soundSuppressionRange`](docs/rules.zh-CN.md#soundsuppressionrange声音抑制范围) | `0` | 声音抑制器（sound suppressor）接收振动（vibration）的距离，最大 64 格（0 = 原版） |
| [`legacyThrowableMovement`](docs/rules.zh-CN.md#legacythrowablemovement旧版投掷物运动) | `off` | 让末影珍珠（`pearl`）或所有投掷物（`all`）的运动与 1.21.1 完全一致，兼容旧珍珠炮。仅 1.21.2+ |
| [`commandPlayerPrefix`](docs/rules.zh-CN.md#commandplayerprefix玩家前缀命令) | `false` | 启用 `/playerprefix <玩家> <前缀> [<颜色>]`，在聊天栏、玩家列表和头顶显示 `[前缀]玩家名` |

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

## 许可证

MIT

图标基于 gnembon 的 [Fabric Carpet](https://github.com/gnembon/fabric-carpet) 图标（MIT）。
