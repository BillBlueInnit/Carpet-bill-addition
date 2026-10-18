# Carpet Bill Addition

<img src="src/main/resources/assets/carpet-bill-addition/icon.png" width="96" align="right" alt="icon">

**English** | [简体中文](README.zh-CN.md)

A [Fabric Carpet](https://github.com/gnembon/fabric-carpet) extension for technical Minecraft, for
Minecraft 1.21 up to the latest release. Every feature is a Carpet rule, off by default, and can be
toggled with `/carpet`.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric Carpet](https://github.com/gnembon/fabric-carpet/releases)
   for your Minecraft version.
2. Download the Carpet Bill Addition jar for your version from
   [Releases](https://github.com/BillBlueInnit/Carpet-bill-addition/releases) and put it into `mods`.

The mod is only needed on the server (or on the client for singleplayer). Fabric API is not required.

## Rules

Full explanations are on the [rules page](docs/rules.md).

| Rule | Default | What it does |
| --- | --- | --- |
| [`reintroduceIllegalBlockEntity`](docs/rules.md#reintroduceillegalblockentity) | `false` | Brings back illegal block entities (removed in 1.21.1), so sound suppression works again |
| [`soundSuppressionRange`](docs/rules.md#soundsuppressionrange) | `0` | How far sound suppressors hear vibrations, up to 64 blocks (0 = vanilla) |
| [`legacyThrowableMovement`](docs/rules.md#legacythrowablemovement) | `off` | Ender pearls (`pearl`) or all throwables (`all`) move exactly like in 1.21.1, for old pearl cannons. 1.21.2+ only |
| [`commandPlayerPrefix`](docs/rules.md#commandplayerprefix) | `false` | Enables `/playerprefix <player> <prefix> [<color>]`, which shows `[prefix]name` in chat, the player list and above the head |

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

- `scripts/server_test.py` builds sound suppressors the survival way and checks
  `reintroduceIllegalBlockEntity` and `soundSuppressionRange`, including after a server restart
  (`setup`, restart, `reload`).
- `scripts/movement_test.py` compares projectile movement tick by tick with a trace recorded on vanilla 1.21.1
  (`scripts/reference/movement-1.21.1.json`).

```
./gradlew runTestServer
python scripts/server_test.py setup
# restart the server
python scripts/server_test.py reload

python scripts/movement_test.py record run/test-server/logs/latest.log traces.json off pearl all
python scripts/movement_test.py compare scripts/reference/movement-1.21.1.json traces.json
```

## License

MIT

The icon is based on the icon of [Fabric Carpet](https://github.com/gnembon/fabric-carpet) (MIT) by gnembon.
