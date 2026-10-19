"""
Tick-by-tick projectile movement test for legacyThrowableMovement.

Needs `gradlew runTestServer` (the dev-only cba-testmod prints a CBA-TRACE line with the exact position and
velocity of every tagged entity at the end of each server tick and when it is removed).

    python scripts/movement_test.py record <server log> <out.json> <mode>...
        Runs every case once per mode and writes the traces. Use the mode "vanilla" on a version without the rule.
    python scripts/movement_test.py compare <reference.json> <target.json>
        reference: "vanilla" traces recorded on Minecraft 1.21.1 (scripts/reference/movement-1.21.1.json)
        target:    "off", "pearl" and "all" traces recorded on the version under test
"""
import json
import re
import sys
import time

from server_test import Rcon

X0, Y0, Z0 = 1024, -40, 1024
LANE = 8
LAST_COMPARED_TICK = 80

PEARL = ["ender_pearl"]
LEGACY_IN_ALL = {"snowball", "egg", "potion", "experience_bottle"}


def wall(rcon, x, y, z):
    rcon.run(f"fill {x + 7} {y - 3} {z - 3} {x + 7} {y + 3} {z + 3} stone")


def water_tank(rcon, x, y, z):
    rcon.run(f"fill {x - 1} {y - 2} {z - 2} {x + 14} {y + 2} {z + 2} glass hollow")
    rcon.run(f"fill {x} {y - 1} {z - 1} {x + 13} {y + 1} {z + 1} water")


def armor_stand(rcon, x, y, z):
    # The pearl passes 0.45 from the stand's center line, inside the 0.3 entity hit margin 1.21.1 always used.
    # 1.21.6+ grows the margin from 0 over the first ticks and misses it.
    # Integer summon coordinates are centered, so the pearl flies along z + 0.5.
    rcon.run(f'summon armor_stand {x + 4.5} {y - 0.5} {z + 0.05} {{NoGravity:1b,Tags:["cba_trace"]}}')


def bubble_column(rcon, x, y, z):
    rcon.run(f"fill {x - 1} {y - 3} {z - 1} {x + 1} {y + 3} {z + 1} glass hollow")
    rcon.run(f"setblock {x} {y - 2} {z} soul_sand")
    rcon.run(f"fill {x} {y - 1} {z} {x} {y + 2} {z} water")


# name, entity ids to try (renamed between versions), start offset, motion, setup
CASES = [
    ("pearl_rest", PEARL, (0, 0, 0), (0.0, 0.0, 0.0), None),
    ("pearl_slow", PEARL, (0, 0, 0), (0.1, 0.0, 0.0), None),
    ("pearl_throw", PEARL, (0, 0, 0), (1.5, 0.4, 0.25), None),
    ("pearl_fast_x", PEARL, (0, 0, 0), (25.0, 0.0, 0.0), None),
    ("pearl_fast_up", PEARL, (0, 0, 0), (0.0, 20.0, 0.0), None),
    ("pearl_extreme", PEARL, (0, 0, 0), (2000.0, 0.0, 0.0), None),
    ("pearl_wall", PEARL, (0, 0, 0), (2.0, 0.05, 0.0), wall),
    ("pearl_water", PEARL, (1, 0, 0), (0.8, 0.0, 0.0), water_tank),
    ("pearl_bubble", PEARL, (0, 0, 0), (0.0, 0.1, 0.0), bubble_column),
    ("snowball", ["snowball"], (0, 0, 0), (1.5, 0.4, 0.25), None),
    ("egg", ["egg"], (0, 0, 0), (1.5, 0.4, 0.25), None),
    ("potion", ["splash_potion", "potion"], (0, 0, 0), (1.5, 0.4, 0.25), None),
    ("experience_bottle", ["experience_bottle"], (0, 0, 0), (1.5, 0.4, 0.25), None),
    ("arrow", ["arrow"], (0, 0, 0), (1.5, 0.4, 0.25), None),
    ("pearl_near_entity", PEARL, (0, 0, 0), (1.0, 0.0, 0.0), armor_stand),
    ("pearl_diagonal", PEARL, (0, 0, 0), (6.0, 1.5, 4.5), None),
]


def game_time(rcon):
    # "The time is 122" before 26.x, "The game time is 122 tick(s)" after
    return int(re.search(r"\d+", rcon.run("time query gametime")).group())


def run_mode(rcon, mode):
    print(f"== mode {mode}")
    if mode != "vanilla":
        rcon.run(f"carpet legacyThrowableMovement {mode}")
    for i, (name, types, offset, motion, setup) in enumerate(CASES):
        x, y, z = X0, Y0, Z0 + LANE * i
        rcon.run(f"fill {x - 2} {y - 4} {z - 3} {x + 40} {y + 4} {z + 3} air")
        if setup:
            setup(rcon, x, y, z)
    time.sleep(2)  # let bubble columns form
    for i, (name, types, offset, motion, setup) in enumerate(CASES):
        x, y, z = X0 + offset[0], Y0 + offset[1], Z0 + LANE * i + offset[2]
        # The test mod sets the velocity from the tag before the first tick; Motion NBT caps components at 10
        motion_tag = "cba_motion=" + ",".join(repr(float(component)) for component in motion)
        nbt = f'{{Tags:["cba_trace","cba_case_{mode}.{name}","{motion_tag}"]}}'
        for entity_type in types:
            if "Summoned" in rcon.run(f"summon {entity_type} {x} {y} {z} {nbt}"):
                break
        else:
            raise RuntimeError(f"could not summon {name}")
    # Wait for game ticks, not seconds: a slow server would otherwise kill the projectiles too early
    start = game_time(rcon)
    while game_time(rcon) - start < LAST_COMPARED_TICK + 10:
        time.sleep(0.5)
    rcon.run("kill @e[tag=cba_trace]")
    rcon.run("kill @e[type=chicken]")
    rcon.run("kill @e[type=experience_orb]")


def read_traces(log_path):
    """Ticks and removals by the projectile itself; the kills at the end and chunk unloads depend on timing."""
    traces = {}
    with open(log_path, encoding="utf-8", errors="replace") as log:
        for line in log:
            at = line.find("CBA-TRACE ")
            if at < 0:
                continue
            case, event, tick, *numbers = line[at:].split()[1:]
            if int(tick) > LAST_COMPARED_TICK or event in ("removed:KILLED", "removed:UNLOADED_TO_CHUNK"):
                continue
            entry = [event, int(tick)] + numbers
            trace = traces.setdefault(case, [])
            if trace and int(tick) < trace[-1][1]:
                continue  # the entity was saved with its chunk and loaded again, which restarts its tick count
            if not trace or trace[-1] != entry:  # an entity outside ticking chunks repeats its last state
                trace.append(entry)
    return traces


def record(log_path, out_path, modes):
    rcon = Rcon()
    rcon.run(f"forceload add {X0 - 16} {Z0 - 16} {X0 + 239} {Z0 + LANE * len(CASES) + 15}")
    time.sleep(3)
    for mode in modes:
        run_mode(rcon, mode)
    rcon.run("forceload remove all")
    time.sleep(1)
    traces = read_traces(log_path)
    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        json.dump(traces, out, indent=1, sort_keys=True)
    print(f"recorded {len(traces)} traces to {out_path}")


def compare(reference_path, target_path):
    reference = json.load(open(reference_path, encoding="utf-8"))
    target = json.load(open(target_path, encoding="utf-8"))
    results = []

    def expect(name, actual, expected, equal=True):
        same = actual == expected
        ok = actual is not None and expected is not None and same == equal
        results.append(ok)
        print(f"[{'PASS' if ok else 'FAIL'}] {name}")
        if not ok and actual and expected:
            for a, e in zip(actual, expected):
                if a != e:
                    print(f"    first difference: got {a}\n                      want {e}")
                    break

    for name, types, *_ in CASES:
        vanilla_1211 = reference.get(f"vanilla.{name}")
        off = target.get(f"off.{name}")
        legacy_in_all = types == PEARL or name in LEGACY_IN_ALL
        if types == PEARL:
            expect(f"pearl mode: {name} moves exactly like 1.21.1", target.get(f"pearl.{name}"), vanilla_1211)
        else:
            expect(f"pearl mode: {name} stays vanilla", target.get(f"pearl.{name}"), off)
        if legacy_in_all:
            expect(f"all mode: {name} moves exactly like 1.21.1", target.get(f"all.{name}"), vanilla_1211)
        else:
            expect(f"all mode: {name} stays vanilla", target.get(f"all.{name}"), off)
    # Sanity check that the test can tell the difference at all
    expect("off mode: a thrown pearl moves differently from 1.21.1", target.get("off.pearl_throw"),
           reference.get("vanilla.pearl_throw"), equal=False)
    print(f"\n{sum(results)}/{len(results)} checks passed")
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    if sys.argv[1] == "record":
        record(sys.argv[2], sys.argv[3], sys.argv[4:])
    elif sys.argv[1] == "compare":
        sys.exit(compare(sys.argv[2], sys.argv[3]))
    else:
        sys.exit(__doc__)
