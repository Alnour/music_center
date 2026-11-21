package com.alnourkhalifa.scales.app

import com.alnourkhalifa.scales.core.music.Capo
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.Tuning

/**
 * One row of the capo picker on the phone and the TV: the fret it puts the capo on, the short
 * [title] shown on the picker's button, and the [label] shown in the list.
 */
data class CapoChoice(
    val fret: Int,
    val title: String,
    val label: String,
)

/**
 * The capo picker's rows for a scale in a tuning. A tuning with the standard chord shapes
 * lists `Capo.suggestions`, so each fret says how many chords turn open there; the others
 * still let the player clamp a capo on, so they list the bare frets ("No capo", "Fret 1" …)
 * from `Capo.fretChoices`. Shared by the phone's sheet and the TV's options panel, so both
 * offer the same rows in the same words.
 */
fun capoChoices(
    scale: Scale,
    tuning: Tuning,
): List<CapoChoice> {
    val choices = mutableListOf<CapoChoice>()
    val suggestions = Capo.suggestions(scale, tuning)
    if (suggestions.isNotEmpty()) {
        for (suggestion in suggestions) {
            choices.add(CapoChoice(suggestion.capoFret, suggestion.title, suggestion.label))
        }
        return choices
    }
    for (fret in Capo.fretChoices) {
        val title = plainCapoTitle(fret)
        choices.add(CapoChoice(fret, title, title))
    }
    return choices
}

/** The fret's name with nothing about chords: "No capo" or "Fret 3", as `CapoSuggestion`
 *  words its title. */
fun plainCapoTitle(fret: Int): String {
    if (fret == 0) {
        return "No capo"
    }
    return "Fret $fret"
}
