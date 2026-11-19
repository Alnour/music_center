#!/usr/bin/env python3
"""Check that every feature in features.json is proved by a tagged test on every platform.

The owner's rule is that a feature asked for once is built and tested on iPhone/iPad/Mac,
Android phone and Android TV alike. features.json says which platforms owe each feature;
a test proves it by carrying `// feature: id1, id2` on one of the three lines above its
declaration. Where the test lives decides what it proves: interface-test folders prove `ui`
features, any test folder proves `any` features, and Android logic tests count for both
Android columns because phone and TV share the `core` module.

Usage: check_parity.py [--release] [--features] [--self-test]
"""

import argparse
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
TAG = re.compile(r"^\s*//\s*feature:\s*(.+)$")
SWIFT_TEST = re.compile(r"@Test\b|\bfunc test\w*\(")
KOTLIN_TEST = re.compile(r"@Test\b")
LOOKAHEAD = 3
ANDROID = ("androidPhone", "androidTv")
# (folder, file suffix, platforms it proves, "logic" or "ui"); missing folders are skipped.
ROOTS = [
    ("ScalesTests", ".swift", ("ios",), "logic"),
    ("ScalesUITests", ".swift", ("ios",), "ui"),
    ("android/core/src/test", ".kt", ANDROID, "logic"),
    ("android/ui/src/test", ".kt", ANDROID, "logic"),
    ("android/app/src/test", ".kt", ANDROID, "logic"),
    ("android/app/src/androidTest", ".kt", None, "ui"),
]


def find_tags(lines, test_pattern):
    """Return (line number, ids, sits on a test) for every tag, so a tag drifting away from
    its test during an edit is reported instead of silently proving nothing."""
    tags = []
    for index, line in enumerate(lines):
        match = TAG.match(line)
        if not match:
            continue
        ids = [part.strip() for part in match.group(1).split(",") if part.strip()]
        following = lines[index + 1 : index + 1 + LOOKAHEAD]
        on_test = any(test_pattern.search(text) for text in following)
        tags.append((index + 1, ids, on_test))
    return tags


def android_ui_platforms(path):
    """Instrumented tests prove the phone or the TV only by living under app/phone or app/tv,
    because both screens run in one APK and the folder is the only honest signal."""
    parts = path.parts
    for index in range(len(parts) - 1):
        if parts[index] == "app" and parts[index + 1] == "phone":
            return ("androidPhone",)
        if parts[index] == "app" and parts[index + 1] == "tv":
            return ("androidTv",)
    return ()


def per_platform(feature, key, platform):
    """`test`, `laterUntil` and `reason` may be one value for the feature or one per platform."""
    value = feature.get(key)
    if isinstance(value, dict):
        return value.get(platform)
    return value


def registry_errors(registry):
    """Reject a registry the check cannot judge: a `later` with no wave or a `no` with no reason
    would let a platform drop a feature without anyone deciding so."""
    errors = []
    platforms = registry["platforms"]
    seen = set()
    for feature in registry["features"]:
        fid = feature["id"]
        if fid in seen:
            errors.append(f"features.json: duplicate id {fid}")
        seen.add(fid)
        if set(feature["platforms"]) != set(platforms):
            errors.append(f"features.json: {fid} must list exactly {', '.join(platforms)}")
        for platform, status in feature["platforms"].items():
            test = per_platform(feature, "test", platform)
            if status not in ("required", "later", "no"):
                errors.append(f"features.json: {fid}/{platform} has unknown status {status}")
            if test not in ("ui", "any", "manual"):
                errors.append(f"features.json: {fid}/{platform} has unknown test {test}")
            if status == "later" and not per_platform(feature, "laterUntil", platform):
                errors.append(f"features.json: {fid}/{platform} is later without laterUntil")
            needs_reason = status == "no" or test == "manual"
            if needs_reason and not per_platform(feature, "reason", platform):
                errors.append(f"features.json: {fid}/{platform} needs a reason")
    return errors


def collect_hits(known_ids, errors, warnings):
    """Read every test file once and record which (platform, kind) each feature id reaches."""
    hits = {}
    for folder, suffix, platforms, kind in ROOTS:
        root = REPO / folder
        if not root.is_dir():
            continue
        test_pattern = SWIFT_TEST if suffix == ".swift" else KOTLIN_TEST
        for path in sorted(root.rglob(f"*{suffix}")):
            relative = path.relative_to(REPO)
            proves = platforms if platforms is not None else android_ui_platforms(relative)
            lines = path.read_text(encoding="utf-8").splitlines()
            for line_number, ids, on_test in find_tags(lines, test_pattern):
                where = f"{relative}:{line_number}"
                if not on_test:
                    errors.append(f"{where}: tag is not within {LOOKAHEAD} lines above a test")
                    continue
                if not proves:
                    warnings.append(f"{where}: not under app/phone or app/tv, proves nothing")
                for fid in ids:
                    if fid not in known_ids:
                        errors.append(f"{where}: unknown feature id {fid}")
                        continue
                    for platform in proves:
                        hits.setdefault((fid, platform, kind), []).append(where)
    return hits


def judge_cell(feature, platform, hits, release):
    """Return (label, error) for one feature on one platform."""
    fid = feature["id"]
    status = feature["platforms"][platform]
    test = per_platform(feature, "test", platform)
    has_ui = (fid, platform, "ui") in hits
    has_logic = (fid, platform, "logic") in hits
    if status == "no":
        return "n/a", None
    if status == "later":
        until = per_platform(feature, "laterUntil", platform)
        error = f"{fid}/{platform} is still later ({until}) in a release" if release else None
        return "later", error
    if test == "manual":
        return "manual", None
    proved = has_ui if test == "ui" else has_ui or has_logic
    if proved:
        return "✓", None
    wanted = "interface" if test == "ui" else "logic or interface"
    return "MISSING", f"{fid}/{platform} is required but no {wanted} test carries its tag"


def print_registry(registry):
    """`make features`: the registry as a table, without reading any test."""
    platforms = registry["platforms"]
    print(f"{'id':22}" + "".join(f"{p:15}" for p in platforms) + f"{'test':20}title")
    for feature in registry["features"]:
        row = f"{feature['id']:22}"
        tests = []
        for platform in platforms:
            status = feature["platforms"][platform]
            if status == "later":
                until = per_platform(feature, "laterUntil", platform)
                status = "later " + until.replace("wave ", "w")
            row += f"{status:15}"
            tests.append(per_platform(feature, "test", platform))
        test_text = tests[0] if len(set(tests)) == 1 else "/".join(tests)
        print(row + f"{test_text:20}" + feature["title"])


def check(registry, release):
    """Print one row per feature, then every warning and error; return the exit status."""
    errors = registry_errors(registry)
    warnings = []
    platforms = registry["platforms"]
    known_ids = {feature["id"] for feature in registry["features"]}
    hits = collect_hits(known_ids, errors, warnings)
    counts = {}
    print(f"{'feature':22}" + "".join(f"{p:14}" for p in platforms))
    for feature in registry["features"]:
        row = f"{feature['id']:22}"
        for platform in platforms:
            label, error = judge_cell(feature, platform, hits, release)
            row += f"{label:14}"
            counts[label] = counts.get(label, 0) + 1
            if error:
                errors.append(error)
            # A logic test under android/core proves both Android columns at once, so it is
            # no mistake for it to carry a feature the TV does not offer; only an interface
            # test written for a platform marked "no" is worth a warning.
            ui_tag = (feature["id"], platform, "ui") in hits
            if label == "n/a" and ui_tag:
                warnings.append(f"{feature['id']} is tagged for {platform}, which is marked no")
        print(row)
    summary = " · ".join(f"{label} {count}" for label, count in sorted(counts.items()))
    print(f"\n{len(registry['features'])} features · {summary}")
    for warning in warnings:
        print(f"warning: {warning}")
    for error in errors:
        print(f"error: {error}")
    print(f"{len(errors)} errors, {len(warnings)} warnings")
    return 1 if errors else 0


def self_test():
    """The matcher on inline samples, so the tag grammar can be checked without a test tree."""
    swift = [
        "// feature: board, labels",
        "@Test func gMajor() {}",
        "    // feature: notes-row",
        "    /// Explains why.",
        "    func testNotesRow() {",
        "// feature: keyboard",
        "",
        "",
        "",
        "@Test func tooFar() {}",
        "let x = 1  // feature: board",
    ]
    kotlin = ["// feature: chord-chips-rings", "@Test", "fun ringsTheChord() {}"]
    assert find_tags(swift, SWIFT_TEST) == [
        (1, ["board", "labels"], True),
        (3, ["notes-row"], True),
        (6, ["keyboard"], False),
    ]
    assert find_tags(kotlin, KOTLIN_TEST) == [(1, ["chord-chips-rings"], True)]
    assert android_ui_platforms(Path("x/scales/app/phone/PhoneScalesTest.kt")) == ("androidPhone",)
    assert android_ui_platforms(Path("x/scales/app/tv/TvBoardTest.kt")) == ("androidTv",)
    assert android_ui_platforms(Path("x/scales/app/GlyphTest.kt")) == ()
    print("self-test passed")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--release", action="store_true", help="fail on any later cell")
    parser.add_argument("--features", action="store_true", help="print the registry only")
    parser.add_argument("--self-test", action="store_true", help="test the tag matcher")
    arguments = parser.parse_args()
    if arguments.self_test:
        return self_test()
    registry = json.loads((REPO / "features.json").read_text(encoding="utf-8"))
    if arguments.features:
        print_registry(registry)
        return 0
    return check(registry, arguments.release)


if __name__ == "__main__":
    sys.exit(main())
