package com.alnourkhalifa.scales.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleTone

/**
 * The scale spelled out as a row of chips, each with its degree underneath, so the player can
 * read the formula (1 2 ♭3 …) as well as the note names. A lesson rings the notes it is about:
 * the one that was changed, and what it became; the Scales screen rings the notes of the chord
 * the player picked. Twin of `Scales/Views/ScaleNotesRow.swift`.
 *
 * What differs: [noteSize] is in dp and the text ignores the system font scale, as SwiftUI's
 * fixed `.system(size:)` does, so the chips keep their proportions on every phone and TV.
 */
@Composable
fun ScaleNotesRow(
    scale: Scale,
    noteSize: Dp,
    palette: FretboardPalette,
    modifier: Modifier = Modifier,
    highlightedNotes: Set<NoteName> = emptySet(),
) {
    val fixedFontDensity = Density(LocalDensity.current.density, fontScale = 1f)
    val spokenNotes = spokenDescription(scale)
    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
        Row(
            modifier = modifier.clearAndSetSemantics { contentDescription = spokenNotes },
            horizontalArrangement = Arrangement.spacedBy(noteSize * 0.45f),
        ) {
            for (tone in scale.tones) {
                val isHighlighted = tone.name in highlightedNotes
                NoteChip(tone, noteSize, palette, isHighlighted)
            }
        }
    }
}

/** One note: its name in a circle and its degree underneath. */
@Composable
private fun NoteChip(
    tone: ScaleTone,
    noteSize: Dp,
    palette: FretboardPalette,
    isHighlighted: Boolean,
) {
    val colors = palette.toneColors(isRoot = tone.isRoot, isInChord = isHighlighted)
    // With a font scale of 1 a dp value is the same number of sp.
    val nameStyle =
        TextStyle(
            color = colors.text,
            fontSize = noteSize.value.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MusicFontFamily,
        )
    val degreeStyle =
        TextStyle(
            color = palette.secondaryText,
            fontSize = (noteSize.value * 0.6f).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MusicFontFamily,
        )
    var circleModifier =
        Modifier
            .size(noteSize * 2)
            .background(colors.fill, CircleShape)
    if (isHighlighted) {
        // Compose borders are drawn inside the shape, like SwiftUI's `strokeBorder`.
        circleModifier = circleModifier.border(noteSize * 0.18f, palette.chordRing, CircleShape)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(noteSize * 0.2f),
    ) {
        Box(modifier = circleModifier, contentAlignment = Alignment.Center) {
            BasicText(text = tone.name.displayName, style = nameStyle, maxLines = 1)
        }
        BasicText(text = tone.degree.label, style = degreeStyle, maxLines = 1)
    }
}

/** What TalkBack reads for the whole row: "Notes: G, A, B, C, D, E, F♯". */
private fun spokenDescription(scale: Scale): String {
    val names = mutableListOf<String>()
    for (tone in scale.tones) {
        names.add(tone.name.displayName)
    }
    return "Notes: " + names.joinToString(separator = ", ")
}
