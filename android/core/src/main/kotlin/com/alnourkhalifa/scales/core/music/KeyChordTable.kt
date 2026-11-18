package com.alnourkhalifa.scales.core.music

/**
 * Twin of `KeyChordRow` in `Scales/Music/KeyChordTable.swift`: one row of a key table, the key
 * and the chords that belong to it. Swift's `Identifiable` id is [id].
 */
data class KeyChordRow(
    val key: Scale,
    /** The seven chords built from the key's own scale. */
    val chords: List<DiatonicChord>,
    /** Chords from a related scale that can stand in for the chord at a given step, keyed by
     *  step index (minor keys only, see [KeyChordTable.minorRows]). */
    val alternatives: Map<Int, DiatonicChord>,
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
    /** The step of the dominant chord (v in natural minor, V in harmonic minor). */
    private const val DOMINANT_STEP = 4

    /** The step of the chord on the seventh note (VII in natural minor, vii° in harmonic). */
    private const val SEVENTH_STEP = 6

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

    /** Steps whose chord changes when the seventh note is raised (harmonic minor): the fifth
     *  step (v becomes V) and the seventh (VII becomes vii°). */
    val harmonicMinorSteps: List<Int> = listOf(DOMINANT_STEP, SEVENTH_STEP)

    fun majorRows(): List<KeyChordRow> {
        val rows = mutableListOf<KeyChordRow>()
        for (root in majorKeyRoots) {
            val key = Scale(root, ScaleType.MAJOR)
            val row =
                KeyChordRow(
                    key = key,
                    chords = key.diatonicTriads(),
                    alternatives = emptyMap(),
                    familyName = "major",
                )
            rows.add(row)
        }
        return rows
    }

    /**
     * Minor keys use the natural minor scale for their seven chords. Harmonic minor raises the
     * seventh note, which turns the minor v into a major V, the chord most minor-key songs use
     * to pull back home, and VII into vii°. Those two are offered as alternatives at the steps
     * they replace.
     */
    fun minorRows(): List<KeyChordRow> {
        val rows = mutableListOf<KeyChordRow>()
        for (root in minorKeyRoots) {
            val key = Scale(root, ScaleType.NATURAL_MINOR)
            val harmonic = Scale(root, ScaleType.HARMONIC_MINOR).diatonicTriads()
            val alternatives = mutableMapOf<Int, DiatonicChord>()
            for (step in harmonicMinorSteps) {
                alternatives[step] = harmonic[step]
            }
            val row =
                KeyChordRow(
                    key = key,
                    chords = key.diatonicTriads(),
                    alternatives = alternatives,
                    familyName = "minor",
                )
            rows.add(row)
        }
        return rows
    }
}
