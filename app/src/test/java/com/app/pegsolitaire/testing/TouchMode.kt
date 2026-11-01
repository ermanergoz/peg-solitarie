package com.app.pegsolitaire.testing

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.google.common.truth.Truth.assertWithMessage

/**
 * Asserts the composed screen's window is in touch mode, the state a phone is in: it starts there,
 * and TalkBack's double-tap reaches a node as an accessibility click, not as a touch, so nothing takes
 * it out. In touch mode a `clickable` cannot take focus (`Focusability.SystemDefined`), which
 * Robolectric's default keyboard mode hid (the talkback-board defect B-2, the defect ruling's §2).
 *
 * Robolectric 4.16 starts a window in touch mode only when it draws with native graphics (its window
 * session takes `inTouchMode` from `ShadowView.useRealGraphics()`), and `Instrumentation.setInTouchMode`
 * does not reach a window that already exists. So a focus test class runs with
 * `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and checks the mode here once its screen is composed.
 */
internal fun ComposeContentTestRule.assertInTouchMode() {
    assertWithMessage("whether the window is in touch mode").that(composeHost().isInTouchMode).isTrue()
}
