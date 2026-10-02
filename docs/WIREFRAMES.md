# docs/WIREFRAMES.md — SPARK screens (Kotlin-native)

Legend: each screen is one full phone screen. Dark theme.

## 0. Nav map

```
Login (gate) → Feed (home) ⇄ Saved ⇄ Stats ⇄ Settings
Feed audio card → Player (overlay) → back to Feed
```

Bottom or top tab row on Feed/Saved/Stats/Settings: `[Feed] [Saved] [Stats] [Settings]`

## 1. Feed card — book_idea / summary (default)

```
┌──────────────────────────┐
│ [BOOK IDEA]         3/20 │  type chip + session counter
│                          │
│ Title, max 80 chars      │
│                          │
│ Body text, large type,   │
│ max ~600 chars,          │
│ 30-60 sec read.          │
│                          │
│ ▸ Try this today: …      │  action_line (when sensible)
│ ──────────────────────── │
│ ♡ Save   👍  👎          │  row 1: state actions
│ 🔊 Listen   ⓘ Why this?  │  row 2: audio + reason
└──────────────────────────┘
```

Swipe up = next card. No swipe-down-back beyond 1 (keep simple).

## 2. Feed card — quote

```
┌──────────────────────────┐
│ [QUOTE]             4/20 │
│                          │
│   "Big centered text,    │
│    1-3 lines."           │
│                          │
│   — Author, Source       │
│                          │
│ ▸ Try this today: …      │
│ ──────────────────────── │
│ ♡ Save  👍 👎 🔊 ⓘ       │
└──────────────────────────┘
```

## 3. Feed card — concept (GATE)

```
┌──────────────────────────┐
│ [CONCEPT·Thermo]    5/20 │  chip shows topic tag
│ Term / Formula           │
│ ┌──────────────────────┐ │
│ │ diagram / formula box│ │
│ └──────────────────────┘ │
│ 3-4 line explanation     │
│ ▸ Try this today: …      │
│ ──────────────────────── │
│ ♡ Save  👍 👎 🔊 ⓘ       │
└──────────────────────────┘
```

## 4. Feed card — quiz

```
┌──────────────────────────┐
│ [QUIZ]              6/20 │
│ Question, 1-2 lines      │
│ ┌──────────────────────┐ │
│ │ A) option            │ │  tap → green/red,
│ │ B) option            │ │  explanation appears,
│ │ C) option            │ │  next review scheduled
│ │ D) option            │ │
│ └──────────────────────┘ │
│ ──────────────────────── │
│ ♡ Save  🔊 ⓘ             │
└──────────────────────────┘
```

## 5. Feed card — audio

```
┌──────────────────────────┐
│ [AUDIO]             7/20 │
│ Chapter title            │
│ LibriVox · 12:04         │  source + duration
│ ┌──────────────────────┐ │
│ │ ▶  ━━━━━●━━━  12:04  │ │  mini player → opens Player
│ └──────────────────────┘ │
│ ──────────────────────── │
│ ♡ Save  👍 👎  ⓘ         │
└──────────────────────────┘
```

## 6. Feed card — challenge

```
┌──────────────────────────┐
│ [CHALLENGE]         8/20 │
│ Do X in 5 minutes        │
│ Short how-to, 3 lines    │
│ ┌──────────────────────┐ │
│ │ [ ✓ I did it ]       │ │  one-tap complete
│ └──────────────────────┘ │
│ ──────────────────────── │
│ ♡ Save  👍 👎  ⓘ         │
└──────────────────────────┘
```

## 7. Why-this dialog (from ⓘ on any card)

```
┌──────────────────────────┐
│ Why am I seeing this?    │
│ Source: Gutenberg …      │  source_ref + license
│ [Grounded ✓] /           │
│ [AI-written, unverified] │  exactly one label
│ Topic: Thermo (saved 5×) │  ranking reason, plain words
│        [ Close ]         │
└──────────────────────────┘
```

## 8. You're-done screen (at 20/20 or 15 min)

```
┌──────────────────────────┐
│                          │
│   You're done.           │
│   20 cards · 12 min      │
│   3 saved                │
│                          │
│ ┌──────────────────────┐ │
│ │  Review saved        │ │
│ │  Do the action now   │ │
│ │  Close app           │ │
│ └──────────────────────┘ │
│ No more cards load.      │
└──────────────────────────┘
```

## 9. Login

```
┌──────────────────────────┐
│                          │
│   SPARK                  │
│   finite feed            │
│                          │
│ ┌──────────────────────┐ │
│ │ Passcode  ••••       │ │
│ └──────────────────────┘ │
│ ┌──────────────────────┐ │
│ │ [ Unlock ]           │ │
│ └──────────────────────┘ │
│ wrong code? red text     │
│ stays logged in after    │
└──────────────────────────┘
```

## 10. Saved tab

```
┌──────────────────────────┐
│ Saved        [Feed tabs] │
│ ┌──────────────────────┐ │
│ │ Search saved…        │ │
│ └──────────────────────┘ │
│ ● Title one      quote   │  rows: dot=type, title, tag
│ ● Title two      idea    │
│ ● Title three    concept │
│ (tap row → full card)    │
└──────────────────────────┘
```

## 11. Stats screen

```
┌──────────────────────────┐
│ Stats        [Feed tabs] │
│ Today: 20 cards · 12 min │
│ Saved ever: 34           │
│ Reviews due: 5           │
│ Weak topics:             │
│  Thermo ██████░░  12     │
│  SOM    ████░░░░   7     │
└──────────────────────────┘
```

## 12. Settings screen

```
┌──────────────────────────┐
│ Settings     [Feed tabs] │
│ Cards per session  [20]  │
│ Minutes per session[15]  │
│ TTS speed        [1.0×]  │
│ [ Clear cache ]           │
│ [ Export saved (JSON) ]   │
│ [ Log out ]               │
│ SPARK v0.1.0 · debug     │
└──────────────────────────┘
```

## 13. Audiobook player (overlay from audio card)

```
┌──────────────────────────┐
│ ▾  Now playing           │
│ ┌──────────────────────┐ │
│ │      COVER           │ │
│ └──────────────────────┘ │
│ Book · Chapter 3         │
│ ━━━━━●━━━━━━━  04:12     │  seek bar, tap/drag
│ ⏮ -10s    ▶/⏸    +30s ⏭ │
│ Speed 1.0× · Sleep ⏲    │
│ resumes where you left   │
└──────────────────────────┘
```

Lock screen shows same controls. Screen off keeps playing.
