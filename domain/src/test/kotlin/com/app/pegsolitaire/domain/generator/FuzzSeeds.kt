package com.app.pegsolitaire.domain.generator

/**
 * Parses `FUZZ_SEEDS`, the number of Challenge levels the fuzz harness walks from level 1
 * (rules/puzzle-design.md invariant 8; docs/tech-design.md §7.3).
 *
 * Strict on purpose (rules/stacks.md contract 6): anything but a positive integer fails the run
 * and names the value. Missing, empty, `abc`, `1e3`, `0` and `-7` all fail; none may quietly turn
 * into another count, or into a harness that walks nothing and reads as a pass. A count above the
 * level space is valid here; the harness caps it and says so.
 */
internal object FuzzSeeds {
    fun parse(raw: String?): Int =
        requireNotNull(raw?.toIntOrNull()?.takeIf { it > 0 }) {
            "FUZZ_SEEDS must be a positive integer; was '$raw'"
        }
}
