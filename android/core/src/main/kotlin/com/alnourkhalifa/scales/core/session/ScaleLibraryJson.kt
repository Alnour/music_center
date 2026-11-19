package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ReferenceDate
import com.alnourkhalifa.scales.core.music.ScaleDefinition
import com.alnourkhalifa.scales.core.music.ScaleDegree
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.TransformationLesson
import com.alnourkhalifa.scales.core.music.Tuning
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.util.UUID

/**
 * The JSON Swift's synthesized `Codable` writes for settings and the scale library, byte for
 * byte where it matters, so a player's saved data reads the same on the iPhone and Android.
 *
 * No Swift twin: Swift gets this from the compiler. The shapes it reproduces:
 * - keys sorted, as `ScaleLibrary.encoded` and the answers generator write them; every
 *   `…Json` class below declares its properties in alphabetical order for that reason;
 * - a nil optional is left out of the object, never written as `null`;
 * - an enum with an associated value is an object keyed by its case, the value under `_0`:
 *   `{"lower":{"_0":{"alteration":0,"number":7}}}`, `{"builtIn":{"_0":"major"}}`;
 * - dates are seconds since 1 January 2001, written without a fraction when whole;
 * - UUIDs are written in upper case; `letter` is the letter's raw number.
 *
 * A file or value that does not match throws [SerializationException] (an
 * [IllegalArgumentException]), which the library and the settings store treat as unreadable.
 */
object ScaleLibraryJson {
    private val json =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }

    fun encodeSettings(settings: PracticeSettings): String {
        val document = SettingsJson.from(settings)
        return json.encodeToString(SettingsJson.serializer(), document)
    }

    /** @throws SerializationException when the text is not settings Swift could read. */
    fun decodeSettings(text: String): PracticeSettings {
        val document = json.decodeFromString(SettingsJson.serializer(), text)
        return document.toModel()
    }

    fun encodeDocument(document: LibraryDocument): String {
        val stored = DocumentJson.from(document)
        return json.encodeToString(DocumentJson.serializer(), stored)
    }

    /** @throws SerializationException when the text is not a library Swift could read. */
    fun decodeDocument(text: String): LibraryDocument {
        val stored = json.decodeFromString(DocumentJson.serializer(), text)
        return stored.toModel()
    }

    /** One saved scale on its own, the shape the settings store keeps under `customScale`. */
    fun encodeCustomScale(scale: CustomScale): String {
        val stored = CustomScaleJson.from(scale)
        return json.encodeToString(CustomScaleJson.serializer(), stored)
    }

    /** @throws SerializationException when the text is not a saved scale Swift could read. */
    fun decodeCustomScale(text: String): CustomScale {
        val stored = json.decodeFromString(CustomScaleJson.serializer(), text)
        return stored.toModel()
    }

    fun encodeLesson(lesson: TransformationLesson): String {
        val stored = LessonJson.from(lesson)
        return json.encodeToString(LessonJson.serializer(), stored)
    }

    /** @throws SerializationException when the text is not a lesson Swift could read. */
    fun decodeLesson(text: String): TransformationLesson {
        val stored = json.decodeFromString(LessonJson.serializer(), text)
        return stored.toModel()
    }

    fun encodeDefinition(definition: ScaleDefinition): String {
        val stored = DefinitionJson.from(definition)
        return json.encodeToString(DefinitionJson.serializer(), stored)
    }

    /** @throws SerializationException when the text is not a scale Swift could read. */
    fun decodeDefinition(text: String): ScaleDefinition {
        val stored = json.decodeFromString(DefinitionJson.serializer(), text)
        return stored.toModel()
    }
}

/** Swift throws when a raw value names no case; this is the Kotlin side of that refusal. */
private fun <T : Any> known(
    value: T?,
    what: String,
    raw: Any,
): T = value ?: throw SerializationException("unknown $what '$raw'")

/** Swift's `UUID.uuidString` is upper case; reading accepts either case, as Swift does. */
private fun uuidText(id: UUID): String = id.toString().uppercase()

private fun parseUuid(text: String): UUID =
    try {
        UUID.fromString(text)
    } catch (error: IllegalArgumentException) {
        throw SerializationException("not a UUID: '$text'", error)
    }

/**
 * A `Date` as Swift's default JSON writes it: a plain number of seconds since 2001. Kotlin
 * would write 800000000.0 as `8.0E8`; Swift writes `800000000`, so a whole number goes out
 * as an integer and a fraction in plain decimal notation.
 */
private object ReferenceSecondsSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReferenceSeconds", PrimitiveKind.DOUBLE)

    override fun serialize(
        encoder: Encoder,
        value: Double,
    ) {
        val jsonEncoder = encoder as JsonEncoder
        val wholeSeconds = value.toLong()
        val isWhole = wholeSeconds.toDouble() == value
        val primitive =
            if (isWhole) {
                JsonPrimitive(wholeSeconds)
            } else {
                JsonPrimitive(BigDecimal.valueOf(value))
            }
        jsonEncoder.encodeJsonElement(primitive)
    }

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return element.jsonPrimitive.double
    }
}

@Serializable
private data class NoteNameJson(
    val accidental: Int,
    val letter: Int,
) {
    fun toModel(): NoteName {
        val decodedLetter = known(Letter.fromRawValue(letter), "letter", letter)
        return NoteName(decodedLetter, accidental)
    }

    companion object {
        fun from(note: NoteName): NoteNameJson = NoteNameJson(note.accidental, note.letter.rawValue)
    }
}

@Serializable
private data class DegreeJson(
    val alteration: Int,
    val number: Int,
) {
    /** Swift reads any number here and fails later; a degree outside 1 to 7 has no step in
     *  the major scale to measure from, so it is refused while reading instead. */
    fun toModel(): ScaleDegree {
        if (number !in ScaleDegree.numbers) {
            throw SerializationException("degree number $number is outside 1 to 7")
        }
        return ScaleDegree(number, alteration)
    }

    companion object {
        fun from(degree: ScaleDegree): DegreeJson = DegreeJson(degree.alteration, degree.number)
    }
}

@Serializable
private data class DefinitionJson(
    val degrees: List<DegreeJson>,
    val name: String,
) {
    fun toModel(): ScaleDefinition {
        val decodedDegrees = mutableListOf<ScaleDegree>()
        for (degree in degrees) {
            decodedDegrees.add(degree.toModel())
        }
        return ScaleDefinition(name, decodedDegrees)
    }

    companion object {
        fun from(definition: ScaleDefinition): DefinitionJson {
            val stored = mutableListOf<DegreeJson>()
            for (degree in definition.degrees) {
                stored.add(DegreeJson.from(degree))
            }
            return DefinitionJson(stored, definition.name)
        }
    }
}

@Serializable
private data class CustomScaleJson(
    @Serializable(with = ReferenceSecondsSerializer::class)
    val createdAt: Double,
    val definition: DefinitionJson,
    val id: String,
) {
    fun toModel(): CustomScale =
        CustomScale(
            id = parseUuid(id),
            definition = definition.toModel(),
            createdAt = ReferenceDate(createdAt),
        )

    companion object {
        fun from(scale: CustomScale): CustomScaleJson =
            CustomScaleJson(
                createdAt = scale.createdAt.secondsSinceReferenceDate,
                definition = DefinitionJson.from(scale.definition),
                id = uuidText(scale.id),
            )
    }
}

/** The payload of a Swift enum case with one unnamed associated value: `{"_0": value}`. */
@Serializable
private data class DegreePayload(
    @SerialName("_0") val value: DegreeJson,
)

@Serializable
private data class TextPayload(
    @SerialName("_0") val value: String,
)

/**
 * `ScaleEdit` as Swift writes it: one key naming the case. Each case is a nullable field, so
 * exactly the chosen one is written and reading finds whichever is present.
 */
@Serializable
private data class EditJson(
    val add: DegreePayload? = null,
    val lower: DegreePayload? = null,
    val raise: DegreePayload? = null,
    val remove: DegreePayload? = null,
) {
    fun toModel(): ScaleEdit {
        val cases = listOfNotNull(add, lower, raise, remove)
        if (cases.size != 1) {
            throw SerializationException("a scale edit needs exactly one case, found ${cases.size}")
        }
        val payload = cases.first()
        val degree = payload.value.toModel()
        return when {
            add != null -> ScaleEdit.Add(degree)
            lower != null -> ScaleEdit.Lower(degree)
            raise != null -> ScaleEdit.Raise(degree)
            else -> ScaleEdit.Remove(degree)
        }
    }

    companion object {
        fun from(edit: ScaleEdit): EditJson {
            val payload = DegreePayload(DegreeJson.from(edit.degree))
            return when (edit) {
                is ScaleEdit.Add -> EditJson(add = payload)
                is ScaleEdit.Lower -> EditJson(lower = payload)
                is ScaleEdit.Raise -> EditJson(raise = payload)
                is ScaleEdit.Remove -> EditJson(remove = payload)
            }
        }
    }
}

/** `ScaleReference` as Swift writes it: `{"builtIn":{"_0":"major"}}` or
 *  `{"custom":{"_0":"<UUID>"}}`. */
@Serializable
private data class ReferenceJson(
    val builtIn: TextPayload? = null,
    val custom: TextPayload? = null,
) {
    fun toModel(): ScaleReference {
        if (builtIn != null && custom == null) {
            val raw = builtIn.value
            val type = known(ScaleType.fromRawValue(raw), "scale type", raw)
            return ScaleReference.BuiltIn(type)
        }
        if (custom != null && builtIn == null) {
            return ScaleReference.Custom(parseUuid(custom.value))
        }
        throw SerializationException("a scale reference needs exactly one case")
    }

    companion object {
        fun from(reference: ScaleReference): ReferenceJson =
            when (reference) {
                is ScaleReference.BuiltIn -> {
                    ReferenceJson(
                        builtIn = TextPayload(reference.type.rawValue),
                    )
                }

                is ScaleReference.Custom -> {
                    ReferenceJson(
                        custom = TextPayload(uuidText(reference.id)),
                    )
                }
            }
    }
}

/**
 * A transformation with its result stored, as Swift's synthesized decoder reads it: the saved
 * `after` is kept as written rather than worked out again, because a scale the player renamed
 * carries its new name there, which the edit alone cannot reproduce.
 */
@Serializable
private data class TransformationJson(
    val after: DefinitionJson,
    val before: DefinitionJson,
    val edit: EditJson,
    val root: NoteNameJson,
) {
    fun toModel(): ScaleTransformation =
        ScaleTransformation(
            root = root.toModel(),
            before = before.toModel(),
            edit = edit.toModel(),
            after = after.toModel(),
        )

    companion object {
        fun from(transformation: ScaleTransformation): TransformationJson =
            TransformationJson(
                after = DefinitionJson.from(transformation.after),
                before = DefinitionJson.from(transformation.before),
                edit = EditJson.from(transformation.edit),
                root = NoteNameJson.from(transformation.root),
            )
    }
}

@Serializable
private data class LessonJson(
    @Serializable(with = ReferenceSecondsSerializer::class)
    val date: Double,
    val id: String,
    val result: ReferenceJson,
    val transformation: TransformationJson,
) {
    fun toModel(): TransformationLesson =
        TransformationLesson(
            id = parseUuid(id),
            date = ReferenceDate(date),
            transformation = transformation.toModel(),
            result = result.toModel(),
        )

    companion object {
        fun from(lesson: TransformationLesson): LessonJson =
            LessonJson(
                date = lesson.date.secondsSinceReferenceDate,
                id = uuidText(lesson.id),
                result = ReferenceJson.from(lesson.result),
                transformation = TransformationJson.from(lesson.transformation),
            )
    }
}

/** Swift's `ScaleLibrary.LibraryDocument`; every key is required, as Swift requires them. */
@Serializable
private data class DocumentJson(
    val lessons: List<LessonJson>,
    val scales: List<CustomScaleJson>,
    val version: Int,
) {
    fun toModel(): LibraryDocument {
        val decodedScales = mutableListOf<CustomScale>()
        for (scale in scales) {
            decodedScales.add(scale.toModel())
        }
        val decodedLessons = mutableListOf<TransformationLesson>()
        for (lesson in lessons) {
            decodedLessons.add(lesson.toModel())
        }
        return LibraryDocument(version, decodedScales, decodedLessons)
    }

    companion object {
        fun from(document: LibraryDocument): DocumentJson {
            val storedScales = mutableListOf<CustomScaleJson>()
            for (scale in document.scales) {
                storedScales.add(CustomScaleJson.from(scale))
            }
            val storedLessons = mutableListOf<LessonJson>()
            for (lesson in document.lessons) {
                storedLessons.add(LessonJson.from(lesson))
            }
            return DocumentJson(storedLessons, storedScales, document.version)
        }
    }
}

/**
 * `PracticeSettings` as Swift saves it. The non-optional keys are required, as Swift's
 * synthesized decoder requires them even though each has a default; the three optionals may
 * be missing, which is how settings saved by the first release still load.
 */
@Serializable
private data class SettingsJson(
    val customScale: CustomScaleJson? = null,
    val fretCount: Int,
    val instrument: String? = null,
    val isLeftHanded: Boolean,
    val keyboardOctaves: Int? = null,
    val labelStyle: String,
    val root: NoteNameJson,
    val scaleType: String,
    val thinStringOnTop: Boolean,
    val tuning: String,
    val tvUsesDarkTheme: Boolean,
) {
    fun toModel(): PracticeSettings {
        val decodedInstrument =
            instrument?.let { raw ->
                known(Instrument.fromRawValue(raw), "instrument", raw)
            }
        return PracticeSettings(
            root = root.toModel(),
            scaleType = known(ScaleType.fromRawValue(scaleType), "scale type", scaleType),
            tuning = known(Tuning.fromRawValue(tuning), "tuning", tuning),
            fretCount = fretCount,
            labelStyle = known(NoteLabelStyle.fromRawValue(labelStyle), "label style", labelStyle),
            isLeftHanded = isLeftHanded,
            thinStringOnTop = thinStringOnTop,
            tvUsesDarkTheme = tvUsesDarkTheme,
            customScale = customScale?.toModel(),
            instrument = decodedInstrument,
            keyboardOctaves = keyboardOctaves,
        )
    }

    companion object {
        fun from(settings: PracticeSettings): SettingsJson {
            val storedScale = settings.customScale?.let { scale -> CustomScaleJson.from(scale) }
            return SettingsJson(
                customScale = storedScale,
                fretCount = settings.fretCount,
                instrument = settings.instrument?.rawValue,
                isLeftHanded = settings.isLeftHanded,
                keyboardOctaves = settings.keyboardOctaves,
                labelStyle = settings.labelStyle.rawValue,
                root = NoteNameJson.from(settings.root),
                scaleType = settings.scaleType.rawValue,
                thinStringOnTop = settings.thinStringOnTop,
                tuning = settings.tuning.rawValue,
                tvUsesDarkTheme = settings.tvUsesDarkTheme,
            )
        }
    }
}
