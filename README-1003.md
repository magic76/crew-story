# crew-story-1003

Implements the requested items:

- **3 — Page-centric Story Editor**
- **6 — Simpler child-first Story Shelf**
- **7 — Architecture cleanup**

This package is designed to merge on top of 1001/1002 without replacing the
player/audio changes from those versions.

## Changed / new files

### Full replacements

- `StoryEditorActivity.java`
- `StoryRepository.java`

### New architecture / UI files

- `StoryStore.java`
- `SharedPreferencesStoryStore.java`
- `StoryShelfView.java`

### MainActivity integration

- `patches/MainActivity-1003.patch`

`MainActivity` is deliberately a patch because it also contains the create-story
dialog and all settings UI. Replacing the whole file would create unnecessary
conflicts with your local branch.

---

## 3. Story Editor redesign

The editor now works one page at a time:

- large current-page illustration
- narration text
- emotion chips
- optional character/dialogue
- bottom horizontal filmstrip
- `+` adds a page
- page tap switches page
- delete current page
- persistent image URI via `ACTION_OPEN_DOCUMENT`

The editing mental model is now:

`Story -> current Page -> illustration/text/mood/dialogue`

instead of:

`Story -> giant form containing every page`.

---

## 6. Story Shelf redesign

The shelf removes these from the main child-facing surface:

- Gemini explanation banner
- language selector
- "My Picture Books" administrative header
- per-card edit/play button clutter

The main screen becomes:

- “今晚想聽哪個故事？”
- simple book cards
- one clear play affordance
- long-press for edit/delete
- one bottom `＋ 創作新故事`

Language/voice/API configuration should stay in Settings.

---

## 7. Architecture cleanup

Before:

`Activity -> StoryRepository -> SharedPreferences JSON`

1003:

`Activity -> StoryRepository -> StoryStore -> SharedPreferencesStoryStore`

No user data migration is needed because the existing preference name/key and
StoryModel JSON format are preserved.

This means a later Room migration can implement:

`RoomStoryStore implements StoryStore`

without rewriting StoryEditorActivity/MainActivity/StoryPlayerActivity.

The old static `StoryRepository` methods are kept, so existing call sites remain
source-compatible.

---

## Merge order

1. Apply 1001 if not already merged.
2. Apply 1002 audio/manual-talk changes.
3. Copy all full/new Java files from 1003.
4. Merge `MainActivity-1003.patch`.
5. Build.

```bash
./gradlew assembleDebug
```

## Manual validation

### Editor

- Open existing story.
- Switch pages via filmstrip.
- Edit page 1, switch to page 2, switch back: changes remain.
- Add page.
- Delete page.
- Cannot delete the last remaining page.
- Select/change illustration and reopen the editor.
- Edit title/summary/emoji and save.
- Player still loads the saved story.

### Shelf

- Home contains books first, not technical configuration.
- Tap book -> player.
- Long press -> play/edit/delete.
- Create button still opens existing create-story flow.
- Settings tab still works.

### Persistence

- Existing locally saved stories still appear.
- Snow White sample remains.
- Save/update/delete still work.
- App restart keeps stories.

## Architecture note

1003 intentionally does **not** add Room yet. That would introduce schema,
migration and dependency/build changes while 1001/1002 are still settling.
The abstraction boundary added here is the safe prerequisite for Room.

## Suggested 1004

After validating 1003, the next high-value improvement is:

- child speech transcript
- teacher answer rendered as text
- interaction history for the current reading session
- optionally persist the most useful teacher answers
