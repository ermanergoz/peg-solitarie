package com.app.pegsolitaire.data.licenses

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.security.MessageDigest

/**
 * LS-AC12: every curated licence text ships byte-identical to its source, pinned by its **full**
 * SHA-256 (brief §2.2 step 4; spec §7).
 *
 * Each text is read through the real `AssetManager` (Robolectric serves the merged debug assets),
 * which is the byte stream `AssetLicenseTextSource` reads and the one the AAB carries — so a pass
 * also proves AGP's asset merge copies the files verbatim. The hashes were taken from the files as
 * fetched on 2026-10-24 and checked against the research-analyst's recorded prefixes (the MPL-2.0
 * text and the Public Suffix List NOTICE against the full hashes `research-analyst-mpl.md`
 * records, Apache-2.0 against tech design §3.6's full hash); the two OFL hashes are the committed
 * files', matching THIRD_PARTY.md §2. A hash changes only with a new verdict, never to make this
 * pass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class BundledLicenseTextHashTest {
    @Test
    fun everyBundledTextHashesToItsPinnedSha256() {
        val assets = RuntimeEnvironment.getApplication().assets

        assertThat(BundledLicenseText.entries).containsExactlyElementsIn(PINNED_SHA256.keys)
        BundledLicenseText.entries.forEach { text ->
            val bytes = assets.open(text.assetPath).use { it.readBytes() }
            assertWithMessage(text.assetPath).that(sha256Hex(bytes)).isEqualTo(PINNED_SHA256.getValue(text))
        }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { "%02x".format(it) }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** `Fredoka-OFL.txt`, 4,482 B, as committed (THIRD_PARTY.md §2: `782c3137…cf31f7e0`). */
private const val FREDOKA_OFL_SHA256 = "782c313774e59f7f0e2face680137ad0b578822ba1582160f30cb333cf31f7e0"

/** `Nunito-OFL.txt`, 4,479 B, as committed (THIRD_PARTY.md §2: `66db16f4…829734a2`). */
private const val NUNITO_OFL_SHA256 = "66db16f4d402431d22c354c3e7c740ab550f1ba1ef7cd7b70ff2ba13829734a2"

/** https://www.apache.org/licenses/LICENSE-2.0.txt, 11,358 B (tech design §3.6). */
private const val APACHE_2_SHA256 = "cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30"

/** `jakarta.inject-api-2.0.1.jar!/META-INF/NOTICE.md`, 1,074 B (`992ed62d…a512214df540`). */
private const val JAKARTA_INJECT_NOTICE_SHA256 = "992ed62dd3ee17d73ce6a4a5e02dd0fed9eb326bf386aa5304e6a512214df540"

/** protobuf `LICENSE` at v22.3, 1,732 B (prefix `6e5e117324afd944`). */
private const val PROTOBUF_BSD3_SHA256 = "6e5e117324afd944dcf67f36cf329843bc1a92229a8cd9bb573d7a83130fea7d"

/** Chromium `LICENSE` at 143.0.7445.0, 1,536 B (prefix `368cca1106be99d3`). */
private const val CHROMIUM_BSD3_SHA256 = "368cca1106be99d39ecd32a38d8305585d802a475effb66380b91ffc9bcf709b"

/** checker-qual 3.33.0 `LICENSE.txt`, 1,126 B (prefix `e90eaa08dea089df`). */
private const val CHECKER_QUAL_MIT_SHA256 = "e90eaa08dea089df10f2bf7f9dba63a5f6237c01562c4741e2e86773e78582d5"

/** OkHttp parent-4.11.0 `publicsuffix/NOTICE`, 218 B (full hash, `research-analyst-mpl.md` §3). */
private const val PUBLIC_SUFFIX_LIST_NOTICE_SHA256 = "8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a"

/** https://www.mozilla.org/media/MPL/2.0/index.txt, 16,726 B (full hash, `research-analyst-mpl.md` §3). */
private const val MPL_2_SHA256 = "3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04"

private val PINNED_SHA256 =
    mapOf(
        BundledLicenseText.FredokaOfl to FREDOKA_OFL_SHA256,
        BundledLicenseText.NunitoOfl to NUNITO_OFL_SHA256,
        BundledLicenseText.Apache2 to APACHE_2_SHA256,
        BundledLicenseText.JakartaInjectNotice to JAKARTA_INJECT_NOTICE_SHA256,
        BundledLicenseText.ProtobufBsd3 to PROTOBUF_BSD3_SHA256,
        BundledLicenseText.ChromiumBsd3 to CHROMIUM_BSD3_SHA256,
        BundledLicenseText.CheckerQualMit to CHECKER_QUAL_MIT_SHA256,
        BundledLicenseText.PublicSuffixListNotice to PUBLIC_SUFFIX_LIST_NOTICE_SHA256,
        BundledLicenseText.Mpl2 to MPL_2_SHA256,
    )
