#!/usr/bin/env python3
"""Turns the solved repo into the attendee starter: every LAB-N region becomes a TODO stub."""
import re, sys, pathlib

STUBS = {
    "LAB-1": '        TODO("LAB-1: decide the tiers. Order matters: privacy, offline, battery, budget, confidence.")\n',
    "LAB-2": (
        "        for (tier in route.tiers) {\n"
        "            TODO(\"LAB-2: try each tier in order, record an Attempt, stop at the first valid answer.\")\n"
        "        }\n"
    ),
    "LAB-3": (
        "    override fun validate(input: Message, output: Extraction): List<String> =\n"
        "        TODO(\"LAB-3: reject anything the message does not literally support.\")\n"
    ),
    "LAB-4": (
        "    fun nudges(now: LocalDateTime, extracted: Map<Message, List<ActionItem>>, horizonDays: Int = 3, max: Int = 2): List<Nudge> =\n"
        "        TODO(\"LAB-4: at most two nudges, soonest first, never for deliveries.\")\n"
    ),
}

REGION = re.compile(r"^[ \t]*// region (LAB-\d)\n.*?^[ \t]*// endregion\n", re.S | re.M)


def strip(root, only):
    for path in root.rglob("*.kt"):
        if "/build/" in str(path):
            continue
        text = path.read_text()
        new = REGION.sub(lambda m: STUBS[m.group(1)] if (only is None or m.group(1) in only) else m.group(0), text)
        if new != text:
            path.write_text(new)
            print(f"stubbed {path.relative_to(root)}")


if __name__ == "__main__":
    root = pathlib.Path(sys.argv[1]).resolve()
    labs = set(sys.argv[2:]) or None
    strip(root, labs)
