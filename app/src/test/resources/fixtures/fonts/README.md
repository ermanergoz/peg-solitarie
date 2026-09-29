# Test-only fonts

`Fredoka-Variable.ttf` is Google Fonts' `ofl/fredoka/Fredoka[wdth,wght].ttf`
(https://github.com/google/fonts/raw/main/ofl/fredoka/Fredoka%5Bwdth,wght%5D.ttf,
sha256 `2ba02e68b152868aef9ba28e24b3648c7d457fe6f25c761f2c2c53fb61a73fc8`), unmodified and
renamed. It is the face the app's type scale downloads at run time for its title and headline
roles. It is licensed under the SIL Open Font License 1.1, and its notice is `Fredoka-OFL.txt`.

It lives on the unit-test classpath only and never ships in the app. The downloadable face never
resolves under Robolectric, and Robolectric's fallback face has tabular digits. Without this file
no JVM test could show how the timer behaves in the shipped face's proportional digits:

- `GameStatusStripTest` checks that the pill keeps one width as the clock ticks (criterion 18d).
- `TimerDigitsTest` checks that the clock takes the face's digit cells when the face lands after
  the first frame, as a downloaded face does.

It sits under `fixtures/`, not in a root `fonts/` directory, because a `fonts/` directory on the
test classpath shadows the one Robolectric loads the platform's own fonts from, and every
Robolectric test then fails to start.
