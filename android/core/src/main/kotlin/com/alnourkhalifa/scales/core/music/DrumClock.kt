package com.alnourkhalifa.scales.core.music

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Where the bouncing ball is at one instant of a bar. Twin of `BallPosition` in
 * `Scales/Music/DrumClock.swift`.
 *
 * The ball lands on every count of the groove and flies in an arc to the next one, so it
 * touches down exactly when the player should strike. [step] is the count it left last,
 * [travel] how far it is towards the next one, and [bounce] its height, 0 on a count and
 * 1 at the top of the arc.
 */
data class BallPosition(
    val step: Int,
    val travel: Double,
    val bounce: Double,
)

/**
 * Turns playing time into a place in the bar. Pure maths, so the view and the tests share
 * it. Twin of `DrumClock` in `Scales/Music/DrumClock.swift`; times are plain seconds in a
 * `Double` where Swift says `TimeInterval`.
 */
object DrumClock {
    private const val SECONDS_PER_MINUTE = 60.0

    /** Seconds one bar takes at this tempo. */
    fun barDuration(
        tempo: Double,
        beatsPerBar: Int,
    ): Double = beatsPerBar.toDouble() * SECONDS_PER_MINUTE / tempo

    /** How far through the bar the player is, 0 at beat one and wrapping at the bar line. */
    fun barProgress(
        elapsed: Double,
        tempo: Double,
        beatsPerBar: Int,
    ): Double {
        val duration = barDuration(tempo = tempo, beatsPerBar = beatsPerBar)
        val bars = max(elapsed, 0.0) / duration
        return bars - floor(bars)
    }

    /**
     * The ball for a bar progress, on a bar of [stepsPerBar] counts. A progress of exactly 1
     * stays on the last count, so the ball never leaves the bar.
     */
    fun ballPosition(
        progress: Double,
        stepsPerBar: Int,
    ): BallPosition {
        val position = progress * stepsPerBar.toDouble()
        val countReached = floor(position).toInt()
        val step = min(countReached, stepsPerBar - 1)
        val travel = position - step.toDouble()
        val bounce = sin(travel * PI)
        return BallPosition(step = step, travel = travel, bounce = bounce)
    }
}
