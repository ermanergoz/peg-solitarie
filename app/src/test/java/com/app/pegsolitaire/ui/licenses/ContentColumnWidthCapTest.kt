package com.app.pegsolitaire.ui.licenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.licenses.GoogleNotice
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Bundled
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Notice
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Device re-run F-3 (the tech-lead's re-run ruling): on a window wider than `content_max_width` the
 * content column of each licences screen is capped at it and centred (spec §3.1, §4, §4A, §4B), and
 * on a window narrower than the cap it still fills the window.
 *
 * A tablet's window, the one QA measured (800 × 1280 dp), and a small phone's for the guard; never a
 * window narrower than the cap for the cap itself, which is how the defect hid. The cap is read from
 * the resources, never written as a number.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [WIDTH_CAP_ROBOLECTRIC_SDK], qualifiers = WIDTH_CAP_TABLET_WINDOW)
class ContentColumnWidthCapTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    /** The screen on show; setting it composes the next screen in the same host. */
    private var shown by mutableStateOf<Shown?>(null)

    @Test
    fun theLicensesColumnIsCappedAtContentMaxWidthAndCentredOnAWideWindow() {
        start(licenses())

        assertCappedAndCentred(LICENSES_LIST_TAG)
    }

    @Test
    fun theLicenseTextColumnIsCappedAtContentMaxWidthAndCentredOnAWideWindow() {
        start(licenseText())

        assertCappedAndCentred(LICENSE_TEXT_COLUMN_TAG)
    }

    @Test
    fun theGoogleNoticesListIsCappedAtContentMaxWidthAndCentredOnAWideWindow() {
        start(notices())

        assertCappedAndCentred(GOOGLE_NOTICES_LIST_TAG)
    }

    @Test
    fun theGoogleNoticeTextListIsCappedAtContentMaxWidthAndCentredOnAWideWindow() {
        start(noticeText())

        assertCappedAndCentred(GOOGLE_NOTICE_TEXT_LIST_TAG)
    }

    /** The guard on the phone: the order that caps the column must still let it fill a narrow window. */
    @Test
    @Config(qualifiers = WIDTH_CAP_NARROW_WINDOW)
    fun theColumnFillsAWindowNarrowerThanTheCap() {
        val screens =
            listOf(
                licenses() to LICENSES_LIST_TAG,
                licenseText() to LICENSE_TEXT_COLUMN_TAG,
                notices() to GOOGLE_NOTICES_LIST_TAG,
                noticeText() to GOOGLE_NOTICE_TEXT_LIST_TAG,
            )
        start(screens.first().first)

        screens.forEach { (screen, tag) ->
            shown = screen
            composeRule.waitForIdle()
            val window =
                composeRule
                    .onRoot()
                    .fetchSemanticsNode()
                    .size.width
            val column = columnOf(tag)
            assertWithMessage("${screen.name}: the column's width, against the window's (${window}px)")
                .that(column.size.width)
                .isEqualTo(window)
            assertWithMessage("${screen.name}: the column's left edge")
                .that(column.positionInRoot.x)
                .isWithin(TOLERANCE_PX)
                .of(0f)
        }
    }

    /** The column is `content_max_width` wide, within a pixel, and its left edge leaves equal margins. */
    private fun assertCappedAndCentred(tag: String) {
        val window =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .size.width
        val cap = app.resources.getDimension(R.dimen.content_max_width)
        val column = columnOf(tag)
        assertWithMessage("the width of $tag on a ${window}px window, against the cap")
            .that(column.size.width.toFloat())
            .isWithin(TOLERANCE_PX)
            .of(cap)
        assertWithMessage("the left edge of $tag, against equal margins")
            .that(column.positionInRoot.x)
            .isWithin(TOLERANCE_PX)
            .of((window - cap) / 2)
    }

    private fun columnOf(tag: String): SemanticsNode =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()

    /** One screen to show, under a [name] that keys its composition and names it in a failure. */
    private class Shown(
        val name: String,
        val content: @Composable () -> Unit,
    )

    private fun start(first: Shown) {
        shown = first
        composeRule.setContent {
            AppTheme { shown?.let { screen -> key(screen.name) { screen.content() } } }
        }
        composeRule.waitForIdle()
    }

    private fun licenses() =
        Shown("Licenses") {
            LicensesScreen(LicenseCatalog.sections, onOpenEntry = {}, onOpenGoogleNotices = {}, onBack = {})
        }

    private fun licenseText() =
        Shown("License text") {
            val fredoka = requireNotNull(LicenseCatalog.entry(LicenseEntryIds.FREDOKA))
            LicenseTextScreen(
                LicenseTextUiState.Loaded(Bundled(fredoka), listOf(LoadedTextPart(null, PARAGRAPHS))),
                onBack = {},
            )
        }

    private fun notices() =
        Shown("Google notices") {
            GoogleNoticesScreen(GoogleNoticesUiState.Loaded(ROWS), onOpenNotice = {}, onBack = {})
        }

    private fun noticeText() =
        Shown("Google notice text") {
            GoogleNoticeTextScreen(
                LicenseTextUiState.Loaded(
                    Notice(NOTICE, NOTICE.sources.joinToString(", "), ordinal = null),
                    listOf(LoadedTextPart(null, PARAGRAPHS)),
                ),
                onBack = {},
            )
        }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val WIDTH_CAP_ROBOLECTRIC_SDK = 34

/** The tablet window QA measured F-3 on: wider than the 480 dp cap. */
private const val WIDTH_CAP_TABLET_WINDOW = "w800dp-h1280dp"

/** A small phone's window: narrower than the cap, so the column fills it. */
private const val WIDTH_CAP_NARROW_WINDOW = "w360dp-h640dp"

/** How far the measured column may sit from its expected bounds: one pixel of rounding. */
private const val TOLERANCE_PX = 1f

private val PARAGRAPHS = listOf("First paragraph.", "Second paragraph.", "Third paragraph.")

private val ROWS =
    listOf(
        GoogleNoticeRow("000000000000000a", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(1, 2)),
        GoogleNoticeRow("000000000000000b", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(2, 2)),
        GoogleNoticeRow("000000000000000c", "OkHttp", "ads-mobile-sdk", ordinal = null),
    )

private val NOTICE =
    GoogleNotice("000000000000000e", "Cronet", listOf("play-services-cronet", "ads-mobile-sdk"), "e000000000000000")
