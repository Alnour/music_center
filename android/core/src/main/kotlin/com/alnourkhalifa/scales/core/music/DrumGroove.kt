package com.alnourkhalifa.scales.core.music

/**
 * The three voices of a basic kit, each with its place on the drum staff.
 *
 * Twin of `DrumVoice` in `Scales/Music/DrumGroove.swift`. Kotlin enums already own a final
 * `name`, so the label the player reads is [displayName].
 */
enum class DrumVoice(
    val displayName: String,
) {
    HI_HAT("Hi-hat"),
    SNARE("Snare"),
    KICK("Kick"),
}

/**
 * One stroke: which voice, and where in the bar it lands. Twin of `DrumHit`.
 *
 * @property step counted in the groove's subdivisions from the start of the bar (0 is beat
 *   one).
 */
data class DrumHit(
    val voice: DrumVoice,
    val step: Int,
)

/**
 * A one-bar pattern the player practises along to. Twin of `DrumGroove` in
 * `Scales/Music/DrumGroove.swift`, with the same members.
 *
 * Every groove is written in subdivisions of the beat: 2 for eighth notes (counted
 * "1 + 2 +"), 4 for sixteenths ("1 e + a"). The counting labels and the staff columns both
 * come from that grid, so a groove of any length in beats draws itself.
 *
 * @property subdivision notes per beat: 2 for eighths, 4 for sixteenths.
 */
data class DrumGroove(
    val name: String,
    val beatsPerBar: Int,
    val subdivision: Int,
    val hits: List<DrumHit>,
) {
    /** Grooves are told apart by name, as in the Swift picker. */
    val id: String
        get() = name

    /** How many columns the bar has: one per subdivision of every beat. */
    val stepsPerBar: Int
        get() = beatsPerBar * subdivision

    /** The voices struck together on one step. */
    fun voices(atStep: Int): Set<DrumVoice> {
        val voices = mutableSetOf<DrumVoice>()
        for (hit in hits) {
            if (hit.step == atStep) {
                voices.add(hit.voice)
            }
        }
        return voices
    }

    /**
     * What the player says on this step: the beat number on the beat, "+" on the and,
     * "e" and "a" for the sixteenths between.
     */
    fun countLabel(forStep: Int): String {
        val beat = forStep / subdivision + 1
        val withinBeat = forStep % subdivision
        if (withinBeat == 0) {
            return beat.toString()
        }
        if (subdivision == SIXTEENTHS) {
            return sixteenthLabel(withinBeat)
        }
        return "+"
    }

    /** The built-in grooves and the helpers that write them. */
    companion object {
        /** Notes per beat in a sixteenth-note groove, the one grid counted "1 e + a". */
        private const val SIXTEENTHS = 4

        /** Grooves to practise, simplest first, in the Swift order. */
        val builtIn: List<DrumGroove> =
            listOf(
                DrumGroove(
                    name = "Basic Rock",
                    beatsPerBar = 4,
                    subdivision = 2,
                    hits =
                        hiHat(steps = 8) +
                            listOf(
                                DrumHit(voice = DrumVoice.KICK, step = 0),
                                DrumHit(voice = DrumVoice.SNARE, step = 2),
                                DrumHit(voice = DrumVoice.KICK, step = 4),
                                DrumHit(voice = DrumVoice.SNARE, step = 6),
                            ),
                ),
                DrumGroove(
                    name = "Four on the Floor",
                    beatsPerBar = 4,
                    subdivision = 2,
                    hits =
                        hiHat(steps = 8) +
                            listOf(
                                DrumHit(voice = DrumVoice.KICK, step = 0),
                                DrumHit(voice = DrumVoice.KICK, step = 2),
                                DrumHit(voice = DrumVoice.SNARE, step = 2),
                                DrumHit(voice = DrumVoice.KICK, step = 4),
                                DrumHit(voice = DrumVoice.KICK, step = 6),
                                DrumHit(voice = DrumVoice.SNARE, step = 6),
                            ),
                ),
                DrumGroove(
                    name = "Kick on the And",
                    beatsPerBar = 4,
                    subdivision = 2,
                    hits =
                        hiHat(steps = 8) +
                            listOf(
                                DrumHit(voice = DrumVoice.KICK, step = 0),
                                DrumHit(voice = DrumVoice.SNARE, step = 2),
                                DrumHit(voice = DrumVoice.KICK, step = 3),
                                DrumHit(voice = DrumVoice.KICK, step = 4),
                                DrumHit(voice = DrumVoice.SNARE, step = 6),
                            ),
                ),
                DrumGroove(
                    name = "Half Time",
                    beatsPerBar = 4,
                    subdivision = 2,
                    hits =
                        hiHat(steps = 8) +
                            listOf(
                                DrumHit(voice = DrumVoice.KICK, step = 0),
                                DrumHit(voice = DrumVoice.KICK, step = 3),
                                DrumHit(voice = DrumVoice.SNARE, step = 4),
                            ),
                ),
                DrumGroove(
                    name = "Sixteenth Hats",
                    beatsPerBar = 4,
                    subdivision = 4,
                    hits =
                        hiHat(steps = 16) +
                            listOf(
                                DrumHit(voice = DrumVoice.KICK, step = 0),
                                DrumHit(voice = DrumVoice.SNARE, step = 4),
                                DrumHit(voice = DrumVoice.KICK, step = 8),
                                DrumHit(voice = DrumVoice.KICK, step = 10),
                                DrumHit(voice = DrumVoice.SNARE, step = 12),
                            ),
                ),
            )

        /** A closed hi-hat on every step, the steady pulse every built-in groove rides on. */
        private fun hiHat(steps: Int): List<DrumHit> {
            val hits = mutableListOf<DrumHit>()
            for (step in 0 until steps) {
                hits.add(DrumHit(voice = DrumVoice.HI_HAT, step = step))
            }
            return hits
        }

        /** The syllable for the second, third or fourth sixteenth of a beat. */
        private fun sixteenthLabel(withinBeat: Int): String {
            if (withinBeat == 2) {
                return "+"
            }
            if (withinBeat == 1) {
                return "e"
            }
            return "a"
        }
    }
}
