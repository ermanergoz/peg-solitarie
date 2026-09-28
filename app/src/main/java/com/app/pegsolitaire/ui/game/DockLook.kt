package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/**
 * The five looks a gameplay dock control can take (ux-spec §4.4.1, D-04e): **one look, one
 * meaning.**
 *
 * The dock holds two kinds of control and they speak two grammars that cannot be confused.
 * **Actions** — Undo, Hint, Restart — have no on/off at all: they are [Available], [Working] (Hint
 * while the Solver searches) or [Unavailable] (Undo with nothing to undo). **The toggle** —
 * Show-moves — is the only control with an on/off, and the only one ever drawn [On] or [Off].
 *
 * Before D-04e, fill-versus-outline carried two meanings at once — importance (the retired D-04c)
 * and state (D-04b) — so a dock with two filled and two outlined controls read as two on and two
 * off, and the fill it relied on measured ≈1.04:1 against the slab. Every look is now a
 * [DockLookRecipe], and every pair of recipes differs in at least two channels that are not hue.
 *
 * A spent free hint or undo is **not** a look: the control stays [Available] and the tap raises
 * the opt-in dialog — the ruling `GameUiState` records beside its assist-funnel fields, applied
 * rather than relaxed.
 */
enum class DockLook {
    /** An action the player can take now: a pale, raised disc and the action's own glyph. */
    Available,

    /** An action at work — Hint while the Solver searches: pressed in, a spinner for a glyph. */
    Working,

    /** An action with nothing to act on — Undo on a fresh board: no disc, a faded glyph. */
    Unavailable,

    /** The toggle, on: a strong disc and the on mark. Show-moves only. */
    On,

    /** The toggle, off: an empty ring and a slashed glyph. Show-moves only. */
    Off,
}

/** How a dock disc is filled — the one channel with three values. */
enum class DockFill {
    /** No disc at all: the slab shows through. */
    None,

    /** The pale `primaryContainer` disc every action wears when it can be pressed. */
    Pale,

    /** The strong `primary` disc — ≥ 3:1 against the slab, and worn by [DockLook.On] alone. */
    Strong,
}

/** What a dock disc draws at its centre. */
enum class DockGlyph {
    /** The control's own glyph, as authored. */
    Normal,

    /** The glyph with a knocked-out diagonal slash — Show-moves Off alone. */
    Slashed,

    /** A progress spinner in place of the glyph — Hint while the Solver searches. */
    Spinner,
}

/**
 * One look, as the channels that draw it (ux-spec §4.4.1, "The five looks").
 *
 * The first five properties are the channels criterion 21b counts — every pair of looks must
 * differ in at least two of them — and hue is deliberately not among them. [faded] (glyph
 * strength) and [respondsToPress] complete the recipe but are never counted as a difference.
 *
 * @property fill how the disc is filled.
 * @property ring whether the disc is outlined by an empty ring.
 * @property shadow whether the disc is raised on a clay shadow.
 * @property glyph what the disc draws at its centre.
 * @property onMark whether the on mark sits on the disc's rim.
 * @property faded whether the glyph is drawn at the unavailable alpha instead of full strength.
 * @property respondsToPress whether the disc squishes under a press.
 */
@Immutable
data class DockLookRecipe(
    val fill: DockFill,
    val ring: Boolean,
    val shadow: Boolean,
    val glyph: DockGlyph,
    val onMark: Boolean,
    val faded: Boolean,
    val respondsToPress: Boolean,
)

/** The recipe this look is drawn from — the single mapping every dock control reads. */
val DockLook.recipe: DockLookRecipe
    get() =
        when (this) {
            DockLook.Available -> AVAILABLE_RECIPE
            DockLook.Working -> WORKING_RECIPE
            DockLook.Unavailable -> UNAVAILABLE_RECIPE
            DockLook.On -> ON_RECIPE
            DockLook.Off -> OFF_RECIPE
        }

private val AVAILABLE_RECIPE =
    DockLookRecipe(
        fill = DockFill.Pale,
        ring = false,
        shadow = true,
        glyph = DockGlyph.Normal,
        onMark = false,
        faded = false,
        respondsToPress = true,
    )

private val WORKING_RECIPE =
    DockLookRecipe(
        fill = DockFill.Pale,
        ring = false,
        shadow = false,
        glyph = DockGlyph.Spinner,
        onMark = false,
        faded = false,
        respondsToPress = false,
    )

private val UNAVAILABLE_RECIPE =
    DockLookRecipe(
        fill = DockFill.None,
        ring = false,
        shadow = false,
        glyph = DockGlyph.Normal,
        onMark = false,
        faded = true,
        respondsToPress = false,
    )

private val ON_RECIPE =
    DockLookRecipe(
        fill = DockFill.Strong,
        ring = false,
        shadow = true,
        glyph = DockGlyph.Normal,
        onMark = true,
        faded = false,
        respondsToPress = true,
    )

private val OFF_RECIPE =
    DockLookRecipe(
        fill = DockFill.None,
        ring = true,
        shadow = false,
        glyph = DockGlyph.Slashed,
        onMark = false,
        faded = false,
        respondsToPress = true,
    )

/**
 * Semantics: the [DockLook] a dock control is drawn in (ux-spec §4.4.1, "Test hook").
 *
 * It replaces the retired `DockActionFilledKey`, a `Boolean` that could express filled or outlined
 * and therefore could not express five looks. The same custom-semantics idiom `GameScreen`'s
 * `PegCountKey` uses, for the same reason: a look is a pixel fact no ordinary assertion can reach,
 * and this makes every row of §4.4.1's table decidable without a screenshot. TalkBack does not
 * read it — the control's name, role and state are what it announces.
 */
val DockLookKey = SemanticsPropertyKey<DockLook>("DockLook")

private var SemanticsPropertyReceiver.dockLookValue by DockLookKey

/**
 * Reports [look] on the control's own node — the one that carries its name, role and enabled
 * state — so a test reads all four off one node, exactly as TalkBack reaches it.
 */
fun Modifier.dockLook(look: DockLook): Modifier = semantics { dockLookValue = look }
