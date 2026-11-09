# SPARK — logo files

Three files, one identity. The wordmark is **drawn**, not typed, so it is the
same drawing in the prototype, in the app, on the launcher and on the splash.

| File | What it is | Where it goes |
|---|---|---|
| `spark-wordmark.svg` | The name, SPARK, in ink `#F4F4F6` | In-app header |
| `spark-mark.svg` | The peak of the A on its own, amber `#F5A844` | Splash, notification icon, anywhere square and small |
| `spark-icon.svg` | Mark on the app ground, 108 × 108 (adaptive-icon canvas) | App icon |

Open `index.html` next to this file for the visual sheet with sizes and rules.

## Using them in the Android app

The SVGs are not used directly by Android. Android Studio converts them to
vector drawables — same drawing, native format.

1. In Android Studio: **File → New → Vector Asset → Local file (SVG, PSD)**,
   pick the file, name it, import. Do this for the wordmark and the mark.
2. That gives you
   `app/src/main/res/drawable/ic_spark_wordmark.xml` and
   `app/src/main/res/drawable/ic_spark_mark.xml`.

### In the Home header (Compose)

```kotlin
Image(
    painter = painterResource(R.drawable.ic_spark_wordmark),
    contentDescription = "SPARK",
    modifier = Modifier.height(21.dp)
)
```

Use it at the height it was drawn for (21 dp, minimum 16 dp). Do not put the
name in a `Text` — a font is a different logo.

### App icon (adaptive)

`minSdk` is 26, so the adaptive icon is the only icon you need.

`app/src/main/res/values/colors.xml`

```xml
<color name="ic_launcher_background">#0E0E11</color>
```

`app/src/main/res/drawable/ic_launcher_foreground.xml` — a 108 × 108 vector
whose content is the mark inside the 72 dp safe zone:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path
        android:pathData="M28,80 L54,26 L80,80"
        android:strokeColor="#F5A844"
        android:strokeWidth="9.5"
        android:strokeLineCap="butt"
        android:strokeLineJoin="round" />
</vector>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

Copy the same file to `ic_launcher_round.xml`. Then set
`android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`
on `<application>` in `AndroidManifest.xml`.

Android 13+ also uses the foreground for themed (monochrome) icons — the peak
is a single shape, so it works there unchanged.

### Splash

On Android 12+ the launcher icon is the splash automatically — nothing to add.
Below Android 12 the app opens on the theme background
(`res/values/themes.xml`, the dark `#0E0E11` ground), optionally with the mark
centred via `windowBackground`. A real `SplashScreen` API needs the
`androidx.core:core-splashscreen` dependency — that is a new dependency, so it
needs the owner's approval before it is added.

## Rules that keep it looking like a logo

- **Never retype the wordmark.** It is a drawing; a font version will always
  look generic, which is what the earlier attempts looked like.
- **Scale by height only.** Stretching and re-spacing break the lockup — the
  letters were spaced by hand.
- **Clear space:** the height of the S around the wordmark; a quarter of the
  icon around the mark.
- **Minimum sizes:** wordmark 16 dp, mark 24 dp, icon 32 dp.
- **One amber per screen.** On the icon the mark is amber. In the app header the
  wordmark is ink, so amber stays for the greeting and the card.
- **Dark ground only.** On amber, the mark goes ink. Never on a photograph.
