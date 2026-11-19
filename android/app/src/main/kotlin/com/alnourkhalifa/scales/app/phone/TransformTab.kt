package com.alnourkhalifa.scales.app.phone

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.session.ScaleLibrary
import com.alnourkhalifa.scales.core.session.ScaleLibraryError
import com.alnourkhalifa.scales.ui.MusicFontFamily
import java.util.UUID

/**
 * A page pushed over the Transform screen, the way `NavigationLink`s push onto the
 * `NavigationStack` in `Scales/Views/TransformView.swift`: a kept lesson, or one of the
 * player's own scales.
 */
sealed class TransformPage {
    /** `LessonDetailView(lessonID:)`. */
    data class Lesson(
        val lessonId: UUID,
    ) : TransformPage()

    /** `CustomScaleDetailView(scaleID:)`. */
    data class MyScale(
        val scaleId: UUID,
    ) : TransformPage()
}

/**
 * What the Transform tab remembers: the key and starting scale being changed, the lesson
 * waiting in the sheet, and the pages pushed over the screen. Twin of `TransformView`'s
 * `@State` in `Scales/Views/TransformView.swift`.
 *
 * Differs from Swift: a `ViewModel` owned by the activity. `RootView` drops a tab's
 * composables when another tab is picked, while SwiftUI's `TabView` keeps every tab's state;
 * keeping it here means a player who picks a key, glances at the fretboard and comes back
 * finds the Transform tab as they left it, as on the iPhone.
 */
class TransformTabModel : ViewModel() {
    var root: NoteName by mutableStateOf(NoteName(Letter.G))
    var start: ScaleReference by mutableStateOf(ScaleReference.BuiltIn(ScaleType.MAJOR))

    /** The change the player just picked, shown in the lesson sheet until saved or cancelled. */
    var pending: ScaleTransformation? by mutableStateOf(null)

    /** Pages above the Transform screen, the last one showing; empty shows the screen. */
    val pages = mutableStateListOf<TransformPage>()

    /** The board as this tab last saw it; null until the first visit. */
    private var lastBoardRoot: NoteName? = null
    private var lastBoardReference: ScaleReference? = null

    /**
     * The rule of `TransformView.swift`: the tab takes the Scales screen's key and scale on
     * its first visit, so it opens on whatever the player is already practising, and after
     * that takes each one again only when it changes on the board. A key picked here is kept
     * while the board stays on its own key.
     *
     * Swift's `onChange` also fires while the tab is out of sight; this tab is not composed
     * then, so it compares with the board it last saw instead, which catches the same changes.
     * Before the first visit nothing has been seen, so everything counts as changed and the
     * tab takes the board whole, Swift's `followTheBoardTheFirstTime`.
     */
    fun followTheBoard(
        boardRoot: NoteName,
        boardReference: ScaleReference,
    ) {
        if (boardRoot != lastBoardRoot) {
            root = boardRoot
        }
        if (boardReference != lastBoardReference) {
            start = boardReference
        }
        lastBoardRoot = boardRoot
        lastBoardReference = boardReference
    }

    /** A saved scale deleted while it was the starting point gives way to Major. */
    fun dropDeletedStart(library: ScaleLibrary) {
        val current = start
        if (current is ScaleReference.Custom && library.scale(current.id) == null) {
            start = ScaleReference.BuiltIn(ScaleType.MAJOR)
        }
    }

    fun open(page: TransformPage) {
        pages.add(page)
    }

    fun goBack() {
        if (pages.isNotEmpty()) {
            pages.removeAt(pages.lastIndex)
        }
    }

    /** "Change a Note in This Scale": the scale becomes the starting point and the player
     *  is taken back to the Transform screen to pick the change. */
    fun startFrom(reference: ScaleReference) {
        start = reference
        goBack()
    }
}

/**
 * The Transform tab: the Transform screen with the lesson and scale pages it opens. Twin of
 * `TransformView`'s `NavigationStack` in `Scales/Views/TransformView.swift`; the screen itself
 * is [TransformView], the pages are in `LessonViews.kt`.
 *
 * The system back gesture closes the top page, as the iPhone's back button and edge swipe do.
 */
@Composable
fun TransformTab(modifier: Modifier = Modifier) {
    val app = rememberScalesApplication()
    val model = rememberTransformTabModel()
    val settings = app.sessionState.value.settings
    val boardRoot = settings.root
    val boardReference = settings.scaleReference
    LaunchedEffect(boardRoot, boardReference) {
        model.followTheBoard(boardRoot, boardReference)
    }
    BackHandler(enabled = model.pages.isNotEmpty()) {
        model.goBack()
    }
    when (val page = model.pages.lastOrNull()) {
        null -> TransformView(model = model, modifier = modifier)
        is TransformPage.Lesson -> LessonDetailView(page.lessonId, model, modifier)
        is TransformPage.MyScale -> CustomScaleDetailView(page.scaleId, model, modifier)
    }
}

/**
 * The Transform tab's slot in RootView. The slot keeps its wave-2 name because `PhoneActivity`
 * wires the tabs by these names; the tab itself is [TransformTab].
 */
@Composable
fun TransformTabStub(modifier: Modifier = Modifier) {
    TransformTab(modifier = modifier)
}

/** The activity's one [TransformTabModel], so it outlives the tab's composables. */
@Composable
private fun rememberTransformTabModel(): TransformTabModel {
    val activity = LocalActivity.current as ComponentActivity
    return remember(activity) {
        val provider = ViewModelProvider(activity)
        provider[TransformTabModel::class.java]
    }
}

/**
 * The sizes of an iOS inset-grouped `Form`, which every page of this tab is drawn as, so the
 * Android pages keep the iPhone's rhythm: rounded cards on a grey ground, rows at least the
 * height of a fingertip, captions above and below each card.
 */
internal object FormMetrics {
    val pageHorizontalPadding = 16.dp
    val rowHorizontalPadding = 16.dp
    val rowMinHeight = 52.dp
    val cardCornerRadius = 26.dp
    val sectionSpacing = 20.dp
    val captionHorizontalPadding = 16.dp
    val accessoryIconSize = 16.dp
    val leadingIconSize = 22.dp
}

/**
 * iOS's grouped-form colours: the grey page and the white cards on it (black and dark grey in
 * dark mode). Fixed rather than Material's tinted surfaces, as `ScalesTheme` keeps the iOS
 * accent, so the Transform tab looks like the iPhone's.
 */
internal data class FormColors(
    val page: Color,
    val card: Color,
    val separator: Color,
) {
    companion object {
        /** iOS's `systemRed`, the colour of a destructive button. */
        val destructive = Color(0xFFFF3B30)

        private val light =
            FormColors(
                page = Color(0xFFF2F2F7),
                card = Color.White,
                separator = Color(0x4A3C3C43),
            )
        private val dark =
            FormColors(
                page = Color.Black,
                card = Color(0xFF1C1C1E),
                separator = Color(0x99545458),
            )

        @Composable
        fun current(): FormColors = if (isSystemInDarkTheme()) dark else light
    }
}

/**
 * One section of a form: an optional caption above, the rows on a rounded card, and an
 * optional footnote below. Twin of SwiftUI's `Section` inside a `Form`.
 */
@Composable
internal fun FormSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FormColors.current()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = FormMetrics.pageHorizontalPadding),
    ) {
        if (header != null) {
            FormCaption(text = header, isHeader = true)
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(FormMetrics.cardCornerRadius))
                    .background(colors.card),
            content = content,
        )
        if (footer != null) {
            FormCaption(text = footer, isHeader = false)
        }
    }
}

/** A section's caption: a heading above the card, or a footnote under it. */
@Composable
private fun FormCaption(
    text: String,
    isHeader: Boolean,
) {
    val style =
        if (isHeader) {
            MaterialTheme.typography.titleMedium
                .copy(fontWeight = FontWeight.SemiBold)
        } else {
            MaterialTheme.typography.bodySmall
        }
    val verticalPadding =
        if (isHeader) {
            Modifier.padding(top = 4.dp, bottom = 8.dp)
        } else {
            Modifier.padding(top = 8.dp)
        }
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            verticalPadding
                .padding(horizontal = FormMetrics.captionHorizontalPadding),
    )
}

/** The hairline between two rows of a card, inset from the leading edge as on iOS. */
@Composable
internal fun FormDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = FormMetrics.rowHorizontalPadding),
        thickness = 0.5.dp,
        color = FormColors.current().separator,
    )
}

/**
 * The SF Symbols the iPhone's forms show (`chevron.up.chevron.down`, `chevron.right`,
 * `chevron.left`, `plus.circle`, `checkmark`, `checkmark.seal.fill`, `arrow.right`), drawn
 * here because the Material icons library is several megabytes for a handful of strokes.
 * All are 24 × 24 so `Icon` tints them.
 */
internal object FormIcons {
    private val ink = SolidColor(Color.Black)

    val chevronUpDown: ImageVector =
        strokedIcon("FormChevronUpDown") {
            moveTo(7.5f, 9.5f)
            lineTo(12f, 5f)
            lineTo(16.5f, 9.5f)
            moveTo(7.5f, 14.5f)
            lineTo(12f, 19f)
            lineTo(16.5f, 14.5f)
        }

    val chevronRight: ImageVector =
        strokedIcon("FormChevronRight") {
            moveTo(9f, 5f)
            lineTo(16f, 12f)
            lineTo(9f, 19f)
        }

    val chevronLeft: ImageVector =
        strokedIcon("FormChevronLeft") {
            moveTo(15f, 4f)
            lineTo(7f, 12f)
            lineTo(15f, 20f)
        }

    val checkmark: ImageVector =
        strokedIcon("FormCheckmark") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 6.5f)
        }

    val arrowRight: ImageVector =
        strokedIcon("FormArrowRight") {
            moveTo(4f, 12f)
            lineTo(19f, 12f)
            moveTo(13f, 6f)
            lineTo(19f, 12f)
            lineTo(13f, 18f)
        }

    /** A ring with a plus inside. */
    val plusCircle: ImageVector =
        strokedIcon("FormPlusCircle") {
            moveTo(2.5f, 12f)
            arcToRelative(9.5f, 9.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 19f, 0f)
            arcToRelative(9.5f, 9.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -19f, 0f)
            moveTo(12f, 7.5f)
            lineTo(12f, 16.5f)
            moveTo(7.5f, 12f)
            lineTo(16.5f, 12f)
        }

    /** A filled disc with the tick cut out of it, standing in for the seal's scalloped edge. */
    val checkmarkSeal: ImageVector =
        ImageVector
            .Builder(
                name = "FormCheckmarkSeal",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = ink, pathFillType = PathFillType.EvenOdd) {
                moveTo(1.5f, 12f)
                arcToRelative(
                    10.5f,
                    10.5f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    21f,
                    0f,
                )
                arcToRelative(
                    10.5f,
                    10.5f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -21f,
                    0f,
                )
                close()
                moveTo(6.4f, 12.2f)
                lineTo(7.8f, 10.8f)
                lineTo(10.4f, 13.4f)
                lineTo(16.2f, 7.6f)
                lineTo(17.6f, 9f)
                lineTo(10.4f, 16.2f)
                close()
            }.build()

    private fun strokedIcon(
        name: String,
        pathBuilder: PathBuilder.() -> Unit,
    ): ImageVector =
        ImageVector
            .Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(
                stroke = ink,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = pathBuilder,
            ).build()
}

/**
 * A page of the tab: its title bar over a scrolling form.
 *
 * A [hasLargeTitle] page (the Transform screen, one of the player's scales) shows its title
 * large at the leading edge and shrinks it into the bar as the form scrolls, as iOS's large
 * navigation title does; Material's `LargeTopAppBar` behaves the same way. Other pages (a
 * lesson) centre a small title, iOS's inline title. A page with [onBack] has a back chevron.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormPage(
    title: String,
    modifier: Modifier = Modifier,
    hasLargeTitle: Boolean = false,
    onBack: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val colors = FormColors.current()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pageModifier =
        if (hasLargeTitle) {
            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            modifier
        }
    Scaffold(
        modifier = pageModifier,
        containerColor = colors.page,
        topBar = { FormTitleBar(title, hasLargeTitle, onBack, scrollBehavior) },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("formPageList"),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(FormMetrics.sectionSpacing),
            content = content,
        )
    }
}

/** The bar on the page's grey ground: large and collapsing, or small and centred. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormTitleBar(
    title: String,
    hasLargeTitle: Boolean,
    onBack: (() -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val page = FormColors.current().page
    val barColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = page,
            scrolledContainerColor = page,
        )
    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(imageVector = FormIcons.chevronLeft, contentDescription = "Back")
            }
        }
    }
    val titleWeight = if (hasLargeTitle) FontWeight.Bold else FontWeight.SemiBold
    val titleText: @Composable () -> Unit = {
        Text(text = title, fontFamily = MusicFontFamily, fontWeight = titleWeight)
    }
    if (hasLargeTitle) {
        LargeTopAppBar(
            title = titleText,
            navigationIcon = navigationIcon,
            colors = barColors,
            scrollBehavior = scrollBehavior,
        )
    } else {
        CenterAlignedTopAppBar(
            title = titleText,
            navigationIcon = navigationIcon,
            colors = barColors,
        )
    }
}

/** A page whose lesson or scale was deleted elsewhere, Swift's `ContentUnavailableView`. */
@Composable
internal fun DeletedPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormPage(title = "", onBack = onBack, modifier = modifier) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp),
            )
        }
    }
}

/**
 * A row that opens another page, Swift's `NavigationLink` in a `Form`: a title, a grey line
 * under it, and a chevron at the trailing edge. Scale names and formulas take the music font.
 */
@Composable
internal fun NavigationRow(
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(tag)
                .clickable(onClick = onClick)
                .heightIn(min = FormMetrics.rowMinHeight)
                .padding(horizontal = FormMetrics.rowHorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontFamily = MusicFontFamily,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                fontFamily = MusicFontFamily,
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
            )
        }
        Icon(
            imageVector = FormIcons.chevronRight,
            contentDescription = null,
            tint = secondary,
            modifier = Modifier.size(FormMetrics.accessoryIconSize),
        )
    }
}

/** A tappable row of accent-coloured text, Swift's `Button` in a `Form`; red when it deletes. */
@Composable
internal fun FormButtonRow(
    title: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (isDestructive) FormColors.destructive else colors.primary
    Text(
        text = title,
        fontFamily = MusicFontFamily,
        style = MaterialTheme.typography.bodyLarge,
        color = tint,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = FormMetrics.rowMinHeight)
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(horizontal = FormMetrics.rowHorizontalPadding),
    )
}

/**
 * Runs one library change and returns its refusal, or null when it went through, so a screen
 * can hold the error in state and show [LibraryErrorAlert]. Swift's `do … catch` around each
 * `try library.…` call.
 */
internal fun libraryChange(change: () -> Unit): ScaleLibraryError? {
    try {
        change()
    } catch (error: ScaleLibraryError) {
        return error
    }
    return null
}

/** What went wrong, in words for the player. Twin of `ScaleLibraryError.message`. */
internal val ScaleLibraryError.playerMessage: String
    get() =
        when (this) {
            ScaleLibraryError.EmptyName -> {
                "Give the new scale a name first."
            }

            ScaleLibraryError.ScaleNotFound -> {
                "That scale is no longer in your library."
            }

            ScaleLibraryError.LessonNotFound -> {
                "That lesson was already deleted."
            }

            is ScaleLibraryError.CouldNotSave -> {
                "Your scales couldn't be saved on this device. $reason"
            }
        }

/** Shows a library error as an alert, and clears it when dismissed. Twin of
 *  `libraryErrorAlert` in `Scales/Views/LessonViews.swift`. */
@Composable
internal fun LibraryErrorAlert(
    error: ScaleLibraryError?,
    onDismiss: () -> Unit,
) {
    if (error == null) {
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Couldn't Update Your Scales") },
        text = { Text(error.playerMessage) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

/**
 * A wide window (a tablet, or a phone's unfolded inner screen), iOS's regular horizontal size
 * class: 600 dp and up, Material's own line between compact and medium widths.
 */
@Composable
internal fun hasRegularWidth(): Boolean {
    val widthPixels = LocalWindowInfo.current.containerSize.width
    val widthDp = with(LocalDensity.current) { widthPixels.toDp() }
    return widthDp >= 600.dp
}

/**
 * A text field drawn as a plain form row: no box or underline, the placeholder in grey, and
 * Done on the keyboard, as SwiftUI's `TextField` in a `Form`.
 */
@Composable
internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val clear = Color.Transparent
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            // iOS's `placeholderText`: the text colour at 30 %, fainter than secondary text.
            val faint =
                MaterialTheme.colorScheme.onSurface
                    .copy(alpha = 0.3f)
            Text(text = placeholder, fontFamily = MusicFontFamily, color = faint)
        },
        textStyle =
            MaterialTheme.typography.bodyLarge
                .copy(fontFamily = MusicFontFamily),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = clear,
                unfocusedContainerColor = clear,
                focusedIndicatorColor = clear,
                unfocusedIndicatorColor = clear,
            ),
    )
}
