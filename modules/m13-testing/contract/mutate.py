#!/usr/bin/env python3
"""Apply one mutant to a copy of an app: python3 mutate.py <mutants.json> <id> <app-dir>.

A mutants file is a JSON list of {"id", "file", "old", "new", "what"}: in <app-dir>/<file>, the text "old"
must occur exactly once and is replaced by "new". Prints "what". Exit 2 when "old" is missing or not unique,
so a mutant that no longer matches the app is an error, not a silent survivor.
"""
import json, sys, pathlib

mutants, wanted, root = sys.argv[1], sys.argv[2], pathlib.Path(sys.argv[3])
for m in json.load(open(mutants)):
    if m["id"] != wanted:
        continue
    p = root / m["file"]
    s = p.read_text()
    if s.count(m["old"]) != 1:
        print(f"mutant {wanted}: expected exactly one match in {m['file']}, found {s.count(m['old'])}", file=sys.stderr)
        sys.exit(2)
    p.write_text(s.replace(m["old"], m["new"]))
    print(m["what"])
    sys.exit(0)
print(f"no mutant {wanted} in {mutants}", file=sys.stderr)
sys.exit(2)
