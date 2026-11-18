package com.alnourkhalifa.scales.ui

import com.alnourkhalifa.scales.core.music.ScaleTone
import com.alnourkhalifa.scales.core.session.NoteLabelStyle

/**
 * The text inside a note circle for a given label style: the note's name, its degree, or
 * nothing. Swift writes this as a private `label(for:)` in both `FretboardView.swift` and
 * `KeyboardView.swift`; Kotlin keeps one copy here so the fretboard and the keyboard can
 * never disagree about what a circle says.
 */
fun NoteLabelStyle.label(tone: ScaleTone): String? =
    when (this) {
        NoteLabelStyle.NOTE_NAMES -> tone.name.displayName
        NoteLabelStyle.DEGREES -> tone.degree.label
        NoteLabelStyle.NONE -> null
    }
