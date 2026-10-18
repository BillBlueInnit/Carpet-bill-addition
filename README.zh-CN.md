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
（`src/testmod`，不会发布）的服务器：它把放在标靶方块上的屏障变成更新抑制器（update suppressor），并每 tick 输出投掷物的精确位置。

- `scripts/server_test.py` 按生存的方式制作声音抑制器，检查 `reintroduceIllegalBlockEntity` 和 `soundSuppressionRange`，
  包括重启服务器之后的情况（`setup`、重启、`reload`）。
- `scripts/movement_test.py` 把投掷物运动与在原版 1.21.1 上录制的轨迹（`scripts/reference/movement-1.21.1.json`）逐 tick 对比。

```
./gradlew runTestServer
python scripts/server_test.py setup
# 重启服务器
python scripts/server_test.py reload

python scripts/movement_test.py record run/test-server/logs/latest.log traces.json off pearl all
python scripts/movement_test.py compare scripts/reference/movement-1.21.1.json traces.json
```

## 许可证

MIT

图标基于 gnembon 的 [Fabric Carpet](https://github.com/gnembon/fabric-carpet) 图标（MIT）。
