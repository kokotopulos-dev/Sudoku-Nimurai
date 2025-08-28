# Sudoku Nimurai (Compose)

Orientalne sudoku z muzyką i dźwiękami. Ten projekt buduje się w GitHub Actions – nie musisz mieć Android Studio.

## Jak zbudować APK bez Android Studio
1. Utwórz **nowe prywatne repozytorium** na GitHub, np. `sudoku-nimurai`.
2. Wgraj pliki z tego projektu (zawartość całego folderu).
3. Wejdź w zakładkę **Actions** → zatwierdź uruchamianie workflow.
4. Uruchom ręcznie workflow **Android CI (Build APK)** (zakładka Actions → Workflow Dispatch).
5. Po zakończeniu pobierz artefakt **sudoku-nimurai-apk** – w środku będzie `app-debug.apk`.

## Nazwa pakietu
`com.nimurai.sudoku`

## Zmiana muzyki / dźwięków
Pliki znajdują się w `app/src/main/res/raw/`:
- `nimurai_bgm.wav` – muzyka tła (oryginalny loop 70s)
- `sfx_digit_1..9.wav` – dźwięki cyfr
- `sfx_error.wav` – dźwięk błędu

Możesz podmienić na własne pliki (polecam OGG/WAV).

## Unikatowe elementy
- **Kombinacje (Kata):** seria poprawnych ruchów zwiększa combo; co 5 ruchów dostajesz **shuriken** (specjalna podpowiedź).
- **Shuriken:** automatycznie wypełnia jedno poprawne pole – preferencyjnie w tym samym boxie 3×3.
- **Zen mode:** tryb relaksu z pulsującym kręgiem, bez pogoni za wynikiem.
- **Sakura:** delikatna animacja płatków w tle.

## Min SDK
Android 7.0 (API 24)
