# Peg Solitaire — Turkish localisation spec (tr)

Status: specified 2026-11-01 for 3.0.0 (founder directive, 2026-11-01: "also add turkish language
support in 3.0.0"). Branch `feature/turkish-locale`, cut from develop 37d3052. Re-voiced to
**sen** the same day on the founder's answer to Q-031; Q-032 (no "Solo Test") recorded (§9.1).
Owner: ux-designer. Builder: android-game-dev. Checker: qa-engineer on a device in Turkish.

Inputs: `app/src/main/res/values/strings.xml` on develop 37d3052 (every key below is from that
file); `docs/store/tr-TR/` (title, short and full description, changelog 30000); `docs/ux-spec.md`
§4.4.2 (the Turkish copy of record for the Undecided notice), §4.2 (Home tile, chip and pill
rules), §4.3.1 (the "Level N" title), §8 (copywriting contract), §11 (accessibility);
`docs/ux/licences-screen.md` §6 (the licences keys).

What this document is: the complete Turkish text of the app, the rules it follows, the rules the
build and QA must honour for a Turkish device, and the acceptance criteria. It is a spec: the
resource file `app/src/main/res/values-tr/strings.xml` is the developer's, written from §5 verbatim.

Sections: 1 Decisions · 2 Register and style · 3 Glossary · 4 Proper names · 5 Translation table ·
6 Truncation watchlist · 7 Locale rules for the build and QA · 8 Acceptance criteria ·
9 Open items, founder answers, the parallel branch · 10 Store copy re-voiced to sen (Q-031) ·
11 Change log

## 1. Decisions

The decisions, each a UX decision unless marked otherwise; the reasons are in the section named.

| # | Decision | Where |
|---|---|---|
| D-1 | Running text addresses the player as **sen** — the founder's answer to Q-031 (2026-11-01, option 2: sen throughout, the tr-TR listing re-edited to match, §10). Commands (buttons, Settings rows, action labels, content descriptions of controls) are the **bare second-person-singular imperative** ("Geri al", "Oturum aç"), which is the same form under either register. The ux-designer had recommended siz (§2.1 keeps the reasons for the record); the founder, a Turkish speaker, chose the warmer voice. | §2 |
| D-2 | **Sentence case everywhere.** Turkish has no title-case convention; the English file's Title Case buttons ("Play Again", "Keep Playing", "Continue Classic") become "Tekrar oyna", "Oynamaya devam et", "Klasik oyuna devam et". The three Results stat labels stay **all caps** as a typographic treatment: "PUAN", "SÜRE", "KALAN PİYON" — with the dotted capital İ. | §2 |
| D-3 | **No case transform on displayed text, ever**: the resource carries its own case. No `uppercase()`, `lowercase()`, `capitalize`, `replaceFirstChar`, `titlecase` or small-caps font feature on a string the player sees. The build has none today (helper grep 2026-11-01, production sources clean); the rule keeps it so. | §7.1 |
| D-4 | **Board names are proper names and stay as written**: English-33, European-37, Asymmetric 3-3-2-2, Diamond-41, Wiegleb-45. The tr-TR listing already keeps them. They become `translatable="false"` in the base file, with `app_name`, the "Play Games" section header, the em-dash placeholder and the `%1$d/%2$d` value. | §4 |
| D-5 | **Mode names**: Classic → **Klasik**, Challenge → **Meydan okuma**, exactly as the tr-TR listing writes them ("Klasik (Classic)", "Meydan okuma (Challenge)"). "Klasik" keeps its capital as a name; "okuma" stays lower-case inside a phrase. | §3 |
| D-6 | **"Level N" is written `Seviye N`** in labels, titles and pills (the chip, the status strip, the ribbon, the cloud-save line). In a sentence that needs a case ending, the number takes an ordinal and the noun takes the ending — "12. seviyeye" — **never a suffix on a format argument** (`%1$d'ye` is wrong for 1, 2, 6, 9, 10 …: Turkish suffixes follow the vowel of the spoken number). | §2.3 |
| D-7 | **Catalog → Tahtalar, Home → Ana menü, Results → Sonuç, Score → Puan, Time → Süre.** "Katalog" exists in Turkish but no Turkish player calls a list of boards that; "Ana menü" is what games call the mode screen. | §3 |
| D-8 | **Plurals**: Turkish has `one` and `other` in CLDR, so `values-tr` declares both items of every plural, and they are textually identical because a Turkish noun after a numeral stays singular ("1 piyon", "32 piyon"). Lint's MissingQuantity wants both; the grammar wants them equal. | §5.4 |
| D-9 | **Per-app language**: the app follows the system language, and declares `android:localeConfig` listing `en` and `tr` so Android 13+ shows Peg Solitaire in the system's per-app language picker. **No in-app language picker** in 3.0.0. | §7.4 |
| D-10 | **TalkBack strings are translated in full**, the whole `board_a11y_*` block included; the three tap-action labels are infinitives so TalkBack's Turkish template ("… için iki kez dokunun") reads as a sentence. | §5.2, §7.5 |
| D-11 | The Undecided notice's Turkish is the copy of record in `docs/ux-spec.md` §4.4.2, **verbatim**: "İpucu bu konumu süresi içinde çözemedi. Belki hâlâ kazanılabilir." (impersonal passive; no register; none of the ruling's forbidden words). | §5.2 |
| D-12 | Where the Turkish runs long for its slot, §6 gives a **shorter alternative**; the developer switches to it only when the device check (§8) shows clipping, and records the switch in the report. The spec's first choice is the better sentence. | §6 |
| D-13 | **Fonts — glyph coverage is proven, not assumed.** The developer proves by cmap that every face draws every Turkish letter it is asked to draw (§7.3). *Amended 2026-11-01:* the proof ran (android-game-dev report, open issue 1), and the remedy this row named — swap in the full file — has nothing to swap in: the shipped Fredoka files are not subsets, they are byte-identical to every official Fredoka v2.001 build, and **none maps Ğ ğ İ Ş ş**. Nunito maps all 13 code points. The fix is D-17 (D-16, the first fix, was rejected by the founder the same day). | §7.3 |
| D-14 | The tr-TR listing's closing line "Oyunun arayüzü İngilizcedir." becomes false the moment this ships and **must be removed** from `docs/store/tr-TR/full_description.txt`; the listing is re-voiced to sen (Q-031) and loses the English glosses "(Classic)", "(Challenge)", "(Settings)", "(Hint)", "(Undo)" that existed only because the UI was English — §10 carries the paste-ready text; the tr-TR and en-US 3.0.0 changelogs gain a Turkish-interface line; `docs/store/README.md` lines 25–26 are updated. Release-manager's files, through the producer (§9). | §9, §10 |
| D-15 | **No "Solo Test"** anywhere — not in the listing, not in the app. The founder's answer to Q-032 (2026-11-01: No); the research-analyst trademark check is dropped. | §4 |
| D-16 | *(Superseded the same day by D-17: the founder rejected it, Q-038 — "change the font for all languages".)* The heading face follows the language: Nunito headings on a Turkish device, Fredoka kept for the wordmark and the numerals. Nothing of it was built; §7.3 keeps the options it weighed as the record. | §7.3, §9.1 |
| D-17 | **Fredoka with Turkish letters, in every language** (founder, Q-039, 2026-11-01, on the research-analyst's specimen). Fredoka Medium and SemiBold gain precomposed Ğ ğ İ Ş ş as composite glyphs built from Fredoka's own G g I S s, breve, dot above and cedilla; every original glyph, advance and kerning pair is untouched, so English is unchanged to the pixel. The files are a **Modified Version under OFL-1.1** (no Reserved Font Name, so the name "Fredoka" stays), rebuilt byte-identically from a committed recipe; the contract is `docs/tech-design.md` §11.1 "Q-039". One heading face in both languages; the look the founder approved, kept. The licences screen says so in one new line, `licenses_fredoka_modified` (§5.4; `docs/ux/licences-screen.md` §3.2), and `licenses_psl_parts` takes the prose date in English too (§5.4). | §7.3, §7.6, §8 C-2 |

## 2. Register and style

### 2.1 Sen or siz — decided: sen (founder, Q-031, 2026-11-01)

**Decision: sen in running text; bare imperative on commands.** The founder chose option 2 of
Q-031: sen throughout, with the tr-TR listing re-edited to match (§10). The ux-designer had
recommended siz — the register of the listing as it stood, of Android's own Turkish UI and of the
Play Games and consent dialogs that open beside ours — and the reasons stay here for the record;
the founder, a Turkish speaker, judged the warmer voice right for a casual game, and one voice
across the app and the listing is kept by re-voicing the listing rather than the app.

What sen changes, mechanically: second-person possessives drop the plural marker ("ilerlemen",
"hakkın", "tahtan"), verbs take the singular ("dokun", "temizle", "devam et", "geç"), questions
end "ister misin?", and "siz seçtiğinizde" becomes "sen seçtiğinde". What it does not change:
commands on buttons, rows and action labels, which were already the bare singular imperative;
impersonal and passive sentences ("Bu tahta yeniden başlatılsın mı?", the Undecided notice, the
rule sentence of tutorial step 2); supporting lines that describe a setting in the third person;
headlines; the spoken cell parts. Twenty-two keys changed (listed in §11's change-log row).

What this looks like:

| Surface | Form | Example |
|---|---|---|
| Buttons, Settings rows, action labels, chip titles, content descriptions of controls | bare imperative, sentence case | "Tekrar oyna", "Tahtayı gör", "Eğitimi atla", "Geri dön" |
| Dialog bodies, captions, banners, snackbars, supporting lines that address the player | sen | "Mevcut ilerlemen kaybolacak.", "Bir piyona dokun, sonra …", "Tekrar denemek ister misin?" |
| Supporting lines that describe a setting | third person, descriptive | "Her atlamada hafifçe titreşir", "Tarayıcınızda açılır" |
| Headlines | exclamations, no person | "Mükemmel!", "Çözüldü!", "Az kaldı!" |
| The one first-person label | kept, it is idiomatic | "Anladım" (Got it) |
| Spoken-only TalkBack cell parts | nouns and adjectives, lower-case | "piyon", "boş", "seçili", "atlama yok" |
| Spoken-only tap-action labels | infinitive | "seçmek", "bırakmak", "buraya atlamak" |

### 2.2 Casing and letters

- Sentence case for every string (D-2). Proper names keep their capitals (Peg Solitaire,
  Play Games, Google, Kenney, English-33). "Klasik" is capitalised as a name; "Meydan okuma" is a
  phrase and stays as the listing writes it.
- The Results stat labels are all caps because the design draws them so: "PUAN", "SÜRE",
  "KALAN PİYON". The İ (U+0130) is in the resource. No code uppercases anything (D-3).
- Dotted and dotless i are two letters: ı/I and i/İ. "İpucu" starts with İ; "Yıldız", "Kalan",
  "yıl", "sıra" use ı. A string that shows "Ipucu" or "YILDIZ" where "Yıldız" was meant is a
  defect, not a style choice.
- Diacritics never dropped: ç ğ ı İ ö ş ü, and the circumflex in "hâlâ" (U+00E2) in the
  Undecided notice, which the copy of record carries.

### 2.3 Numbers, ordinals, suffixes

- **Never attach a Turkish suffix to a format argument.** "%1$d'ye" is right for 12 and wrong for
  1, 2, 6, 9, 10, 20 … because the suffix follows the vowel of the number as spoken. Every string
  below is phrased so the suffix sits on a noun: "%1$d. seviyeye", "%2$d yıldızın %1$d tanesi",
  "Satır %1$d, sütun %2$d konumuna".
- Labels write the number after the noun: "Seviye 12", "Adım 2". Sentences that need an ordinal
  write "12. seviye" (a full stop after the number is the Turkish ordinal mark).
- Digits are Western Arabic in Turkish; the clock "%02d:%02d" needs no change. The app shows no
  decimals; if one ever appears it uses the decimal comma ("1,5") through `NumberFormat`, never a
  hand-built string. Thousands group with a full stop ("10.000"), as the listing writes; the
  Results score is shown ungrouped in both languages today and stays so (not a localisation
  defect; noted in §9).
- Dates in prose: "15 Şubat 2023" (the one date, in the licences screen), never "02/15/2023".

### 2.4 Punctuation

- The em dash "—" keeps the English "problem — what to do" shape in the banner and the
  ad-unavailable snackbars, so the two languages share a look; Turkish typography accepts it.
- "Ayarlar'dan", "Play Games'te", "OkHttp'ye": an apostrophe separates a suffix from a proper
  name or a UI name, as Turkish spelling requires and as the listing writes. In XML these are
  escaped `\'` exactly like the English `Couldn\'t`.
- Quotation marks: “ ” (U+201C/U+201D), as the English file uses for the sound titles.
- Question dialogs use the Android pattern "… yapılsın mı?" ("Bu tahta yeniden başlatılsın mı?")
  rather than a literal "Yeniden başlatmak ister misiniz?", which is longer and sounds like a form.

## 3. Glossary

One Turkish word per game term, used the same way in every string and matching the tr-TR
listing where the listing has the term. The "Listing" column quotes the listing as it stood on
2026-11-01, before Q-031 re-voiced it (so some quotes are in siz); the terms themselves are
unchanged, and §10 carries the sen version of the listing.

| English | Türkçe | Listing | Notes |
|---|---|---|---|
| peg | piyon | "piyon" | never "taş" or "pim"; the listing chose piyon |
| hole (empty) | delik / boş delik | "boş deliğe" | "boş" alone for the spoken cell state |
| jump (noun) | atlama | "Atlamalar" | "2 atlama", "atlama hedefi" |
| jump (verb, the player makes a peg jump) | atlatmak | "atlatın" | "Atlatmaya devam et" |
| jump (verb, the peg jumps / TalkBack action) | atlamak | — | "yukarı atla", "buraya atlamak" |
| board | tahta | "tahta" | |
| clear the board | tahtayı temizlemek | "tahtayı temizleyin" | |
| move (a move in the game) | hamle | "hamle" | "Hamle kalmadı", "Olası hamleleri göster" |
| winning move | kazandıran hamle | "kazandıran gerçek bir hamle" | |
| Classic (mode) | Klasik | "Klasik (Classic)" | capitalised as a name |
| Challenge (mode) | Meydan okuma | "Meydan okuma (Challenge)" | sentence case inside phrases |
| level | seviye | "seviye" | "Seviye 12" in labels; "12. seviyeye" in sentences (D-6) |
| ladder (Challenge) | merdiven | "merdiven" | not used in any app string; listing only |
| hint | ipucu / İpucu | "İpucu (Hint)" | capital İ at the start of a label |
| undo | geri al / geri alma | "Geri al (Undo)" | the control "Geri al"; the count noun "geri alma" |
| free allowance | ücretsiz hak | "ücretsiz haklarınız" | "ücretsiz ipucu hakkınız" |
| restart | yeniden başlat | — | the control; "Baştan" only as a §6 fallback |
| retry | yeniden dene | — | |
| replay / play again | tekrar oyna | — | one Turkish label for both English labels |
| continue | devam et | — | "Oynamaya devam et", "Klasik oyuna devam et" |
| next level | sonraki seviye | — | |
| stars | yıldız | "yıldız" | singular after numbers and as a pill label |
| best (score/time) | rekor / en iyi | "en iyi puanınızı ve sürenizi" | "Yeni rekor!" for the cue |
| score | puan | "puanınızı" | "PUAN" on Results |
| time (elapsed) | süre | "sürenizi" | "SÜRE"; "Geçen süre" for the clock's name |
| pegs left | kalan piyon | — | "KALAN PİYON"; "%d piyon kaldı" in sentences |
| catalog (the Classic list) | Tahtalar | — | D-7 |
| Home | Ana menü | — | D-7 |
| Results | Sonuç | "sonuç ekranından" | |
| Settings | Ayarlar | "Ayarlar'dan (Settings)" | |
| tutorial | eğitim | "Kısa bir eğitim" | "Eğitimi atla", "Eğitimi yeniden oynat" |
| How to play | Nasıl oynanır | — | |
| sound | ses | "ses" | |
| haptics | titreşim | — | Android's "dokunsal geri bildirim" is opaque; the supporting line explains |
| colorblind palette | renk körlüğü paleti | "renk körlüğüne uygun bir renk paleti" | |
| ad / rewarded ad | reklam / ödüllü reklam | "reklam", "Ödüllü reklam" | "Reklam izle" |
| sign in | oturum aç | "Oturum açarak" | Google's own term |
| leaderboards | skor tabloları | "skor tablosunda" | |
| achievements | başarımlar | "başarım" | |
| cloud save | bulut kaydı | "bulutta saklayın" | "bulut kaydı" as a noun; "bulutta yedekli tutun" as the verb |
| progress | ilerleme | "ilerlemenizi" | |
| account | hesap | — | |
| privacy policy | gizlilik politikası | "Gizlilik politikası" | |
| consent (ads) | reklam izni | — | "Reklam izni tercihlerinizi değiştirin" |
| licenses | lisanslar | — | |
| fonts | yazı tipleri | — | Android's term |
| open-source software | açık kaynaklı yazılım | — | |
| notice (legal) | bildirim | — | |
| loading | yükleniyor | — | "Seviye yükleniyor", "Reklam yükleniyor" |
| row / column | satır / sütun | — | spoken board only |
| up / down / left / right | yukarı / aşağı / sola / sağa | "yukarı, aşağı, sağa ve sola" | directions take the dative where the verb needs it |
| step (tutorial) | adım | "her adımda" | |

## 4. Proper names

A proper name is shown as its owner writes it, in every language.

- **The title**: "Peg Solitaire" everywhere — launcher label, `app_name`, the sentences in
  `licenses_intro` and `play_games_prompt_body`, and the tr-TR store title, which already is
  "Peg Solitaire". Turkey knows the physical game as "Solo Test"; the founder declined to name it
  in the listing (Q-032, 2026-11-01), so it appears nowhere (D-15).
- **Board names**: English-33, European-37, Asymmetric 3-3-2-2, Diamond-41, Wiegleb-45 — as the
  tr-TR listing keeps them (it writes "Asymmetric-39"; the app's "Asymmetric 3-3-2-2" is the app's
  name for the same board in both languages, so this is not a localisation difference). No
  "İngiliz-33" or "Avrupa-37": the names are the boards' identities across the catalog, the
  Play Games leaderboard titles and the cloud-save line, and a Turkish player searching for help
  finds "English board", not "İngiliz tahtası".
- **Product and library names**: Play Games, Google Play, AndroidX, Jetpack Compose, Kotlin,
  Dagger, Hilt, OkHttp, Okio, Gson, Tink, Guava, Cronet, Chromium, Protocol Buffers, DataStore,
  Google Mobile Ads, User Messaging Platform, Fredoka, Nunito, Kenney, LilMati, CogFireStudios,
  Mozilla, JetBrains, Square. Suffixes attach with an apostrophe: "Play Games'te", "OkHttp'ye",
  "Google'ın".
- **Licence names and addresses** are already `translatable="false"` in the base file and are
  not in §5.
- **What is translated around a name**: "AndroidX **ve** Jetpack Compose", "Google Play
  hizmetleri ve SDK'ları", "Interface Sounds — Kenney". The English "by" credit becomes a
  dash-separated "title — author", which needs no suffix on the author's name.

Keys that become `translatable="false"` in `values/strings.xml` (D-4), so lint asks for no Turkish
and the name cannot drift: `app_name`, `board_english33`, `board_european37`,
`board_asymmetric39`, `board_diamond41`, `board_wiegleb45`, `settings_section_play_games`,
`stat_value_unknown`, `home_stat_boards_value`.

## 5. Translation table

How to read the table: one row per key of `values/strings.xml` on develop 37d3052, in the file's
own order. The Turkish column is the resource value **verbatim** — the developer copies it, then
escapes `'` as `\'` for XML. Format arguments are kept exactly (same indices, same types; an index
may repeat, as `%1$d` does in the two ad bodies); where Turkish reorders them the positional index
carries the order. "(!) §6" marks a string that is much longer than its English and has a shorter
alternative in §6. "same" means the Turkish value equals the English and is still written into
`values-tr` (the key is locale-specific by design: a separator, a format join); keys that are
`translatable="false"` are listed in §4 and §5.5, not here.

### 5.1 Gameplay dialogs, Home, catalog, status strip and controls

| Key | English | Türkçe | Note |
|---|---|---|---|
| `app_name` | Peg Solitaire | — | `translatable="false"` (D-4) |
| `restart_board` | Restart Board | Tahtayı yeniden başlat | content description, icon-only |
| `play_again` | Play Again | Tekrar oyna | dialog button |
| `view_board` | View Board | Tahtayı gör | dialog button |
| `game_over_perfect_headline` | Perfect! | Mükemmel! | |
| `game_over_perfect_body` | You cleared the board down to a single peg. | Tahtayı tek piyona kadar temizledin. | sen |
| `game_over_near_headline` | So close! | Az kaldı! | |
| `game_over_stuck_headline` | No moves left | Hamle kalmadı | |
| `restart_confirm_title` | Restart this board? | Bu tahta yeniden başlatılsın mı? | (!) §6 |
| `restart_confirm_body` | Your current progress will be lost. | Mevcut ilerlemen kaybolacak. | sen |
| `keep_playing` | Keep Playing | Oynamaya devam et | (!) §6 |
| `board_english33` … `board_wiegleb45` | English-33, European-37, Asymmetric 3-3-2-2, Diamond-41, Wiegleb-45 | — | `translatable="false"` (D-4) |
| `classic_tile_title` | Classic | Klasik | Home tile |
| `classic_tile_supporting` | 5 boards | 5 tahta | |
| `challenge_tile_title` | Challenge | Meydan okuma | Home tile; (!) §6 |
| `challenge_tile_continue` | Continue — Level %1$d | Devam et — Seviye %1$d | tile chip |
| `coming_soon` | Coming soon | Çok yakında | |
| `catalog_title` | Classic | Klasik | screen title |
| `catalog_stars_earned` | %1$d of %2$d stars | %2$d yıldızın %1$d tanesi | spoken; args reordered by index |
| `navigate_back` | Navigate back | Geri dön | content description |
| `elapsed_time` | Elapsed time | Geçen süre | content description; joined by `elapsed_time_spoken` |
| `elapsed_time_spoken` | %1$s, %2$s | same | format join; copy as is |
| `duration_under_a_minute` | under a minute | bir dakikadan az | spoken: "Geçen süre, bir dakikadan az" |
| `challenge_level_indicator` | Level %1$d | Seviye %1$d | status-strip title, `maxLines = 1`; "Seviye 1234" is one glyph wider than "Level 1234", inside the ≥ 25dp spare of ux-spec §4.3.1 |
| `challenge_level_indicator_desc` | Challenge level %1$d | Meydan okuma seviyesi %1$d | spoken |
| `undo_action` | Undo | Geri al | content description and banner label |
| `hint_action` | Hint | İpucu | content description; capital İ |
| `hint_thinking` | Finding a move | Hamle aranıyor | announced |
| `show_moves_on` | Show possible moves | Olası hamleleri göster | the switch's one name in both states |
| `loading_level` | Loading level | Seviye yükleniyor | |

### 5.2 The board with TalkBack, the rescue banner, the Undecided notice, Results

Every string here is spoken, never drawn (ux-spec §11.1). Rows and columns are spoken from 1.
Cell parts are joined by `board_a11y_separator` in the base order, so a cell reads
"Satır 3, sütun 4, piyon, seçili, 2 atlama" or "Satır 3, sütun 6, boş, atlama hedefi". The three
tap-action labels are infinitives (D-10) because TalkBack's Turkish hint template is "<label> için
iki kez dokunun": "seçmek için iki kez dokunun" is a sentence, "seç için" is not. The per-jump
custom actions are read as labels in the actions menu, so they are imperatives.

| Key | English | Türkçe | Note |
|---|---|---|---|
| `board_a11y_label` | Board, %1$s | Tahta, %1$s | "Tahta, 32 piyon kaldı" |
| `board_a11y_separator` | ", " | same | copy as is, quotes included |
| `board_a11y_position` | Row %1$d, column %2$d | Satır %1$d, sütun %2$d | |
| `board_a11y_peg` | peg | piyon | |
| `board_a11y_empty` | empty | boş | |
| `board_a11y_selected` | selected | seçili | |
| `board_a11y_no_jumps` | no jumps | atlama yok | |
| `board_a11y_jump_target` | jump target | atlama hedefi | |
| `board_a11y_hint_source` | hint, move this peg | ipucu, bu piyonu oynat | |
| `board_a11y_hint_target` | hint, land here | ipucu, buraya in | |
| `board_a11y_action_select` | select | seçmek | infinitive (D-10) |
| `board_a11y_action_put_down` | put down | bırakmak | infinitive (D-10) |
| `board_a11y_action_jump_here` | jump here | buraya atlamak | infinitive (D-10) |
| `board_a11y_action_jump` | Jump %1$s to row %2$d, column %3$d | Satır %2$d, sütun %3$d konumuna %1$s atla | custom action label; "Satır 4, sütun 4 konumuna yukarı atla" |
| `board_a11y_direction_up` | up | yukarı | |
| `board_a11y_direction_down` | down | aşağı | |
| `board_a11y_direction_left` | left | sola | dative: "sola atla" |
| `board_a11y_direction_right` | right | sağa | dative: "sağa atla" |
| `board_a11y_announce_selected` | Selected row %1$d, column %2$d, %3$s. | Satır %1$d, sütun %2$d seçildi, %3$s. | %3$s is the jumps plural or "atlama yok" |
| `board_a11y_announce_put_down` | Peg put down, row %1$d, column %2$d. | Piyon bırakıldı, satır %1$d, sütun %2$d. | |
| `board_a11y_announce_jump` | Row %1$d, column %2$d to row %3$d, column %4$d. Removed row %5$d, column %6$d. %7$s. | Satır %1$d, sütun %2$d konumundan satır %3$d, sütun %4$d konumuna. Satır %5$d, sütun %6$d kaldırıldı. %7$s. | %7$s is the pegs-left plural |
| `board_a11y_rejected_no_peg` | No peg here. | Burada piyon yok. | |
| `board_a11y_rejected_not_target` | Not a jump target. | Atlama hedefi değil. | |
| `board_a11y_rejected_put_down_first` | Put down the selected peg first. | Önce seçili piyonu bırak. | sen |
| `board_a11y_announce_undo` | Undid row %1$d, column %2$d to row %3$d, column %4$d. %5$s. | Satır %1$d, sütun %2$d konumundan satır %3$d, sütun %4$d konumuna atlama geri alındı. %5$s. | |
| `board_a11y_announce_hint` | Hint: row %1$d, column %2$d, jump %3$s to row %4$d, column %5$d. | İpucu: satır %1$d, sütun %2$d; satır %4$d, sütun %5$d konumuna %3$s atla. | %3$s is a direction |
| `board_a11y_announce_restart` | Board restarted. %1$s. | Tahta yeniden başlatıldı. %1$s. | |
| `board_a11y_announce_step` | %1$s. %2$s | same | format join; copy as is |
| `results_a11y_summary` | %1$s — %2$s, %3$s. | same | format join; copy as is |
| `nowin_message` | No winning move — undo or restart | Kazandıran hamle yok — geri al ya da yeniden başlat | banner; (!) §6 |
| `stuck_message` | No moves left — undo or restart | Hamle kalmadı — geri al ya da yeniden başlat | banner; (!) §6 |
| `hint_undecided_message` | Hint couldn't work this position out in time. It may still be winnable. | İpucu bu konumu süresi içinde çözemedi. Belki hâlâ kazanılabilir. | **verbatim** copy of record, ux-spec §4.4.2 (D-11) |
| `rescue_restart` | Restart | Yeniden başlat | banner label; (!) §6 |
| `results_title` | Results | Sonuç | |
| `results_headline_perfect` | Perfect! | Mükemmel! | |
| `results_headline_solved` | Solved! | Çözüldü! | |
| `results_headline_stuck` | Out of moves | Hamle kalmadı | the same event as `game_over_stuck_headline`; one wording |
| `results_label_score` | SCORE | PUAN | all caps by design (D-2) |
| `results_label_time` | TIME | SÜRE | all caps |
| `results_label_pegs_left` | PEGS LEFT | KALAN PİYON | all caps, dotted İ; (!) §6 |
| `action_replay` | Replay | Tekrar oyna | Results action; (!) §6 |
| `action_catalog` | Catalog | Tahtalar | Results action (D-7) |
| `action_home` | Home | Ana menü | Results and banner action (D-7) |
| `results_new_best` | New best! | Yeni rekor! | cue; (!) §6 |
| `results_ad_scrim_description` | Preparing | Hazırlanıyor | content description |
| `action_next_level` | Next level | Sonraki seviye | (!) §6 |
| `action_retry` | Retry | Yeniden dene | (!) §6 |
| `challenge_load_error` | Couldn't load this level. Tap Retry. | Bu seviye yüklenemedi. Yeniden dene düğmesine dokun. | sen; names the button without a suffix on it |

### 5.3 Settings, the Home ribbon and Continue tiles, the tutorial, ads and privacy

| Key | English | Türkçe | Note |
|---|---|---|---|
| `settings_title` | Settings | Ayarlar | |
| `settings_action` | Settings | Ayarlar | the Home gear's content description |
| `settings_sound_label` | Sound | Ses | |
| `settings_sound_supporting` | Play the jump, button and celebration sounds | Atlama, düğme ve kutlama seslerini çalar | descriptive (§2.1) |
| `settings_haptics_label` | Haptics | Titreşim | |
| `settings_haptics_supporting` | Vibrate lightly on each jump | Her atlamada hafifçe titreşir | |
| `settings_colorblind_label` | Colorblind palette | Renk körlüğü paleti | |
| `settings_colorblind_supporting` | Clearer moves, hints and stars | Hamleler, ipuçları ve yıldızlar daha net görünür | the listing's own sentence |
| `settings_how_to_play_label` | How to play | Nasıl oynanır | |
| `settings_how_to_play_supporting` | Replay the tutorial | Eğitimi yeniden oynat | |
| `stat_value_unknown` | — | — | `translatable="false"` (D-4) |
| `stat_value_unknown_desc` | not yet available | henüz yok | spoken: "Yıldız, henüz yok" |
| `home_stat_pill_desc` | %1$s, %2$s | same | format join; copy as is |
| `home_stat_stars_label` | Stars | Yıldız | pill label; singular (D-8) |
| `home_stat_boards_label` | Boards | Tahta | pill label; singular |
| `home_stat_level_label` | Level | Seviye | pill label |
| `home_stat_boards_value` | %1$d/%2$d | — | `translatable="false"` (D-4) |
| `resume_classic_title` | Continue Classic | Klasik oyuna devam et | Continue tile; (!) §6 |
| `resume_classic_supporting` | Pick up your saved board | Kaydettiğin tahtadan devam et | sen |
| `resume_challenge_title` | Continue Challenge | Meydan okumaya devam et | Continue tile; (!) §6 |
| `resume_challenge_supporting` | Pick up Level %1$d where you left off | %1$d. seviyeye kaldığın yerden devam et | sen; ordinal on the number, suffix on the noun (D-6) |
| `tutorial_step_progress` | Step %1$d of %2$d | Adım %1$d, toplam %2$d | spoken only (the pip row); no slash, which TTS would read |
| `tutorial_step_1_caption` | Tap a peg, then tap the empty hole two cells away. | Bir piyona dokun, sonra iki kare ötedeki boş deliğe dokun. | sen; caption wraps |
| `tutorial_step_2_caption` | Jumps work up, down, left and right — never diagonally. | Atlamalar yukarı, aşağı, sola ve sağa yapılır — asla çapraz yapılmaz. | the listing's rule sentence |
| `tutorial_step_3_caption` | Keep jumping. Clear the board down to one peg. | Atlatmaya devam et. Tahtayı tek piyona kadar temizle. | sen |
| `tutorial_done_caption` | That's the whole game. Go clear a board. | Oyun bundan ibaret. Hadi, bir tahta temizle. | sen; "Hadi" carries the English "Go" |
| `onboarding_got_it` | Got it | Anladım | |
| `onboarding_skip` | Skip tutorial | Eğitimi atla | |
| `ad_offer_watch` | Watch ad | Reklam izle | dialog button |
| `ad_offer_decline` | No thanks | Hayır, teşekkürler | dialog button; (!) §6 |
| `ad_offer_hint_title` | Out of free hints | Ücretsiz ipucu hakkın bitti | sen |
| `ad_offer_undo_title` | Out of free undos | Ücretsiz geri alma hakkın bitti | sen |
| `ad_offer_hint_body` | You've used your %1$d free hints for this level. Watch a short ad for %1$d more? | Bu seviyedeki %1$d ücretsiz ipucu hakkını kullandın. %1$d ipucu daha için kısa bir reklam izlemek ister misin? | sen; `%1$d` twice, as in English |
| `ad_offer_undo_body` | You've used your %1$d free undos for this level. Watch a short ad for %1$d more? | Bu seviyedeki %1$d ücretsiz geri alma hakkını kullandın. %1$d geri alma daha için kısa bir reklam izlemek ister misin? | sen; `%1$d` twice |
| `ad_offer_loading` | Loading ad | Reklam yükleniyor | content description |
| `ad_unavailable_hints` | Ad unavailable — hints refilled for free | Reklam gösterilemedi — ipuçları ücretsiz yenilendi | snackbar, two lines allowed |
| `ad_unavailable_undos` | Ad unavailable — undos refilled for free | Reklam gösterilemedi — geri almalar ücretsiz yenilendi | snackbar |
| `settings_section_general` | General | Genel | |
| `settings_section_privacy` | Privacy | Gizlilik | |
| `settings_privacy_policy_label` | Privacy policy | Gizlilik politikası | |
| `settings_privacy_policy_supporting` | Opens in your browser | Tarayıcında açılır | sen |
| `settings_privacy_policy_error` | Couldn't open the privacy policy — no browser app found. | Gizlilik politikası açılamadı — tarayıcı uygulaması bulunamadı. | |
| `settings_privacy_choices_label` | Manage privacy choices | Gizlilik tercihlerini yönet | |
| `settings_privacy_choices_supporting` | Change your ad consent choices | Reklam izni tercihlerini değiştir | sen |

### 5.4 Play Games, cloud save, licences

| Key | English | Türkçe | Note |
|---|---|---|---|
| `play_games_prompt_title` | Sign in to Play Games? | Play Games'te oturum açılsın mı? | |
| `play_games_prompt_body` | Compete on leaderboards, unlock achievements, and keep your progress backed up to the cloud. Peg Solitaire plays exactly the same either way — you can sign in later from Settings. | Skor tablolarında yarış, başarımların kilidini aç ve ilerlemeni bulutta yedekli tut. Peg Solitaire her iki durumda da aynı şekilde oynanır — daha sonra Ayarlar'dan oturum açabilirsin. | sen; names the same three benefits as the Settings row |
| `play_games_prompt_accept` | Sign in | Oturum aç | |
| `play_games_prompt_decline` | Not now | Şimdi değil | |
| `play_games_signin_error` | Couldn't sign in to Play Games | Play Games'te oturum açılamadı | |
| `settings_section_play_games` | Play Games | — | `translatable="false"` (D-4) |
| `settings_play_games_signin_label` | Sign in to Play Games | Play Games'te oturum aç | |
| `settings_play_games_signin_supporting` | Leaderboards, achievements and cloud save | Skor tabloları, başarımlar ve bulut kaydı | |
| `settings_play_games_signed_in_as` | Signed in as %1$s | %1$s olarak oturum açıldı | |
| `settings_play_games_signed_in_fallback` | Signed in to Play Games | Play Games'te oturum açıldı | |
| `settings_play_games_leaderboards_label` | Leaderboards | Skor tabloları | |
| `settings_play_games_leaderboards_supporting` | See how your scores rank | Puanlarının sıralamasını gör | sen |
| `settings_play_games_achievements_label` | Achievements | Başarımlar | |
| `settings_play_games_achievements_supporting` | View the achievements you've earned | Kazandığın başarımları gör | sen |
| `cloud_save_description` | Challenge level %1$d · %2$d stars · %3$d/%4$d Classic boards | Meydan okuma seviyesi %1$d · %2$d yıldız · %3$d/%4$d Klasik tahta | written into the snapshot in the device locale at commit time, by design |
| `profile_swap_title` | Moving to a different account? | Farklı bir hesaba mı geçiyorsun? | sen |
| `profile_swap_body` | You're signed in as %1$s. Keeping this device's progress merges it into their cloud save — nothing is lost on either side. Using their cloud progress instead replaces everything on this device: your level results, your play history, your Challenge level and both in-progress games. | %1$s olarak oturum açtın. Bu cihazın ilerlemesini korumak, onu bu hesabın bulut kaydıyla birleştirir — iki tarafta da hiçbir şey kaybolmaz. Bunun yerine bu hesabın bulut ilerlemesini kullanmak, bu cihazdaki her şeyin yerine geçer: seviye sonuçların, oyun geçmişin, Meydan okuma seviyen ve devam eden iki oyunun. | sen; all four wiped categories kept (D-14 of phase 13) |
| `profile_swap_body_unknown_account` | You're signed in with a different Play Games account. Keeping this device's progress merges it into this account's cloud save — … and both in-progress games. | Farklı bir Play Games hesabıyla oturum açtın. Bu cihazın ilerlemesini korumak, onu bu hesabın bulut kaydıyla birleştirir — iki tarafta da hiçbir şey kaybolmaz. Bunun yerine bu hesabın bulut ilerlemesini kullanmak, bu cihazdaki her şeyin yerine geçer: seviye sonuçların, oyun geçmişin, Meydan okuma seviyen ve devam eden iki oyunun. | sen; same wording, "bu hesabın" |
| `profile_swap_keep` | Keep this device's progress | Bu cihazın ilerlemesini koru | gradient CTA; same length as English |
| `profile_swap_use_cloud` | Use their cloud progress | Bulut ilerlemesini kullan | text button; account-neutral by design |
| `settings_section_about` | About | Hakkında | |
| `settings_licenses_label` | Licenses | Lisanslar | |
| `settings_licenses_supporting` | Fonts, sounds and open-source software | Yazı tipleri, sesler ve açık kaynaklı yazılım | |
| `licenses_title` | Licenses | Lisanslar | |
| `licenses_intro` | Peg Solitaire is made with these fonts, sounds and open-source software. Thank you to their authors. | Peg Solitaire bu yazı tipleri, sesler ve açık kaynaklı yazılımlarla yapıldı. Yazarlarına teşekkürler. | |
| `licenses_section_fonts` | Fonts | Yazı tipleri | |
| `licenses_section_sounds` | Sounds | Sesler | |
| `licenses_section_software` | Open-source software | Açık kaynaklı yazılım | |
| `licenses_section_google` | Google libraries | Google kitaplıkları | |
| `licenses_read_action` | Read the license | Lisansı oku | |
| `licenses_loading` | Loading license | Lisans yükleniyor | |
| `licenses_text_error` | This license text could not be shown. | Bu lisans metni gösterilemedi. | |
| `licenses_fredoka_modified` | Modified for Peg Solitaire under the SIL Open Font License: Ğ ğ İ Ş ş added from Fredoka\'s own letters and marks; nothing else changed. | Peg Solitaire için SIL Open Font License kapsamında değiştirildi: Ğ ğ İ Ş ş harfleri Fredoka\'nın kendi harf ve işaretlerinden eklendi; başka hiçbir şey değiştirilmedi. | **new 2026-11-01 (D-17)**: the Fredoka entry's third line, after the credit and the licence name. A sentence, not a name, so it is **translatable** with this row (the credit lines beside it stay `translatable="false"`); descriptive, no person (§2.1). The licence name stays as its owner writes it and takes no suffix ("kapsamında" is a postposition); the apostrophes are `\'` in XML. Multi-line by design, like the other parts lines |
| `licenses_kenney_title` | Interface Sounds by Kenney | Interface Sounds — Kenney | "title — author" (§4) |
| `licenses_lilmati_title` | “Pop 01” by LilMati | “Pop 01” — LilMati | |
| `licenses_cogfire_title` | “Positive Blip Effect” by CogFireStudios | “Positive Blip Effect” — CogFireStudios | |
| `licenses_androidx_title` | AndroidX and Jetpack Compose | AndroidX ve Jetpack Compose | |
| `licenses_kotlin_title` | Kotlin and kotlinx | Kotlin ve kotlinx | |
| `licenses_kotlin_parts` | Standard library, coroutines, serialization | Standart kitaplık, coroutine'ler, serileştirme | |
| `licenses_dagger_title` | Dagger and Hilt | Dagger ve Hilt | |
| `licenses_okhttp_title` | OkHttp and Okio | OkHttp ve Okio | |
| `licenses_okhttp_parts` | Networking and I/O inside Google Mobile Ads | Google Mobile Ads içindeki ağ iletişimi ve G/Ç | |
| `licenses_gson_title` | Gson, Tink and Guava | Gson, Tink ve Guava | |
| `licenses_annotations_title` | Annotation and injection APIs | Ek açıklama ve bağımlılık ekleme API'leri | |
| `licenses_protobuf_parts` | The Java Lite runtime, and the copy inside AndroidX DataStore | Java Lite çalışma zamanı ve AndroidX DataStore içindeki kopyası | |
| `licenses_chromium_parts` | The Cronet networking classes inside Google Mobile Ads | Google Mobile Ads içindeki Cronet ağ sınıfları | |
| `licenses_psl_credit` | Mozilla and the list's contributors | Mozilla ve listeye katkıda bulunanlar | |
| `licenses_psl_parts` | The public-suffix data compiled into OkHttp, as it stood on 15 February 2023 | OkHttp'ye derlenmiş genel sonek verileri, 15 Şubat 2023 tarihindeki hâliyle | the date in words needs no non-breaking hyphens. **English changed 2026-11-01** (tech-lead, tech-design §16 #41; confirmed by the ux-designer): it was "2023‑02‑15" with U+2011 non-breaking hyphens, which Nunito's cmap lacks — the body face was already falling back for them; the prose date is whole without any hyphen, matches the Turkish and §2.3, and is the wording of record |
| `licenses_google_sdk_title` | Google Play services and SDKs | Google Play hizmetleri ve SDK'ları | |
| `licenses_google_notices_title` | Open-source notices in Google libraries | Google kitaplıklarındaki açık kaynak bildirimleri | |
| `licenses_google_notices_supporting` | The notices Google supplies for the open-source software inside its libraries | Google'ın kitaplıklarındaki açık kaynaklı yazılım için sağladığı bildirimler | |
| `licenses_google_notices_action` | Open the notices | Bildirimleri aç | |
| `license_text_part_notice` | Notice | Bildirim | |
| `license_text_end` | End of text | Metnin sonu | |
| `google_notices_title` | Open-source notices | Açık kaynak bildirimleri | |
| `google_notices_intro` | Google supplies these notices for the open-source software inside its libraries. Each one is shown as supplied. | Google bu bildirimleri kitaplıklarındaki açık kaynaklı yazılım için sağlar. Her biri sağlandığı şekliyle gösterilir. | |
| `google_notices_found_in` | Found in %1$s | %1$s içinde bulundu | |
| `google_notices_ordinal` | Notice %1$d of %2$d | Bildirim %1$d / %2$d | drawn, not spoken-only |
| `google_notices_read_action` | Read the notice | Bildirimi oku | |
| `google_notices_loading` | Loading notices | Bildirimler yükleniyor | |
| `google_notices_error` | These notices could not be shown. | Bu bildirimler gösterilemedi. | |
| `google_notice_text_error` | This notice could not be shown. | Bu bildirim gösterilemedi. | |
| `google_notice_text_unknown_title` | Notice | Bildirim | |

The 26 licence keys already marked `translatable="false"` in the base file (licence names, the
font and credit lines, addresses, `licenses_androidx_parts`, `licenses_gson_parts`,
`licenses_annotations_parts`, `licenses_google_sdk_parts`, the Jakarta, Protocol Buffers,
Chromium, Public Suffix List and Checker Framework titles and credits) are untouched and get no
Turkish row.

### 5.5 Plurals

Turkish declares `one` and `other` (CLDR), and the two items are identical because the noun after a
number stays singular (D-8). Both items are written out so lint's MissingQuantity is satisfied.

| Plural | English one / other | Türkçe `one` | Türkçe `other` |
|---|---|---|---|
| `pegs_remaining` | %d peg remaining. / %d pegs remaining. | %d piyon kaldı. | %d piyon kaldı. |
| `pegs_remaining_retry` | %d peg remaining. Try again? / %d pegs remaining. Try again? | %d piyon kaldı. Tekrar denemek ister misin? | %d piyon kaldı. Tekrar denemek ister misin? |
| `holes_count` | %d hole / %d holes | %d delik | %d delik |
| `duration_minutes` | %d minute / %d minutes | %d dakika | %d dakika |
| `board_a11y_pegs_left` | %d peg left / %d pegs left | %d piyon kaldı | %d piyon kaldı |
| `board_a11y_jumps` | %d jump / %d jumps | %d atlama | %d atlama |
| `licenses_sound_effects` | %d sound effect / %d sound effects | %d ses efekti | %d ses efekti |

### 5.6 Count

Base file on develop 37d3052, counted by hand: 223 `<string>` entries and 7 `<plurals>`. Of the
strings, 35 are already `translatable="false"` and 9 more become so (§4), which leaves about
**179 strings and 7 plurals** in `values-tr/strings.xml` (180 strings from 2026-11-01, with
`licenses_fredoka_modified`, D-17): 174 translated (175 with it), 5 copied verbatim
(`elapsed_time_spoken`, `board_a11y_separator`, `board_a11y_announce_step`,
`results_a11y_summary`, `home_stat_pill_desc`). The developer's parity test (§7.6) is the
authority on the count, not this paragraph: every translatable key in the base file has a row in
§5.1–5.5, and a key the test finds without one is a spec defect to report, not to fill in.

## 6. Truncation watchlist

Turkish runs 20–60 % longer than English in these slots. The first choice in §5 stands unless
the device check in §8 shows clipping, an ellipsis, or a button column where a row was designed;
then the developer switches to the alternative and says so in the report. Character counts are
of the visible text. Slot facts are from `docs/ux-spec.md` and the 2026-11-01 source read: the
ribbon pills wrap their label and never truncate (§4.2.3); the "Level N" title has
`maxLines = 1` with an ellipsis and ≥ 25dp spare at "Level 1234" (§4.3.1); the Home tile and
chip text (`GelTile.kt`, `HomeScreen.kt`) and the Results `MetricBlock` have no `maxLines`, so
they wrap and grow rather than clip; the rescue banner has three text buttons (§4.4).

| Key | Slot | EN | TR | Risk | Shorter alternative |
|---|---|---|---|---|---|
| `challenge_tile_title` | Home mode tile, ≥ 96dp wide, Fredoka title | Challenge (9) | Meydan okuma (12) | wraps to two lines at 200 % in a 108dp tile; tile height grows | none that keeps the listing's term; accept the wrap |
| `resume_classic_title` | Continue tile/chip title, ≥ 72dp | Continue Classic (16) | Klasik oyuna devam et (21) | two lines likely at 200 % | "Klasik'e devam" (14) |
| `resume_challenge_title` | Continue tile/chip title | Continue Challenge (18) | Meydan okumaya devam et (23) | two to three lines at 200 % | "Meydan okumaya devam" (20) |
| `results_label_pegs_left` | Results stat row, one of three columns at 360dp | PEGS LEFT (9) | KALAN PİYON (11) | wraps under 120dp at 200 %; the other two labels are shorter than English | "KALAN" (5) — only if the row breaks; the value beneath it is a peg count, so "KALAN" still reads |
| `action_replay` | Results action row of three | Replay (6) | Tekrar oyna (11) | row may wrap to a column at 200 % | "Tekrar" (6) |
| `action_next_level` + `action_retry` | Challenge Results action row | Next level (10), Retry (5) | Sonraki seviye (14), Yeniden dene (12) | same | "Sonraki" (7), "Tekrar" (6) |
| `results_new_best` | Results cue pill | New best! (9) | Yeni rekor! (11) | low | "Rekor!" (6) |
| `nowin_message` | rescue banner message over the board | 34 | 51 | second line at 100 %, third at 200 %; the banner is a column so it grows, but it covers more board | "Kazanan hamle yok — geri al ya da yeniden başlat" (48); last resort "Kazandıran hamle yok" (20) with the two buttons as the instruction |
| `stuck_message` | rescue banner | 31 | 44 | second line at 200 % | "Hamle kalmadı" (13) with the buttons as the instruction |
| `rescue_restart` | rescue banner text button, one of three | Restart (7) | Yeniden başlat (14) | three buttons "Geri al · Yeniden başlat · Ana menü" (7+14+8) against "Undo · Restart · Home" (4+7+4): wraps to a column at 200 % | "Baştan" (6) — "from the start", understood in a game |
| `keep_playing` | restart-confirm dialog button, beside "Yeniden başlat" | Keep Playing (12) | Oynamaya devam et (17) | M3 stacks the two buttons when they do not fit; acceptable | "Devam et" (8) |
| `restart_confirm_title` | dialog title | 19 | 33 | two lines; fine | "Yeniden başlatılsın mı?" (23) |
| `ad_offer_decline` | ad-offer dialog button beside "Reklam izle" | No thanks (9) | Hayır, teşekkürler (18) | stacks at 200 % | "Hayır" (5) |
| `play_games_prompt_decline` | beside "Oturum aç" | Not now (7) | Şimdi değil (11) | low | none needed |
| `profile_swap_keep` / `profile_swap_use_cloud` | account dialog buttons | 27 / 24 | 27 / 25 | same as English, which already stacks | none |
| `tutorial_step_*_caption` | caption under the tutorial board | 47–55 | 53–66 (sen is shorter than siz here) | one more line at 200 %; the caption column scrolls per ux-spec §4.1 | none; the sentences teach the rule |
| `settings_privacy_choices_label` | Settings row label | 22 | 27 | rows wrap | none |
| `ad_offer_*_title` | dialog title | 17 | 29–33 | two lines | none |

Not at risk, checked: the three ribbon pill labels ("Yıldız", "Tahta", "Seviye") are at most one
glyph longer than English and the pill wraps by rule; the status-strip title "Seviye 1234" is one
glyph wider than "Level 1234" with ≥ 25dp spare; "Klasik" and "Ayarlar" are shorter than English;
dialog bodies, snackbars, supporting lines and the licences screen are multi-line by design.

Face widths. *(The D-16 note of 2026-11-01 — Nunito headings ≈ 2–4 % wider than Fredoka, and
taller — is withdrawn with D-17: the headings are Fredoka in both languages, and the five
composites take their bases' advances and kerning, so every count above is read in the same face
as English and no slot moves.)*

## 7. Locale rules for the build and QA

### 7.1 Turkish casing — the dotted and dotless i

The classic Turkish-locale defect is `"sign in".toUpperCase()` on a Turkish device, which yields
"SİGN İN", and its mirror, `"İpucu".lowercase()` under the invariant locale, which yields
"i̇pucu" with a combining dot. Both are avoided by one rule, D-3: **no case transform on text the
player sees.** The copy carries its own case ("PUAN", "KALAN PİYON" are typed that way).

- Production sources had no `uppercase()`, `lowercase()`, `capitalize`, `replaceFirstChar`,
  `titlecase`, `toUpperCase` or `toLowerCase` on 2026-11-01 (helper grep of `app/src/main` and
  `domain/src/main`). The code-auditor rejects any that arrives with this branch. The ten hits in
  `app/src/test` compare identifiers and enum names, not displayed text; a test that touches the
  Turkish file's values uses `Locale.ROOT` explicitly or compares without casing.
- No `TextStyle` small-caps feature (`fontFeatureSettings = "smcp"`/`"c2sc"`) and no M2-style
  all-caps button: Material 3 buttons do not uppercase, and the theme must not reintroduce it.
- Sorting: no string list in the app is sorted by display text (the licences order is fixed by its
  spec), so no `Collator` is needed. If one is ever sorted, it uses `Collator.getInstance(locale)`,
  because "İ" and "ı" sort in Turkish positions, not ASCII ones.
- Comparisons in logic never use displayed text (CLAUDE.md's no-magic-strings rule already
  forbids it); a Turkish device must change no behaviour, only words.

### 7.2 Numbers and time

- Turkish uses Western Arabic digits, so `GameTimerFormat`'s `"%02d:%02d".format(minutes,
  seconds)` (default locale) renders the same clock; nothing changes here for this locale. (For a
  future locale with other digits the right fix would be `Locale.ROOT` in that one call; out of
  scope.)
- The Results score and the pegs-left value are `toString()` without grouping in both languages
  ("12345"); unchanged, not a localisation defect, noted in §9.
- Every number that reaches a resource goes through `stringResource(id, args)` /
  `pluralStringResource`, which format in the resource's locale; no string is assembled with `+`
  in production (helper grep). Keep it so.
- The clock's spoken form is minutes through `duration_minutes` (floored, D-05e), so TalkBack
  says "Geçen süre, 5 dakika" / "Geçen süre, bir dakikadan az".
- No decimal is shown anywhere; if one is ever shown it goes through `NumberFormat` (decimal
  comma in Turkish).

### 7.3 Fonts — glyph coverage, and the heading face in Turkish (D-13, D-16)

Turkish needs, beyond ASCII: Ç U+00C7, ç U+00E7, Ğ U+011E, ğ U+011F, İ U+0130, ı U+0131,
Ö U+00D6, ö U+00F6, Ş U+015E, ş U+015F, Ü U+00DC, ü U+00FC, and â U+00E2 (in "hâlâ").

**What the proof found** (android-game-dev report, 2026-11-01, open issue 1; this section was
rewritten on it — the first version assumed the bundled Fredoka was a subset and named the full
file as the remedy). The five bundled files are the unmodified Google Fonts static instances
(`tech-lead-fonts.md` D1). Read by cmap: Nunito Regular, SemiBold and Bold (v3.602) map all 13
code points and every character of §5. Fredoka Medium and SemiBold (v2.001) map Ç ç ı Ö ö Ü ü â
and **not Ğ ğ İ Ş ş** — and neither does any official Fredoka build: the Google Fonts v17 static
and variable files, the `google/fonts` repository file (whose METADATA.pb declares `latin-ext`)
and the designer's upstream. Fredoka's whole Latin Extended-A is ı Ł ł Œ œ Š š Ÿ Ž ž. There is
nothing to swap in. The font ruling's line "both faces carry … Turkish" (`tech-lead-fonts.md`,
Glyph coverage, read with `hb-shape`) is wrong for Fredoka; Android itemises text by cmap
coverage, so the cmap read is the one that predicts what a device draws.

**What it costs on screen without a fix.** Fredoka draws the `display*`, `headline*` and
`title*` roles (`ui/theme/Type.kt`): 42 Turkish keys. Five contain a missing letter and would show
it in the platform face — a different weight and width inside one word: `restart_confirm_title`
"Bu tahta yeniden başlatılsın mı?" (headlineSmall), `settings_haptics_label` "Titreşim",
`settings_colorblind_label` "Renk körlüğü paleti", `settings_play_games_achievements_label`
"Başarımlar" (all titleMedium), `licenses_annotations_title` "Ek açıklama ve bağımlılık ekleme
API'leri" (entry title and text-screen heading). `settings_play_games_signed_in_as` draws the
player's name in the same role, so "Ayşe" hits the gap too. The other 37 avoid the five letters by
chance; the next string, or the next player, would not.

**Decision D-17 (founder, Q-038 and Q-039, 2026-11-01): Fredoka with Turkish letters, in every
language.** The first ruling, D-16 — the heading roles in Nunito on a Turkish device, Fredoka kept
for the wordmark and the numerals — went to the founder as Q-038 and was rejected: "change the
font for all languages". The research-analyst then built a specimen, Fredoka Medium and SemiBold
with Ğ ğ İ Ş ş composed from the family's own G g I S s, breve, dot above and cedilla, and the
founder chose it (Q-039). The design:

- **One heading face in both languages; the look the founder approved, unchanged.**
  `AppTypography`, `AppFonts`, every role and every call site stay as they are. The gradient
  wordmark, the clock and the numerals were never at risk and stay Fredoka.
- **The five letters are Fredoka's own parts.** Ğ ğ are G g with the family's breve; İ is I with
  its dot above, on the row of Fredoka's own Ï dots and centred on the stem; Ş ş are S s with the
  cedilla Ç ç carry. Each takes its base's advance, left side bearing and every kerning pair, so
  "Başarımlar" is set as "Basarimlar" would be, and no English string moves by a unit — proven by
  a test (tech-design §11.1 (2)).
- **A Modified Version under OFL-1.1** (§1). No Reserved Font Name is declared, so the family
  keeps its name; the licence file ships unchanged; the modification is stated in the font's own
  name table and on the licences screen, in one line (`licenses_fredoka_modified`, §5.4;
  `docs/ux/licences-screen.md` §3.2). The `THIRD_PARTY.md` row is the research-analyst's and the
  release-manager's (§9.2).
- **Rebuilt byte-identically from a committed recipe** (`fonts/fredoka-tr/`), the audio-recipe
  rule applied to a font. The mechanism, the tests and the developer's contract are tech-design
  §11.1 "Q-039" and are not repeated here.

What the ux-designer holds the five glyphs to — the specimen the founder saw is the reference;
QA reads it as C-2b on the device and screenshots each letter at both scales for this role's
read:

1. **Each mark reads as Fredoka's.** The cedilla of Ş ş has the stroke, the depth and the join of
   Ç ç beside it ("Çözüldü!", "Geçen süre"); the dot of İ is the dot of ö and ü ("körlüğü"), at
   their height; the breve of Ğ ğ is the family's monoline stroke, clear of the base by the gap
   Fredoka leaves between Ç and its cedilla, and never touches the G's bar or the g's bowl.
2. **One face inside a word.** In "Bu tahta yeniden başlatılsın mı?", "Titreşim", "Renk körlüğü
   paleti", "Başarımlar" and "Ek açıklama ve bağımlılık ekleme API'leri" every letter shares one
   stroke weight and one baseline at 100 % and 200 %, light and dark; no mark is clipped by a
   line box at 200 %.
3. **Spacing as the base's.** The gap on either side of each composite equals its base letter's:
   "Ba-ş-a" is spaced as "Ba-s-a".
4. **English screens are pixel-identical** to the pre-branch screenshots (C-2c).

What was weighed before the founder answered (kept as the record; Q-038 and Q-039 settle it):

- **Rewording the five strings around the font.** "Başarımlar" is Google's own term and
  "Titreşim" the plain word; Turkish without ş, ğ and İ is not Turkish, and the next string or a
  player's name hits the gap again. The copy is not constrained by a font. Rejected.
- **Nunito headings on a Turkish device, Fredoka kept for the wordmark and the numerals (D-16).**
  No file, no licence row; one consistent app per language, but plainer Turkish headings than
  the approved English ones. Put to the founder as Q-038 and **rejected**: one font for all
  languages.
- **A different rounded display face for every language.** Baloo 2, Quicksand, Comfortaa,
  Varela Round and Mitr are OFL faces on Google Fonts in Fredoka's register; each would need its
  coverage proven by cmap (Fredoka's own metadata claims `latin-ext`), a licence verdict, new
  files, new T2 oracles and a full visual re-walk, and it changes the approved look. Not chosen:
  the specimen of Q-039 keeps Fredoka itself.
- **A per-glyph fallback chain Fredoka → Nunito** (`Typeface.CustomFallbackBuilder`). It puts a
  Nunito ş inside a Fredoka word, which is the defect C-2 exists to catch, and it is API 29+
  against minSdk 24. Rejected.
- **A Modified Version of Fredoka with the five letters built from its own parts.** OFL-1.1
  permits it (no Reserved Font Name); the only path that keeps Fredoka headings in Turkish.
  **Chosen (Q-039)** — the research-analyst's specimen showed it is composition, not drawing:
  Fredoka already carries the breve, the dot above and the cedilla as marks. An upstream report
  to `google/fonts` (issue #2411, open since 2020) stays with the research-analyst.
- **Shipping the fallback letters.** A visible defect in Settings on every Turkish phone, in the
  founder's own language, on the row whose supporting line explains the feature. Rejected.

What the build must honour is tech-design §11.1 "Q-039", the developer's contract. From this
spec, in addition: (1) no change to any role, call site or theme — the look is the proof;
(2) the two strings of §5.4: `licenses_fredoka_modified` in both languages, and
`licenses_psl_parts` with the prose date in English; (3) the coverage test's derived shape with
no allowance list (§7.6), so the next locale and the next "deliberate" special character are red
tests, never device findings; (4) a later locale is still proven by cmap before it is specified —
a locale Fredoka cannot be given by composition is a font decision, not a fallback.

What QA checks on the device: §8 C-2 (every Turkish letter in its surrounding face), C-2a (the
heading face is Nunito), C-2b (the wordmark and the numbers kept Fredoka), C-2c (English
unchanged), C-2d (a player name with ş, ğ or İ), and the taller-ink clause of C-7. The pangram
"Pijamalı hasta yağız şoföre çabucak güvendi" is a useful probe wherever QA can type: every
letter in a word shares the word's stroke weight and baseline.

### 7.4 Resource layout and per-app language

- The folder is `app/src/main/res/values-tr/` (language only, not `values-tr-rTR`): it serves
  every Turkish-language locale (tr-TR, tr-CY). UTF-8, same key order and section comments as the
  base so the two files diff side by side; no `translatable` attribute in the Turkish file
  (it is a base-file attribute).
- `AndroidManifest.xml` gains `android:localeConfig="@xml/locales_config"` on `<application>`,
  and `res/xml/locales_config.xml` lists `en` and `tr` (or AGP's `generateLocaleConfig = true`
  under `androidResources` with `resources.properties` naming `unqualifiedResLocale=en-US`;
  the developer picks one and the tech-lead signs it off). Android 13+ then lists Peg Solitaire
  under Settings › System › Languages › App languages with English and Türkçe, and that is the
  only language picker in 3.0.0 (D-9): two languages do not earn a Settings screen, the OS
  already offers the choice where users look for it, and an in-app picker would need AppCompat's
  `setApplicationLocales` on an app that runs on `ComponentActivity`. Android 12 and below
  follow the system language.
- `androidResources { localeFilters += listOf("en", "tr") }` (AGP 9; the older
  `resourceConfigurations`) so the bundle carries only the two languages the app speaks: a
  device in a third language then sees English throughout, Google's library strings included,
  instead of English screens with German dialogs. Build config; tech-lead's sign-off.
- A locale change recreates the Activity. Mid-game state (board, move history, clock, the
  Challenge level) already survives recreation through the ViewModel and the resume snapshot;
  §8 C-4 proves it in Turkish.
- Lint: with `values-tr` present, `MissingTranslation` is an error for any translatable key
  without a Turkish value, `ExtraTranslation` for a Turkish key the base lacks, `MissingQuantity`
  for a plural without `one` and `other`; the Check already runs lint with warnings as errors.
  Lint's typo dictionary for Turkish (`typos-tr`) runs on the new file; a flag on a word in §5
  is reported, not silenced.
- `supportsRtl` and the pseudo-locale tests (`ar-XB`) are untouched; Turkish is left-to-right.

### 7.5 TalkBack

- Every content description and announcement is translated (§5.2 whole). On a Turkish device
  TalkBack's own words ("iki kez dokunun", "düğme", "anahtar", "açık/kapalı") come from the
  system, so a screen reads as one language.
- The three tap-action labels are infinitives (D-10) so the hint template reads "seçmek için iki
  kez dokunun". QA listens to one of each (A-7); if TalkBack's Turkish template turns out to be
  "iki kez dokunarak <label>", the labels switch to the bare imperative ("seç", "bırak", "buraya
  atla") — a three-string change the developer records.
- `stat_value_unknown_desc` ("henüz yok") replaces the em dash in speech, as in English.
- The em dash in the banner and snackbars is read as a pause by Google's Turkish TTS, which is
  the intent; no change.
- The Play Games stock screens (sign-in sheet, leaderboards, achievements) and the UMP consent
  form are Google's and arrive in Turkish on their own; their wording is not ours to check.

### 7.6 Tests the branch carries

- **A JVM parity test** (name reads as a sentence, e.g. "every translatable string has a Turkish
  value with the same format arguments"): parses `values/strings.xml` and
  `values-tr/strings.xml`; asserts the key sets are equal once `translatable="false"` keys are
  removed; for every key, the multiset of `%n$x` specifiers (index and conversion) is equal; every
  plural has `one` and `other` in Turkish; and no Turkish value equals its English value except
  the five verbatim keys of §5.6 — so an untranslated leftover fails the build, not QA.
- **The existing `UserFacingTextHasNoProtectedNamesTest`** is extended to scan `values-tr` with
  the same rules.
- **Compose UI tests in Turkish**: the per-screen main-flow tests run once more under a `tr`
  configuration (`@Config(qualifiers = "tr")` on Robolectric) for Home, the Results screen and
  Settings — asserting by resource id, not by English text.
- **The glyph-coverage test** (D-13, D-17): `BundledFontGlyphCoverageTest` in its derived
  shape — every shipped locale × every role's resolved font file, every character of that
  locale's strings plus the base file's untranslated keys, the 13 code points of §7.3 as a floor
  for `tr`, **no allowance list** (tech-design §11.1 (2)). Red on the upstream Fredoka for
  Ğ ğ İ Ş ş and on U+2011 until `licenses_psl_parts` takes the prose date; green on the rebuilt
  files. The next locale and the next special character are red tests, not device findings.
- **The recipe test** (D-17): `BundledFontRecipeTest` — the shipped Fredoka files' sha256 equal
  the recipe's outputs and the committed upstream files' equal its inputs; the displayable set
  differs by exactly the five; every upstream code point's advance and every English string's
  kerned advance are equal in both — the test that keeps C-2c true by construction (tech-design
  §11.1 (2)).
- **Roborazzi goldens** in `tr` at 360×640dp, font scale 1.0 and 2.0, for the eight slots of §6:
  Home (tiles, chips, ribbon), Results in the Perfect, Solved and Out-of-moves states, the
  restart-confirm dialog, the ad-offer dialog, the rescue banner, the Undecided notice. QA reviews
  the goldens against the device. *(2026-11-01: the developer reports that `docs/tech-design.md`
  §8.5 keeps stored goldens out of the repo until a QA-owned golden suite exists, and built none;
  the tech-lead decides, §9.2. If they stay out, the §6 decisions rest on QA's device
  screenshots, C-7.)*
- **Lint** as in §7.4; the Check (`./gradlew check`) stays the gate.

### 7.7 What is English on a Turkish device and is not a defect

Proper names (§4); the licence bodies and Google's notices, shown as supplied; the board names;
"Play Games"; the sound titles "Interface Sounds", "Pop 01", "Positive Blip Effect"; the
privacy policy page itself (a web page outside the app). Everything else on screen is Turkish.

## 8. Acceptance criteria

Device: the emulator or phone QA uses for the release, system language Türkçe (Türkiye), a
360×640dp window class, light and dark theme where a criterion names it, font scale 100 % and
200 % (Settings › Display › Font size, largest). A build with §5 and §7 applied. "No English" means
no English word on screen other than §7.7's list. "Readable" means every string is whole: no
ellipsis, no glyph cut by a bound, no text drawn over another, no button pushed off screen.

### Cross-cutting

- **C-1 Lint and parity.** Given the branch, when `./gradlew check` runs, then it is green, the
  lint report has zero MissingTranslation, ExtraTranslation and MissingQuantity, and the parity
  test of §7.6 passes.
- **C-2 Fonts — every Turkish letter in its surrounding face.** Given a Turkish device, when the
  tutorial, Home, gameplay, Results, Settings and Licences are opened, then every Turkish letter
  in "İpucu", "Mükemmel!", "Çözüldü!", "Yazı tipleri", "Gizlilik", "KALAN PİYON", "Geçen süre"
  and in the five D-16 words — "Bu tahta yeniden başlatılsın mı?", "Titreşim", "Renk körlüğü
  paleti", "Başarımlar", "Ek açıklama ve bağımlılık ekleme API'leri" — renders in the surrounding
  face (same stroke weight, same baseline): no fallback glyph, no tofu, at 100 % and 200 %.
- **C-2a The heading face is Fredoka in Turkish (D-17).** Given a Turkish device, then the
  heading-role text is Fredoka, by the font ruling's own discriminator: with the Challenge
  ladder seeded to level 111, the "111" of the gameplay title "Seviye 111" (titleLarge 22sp)
  measures ≈ 25.6dp of advance (3 × 0.388 em, Fredoka Medium; a Nunito fallback would read
  ≈ 39.6dp), the Home Level pill "111" ≈ 17.5dp of ink, and the Settings row labels show the
  same letterforms as the English build's.
- **C-2b The five letters are Fredoka's (D-17).** Given a Turkish device at 100 % and 200 %,
  light and dark, then in "Bu tahta yeniden başlatılsın mı?" (restart dialog), "Titreşim",
  "Renk körlüğü paleti", "Başarımlar" (Settings) and "Ek açıklama ve bağımlılık ekleme
  API'leri" (Licences) each Ğ ğ İ Ş ş is one Fredoka glyph: the cedilla of ş has the stroke,
  depth and join of ç in "Çözüldü!" and "Geçen süre"; the dot of İ is the dot of ö and ü in
  "körlüğü", at their height; the breve of ğ is clear of the g and touches nothing; no mark is
  clipped by a line box at 200 %; the spacing on either side of each letter is its base's
  ("Ba-ş-a" as "Ba-s-a"). QA screenshots each of the five words at both scales for the
  ux-designer's read against the Q-039 specimen (§7.3).
- **C-2c English unchanged (D-17).** Given an English device on the same build, then Home,
  gameplay at level 111, Results, Settings and the Licences list are pixel-identical to the
  screenshots taken before this branch at the same window, scale and theme — the
  `tech-lead-fonts.md` readings hold exactly: "Level 111" ≈ 82.1dp, the Level pill "111"
  ≈ 17.5dp, "Peg Solitaire" ≈ 205.7dp. The recipe promises no advance moved; one that did is a
  FAIL.
- **C-2d A player name (D-17).** Given a Play Games account whose display name carries ş, ğ or İ
  (an "Ayşe"), when Settings is opened on a Turkish *or* an English device, then "<name> olarak
  oturum açıldı" / "Signed in as <name>" renders whole in Fredoka — the letters are now in the
  face in both languages. If no such account is available, QA records the criterion as not
  exercised, not as a fail.
- **C-2e The licences line (D-17).** Given Settings › Hakkında › Lisanslar, then the Fredoka
  entry shows three lines — "© 2016 The Fredoka Project Authors", "SIL Open Font License 1.1",
  and "Peg Solitaire için SIL Open Font License kapsamında değiştirildi: Ğ ğ İ Ş ş harfleri
  Fredoka'nın kendi harf ve işaretlerinden eklendi; başka hiçbir şey değiştirilmedi." — whole at
  100 % and 200 %, and the English build shows "Modified for Peg Solitaire under the SIL Open
  Font License: Ğ ğ İ Ş ş added from Fredoka's own letters and marks; nothing else changed."; the
  Public Suffix List entry's parts line reads "… as it stood on 15 February 2023" in English and
  "… 15 Şubat 2023 tarihindeki hâliyle" in Turkish, the date never split across lines at 200 %.
- **C-3 Casing.** Given a Turkish device, when every screen is walked, then "İpucu" and
  "KALAN PİYON" show the dotted capital İ, "Yıldız" shows the dotless ı, and no string shows "I"
  for ı or "İ" for I in either language ("SİGN İN"-style artefacts: zero).
- **C-4 Language switch mid-game.** Given a Classic game in progress with three jumps made and
  the clock at 00:40 in English, when the system language is switched to Türkçe and the app is
  returned to, then the same board, the same three-move history (Undo available) and a clock
  ≥ 00:40 are shown, and every string is Turkish; and the same in reverse.
- **C-5 Per-app language.** Given Android 13 or newer with the system in English, when Settings ›
  System › Languages › App languages › Peg Solitaire is opened, then Türkçe and English are
  listed; when Türkçe is chosen, then the app is Turkish while the system stays English. Given
  Android 12 or older, then no such row exists and the app follows the system language.
- **C-6 Fallback.** Given a device in German, when the app is opened, then it is English
  throughout, including the Play Games and consent dialogs if `localeFilters` is applied (§7.4),
  and no screen mixes a third language with English.
- **C-7 Font scale.** Given 200 %, when every screen of this section is walked, then every
  string is readable per the definition above, and a slot that wrapped rather than clipped is
  recorded with a screenshot for the §6 decision. *(The D-16 taller-ink clause is withdrawn with
  D-17: the headings are Fredoka in both languages.)*

### Tutorial (ONB-01)

- **T-1.** Given a first run in Turkish, when the tutorial opens, then the step 1 caption reads
  "Bir piyona dokun, sonra iki kare ötedeki boş deliğe dokun." in full at 100 % and 200 %,
  and "Eğitimi atla" is a visible, whole action.
- **T-2.** When the three steps are completed, then the captions of steps 2 and 3 and the done
  caption "Oyun bundan ibaret. Hadi, bir tahta temizle." are shown whole, and the CTA reads
  "Anladım".
- **T-3 (TalkBack).** Given TalkBack on, when the step 2 board gains focus, then "Adım 2, toplam 3."
  is spoken before the caption.

### Home

- **H-1.** Given Home, then the tiles read "Klasik" / "5 tahta" and "Meydan okuma" with its chip
  "Devam et — Seviye N"; the ribbon pills read "Yıldız", "Tahta", "Seviye" with their values; the
  gear's description is "Ayarlar". At 200 % the tile titles wrap and the tiles grow; nothing clips.
- **H-2.** Given a saved Classic and a saved Challenge game, then both Continue tiles show
  "Klasik oyuna devam et" / "Kaydettiğin tahtadan devam et" and "Meydan okumaya devam et" /
  "N. seviyeye kaldığın yerden devam et", whole, at 100 % and 200 % (a wrap is allowed; a
  clip is not).
- **H-3.** Given a pill with no value yet, when TalkBack focuses it, then "Yıldız, henüz yok" is
  spoken, never the dash.

### Classic catalog

- **K-1.** Given the catalog, then its title is "Klasik", each card keeps its English board name
  and shows "33 delik" style subtitles, and a card's star row is spoken as "3 yıldızın 1 tanesi".

### Gameplay (Classic and Challenge)

- **G-1.** Given a Challenge level, then the title reads "Seviye N" un-truncated up to N = 1234,
  the back control is spoken as "Geri dön", the clock as "Geçen süre, 5 dakika" (after five
  minutes) and "Geçen süre, bir dakikadan az" before one.
- **G-2.** Given the dock, then Undo, Hint and the moves switch are spoken as "Geri al",
  "İpucu", "Olası hamleleri göster" (the switch's name unchanged in both states), and the hint
  wait is announced as "Hamle aranıyor".
- **G-3 Rescue banner.** Given a dead position, when the banner appears, then it reads
  "Kazandıran hamle yok — geri al ya da yeniden başlat" with the three actions "Geri al",
  "Yeniden başlat", "Ana menü" all tappable (≥ 48dp) at 100 % and 200 %; a stuck board shows
  "Hamle kalmadı — …". If the three actions stack at 200 %, the screenshot goes to the §6
  decision.
- **G-4 Undecided notice.** Given a hint that times out, then the notice reads exactly "İpucu bu
  konumu süresi içinde çözemedi. Belki hâlâ kazanılabilir." with the circumflexes, and TalkBack
  reads it once, politely.
- **G-5 Restart dialog.** Given a mid-game restart tap, then the dialog reads "Bu tahta yeniden
  başlatılsın mı?" / "Mevcut ilerlemen kaybolacak." with "Oynamaya devam et" and "Yeniden
  başlat"; at 200 % the buttons may stack but are whole.
- **G-6 Game-over dialog.** Given a perfect clear, then "Mükemmel!" / "Tahtayı tek piyona kadar
  temizledin." with "Tekrar oyna" and "Tahtayı gör"; given two pegs left, "Az kaldı!" with
  "2 piyon kaldı. Tekrar denemek ister misin?"; given no moves, "Hamle kalmadı".
- **G-7 Loading.** Given a Challenge level generating, then "Seviye yükleniyor" is shown; given
  a load failure, "Bu seviye yüklenemedi. Yeniden dene düğmesine dokun." with "Yeniden dene".

### The board with TalkBack

- **A-1.** Given TalkBack on a fresh English-33 board, when the board gains focus, then "Tahta,
  32 piyon kaldı" is spoken.
- **A-2.** When a peg with two legal jumps is focused, then "Satır 4, sütun 2, piyon, 2 atlama"
  and the hint "seçmek için iki kez dokunun" (or the template of §7.5) are spoken; when an empty
  cell is focused after a selection, "Satır 4, sütun 4, boş, atlama hedefi" and "buraya atlamak
  için iki kez dokunun".
- **A-3.** When a peg is selected, then "Satır 4, sütun 2 seçildi, 2 atlama." is announced;
  when the jump is made, "Satır 4, sütun 2 konumundan satır 4, sütun 4 konumuna. Satır 4, sütun 3
  kaldırıldı. 31 piyon kaldı."
- **A-4.** When the actions menu of a selected peg is opened, then each legal jump reads "Satır 4,
  sütun 4 konumuna sağa atla" style, one per direction, with "yukarı", "aşağı", "sola", "sağa".
- **A-5.** When an empty cell is tapped with no selection, then "Burada piyon yok."; a non-target
  with a selection, "Atlama hedefi değil."; a second peg with a selection, "Önce seçili piyonu
  bırak."
- **A-6.** When Undo, Hint and Restart are used, then "… atlama geri alındı. 32 piyon kaldı.",
  "İpucu: satır 4, sütun 2; satır 4, sütun 4 konumuna sağa atla." and "Tahta yeniden başlatıldı.
  32 piyon kaldı." are announced once each.
- **A-7.** QA records the exact spoken hint sentence for one tap action; if it is not a sentence
  (§7.5), the finding names the template heard.

### Results

- **R-1.** Given each of the three outcomes, then the headline is "Mükemmel!", "Çözüldü!" or
  "Hamle kalmadı"; the stat labels read "PUAN", "SÜRE", "KALAN PİYON" over tabular values, whole
  at 100 % and 200 %; the actions read "Tekrar oyna", "Tahtalar", "Ana menü" (Classic) or
  "Sonraki seviye", "Yeniden dene", "Ana menü" (Challenge), each ≥ 48dp and whole.
- **R-2.** Given an improved best, then "Yeni rekor!" is shown whole; given the interstitial
  wait, TalkBack says "Hazırlanıyor" for the scrim.
- **R-3 (TalkBack).** When Results opens, then the summary is spoken once as "Çözüldü! — 3
  yıldızın 2 tanesi, 3 piyon kaldı." in that order.

### Settings, ads, Play Games, account

- **S-1.** Given Settings, then the sections read "Genel", "Play Games", "Gizlilik", "Hakkında";
  the rows "Ses", "Titreşim", "Renk körlüğü paleti", "Nasıl oynanır", "Gizlilik politikası",
  "Gizlilik tercihlerini yönet", "Lisanslar" with their supporting lines from §5.3–5.4, all
  whole at 200 %.
- **S-2.** Given no browser, when the policy row is tapped, then the snackbar reads "Gizlilik
  politikası açılamadı — tarayıcı uygulaması bulunamadı."
- **S-3 Ads.** Given the free hints spent, when Hint is tapped, then the dialog reads "Ücretsiz
  ipucu hakkın bitti" / "Bu seviyedeki N ücretsiz ipucu hakkını kullandın. N ipucu daha için
  kısa bir reklam izlemek ister misin?" with "Reklam izle" and "Hayır, teşekkürler"; the undo
  twin likewise; given no ad, the snackbar "Reklam gösterilemedi — ipuçları ücretsiz yenilendi"
  and the hint lands first.
- **S-4 Play Games.** Given the first-run prompt, then "Play Games'te oturum açılsın mı?" with the
  body of §5.4, "Oturum aç" and "Şimdi değil"; given a signed-in player, the Settings section shows
  "<name> olarak oturum açıldı", "Skor tabloları", "Başarımlar"; given an SDK error, "Play
  Games'te oturum açılamadı". The Play Games sheets themselves are Google's Turkish (not judged).
- **S-5 Account change.** Given a second account, then the dialog reads "Farklı bir hesaba mı
  geçiyorsun?" with the body naming all four wiped categories, and the two buttons whole (they
  may stack, as in English).
- **S-6 Cloud save line.** Given a solve on a Turkish device, when the Play Games saved-games
  list is opened, then the entry reads "Meydan okuma seviyesi N · S yıldız · B/5 Klasik tahta".

### Licences

- **L-1.** Given Settings › Hakkında › Lisanslar, then the intro, the four section headers,
  every translated entry title and part line of §5.4, "Lisansı oku", "Bildirimleri aç" are
  Turkish; licence names, credits, addresses and licence texts are as supplied; "Metnin sonu"
  closes a text; the notices list shows "Bildirim 1 / 24"-style ordinals and "%s içinde bulundu".
- **L-2.** Given the pseudo-locale RTL test already in the suite, then it still passes: Turkish
  adds no RTL change.

## 9. Open items, founder questions, the parallel branch

### 9.1 Founder answers (2026-11-01; ids assigned by the producer)

- **Q-031 — Register: siz or sen?** Asked as the ux-designer's Q-A (recommendation: siz in
  sentences, bare imperative on buttons; options 2 sen throughout with the listing re-edited,
  3 sen in the tutorial and game-over copy only). **Answer: 2 — sen throughout, the tr-TR listing
  re-edited to match** (founder, 2026-11-01). Applied: D-1, §2.1, the 22 keys of §11's change-log
  row, the §8 quotes, and the listing in §10.
- **Q-032 — "Solo Test" in the Turkish listing?** Asked as Q-B (recommendation: one sentence
  after a trademark verdict). **Answer: No** (founder, 2026-11-01). Applied: D-15, §4; the
  research-analyst check is dropped and the listing does not mention the name.
- **Q-038 — The Turkish headings are Nunito, not Fredoka (D-16).** Asked by the ux-designer
  (recommendation: 1, Nunito headings in Turkish; 2, one new rounded face for every language;
  3, keep Fredoka and accept the fallback letters). **Answer: "change the font for all
  languages"** (founder, 2026-11-01) — D-16 rejected; nothing of it had been built.
- **Q-039 — Fredoka with Turkish composites, in every language.** Put by the producer on the
  research-analyst's specimen (Fredoka Medium and SemiBold with Ğ ğ İ Ş ş built from the
  family's own letters and marks). **Answer: yes — Fredoka + Turkish composites in every
  language, English unchanged** (founder, 2026-11-01). Applied: D-17, §7.3, §7.6, §8 C-2a–C-2e,
  the two strings of §5.4; the mechanism is tech-design §11.1 "Q-039".

### 9.2 Items for other roles, through the producer

- **release-manager**: (a) replace `docs/store/tr-TR/full_description.txt` and
  `short_description.txt` with the sen text of §10 (Q-031), which already omits "Oyunun arayüzü
  İngilizcedir." (D-14) and the English glosses; (b) the tr-TR 3.0.0 changelog: §10.3 gives a
  sen version with the Turkish-interface line, and `en-US/changelogs/30000.txt` gains "Turkish
  interface"; (c) update `docs/store/README.md` lines 25–26, which say the app has no Turkish
  strings; (d) note, not a change: both listings call the third board "Asymmetric-39" while the
  app calls it "Asymmetric 3-3-2-2" in both languages — pre-existing, outside this task;
  (e) at the cut, checklist 4.6a: the AAB's two Fredoka files hash to the recipe's outputs
  (tech-design §11.1 (3)); the `THIRD_PARTY.md` Fredoka row as a Modified Version, with the
  research-analyst; no store screenshot changes (the headings are Fredoka in both languages).
- **research-analyst**: the Modified-Version verdict line ("CLEAN as a Modified Version") and
  the `THIRD_PARTY.md` §2 row and §8 build-tool lines (tech-design §11.1 (3)); the "Solo Test"
  check stays dropped (Q-032); the `google/fonts` issue (#2411) stays its own.
- **tech-lead**: signed off `localeConfig` and `localeFilters` (§7.4) and ruled the goldens out
  for 3.0.0 (`turkish-locale/tech-lead-rulings.md`, tech-design §8.5, §11.2); D-17's contract is
  tech-design §11.1 "Q-039", and the sign-off after the review loop is a fresh dispatch.
- **android-game-dev**: the Q-039 contract; from this spec the two strings — the
  `licenses_fredoka_modified` row of §5.4 in `values` (translatable) and `values-tr`, and the
  prose date in `licenses_psl_parts` (its base-file comment about U+2011 goes with it) — and the
  catalogue line with `LicenseCatalogTest.EXPECTED`.
- **game-designer**: none; no rule or wording of the game changes.

### 9.3 The parallel branch

`feature/home-continue-live-boards` withdraws Home's stage art and changes the Classic Continue
chip's thumbnail; it may add or remove a few strings. This spec translates develop 37d3052. When
that branch lands, the developer reconciles the delta against §5: a removed key leaves
`values-tr` too (lint's ExtraTranslation enforces it); a new key is translated in the glossary's
terms and §2's register and sent to the ux-designer in review mode for a one-line ruling before
the merge, with the §5 row added to this spec in the same commit. A string that only moved
keeps its Turkish.

### 9.4 Noted, not changed

- The Results score is shown ungrouped ("12345") in both languages; Turkish would group with a
  full stop. Both languages behave the same, so it is not a localisation defect; a future
  `NumberFormat` pass would change both.
- `GameTimerFormat` formats the clock in the default locale; harmless for en and tr (§7.2).
- The TalkBack hint template for custom click labels is assumed to be "<label> için iki kez
  dokunun" (§7.5); A-7 verifies it, and the fallback is three strings.

## 10. Store copy re-voiced to sen (Q-031)

Paste-ready text for the release-manager; this spec does not edit `docs/store/`. Terms are the
glossary's; the only changes from the files as they stood on 2026-11-01 are the register (siz →
sen), the removal of "Oyunun arayüzü İngilizcedir." (D-14), and the removal of the English
glosses "(Classic)", "(Challenge)", "(Settings)", "(Hint)", "(Undo)", which were there to map a
Turkish listing onto an English UI and now would map a Turkish word onto itself. Board names,
the privacy URL and every number are unchanged. Nothing about "Solo Test" (Q-032).

### 10.1 `docs/store/tr-TR/short_description.txt` (67 characters; the limit is 80)

```
Piyonları atlat, tahtayı temizle: 5 klasik tahta ve 10.000 seviye.
```

### 10.2 `docs/store/tr-TR/full_description.txt`

```
Peg Solitaire, tek kişilik klasik bir masa oyunudur. Bir piyonu, yanındaki piyonun üzerinden hemen arkasındaki boş deliğe atlat; üzerinden atlanan piyon tahtadan kalkar. Atlamalar yukarı, aşağı, sağa ve sola yapılır, asla çapraz yapılmaz. Tahtada olabildiğince az piyon bırak, en iyisi tek bir piyon.

İki oyun modu

Klasik: bilinen beş tahta, English-33, European-37, Asymmetric-39, Diamond-41 ve Wiegleb-45. Her tahtanın yıldız hedefleri, o tahtada gerçekten ulaşılabilen sonuca göre belirlenir.

Meydan okuma: yükseldikçe zorlaşan, 10.000 üretilmiş seviyelik bir merdiven. Her seviye çözülmüş bir tahtadan geriye doğru kurulur, bu yüzden her seviyenin bir çözümü vardır.

Oynayarak öğren

Kısa bir eğitim, kuralları gerçek bir tahta üzerinde, her adımda tek bir atlamayla öğretir. Yanlış yere dokunmanın hiçbir bedeli yoktur. Eğitimi istediğin an geçebilir, daha sonra Ayarlar'dan yeniden oynatabilirsin.

İstediğinde yardım

İpucu, kazandıran gerçek bir hamle gösterir. Geri al, son atlamanı geri alır. Her seviyede ikisi için de ücretsiz hakların vardır. Hakların bitince, bir hak daha için reklam izlemeyi seçebilir ya da izlemeden devam edebilirsin.

Yıldızlar ve rekorlar

Her seviyeyi bir ile üç yıldız arasında bitir, kendi en iyi puanını ve süreni geç.

Google Play Games, istersen

Oturum açarak 20 başarımın kilidini aç, 7 skor tablosunda yarış ve ilerlemeni bulutta sakla; böylece ilerlemen yeni cihazına da taşınır. Oturum açmak isteğe bağlıdır: her mod ve her tahta oturum açmadan da çalışır.

Rahat oynaman için

- Her hamle için bir ses, medya ses düzeyinde. Müziğini asla durdurmaz; Ayarlar'dan kapatabilirsin.
- Sistem ayarına uyan açık ve koyu tema.
- Ayarlar'da renk körlüğüne uygun bir renk paleti seçeneği; hamleler, ipuçları ve yıldızlar daha net görünür.
- Parlak yeni bir görünüm; menünün ve tahtanın arkasında yavaşça süzülen yumuşak ışıklar.
- Geri al, İpucu ve diğer oyun düğmeleri ekranın alt yarısında, başparmağının ulaşabileceği yerde.

Reklamlar hakkında

Peg Solitaire reklam gösterir. Ödüllü reklam yalnızca sen seçtiğinde oynatılır ve ekrandayken oyun saati durur. Tam ekran bir reklam, bir seviyeyi bitirip sonuç ekranından devam ettiğinde çıkabilir; bir tahtayı oynarken asla çıkmaz.

Gizlilik politikası: https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html
```

### 10.3 `docs/store/tr-TR/changelogs/30000.txt` — optional, same voice

The changelog also speaks siz today ("başparmağınızın", "müziğinizi", "siz seçtiğinizde") and
D-14 asks it to name the Turkish interface. Offered in sen so the three files agree; the
release-manager decides and counts the Play limit of 500 characters (this is about 490).

```
Peg Solitaire sıfırdan yeniden yapıldı:
- Türkçe arayüz
- Uygulamalı bir eğitim, daha dolu bir ana ekran ve başparmağının ulaştığı oyun düğmeleri
- Büyük tahtalarda, yerleşik bir açılış kitabıyla daha hızlı ipuçları
- Her hamle için ses, müziğini durdurmadan
- İsteğe bağlı Play Games oturumu: 20 başarım, 7 skor tablosu ve bulut kaydı
- Parlak yeni bir görünüm ve renk körlüğüne uygun palet seçeneği
- Reklamlar: ödüllü reklam yalnızca sen seçtiğinde, seviyeler arasında ara sıra reklam
```

## 11. Change log

| Date | Change | By |
|---|---|---|
| 2026-11-01 | First complete version: decisions D-1..D-14, register, glossary, proper names, the full table for develop 37d3052 (§5.1–5.6), truncation watchlist, build and QA rules, acceptance criteria, two proposed founder questions. | ux-designer |
| 2026-11-01 | Founder answers applied. Q-031 (sen throughout): D-1 and §2.1 rewritten; 22 keys re-voiced — `game_over_perfect_body`, `restart_confirm_body`, `pegs_remaining_retry` (both items), `board_a11y_rejected_put_down_first`, `challenge_load_error`, `resume_classic_supporting`, `resume_challenge_supporting`, `tutorial_step_1_caption`, `tutorial_step_3_caption`, `tutorial_done_caption`, `ad_offer_hint_title`, `ad_offer_undo_title`, `ad_offer_hint_body`, `ad_offer_undo_body`, `settings_privacy_policy_supporting`, `settings_privacy_choices_supporting`, `play_games_prompt_body`, `settings_play_games_leaderboards_supporting`, `settings_play_games_achievements_supporting`, `profile_swap_title`, `profile_swap_body`, `profile_swap_body_unknown_account`; keys, format arguments and §6 flags unchanged (the caption lengths fell to 53–66); §8 quotes updated; §10 added with the sen listing. Q-032 (no "Solo Test"): D-15, §4, §9.2. | ux-designer |
| 2026-11-01 | Fredoka ruling (developer escalation 1: no official Fredoka build maps Ğ ğ İ Ş ş). D-13 amended; **D-16** added: the heading face follows the language — Nunito headings on a Turkish device (Medium → SemiBold, SemiBold → Bold), Fredoka kept everywhere for the wordmark and the numeral surfaces, English untouched. §7.3 rewritten (the finding, the decision table, the options weighed, the build requirements); §6 face-width note; §7.6 glyph-coverage and typography tests; §8 C-2 rewritten and C-2a–C-2d added, C-7 taller-ink clause; §9.1 Q-038 drafted (non-blocking); §9.2 items for the release-manager, research-analyst and tech-lead. Mirrored in `docs/ux-spec.md` §6.3 and §9, and `docs/ui/DESIGN-SYSTEM.md` §Typography. | ux-designer |
| 2026-11-01 | Founder answers Q-038 (D-16 rejected: one font for all languages) and Q-039 (Fredoka + Turkish composites, on the research-analyst's specimen). **D-17** added, D-16 marked superseded, D-13 pointed at D-17; §7.3's decision rewritten (the five letters as Fredoka's own parts, the Modified Version under OFL-1.1, the recipe; the four things the ux-designer holds the glyphs to; the options kept as the record; the build pointer to tech-design §11.1 "Q-039"); §6 face-width note withdrawn; §7.6 the derived coverage test and the recipe test; §8 C-2a–C-2d rewritten for Fredoka in both languages, **C-2e** (the licences line and the PSL date) added, C-7's taller-ink clause withdrawn; §5.4 new row `licenses_fredoka_modified` (en + tr; translatable) and `licenses_psl_parts` English to the prose date "15 February 2023" (tech-design §16 #41, confirmed); §5.6 count; §9.1 both answers; §9.2 items. Mirrored in `docs/ux-spec.md` §6.3 and §9, `docs/ui/DESIGN-SYSTEM.md` §Typography and `docs/ux/licences-screen.md` §3.2. | ux-designer |
