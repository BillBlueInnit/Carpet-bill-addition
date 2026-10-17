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

PHASE = sys.argv[1] if len(sys.argv) > 1 else "setup"
HOST = sys.argv[2] if len(sys.argv) > 2 else "127.0.0.1"
PORT = int(sys.argv[3]) if len(sys.argv) > 3 else 25575
PASSWORD = sys.argv[4] if len(sys.argv) > 4 else "cba-test"

FAILED = "An unexpected error occurred"


class Rcon:
    def __init__(self):
        self.sock = socket.create_connection((HOST, PORT), timeout=30)
        self.request_id = 0
        self._send(3, PASSWORD)

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
    """Replace an active sculk sensor with a chest while its removal neighbor update is suppressed."""
    rcon.run(f"fill {x - 2} {y} {z - 2} {x + 4} {y + 1} {z + 2} air")
    rcon.run(f"setblock {x} {y} {z} sculk_sensor[sculk_sensor_phase=active]")
    rcon.run(f"setblock {x + 1} {y - 1} {z} target")
    rcon.run(f"setblock {x + 1} {y} {z} barrier")
    swap = rcon.run(f"setblock {x} {y} {z} chest")
    rcon.run(f"setblock {x + 1} {y} {z} air")
    rcon.run(f"setblock {x + 1} {y - 1} {z} grass_block")
    return swap


def block_entity_id(rcon, x, y, z):
    out = rcon.run(f"data get block {x} {y} {z} id")
    return out


def hear_vibration(rcon, x, y, z):
    rcon.run(f"kill @e[type=armor_stand]")
    rcon.run(f"summon armor_stand {x + 3} {y} {z} {{NoGravity:1b}}")
    return rcon.run(f"kill @e[type=armor_stand,x={x + 3},y={y},z={z},distance=..1]")


def load_chunk(rcon, x, z):
    rcon.run(f"forceload add {x} {z}")
    time.sleep(2)


# Every scenario gets its own chunk, far from spawn and from each other.
Y = -60
VANILLA = (520, 520)
KEEP = (680, 520)
DROP = (840, 520)


def setup(rcon):
    """First server run: build suppressors and check them while their chunks stay loaded."""
    print("== rule off: vanilla behaviour")
    rcon.run("carpet reintroduceIllegalBlockEntity false")
    x, z = VANILLA
    load_chunk(rcon, x, z)
    build_swapped_sensor(rcon, x, Y, z)
    rcon.run(f"setblock {x} {Y} {z} chest[facing=east]")
    check("rule off: mismatched block entity is replaced by a chest one", "minecraft:chest" in block_entity_id(rcon, x, Y, z))
    check("rule off: no sound suppression", FAILED not in hear_vibration(rcon, x, Y, z))

    print("== rule on: illegal block entity survives the swap")
    rcon.run("carpet reintroduceIllegalBlockEntity true")
    for x, z in (KEEP, DROP):
        load_chunk(rcon, x, z)
        build_swapped_sensor(rcon, x, Y, z)
        check("rule on: sculk sensor block entity kept under the chest", "minecraft:sculk_sensor" in block_entity_id(rcon, x, Y, z))
        rcon.run(f"setblock {x} {Y} {z} chest[facing=east]")
        check("rule on: block entity survives a block state update", "minecraft:sculk_sensor" in block_entity_id(rcon, x, Y, z))
    x, z = KEEP
    check("rule on: vibration throws (sound suppression)", FAILED in hear_vibration(rcon, x, Y, z))
    check("rule on: suppressor still in place after suppressing", "minecraft:sculk_sensor" in block_entity_id(rcon, x, Y, z))

    # No tickets left, so the test chunks are not loaded when the server starts again
    rcon.run("forceload remove all")
    rcon.run("carpet reintroduceIllegalBlockEntity false")
    rcon.run("save-all flush")


def reload(rcon):
    """Second server run: the chunks are read back from disk."""
    print("== rule on: illegal block entity survives loading the chunk")
    rcon.run("carpet reintroduceIllegalBlockEntity true")
    x, z = KEEP
    check("chunk is not loaded before the test", "not loaded" in block_entity_id(rcon, x, Y, z))
    load_chunk(rcon, x, z)
    check("rule on: block entity still a sculk sensor after loading", "minecraft:sculk_sensor" in block_entity_id(rcon, x, Y, z))
    check("rule on: vibration throws after loading", FAILED in hear_vibration(rcon, x, Y, z))

    print("== rule off: loading the chunk drops the illegal block entity like vanilla 1.21.1+")
    rcon.run("carpet reintroduceIllegalBlockEntity false")
    x, z = DROP
    check("chunk is not loaded before the test", "not loaded" in block_entity_id(rcon, x, Y, z))
    load_chunk(rcon, x, z)
    check("rule off: illegal block entity gone after loading", "minecraft:sculk_sensor" not in block_entity_id(rcon, x, Y, z))
    check("rule off: no sound suppression after loading", FAILED not in hear_vibration(rcon, x, Y, z))
    rcon.run("forceload remove all")


def main():
    phase = PHASE
    rcon = Rcon()
    {"setup": setup, "reload": reload}[phase](rcon)
    passed = sum(1 for _, ok in results if ok)
    print(f"\n{phase}: {passed}/{len(results)} checks passed")
    return 0 if passed == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())
