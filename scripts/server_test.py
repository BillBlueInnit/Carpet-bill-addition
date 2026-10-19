"""
End-to-end test for reintroduceIllegalBlockEntity, driven over RCON against a running
`gradlew runTestServer` (the dev-only cba-testmod turns a barrier on a target block into an update suppressor).

Run it twice against the same world, restarting the server in between, so chunks are really read from disk:
    python scripts/server_test.py setup
    python scripts/server_test.py reload
Optional arguments after the phase: [host] [port] [password]
"""
import socket
import struct
import sys
import time


FAILED = "An unexpected error occurred"


class Rcon:
    def __init__(self, host="127.0.0.1", port=25575, password="cba-test"):
        self.sock = socket.create_connection((host, port), timeout=30)
        self.request_id = 0
        self._send(3, password)

    def _send(self, kind, body):
        self.request_id += 1
        payload = struct.pack("<ii", self.request_id, kind) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        length = struct.unpack("<i", self._read(4))[0]
        data = self._read(length)
        response_id = struct.unpack("<i", data[:4])[0]
        if response_id == -1:
            raise RuntimeError("RCON authentication failed")
        return data[8:-2].decode("utf-8", "replace")

    def _read(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("RCON connection closed")
            buf += chunk
        return buf

    def run(self, command):
        out = self._send(2, command)
        print(f"  > {command}\n    {out.strip()}")
        return out


results = []


def check(name, condition):
    results.append((name, condition))
    print(f"[{'PASS' if condition else 'FAIL'}] {name}")


def build_swapped_sensor(rcon, x, y, z):
    """Break an active sculk sensor while its removal neighbor update is suppressed, then put chests there."""
    rcon.run(f"fill {x - 2} {y} {z - 2} {x + 4} {y + 1} {z + 2} air")
    rcon.run(f"setblock {x} {y} {z} sculk_sensor[sculk_sensor_phase=active]")
    rcon.run(f"setblock {x + 1} {y - 1} {z} target")
    rcon.run(f"setblock {x + 1} {y} {z} barrier")
    # destroy mode breaks the block with block updates, like a player would
    swap = rcon.run(f"setblock {x} {y} {z} air destroy")
    rcon.run(f"setblock {x + 1} {y} {z} air")
    rcon.run(f"setblock {x + 1} {y - 1} {z} grass_block")
    kept_after_break = "minecraft:sculk_sensor" in block_entity_id(rcon, x, y, z)
    rcon.run(f"setblock {x} {y} {z} chest")
    rcon.run(f"setblock {x} {y} {z} chest[facing=east]")
    return swap, kept_after_break


def block_entity_id(rcon, x, y, z):
    out = rcon.run(f"data get block {x} {y} {z} id")
    return out


def hear_vibration(rcon, x, y, z, distance=3):
    """An armor stand dies `distance` blocks east of (x, y, z), which is a vibration (entity_die, radius 16)."""
    rcon.run(f"kill @e[type=armor_stand]")
    rcon.run(f"summon armor_stand {x + distance} {y} {z} {{NoGravity:1b}}")
    return rcon.run(f"kill @e[type=armor_stand,x={x + distance},y={y},z={z},distance=..1]")


def load_chunk(rcon, x, z):
    rcon.run(f"forceload add {x} {z}")
    time.sleep(2)


# Every scenario gets its own chunk, far from spawn and from each other.
Y = -60
VANILLA = (520, 520)
KEEP = (680, 520)
DROP = (840, 520)
# Lower case, so Carpet's name lookup cannot change them
BOT_A = "cba_bot_a"
BOT_B = "cba_bot_b"


def translations(rcon):
    print("== translations")
    for lang, name in (("zh_cn", "重新引入非法方块实体"), ("zh_tw", "重新引入非法方塊實體"), ("en_us", "reintroduceIllegalBlockEntity")):
        rcon.run(f"carpet language {lang}")
        check(f"{lang}: rule name is translated", name in rcon.run("carpet reintroduceIllegalBlockEntity"))


def setup(rcon):
    """First server run: build suppressors and check them while their chunks stay loaded."""
    translations(rcon)
    # A dropped sculk sensor item landing next to an illegal sculk sensor would trigger sound suppression
    # inside an entity tick and crash the server, which is real but not what this test is about.
    for rule in ("block_drops", "doTileDrops"):  # renamed in 1.21.11
        if "Incorrect argument" not in rcon.run(f"gamerule {rule} false"):
            break
    print("== rule off: vanilla behaviour")
    rcon.run("carpet reintroduceIllegalBlockEntity false")
    x, z = VANILLA
    load_chunk(rcon, x, z)
    build_swapped_sensor(rcon, x, Y, z)
    check("rule off: mismatched block entity is replaced by a chest one", "minecraft:chest" in block_entity_id(rcon, x, Y, z))
    check("rule off: no sound suppression", FAILED not in hear_vibration(rcon, x, Y, z))

    print("== rule on: illegal block entity survives the swap")
    rcon.run("carpet reintroduceIllegalBlockEntity true")
    for x, z in (KEEP, DROP):
        load_chunk(rcon, x, z)
        swap, kept_after_break = build_swapped_sensor(rcon, x, Y, z)
        check("rule on: breaking the sensor was update suppressed", FAILED in swap)
        check("rule on: sculk sensor block entity kept after its block is broken", kept_after_break)
        check("rule on: sculk sensor block entity kept under the chests placed there", "minecraft:sculk_sensor" in block_entity_id(rcon, x, Y, z))
    x, z = KEEP
    check("rule on: vibration throws (sound suppression)", FAILED in hear_vibration(rcon, x, Y, z))
    check("rule on: suppressor still in place after suppressing", "minecraft:sculk_sensor" in block_entity_id(rcon, x, Y, z))

    sound_suppression_range(rcon)
    lectern_comparator_relay(rcon)
    player_prefix(rcon)

    # No tickets left, so the test chunks are not loaded when the server starts again
    rcon.run("forceload remove all")
    rcon.run("carpet reintroduceIllegalBlockEntity false")
    rcon.run("save-all flush")


def sound_suppression_range(rcon):
    print("== soundSuppressionRange")
    x, z = KEEP
    rcon.run(f"forceload add {x} {z} {x + 80} {z}")
    time.sleep(2)
    # rule value, distance of the vibration from the suppressor (a plain sculk sensor), suppressed?
    # Vibrations 40+ blocks away come from sections vanilla does not look at for a 16 block event.
    cases = [
        (0, 6, True), (0, 12, False),
        (64, 12, True), (64, 40, True), (64, 63, True), (64, 70, False),
        (32, 20, True), (32, 40, False),
        (4, 6, False),
    ]
    for value, distance, expected in cases:
        rcon.run(f"carpet soundSuppressionRange {value}")
        suppressed = FAILED in hear_vibration(rcon, x, Y, z, distance)
        check(f"range {value}: vibration {distance} blocks away {'is' if expected else 'is not'} suppressed", suppressed == expected)
    rcon.run("carpet soundSuppressionRange 32")
    sensor_x = x + 40
    rcon.run(f"setblock {sensor_x} {Y} {z} sculk_sensor")
    time.sleep(1)
    hear_vibration(rcon, sensor_x, Y, z, 12)  # 52 blocks from the suppressor
    time.sleep(1)
    check("range 32: a normal sculk sensor still hears only 8 blocks",
          "passed" in rcon.run(f"execute if block {sensor_x} {Y} {z} minecraft:sculk_sensor[sculk_sensor_phase=inactive]"))
    rcon.run(f"setblock {sensor_x} {Y} {z} air")
    rcon.run("carpet soundSuppressionRange 0")


def lectern_comparator_relay(rcon):
    """
    The usual way to swap a lectern's block entity under a shulker box (a CCE box): breaking a lectern with a book
    clears it, which updates the comparators next to it before its block entity is deleted (until 1.21.4; restored
    by the rule on 1.21.5+). A comparator with nothing under it breaks on that update and updates the suppressor.
    """
    print("== lectern block entity kept through a comparator relay")
    for rule_on, (x, z) in ((True, (760, 560)), (False, (760, 600))):
        state = "on" if rule_on else "off"
        load_chunk(rcon, x, z)
        rcon.run(f"carpet reintroduceIllegalBlockEntity {str(rule_on).lower()}")
        rcon.run(f"fill {x - 2} {Y} {z - 2} {x + 4} {Y + 1} {z + 2} air")
        rcon.run(f"setblock {x} {Y} {z} lectern[has_book=true]")
        # a comparator reading the lectern, left without the block under it, and the suppressor next to it
        rcon.run("carpet fillUpdates false")
        rcon.run(f"setblock {x + 1} {Y} {z} comparator[facing=west]")
        rcon.run(f"setblock {x + 1} {Y - 1} {z} air")
        rcon.run(f"setblock {x + 2} {Y - 1} {z} target")
        rcon.run(f"setblock {x + 2} {Y} {z} barrier")
        rcon.run("carpet fillUpdates true")
        broke = rcon.run(f"setblock {x} {Y} {z} air destroy")
        rcon.run(f"setblock {x + 2} {Y} {z} air")
        rcon.run(f"setblock {x + 2} {Y - 1} {z} grass_block")
        rcon.run(f"setblock {x} {Y} {z} shulker_box")
        under = block_entity_id(rcon, x, Y, z)
        if rule_on:
            check("rule on: breaking the lectern was update suppressed through the comparator", FAILED in broke)
            check("rule on: lectern block entity kept under the shulker box (CCE box)", "minecraft:lectern" in under)
        else:
            check("rule off: the shulker box gets its own block entity", "minecraft:shulker_box" in under)


def spawn_bots(rcon, *names):
    """Carpet looks fake player names up in the background, so wait until they are really online."""
    for i, name in enumerate(names):
        rcon.run(f"player {name} spawn at {2 * i} -60 0")
    for _ in range(40):
        online = rcon.run("list")
        if all(name in online for name in names):
            return
        time.sleep(0.5)
    raise RuntimeError(f"fake players did not join: {names}")


def player_prefix(rcon):
    """Two Carpet fake players; /cbatest as runs a command with BOT_A's own (non-operator) permission."""
    print("== commandPlayerPrefix")
    rcon.run("carpet commandPlayerPrefix false")
    check("rule false: /playerprefix does not exist", "Unknown or incomplete command" in rcon.run(f"playerprefix {BOT_A} Admin"))
    spawn_bots(rcon, BOT_A, BOT_B)
    rcon.run("carpet commandPlayerPrefix true")

    rcon.run(f"playerprefix {BOT_A} Admin red")
    check("operator: prefix shown as [prefix]name", f"[Admin]{BOT_A}" in rcon.run("list"))
    check("operator: prefix keeps its color", "red" in rcon.run(f"playerprefix {BOT_A}"))
    check("prefix is a team prefix, so it shows above the head", BOT_A in rcon.run(f"team list cba_prefix_{BOT_A}"))

    rcon.run(f"cbatest as {BOT_A} playerprefix {BOT_A} Me")
    check("non-operator: sets their own prefix", f"[Me]{BOT_A}" in rcon.run("list"))
    rcon.run(f"cbatest as {BOT_A} playerprefix {BOT_B} Bot gold")
    check("non-operator: sets a fake player's prefix", f"[Bot]{BOT_B}" in rcon.run("list"))
    refused = rcon.run(f"cbatest as {BOT_A} playerprefix Steve Hi")
    check("non-operator: cannot set another player's prefix",
          "only set your own" in refused and "has no prefix" in rcon.run("playerprefix Steve"))
    rcon.run("playerprefix Steve Hi")
    check("operator: can set anyone's prefix, even offline", "[Hi]" in rcon.run("playerprefix Steve"))
    rcon.run('playerprefix Steve ""')

    rcon.run("team add red_team")
    rcon.run(f"team join red_team {BOT_B}")
    rcon.run(f"playerprefix {BOT_B} Bot gold")
    check("a player in another team keeps it", BOT_B in rcon.run("team list red_team"))
    check("... and still shows the prefix in chat and the player list", f"[Bot]{BOT_B}" in rcon.run("list"))

    rcon.run(f'playerprefix {BOT_A} ""')
    names = rcon.run("list")
    check("an empty prefix removes it", BOT_A in names and f"]{BOT_A}" not in names)
    check("... together with its team", f"cba_prefix_{BOT_A}" not in rcon.run("team list"))

    rcon.run("carpet commandPlayerPrefix ops")
    check("rule ops: non-operators cannot use it", "ERROR" in rcon.run(f"cbatest as {BOT_A} playerprefix {BOT_A} X"))
    check("rule ops: operators can", "has no prefix" in rcon.run(f"playerprefix {BOT_A}"))
    rcon.run("carpet commandPlayerPrefix false")
    # BOT_B keeps its prefix and team, to be checked after the restart
    rcon.run(f"player {BOT_A} kill")
    rcon.run(f"player {BOT_B} kill")
    time.sleep(1)


def reload(rcon):
    """Second server run: the chunks are read back from disk."""
    print("== rule on: illegal block entity survives loading the chunk")
    rcon.run("carpet reintroduceIllegalBlockEntity true")
    x, z = KEEP
    check("chunk is not loaded before the test", "not loaded" in block_entity_id(rcon, x, Y, z))
    load_chunk(rcon, x, z)
    check("rule on: block entity still a sculk sensor after loading", "minecraft:sculk_sensor" in block_entity_id(rcon, x, Y, z))
    check("rule on: vibration throws after loading", FAILED in hear_vibration(rcon, x, Y, z))
    rcon.run(f"forceload add {x} {z} {x + 80} {z}")
    rcon.run("carpet soundSuppressionRange 64")
    time.sleep(2)
    check("range 64: a suppressor loaded from disk hears a vibration 40 blocks away", FAILED in hear_vibration(rcon, x, Y, z, 40))
    rcon.run("carpet soundSuppressionRange 0")

    print("== commandPlayerPrefix after a restart")
    spawn_bots(rcon, BOT_B)
    check("a saved prefix is shown again when the player joins", f"[Bot]{BOT_B}" in rcon.run("list"))
    rcon.run("carpet commandPlayerPrefix true")
    check("... with its color", "gold" in rcon.run(f"playerprefix {BOT_B}"))
    rcon.run("carpet commandPlayerPrefix false")
    rcon.run(f"player {BOT_B} kill")

    print("== rule off: loading the chunk drops the illegal block entity like vanilla 1.21.1+")
    rcon.run("carpet reintroduceIllegalBlockEntity false")
    x, z = DROP
    check("chunk is not loaded before the test", "not loaded" in block_entity_id(rcon, x, Y, z))
    load_chunk(rcon, x, z)
    check("rule off: illegal block entity gone after loading", "minecraft:sculk_sensor" not in block_entity_id(rcon, x, Y, z))
    check("rule off: no sound suppression after loading", FAILED not in hear_vibration(rcon, x, Y, z))
    rcon.run("forceload remove all")


def main():
    phase = sys.argv[1] if len(sys.argv) > 1 else "setup"
    rcon = Rcon(*sys.argv[2:3], *[int(port) for port in sys.argv[3:4]], *sys.argv[4:5])
    {"setup": setup, "reload": reload}[phase](rcon)
    passed = sum(1 for _, ok in results if ok)
    print(f"\n{phase}: {passed}/{len(results)} checks passed")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.exit(main())
