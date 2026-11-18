// The file keeps the name of its Swift twin, which the mirror check pairs it by, although its
// one class is EditChoices.
@file:Suppress("MatchingDeclarationName")

package com.alnourkhalifa.scales.core.music

// Twin of `Scales/Music/ScaleEditMenus.swift`: what the note-change menus offer, as plain
// data. The phone and TV menus read these, and `expected/answers.json` pins the titles.

/**
 * Twin of `EditChoices` in `Scales/Music/ScaleEditMenus.swift`: the changes a scale allows,
 * sorted once into the menus that offer them, one list per note (lower, raise, remove) and
 * one list of notes that could be added.
 *
 * Differs from Swift: `edits(for:)` is `edits(degree)`, since `for` is a Kotlin keyword.
 */
class EditChoices(
    definition: ScaleDefinition,
) {
    private val editsByDegree: Map<ScaleDegree, List<ScaleEdit>>

    /** Notes that could be added, in letter order. */
    val additions: List<ScaleEdit>

    init {
        val editsByDegree = mutableMapOf<ScaleDegree, MutableList<ScaleEdit>>()
        val additions = mutableListOf<ScaleEdit>()
        for (edit in definition.legalEdits()) {
            if (edit is ScaleEdit.Add) {
                additions.add(edit)
            } else {
                val degreeEdits = editsByDegree.getOrPut(edit.degree) { mutableListOf() }
                degreeEdits.add(edit)
            }
        }
        this.editsByDegree = editsByDegree
        this.additions = additions
    }

    /** The lower, raise and remove choices for one note, in that order; empty for the root. */
    fun edits(degree: ScaleDegree): List<ScaleEdit> = editsByDegree[degree] ?: emptyList()
}

/**
 * The edit as a menu item, with the notes spelled in the key it is offered in: "Lower to F
 * (♭7)", "Raise to G♯ (♯4)", "Remove F♯", "Add D♭ (♭2)".
 */
fun ScaleEdit.menuTitle(above: NoteName): String {
    val note = Scale.spell(degree, above).displayName
    val result = resultingDegree ?: return "Remove $note"
    val newNote = Scale.spell(result, above).displayName
    return when (this) {
        is ScaleEdit.Lower -> "Lower to $newNote (${result.label})"
        is ScaleEdit.Raise -> "Raise to $newNote (${result.label})"
        is ScaleEdit.Add, is ScaleEdit.Remove -> "Add $newNote (${result.label})"
    }
}
