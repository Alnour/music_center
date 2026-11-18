#!/usr/bin/env python3
"""Check that the Swift logic files and their Kotlin twins in android/core come in pairs.

Android mirrors the Swift logic file for file, with the same names, so a change on one side
points at exactly one file on the other. A Swift logic file with no Kotlin twin, or a Kotlin
file with no Swift twin, means a feature landed on one platform only. PENDING lists the
Swift files not ported yet; it shrinks wave by wave and is empty once Android core is done.
"""

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
CORE = "android/core/src/main/kotlin/com/alnourkhalifa/scales/core"
# Every Swift file in these folders has a same-named Kotlin file in the paired folder.
FOLDERS = {
    "Scales/Music": f"{CORE}/music",
    "Scales/Session": f"{CORE}/session",
    "Scales/Drums": f"{CORE}/drums",
}
# Swift files outside those folders whose logic Kotlin also keeps.
EXPLICIT = {
    "Scales/Views/FretboardGeometry.swift": f"{CORE}/board/FretboardGeometry.kt",
    "Scales/Views/KeyboardGeometry.swift": f"{CORE}/board/KeyboardGeometry.kt",
    "Scales/Views/DrumStaffLayout.swift": f"{CORE}/board/DrumStaffLayout.kt",
    "Scales/Views/BoardChordChips.swift": f"{CORE}/board/BoardChordChips.kt",
    "Scales/Views/ChordTableMetrics.swift": f"{CORE}/board/ChordTableMetrics.kt",
    "Scales/Views/TVLayoutMetrics.swift": f"{CORE}/board/TVLayoutMetrics.kt",
}
# Apple-only: the audio engine player and the AirPlay casting protocol.
SWIFT_ONLY = {"Scales/Drums/DrumPlayer.swift", "Scales/Session/BoardCaster.swift"}
# Kotlin-only: the immutable snapshot Compose observes, and the storage seam and library JSON
# that UserDefaults and Codable provide for free on iOS.
KOTLIN_ONLY = {
    f"{CORE}/session/SessionState.kt",
    f"{CORE}/session/KeyValueStore.kt",
    f"{CORE}/session/SettingsStore.kt",
    f"{CORE}/session/ScaleLibraryJson.kt",
}
# Not ported yet; wave 1 empties this.
PENDING = {
    "Scales/Music/ChordChangeReport.swift",
    "Scales/Music/ScaleEdit.swift",
    "Scales/Music/ScaleIdentifier.swift",
    "Scales/Music/TransformationLesson.swift",
    "Scales/Session/PracticeSession.swift",
    "Scales/Session/PracticeSettings.swift",
    "Scales/Session/ScaleLibrary.swift",
    "Scales/Views/FretboardGeometry.swift",
    "Scales/Views/KeyboardGeometry.swift",
    "Scales/Views/DrumStaffLayout.swift",
    "Scales/Music/ScaleEditMenus.swift",
    "Scales/Views/BoardChordChips.swift",
    "Scales/Views/ChordTableMetrics.swift",
    "Scales/Views/TVLayoutMetrics.swift",
}


def expected_pairs():
    """Map each Swift logic file present today to the Kotlin file that must mirror it."""
    pairs = {}
    for swift_folder, kotlin_folder in FOLDERS.items():
        for path in sorted((REPO / swift_folder).glob("*.swift")):
            pairs[f"{swift_folder}/{path.name}"] = f"{kotlin_folder}/{path.stem}.kt"
    for swift_file, kotlin_file in EXPLICIT.items():
        if (REPO / swift_file).is_file():
            pairs[swift_file] = kotlin_file
    return pairs


def main():
    if not (REPO / "android/core").is_dir():
        print("mirror: android/core does not exist yet, skipped")
        return 0
    pairs = expected_pairs()
    problems = []
    for swift_file, kotlin_file in pairs.items():
        if swift_file in SWIFT_ONLY or swift_file in PENDING:
            continue
        if not (REPO / kotlin_file).is_file():
            problems.append(f"{swift_file} has no Kotlin twin {kotlin_file}")
    mirrored = set(pairs.values())
    kotlin_folders = set(FOLDERS.values()) | {f"{CORE}/board"}
    for folder in sorted(kotlin_folders):
        for path in sorted((REPO / folder).glob("*.kt")):
            kotlin_file = f"{folder}/{path.name}"
            if kotlin_file not in mirrored and kotlin_file not in KOTLIN_ONLY:
                problems.append(f"{kotlin_file} has no Swift twin")
    for problem in problems:
        print(f"error: {problem}")
    pending_count = len(PENDING & set(pairs))
    print(f"mirror: {len(pairs)} Swift logic files, {pending_count} pending, {len(problems)} errors")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
