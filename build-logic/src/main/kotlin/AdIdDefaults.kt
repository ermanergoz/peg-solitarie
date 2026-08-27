/**
 * Google's published **test** ad units, used for every debug build (PLAT-06 / D-05).
 *
 * A real ad unit in a build a developer or CI can run is what gets an AdMob account
 * suspended for invalid traffic, and that suspension is non-appealable. So debug is
 * hard-wired to these values and a fresh clone — which resolves nothing from
 * `local.properties` or the environment — still builds and still cannot serve a real ad.
 *
 * A plain `.kt` file rather than a `.gradle.kts` script so it is readable from both
 * `app/build.gradle.kts` and `AdIdResolutionTest`.
 */
object AdIdDefaults {
    /** Google's sample AdMob **app** ID (note the `~` separator, not `/`). D-05. */
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /** Google's test **rewarded** ad unit — the hint/undo/retry surface. D-05. */
    const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    /** Google's test **interstitial** ad unit — the between-levels surface. D-05. */
    const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Debug Play Games project ID. D-05.
     *
     * Google publishes **no** test Play Games project ID, so there is nothing safe to
     * hard-wire here; `"0"` is an inert placeholder that keeps the generated
     * `game_services_project_id` string resource well-formed (the manifest `meta-data`
     * value must be a `@string` resource — a bare numeric `android:value` is parsed as
     * an integer). Phase 11 supplies a real debug value from `local.properties` once the
     * Play Console project exists; until then nothing consumes this resource.
     */
    const val TEST_GAMES_PROJECT_ID = "0"
}
