package com.alnourkhalifa.scales.core.music

/**
 * Twin of `KeyChordRow` in `Scales/Music/KeyChordTable.swift`: one row of a key table, the key
 * and the chords that belong to it. Swift's `Identifiable` id is [id].
 */
data class KeyChordRow(
    val key: Scale,
    /** The seven chords built from the key's own scale. */
    val chords: List<DiatonicChord>,
    /** "major" or "minor", the word musicians put after the key's root. */
    val familyName: String,
) {
    val id: String
        get() = key.title

    /** The way musicians say the key: "C major", "A minor". */
    val keyName: String
        get() = "${key.root.displayName} $familyName"
}

/**
 * Twin of `KeyChordTable` in `Scales/Music/KeyChordTable.swift`: the major and minor key
 * tables, one row per key, sorted by key name.
 *
 * Rows run alphabetically from A to G so a player can find a key by name at a glance; within
 * a letter the flat comes first, then the natural, then the sharp (A♭, A, A♯), which is also
 * their order in pitch. The twelve keys are the ones with the usual spelling (D♭ major rather
 * than C♯ major, G♯ minor rather than A♭ minor).
 */
object KeyChordTable {
    val majorKeyRoots: List<NoteName> =
        listOf(
            NoteName(Letter.A, -1),
            NoteName(Letter.A),
            NoteName(Letter.B, -1),
            NoteName(Letter.B),
            NoteName(Letter.C),
            NoteName(Letter.D, -1),
            NoteName(Letter.D),
            NoteName(Letter.E, -1),
            NoteName(Letter.E),
            NoteName(Letter.F),
            NoteName(Letter.F, 1),
            NoteName(Letter.G),
        )

    val minorKeyRoots: List<NoteName> =
        listOf(
            NoteName(Letter.A),
            NoteName(Letter.B, -1),
            NoteName(Letter.B),
            NoteName(Letter.C),
            NoteName(Letter.C, 1),
            NoteName(Letter.D),
            NoteName(Letter.D, 1),
            NoteName(Letter.E),
            NoteName(Letter.F),
            NoteName(Letter.F, 1),
            NoteName(Letter.G),
            NoteName(Letter.G, 1),
        )

    fun majorRows(): List<KeyChordRow> {
        val rows = mutableListOf<KeyChordRow>()
        for (root in majorKeyRoots) {
            val key = Scale(root, ScaleType.MAJOR)
            val row =
                KeyChordRow(
                    key = key,
                    chords = key.diatonicTriads(),
                    familyName = "major",
                )
            rows.add(row)
        }
        return rows
    }

    /**
     * Minor keys use the natural minor scale for their seven chords, the standard listing
     * (i ii° III iv v VI VII); harmonic-minor stand-ins are left out on purpose.
     */
    fun minorRows(): List<KeyChordRow> {
        val rows = mutableListOf<KeyChordRow>()
        for (root in minorKeyRoots) {
            val key = Scale(root, ScaleType.NATURAL_MINOR)
            val row =
                KeyChordRow(
                    key = key,
                    chords = key.diatonicTriads(),
                    familyName = "minor",
                )
            rows.add(row)
        }
        return rows
    }
}
