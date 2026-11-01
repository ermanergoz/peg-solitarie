# Peg Solitaire — Turkish localisation spec (tr)

Status: specified 2026-11-01 for 3.0.0 (founder directive, 2026-11-01: "also add turkish language
support in 3.0.0"). Branch `feature/turkish-locale`, cut from develop 37d3052.
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
9 Open items, founder questions, the parallel branch · 10 Change log

## 1. Decisions

The decisions, each a UX decision unless marked otherwise; the reasons are in the section named.

| # | Decision | Where |
|---|---|---|
| D-1 | Running text addresses the player as **siz**; commands (buttons, Settings rows, action labels, content descriptions of controls) are the **bare second-person-singular imperative** ("Geri al", "Oturum aç"). This is the convention of Android's own Turkish UI and of the tr-TR store listing, which already speaks in siz. A founder taste question offers "sen" (§9, Q-A); the build proceeds on siz. | §2 |
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
| D-13 | **Fonts**: the bundled Fredoka files are ~50 KB each, small enough to be Latin-only subsets that lack İ, Ğ, ğ, Ş, ş (Google Fonts' "latin" range has ı but not those five). The developer **proves glyph coverage** of all five TTFs before QA (§7.3); if a glyph is missing, the full OFL file replaces the subset. Not confirmable from this seat: flagged. | §7.3 |
| D-14 | The tr-TR listing's closing line "Oyunun arayüzü İngilizcedir." becomes false the moment this ships and **must be removed** from `docs/store/tr-TR/full_description.txt`; the tr-TR and en-US 3.0.0 changelogs gain a Turkish-interface line; `docs/store/README.md` lines 25–26 are updated. Release-manager's files, through the producer (§9). | §9 |

## 2. Register and style

### 2.1 Sen or siz

**Decision: siz in running text; bare imperative on commands.** Three reasons, in order of weight.

1. **The store listing already decided.** `docs/store/tr-TR/full_description.txt` speaks in siz
   throughout ("atlatın", "bırakın", "isterseniz", "Ayarlar'dan yeniden oynatabilirsiniz"). The
   dispatch asks for the two to stay consistent; a sen app under a siz listing would read as two
   products.
2. **It is the platform's register.** Android's own Turkish UI, Google Play, Play Games and the UMP
   consent form all put commands in the bare imperative ("Kaydet", "Oturum açın" only in prose,
   "Devam et" on the button) and address the user as siz in sentences. Our dialogs sit next to
   theirs (the Play Games sign-in sheet opens from ours), so matching them keeps one voice on screen.
3. **Siz is Turkish software's neutral, not its formal.** It does not read stiff in a game; "sen"
   reads as a deliberate chummy brand voice, which this calm, classic game does not have. Choosing
   sen would also mean re-editing the listing and every future changelog in that voice.

What this looks like:

| Surface | Form | Example |
|---|---|---|
| Buttons, Settings rows, action labels, chip titles, content descriptions of controls | bare imperative, sentence case | "Tekrar oyna", "Tahtayı gör", "Eğitimi atla", "Geri dön" |
| Dialog bodies, captions, banners, snackbars, supporting lines that address the player | siz | "Mevcut ilerlemeniz kaybolacak.", "Bir piyona dokunun, sonra …" |
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
listing where the listing has the term. The "Listing" column quotes the listing's own form.

| English | Türkçe | Listing | Notes |
|---|---|---|---|
| peg | piyon | "piyon" | never "taş" or "pim"; the listing chose piyon |
| hole (empty) | delik / boş delik | "boş deliğe" | "boş" alone for the spoken cell state |
| jump (noun) | atlama | "Atlamalar" | "2 atlama", "atlama hedefi" |
| jump (verb, the player makes a peg jump) | atlatmak | "atlatın" | "Atlatmaya devam edin" |
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
  "Peg Solitaire". Turkey knows the physical game as "Solo Test"; that is a possible search term
  for the listing, not a rename (§9, Q-B, after a trademark verdict).
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
| `game_over_perfect_body` | You cleared the board down to a single peg. | Tahtayı tek piyona kadar temizlediniz. | |
| `game_over_near_headline` | So close! | Az kaldı! | |
| `game_over_stuck_headline` | No moves left | Hamle kalmadı | |
| `restart_confirm_title` | Restart this board? | Bu tahta yeniden başlatılsın mı? | (!) §6 |
| `restart_confirm_body` | Your current progress will be lost. | Mevcut ilerlemeniz kaybolacak. | |
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
| `board_a11y_rejected_put_down_first` | Put down the selected peg first. | Önce seçili piyonu bırakın. | siz |
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
| `challenge_load_error` | Couldn't load this level. Tap Retry. | Bu seviye yüklenemedi. Yeniden dene düğmesine dokunun. | names the button without a suffix on it |

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
| `resume_classic_supporting` | Pick up your saved board | Kaydettiğiniz tahtadan devam edin | |
| `resume_challenge_title` | Continue Challenge | Meydan okumaya devam et | Continue tile; (!) §6 |
| `resume_challenge_supporting` | Pick up Level %1$d where you left off | %1$d. seviyeye kaldığınız yerden devam edin | ordinal on the number, suffix on the noun (D-6) |
| `tutorial_step_progress` | Step %1$d of %2$d | Adım %1$d, toplam %2$d | spoken only (the pip row); no slash, which TTS would read |
| `tutorial_step_1_caption` | Tap a peg, then tap the empty hole two cells away. | Bir piyona dokunun, sonra iki kare ötedeki boş deliğe dokunun. | caption wraps |
| `tutorial_step_2_caption` | Jumps work up, down, left and right — never diagonally. | Atlamalar yukarı, aşağı, sola ve sağa yapılır — asla çapraz yapılmaz. | the listing's rule sentence |
| `tutorial_step_3_caption` | Keep jumping. Clear the board down to one peg. | Atlatmaya devam edin. Tahtayı tek piyona kadar temizleyin. | |
| `tutorial_done_caption` | That's the whole game. Go clear a board. | Oyun bundan ibaret. Şimdi bir tahta temizleyin. | |
| `onboarding_got_it` | Got it | Anladım | |
| `onboarding_skip` | Skip tutorial | Eğitimi atla | |
| `ad_offer_watch` | Watch ad | Reklam izle | dialog button |
| `ad_offer_decline` | No thanks | Hayır, teşekkürler | dialog button; (!) §6 |
| `ad_offer_hint_title` | Out of free hints | Ücretsiz ipucu hakkınız bitti | |
| `ad_offer_undo_title` | Out of free undos | Ücretsiz geri alma hakkınız bitti | |
| `ad_offer_hint_body` | You've used your %1$d free hints for this level. Watch a short ad for %1$d more? | Bu seviyedeki %1$d ücretsiz ipucu hakkınızı kullandınız. %1$d ipucu daha için kısa bir reklam izlemek ister misiniz? | `%1$d` twice, as in English |
| `ad_offer_undo_body` | You've used your %1$d free undos for this level. Watch a short ad for %1$d more? | Bu seviyedeki %1$d ücretsiz geri alma hakkınızı kullandınız. %1$d geri alma daha için kısa bir reklam izlemek ister misiniz? | `%1$d` twice |
| `ad_offer_loading` | Loading ad | Reklam yükleniyor | content description |
| `ad_unavailable_hints` | Ad unavailable — hints refilled for free | Reklam gösterilemedi — ipuçları ücretsiz yenilendi | snackbar, two lines allowed |
| `ad_unavailable_undos` | Ad unavailable — undos refilled for free | Reklam gösterilemedi — geri almalar ücretsiz yenilendi | snackbar |
| `settings_section_general` | General | Genel | |
| `settings_section_privacy` | Privacy | Gizlilik | |
| `settings_privacy_policy_label` | Privacy policy | Gizlilik politikası | |
| `settings_privacy_policy_supporting` | Opens in your browser | Tarayıcınızda açılır | |
| `settings_privacy_policy_error` | Couldn't open the privacy policy — no browser app found. | Gizlilik politikası açılamadı — tarayıcı uygulaması bulunamadı. | |
| `settings_privacy_choices_label` | Manage privacy choices | Gizlilik tercihlerini yönet | |
| `settings_privacy_choices_supporting` | Change your ad consent choices | Reklam izni tercihlerinizi değiştirin | |

### 5.4 Play Games, cloud save, licences

| Key | English | Türkçe | Note |
|---|---|---|---|
| `play_games_prompt_title` | Sign in to Play Games? | Play Games'te oturum açılsın mı? | |
| `play_games_prompt_body` | Compete on leaderboards, unlock achievements, and keep your progress backed up to the cloud. Peg Solitaire plays exactly the same either way — you can sign in later from Settings. | Skor tablolarında yarışın, başarımların kilidini açın ve ilerlemenizi bulutta yedekli tutun. Peg Solitaire her iki durumda da aynı şekilde oynanır — daha sonra Ayarlar'dan oturum açabilirsiniz. | names the same three benefits as the Settings row |
| `play_games_prompt_accept` | Sign in | Oturum aç | |
| `play_games_prompt_decline` | Not now | Şimdi değil | |
| `play_games_signin_error` | Couldn't sign in to Play Games | Play Games'te oturum açılamadı | |
| `settings_section_play_games` | Play Games | — | `translatable="false"` (D-4) |
| `settings_play_games_signin_label` | Sign in to Play Games | Play Games'te oturum aç | |
| `settings_play_games_signin_supporting` | Leaderboards, achievements and cloud save | Skor tabloları, başarımlar ve bulut kaydı | |
| `settings_play_games_signed_in_as` | Signed in as %1$s | %1$s olarak oturum açıldı | |
| `settings_play_games_signed_in_fallback` | Signed in to Play Games | Play Games'te oturum açıldı | |
| `settings_play_games_leaderboards_label` | Leaderboards | Skor tabloları | |
| `settings_play_games_leaderboards_supporting` | See how your scores rank | Puanlarınızın sıralamasını görün | |
| `settings_play_games_achievements_label` | Achievements | Başarımlar | |
| `settings_play_games_achievements_supporting` | View the achievements you've earned | Kazandığınız başarımları görün | |
| `cloud_save_description` | Challenge level %1$d · %2$d stars · %3$d/%4$d Classic boards | Meydan okuma seviyesi %1$d · %2$d yıldız · %3$d/%4$d Klasik tahta | written into the snapshot in the device locale at commit time, by design |
| `profile_swap_title` | Moving to a different account? | Farklı bir hesaba mı geçiyorsunuz? | |
| `profile_swap_body` | You're signed in as %1$s. Keeping this device's progress merges it into their cloud save — nothing is lost on either side. Using their cloud progress instead replaces everything on this device: your level results, your play history, your Challenge level and both in-progress games. | %1$s olarak oturum açtınız. Bu cihazın ilerlemesini korumak, onu bu hesabın bulut kaydıyla birleştirir — iki tarafta da hiçbir şey kaybolmaz. Bunun yerine bu hesabın bulut ilerlemesini kullanmak, bu cihazdaki her şeyin yerine geçer: seviye sonuçlarınız, oyun geçmişiniz, Meydan okuma seviyeniz ve devam eden iki oyununuz. | all four wiped categories kept (D-14 of phase 13) |
| `profile_swap_body_unknown_account` | You're signed in with a different Play Games account. Keeping this device's progress merges it into this account's cloud save — … and both in-progress games. | Farklı bir Play Games hesabıyla oturum açtınız. Bu cihazın ilerlemesini korumak, onu bu hesabın bulut kaydıyla birleştirir — iki tarafta da hiçbir şey kaybolmaz. Bunun yerine bu hesabın bulut ilerlemesini kullanmak, bu cihazdaki her şeyin yerine geçer: seviye sonuçlarınız, oyun geçmişiniz, Meydan okuma seviyeniz ve devam eden iki oyununuz. | same wording, "bu hesabın" |
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
| `licenses_psl_parts` | The public-suffix data compiled into OkHttp, as it stood on 2023‑02‑15 | OkHttp'ye derlenmiş genel sonek verileri, 15 Şubat 2023 tarihindeki hâliyle | the date in words needs no non-breaking hyphens |
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
| `pegs_remaining_retry` | %d peg remaining. Try again? / %d pegs remaining. Try again? | %d piyon kaldı. Tekrar denemek ister misiniz? | %d piyon kaldı. Tekrar denemek ister misiniz? |
| `holes_count` | %d hole / %d holes | %d delik | %d delik |
| `duration_minutes` | %d minute / %d minutes | %d dakika | %d dakika |
| `board_a11y_pegs_left` | %d peg left / %d pegs left | %d piyon kaldı | %d piyon kaldı |
| `board_a11y_jumps` | %d jump / %d jumps | %d atlama | %d atlama |
| `licenses_sound_effects` | %d sound effect / %d sound effects | %d ses efekti | %d ses efekti |

### 5.6 Count

Base file on develop 37d3052, counted by hand: 223 `<string>` entries and 7 `<plurals>`. Of the
strings, 35 are already `translatable="false"` and 9 more become so (§4), which leaves about
**179 strings and 7 plurals** in `values-tr/strings.xml`: 174 translated, 5 copied verbatim
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
| `tutorial_step_*_caption` | caption under the tutorial board | 47–55 | 55–68 | one more line at 200 %; the caption column scrolls per ux-spec §4.1 | none; the sentences teach the rule |
| `settings_privacy_choices_label` | Settings row label | 22 | 27 | rows wrap | none |
| `ad_offer_*_title` | dialog title | 17 | 29–33 | two lines | none |

Not at risk, checked: the three ribbon pill labels ("Yıldız", "Tahta", "Seviye") are at most one
glyph longer than English and the pill wraps by rule; the status-strip title "Seviye 1234" is one
glyph wider than "Level 1234" with ≥ 25dp spare; "Klasik" and "Ayarlar" are shorter than English;
dialog bodies, snackbars, supporting lines and the licences screen are multi-line by design.

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

### 7.3 Fonts — glyph coverage of Fredoka and Nunito

Turkish needs, beyond ASCII: Ç U+00C7, ç U+00E7, Ğ U+011E, ğ U+011F, İ U+0130, ı U+0131,
Ö U+00D6, ö U+00F6, Ş U+015E, ş U+015F, Ü U+00DC, ü U+00FC, and â U+00E2 (in "hâlâ").

The published Fredoka and Nunito families cover Latin Extended-A, so a full file has every one
of these. The files in `app/src/main/res/font/` are `fredoka_medium.ttf` (49,932 bytes),
`fredoka_semibold.ttf` (49,900), `nunito_regular.ttf` (132,200), `nunito_semibold.ttf`
(132,152), `nunito_bold.ttf` (132,148). The Fredoka pair is small enough to be a subset: Google
Fonts' "latin" unicode-range includes U+0131 (ı) but **not** U+0130, U+011E/011F or U+015E/015F,
so a "latin"-only Fredoka would draw "İpucu", "Mükemmel!", "Çözüldü!", "Başarımlar" and
"KALAN PİYON" with the missing letters in the system fallback font — visibly a different weight
and width inside one word. **This cannot be confirmed from a spec seat and is flagged (D-13).**

What the developer does, before QA:

1. Proves coverage of all five files for the 13 code points above (any of: `fontTools`'
   `ttx -t cmap`, `otfinfo -u`, or Android Studio's font preview typed with the §8 pangram) and
   records the result in the report.
2. If any file lacks any of them, replaces it with the full file of the same family and weight
   from the Google Fonts release (same SIL OFL 1.1; the THIRD_PARTY.md row and the licences
   screen name the family, not the file, so neither changes; the byte size in any doc that lists
   it is updated).

What QA checks on the device (§8, A-2): the pangram "Pijamalı hasta yağız şoföre çabucak
güvendi" plus "İpucu" and "KALAN PİYON" render in both families with no fallback glyphs — every
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
- **Roborazzi goldens** in `tr` at 360×640dp, font scale 1.0 and 2.0, for the eight slots of §6:
  Home (tiles, chips, ribbon), Results in the Perfect, Solved and Out-of-moves states, the
  restart-confirm dialog, the ad-offer dialog, the rescue banner, the Undecided notice. QA reviews
  the goldens against the device.
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
- **C-2 Fonts.** Given a Turkish device, when the tutorial, Home, Results and Settings are opened,
  then every Turkish letter in "İpucu", "Mükemmel!", "Çözüldü!", "Başarımlar", "Yazı tipleri",
  "Gizlilik", "KALAN PİYON" and "Geçen süre" renders in the surrounding font (same weight, same
  baseline) in both Fredoka and Nunito — no fallback glyph, no tofu — at 100 % and 200 %.
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
  recorded with a screenshot for the §6 decision.

### Tutorial (ONB-01)

- **T-1.** Given a first run in Turkish, when the tutorial opens, then the step 1 caption reads
  "Bir piyona dokunun, sonra iki kare ötedeki boş deliğe dokunun." in full at 100 % and 200 %,
  and "Eğitimi atla" is a visible, whole action.
- **T-2.** When the three steps are completed, then the captions of steps 2 and 3 and the done
  caption "Oyun bundan ibaret. Şimdi bir tahta temizleyin." are shown whole, and the CTA reads
  "Anladım".
- **T-3 (TalkBack).** Given TalkBack on, when the step 2 board gains focus, then "Adım 2, toplam 3."
  is spoken before the caption.

### Home

- **H-1.** Given Home, then the tiles read "Klasik" / "5 tahta" and "Meydan okuma" with its chip
  "Devam et — Seviye N"; the ribbon pills read "Yıldız", "Tahta", "Seviye" with their values; the
  gear's description is "Ayarlar". At 200 % the tile titles wrap and the tiles grow; nothing clips.
- **H-2.** Given a saved Classic and a saved Challenge game, then both Continue tiles show
  "Klasik oyuna devam et" / "Kaydettiğiniz tahtadan devam edin" and "Meydan okumaya devam et" /
  "N. seviyeye kaldığınız yerden devam edin", whole, at 100 % and 200 % (a wrap is allowed; a
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
  başlatılsın mı?" / "Mevcut ilerlemeniz kaybolacak." with "Oynamaya devam et" and "Yeniden
  başlat"; at 200 % the buttons may stack but are whole.
- **G-6 Game-over dialog.** Given a perfect clear, then "Mükemmel!" / "Tahtayı tek piyona kadar
  temizlediniz." with "Tekrar oyna" and "Tahtayı gör"; given two pegs left, "Az kaldı!" with
  "2 piyon kaldı. Tekrar denemek ister misiniz?"; given no moves, "Hamle kalmadı".
- **G-7 Loading.** Given a Challenge level generating, then "Seviye yükleniyor" is shown; given
  a load failure, "Bu seviye yüklenemedi. Yeniden dene düğmesine dokunun." with "Yeniden dene".

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
  bırakın."
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
  ipucu hakkınız bitti" / "Bu seviyedeki N ücretsiz ipucu hakkınızı kullandınız. N ipucu daha için
  kısa bir reklam izlemek ister misiniz?" with "Reklam izle" and "Hayır, teşekkürler"; the undo
  twin likewise; given no ad, the snackbar "Reklam gösterilemedi — ipuçları ücretsiz yenilendi"
  and the hint lands first.
- **S-4 Play Games.** Given the first-run prompt, then "Play Games'te oturum açılsın mı?" with the
  body of §5.4, "Oturum aç" and "Şimdi değil"; given a signed-in player, the Settings section shows
  "<name> olarak oturum açıldı", "Skor tabloları", "Başarımlar"; given an SDK error, "Play
  Games'te oturum açılamadı". The Play Games sheets themselves are Google's Turkish (not judged).
- **S-5 Account change.** Given a second account, then the dialog reads "Farklı bir hesaba mı
  geçiyorsunuz?" with the body naming all four wiped categories, and the two buttons whole (they
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

### 9.1 Proposed founder questions (taste; non-blocking; the producer assigns ids and queues them)

**Q-A — Register: siz or sen?**
Context: The Turkish UI addresses the player as "siz" in sentences, with bare-imperative buttons —
the platform's convention and the voice of the tr-TR store listing. "Sen" would be warmer and is
common in casual games; it would also mean re-editing the listing in that voice.
Options: 1) siz in sentences, bare imperative on buttons (specified, being built)
2) sen throughout, listing re-edited to match 3) sen in the tutorial and game-over copy only,
siz elsewhere.
Recommendation: ux-designer — 1. One voice across the app, the listing and Google's own dialogs
beside ours; a switch to 2 later is a wording pass over §5 with no code change.
Blocks: nothing.

**Q-B — "Solo Test" in the Turkish listing?**
Context: Turkey knows the physical game as "Solo Test". The app's title stays "Peg Solitaire"
everywhere (§4); the question is only whether the tr-TR short or full description names
"Solo Test" so Turkish players searching that term find the game.
Options: 1) Add one sentence to the tr-TR full description ("Türkiye'de Solo Test olarak da
bilinir") after the research-analyst's trademark verdict 2) Leave the listing as it is.
Recommendation: ux-designer — 1, if the verdict is clean; it is a search term, not a rename.
Blocks: nothing (the listing edit is the release-manager's and waits for the verdict).

### 9.2 Items for other roles, through the producer

- **release-manager**: (a) delete the line "Oyunun arayüzü İngilizcedir." from
  `docs/store/tr-TR/full_description.txt` (D-14) — it is false the moment 3.0.0 ships with
  this branch; (b) add a Turkish-interface line to `docs/store/tr-TR/changelogs/30000.txt`
  ("Türkçe arayüz") and `en-US/changelogs/30000.txt` ("Turkish interface"); (c) update
  `docs/store/README.md` lines 25–26, which say the app has no Turkish strings; (d) note, not a
  change: both listings call the third board "Asymmetric-39" while the app calls it
  "Asymmetric 3-3-2-2" in both languages — pre-existing, outside this task.
- **research-analyst**: a trademark check on "Solo Test" before Q-B's listing edit.
- **tech-lead**: sign-off on `localeConfig` (generated vs hand-written) and `localeFilters`
  (§7.4), and on the font replacement if §7.3 finds a subset.
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

## 10. Change log

| Date | Change | By |
|---|---|---|
| 2026-11-01 | First complete version: decisions D-1..D-14, register, glossary, proper names, the full table for develop 37d3052 (§5.1–5.6), truncation watchlist, build and QA rules, acceptance criteria, two proposed founder questions. | ux-designer |
