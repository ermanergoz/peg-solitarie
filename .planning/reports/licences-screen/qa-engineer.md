# qa-engineer report — licences-screen (device)
Date: 2026-10-24
Branch: feature/licences-screen (head 0d38b27, code a71f21d)
Ran on: claude-opus-5-5 (Q-019). Loops: 0. Lessons applied: none.
> Producer note: returned as text; condensed.

## F-1 (High) — LS-AC06 / §5.1 pass lines 1, 3: each source address is its own TalkBack stop
Pixel_9_clean API 36, TalkBack 16. Kenney, LilMati, CogFireStudios, PSL (saved in tb-licenses-linear.txt) and the Google SDK terms address (seen live) are each read as a separate stop after their card; the SelectionContainer splits the address out of the card's merge. The LS-AC06 Compose oracle passes in CI, so the code-auditor re-reads it with the fix.

## PASS
LS-AC01 headings (General, Play Games, About, Privacy); LS-AC02 round trips (gesture nav); LS-AC03 18 entries/4 sections/strings incl. PSL address; LS-AC05 Fredoka + Jakarta two-text; §8.2-8 selection + copy; §8.2-1 light/dark 100/200 % (cosmetic: play-services-ads-identifier breaks mid-word at 200 %); LS-AC13 last row 'Tink' = index.tsv last line, 108 lines, debug index = release index; LS-AC15 Cronet opens/ends, am_anr 0; gfxinfo emulator Janky 51.34 %, p50 34 ms, p90 133 ms, p99 450 ms (for the tech-lead).

## BLOCKED
TalkBack on the other three screens; Cronet continuous read; '108 of 108'; LS-AC16; three-button nav; §8.2-4 airplane (radio toggles prohibited — needs a ruling or substitute); animator scale 0; ar-XB; 360x640 and tablet; sound.

Artifacts: .planning/qa/licences-screen/ (git-ignored, 19 MB). Device restored and shut down (TalkBack dev prefs 'Display speech output' + VERBOSE left on).

VERDICT: FAIL
