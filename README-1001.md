# crew-story-1001

Focused first-pass UX refactor for `magic76/crew-story`.

## Changed files

- `app/src/main/java/com/crewpocket/story/StoryPlayerActivity.java`
- `app/src/main/java/com/crewpocket/story/StoryPlayerUiState.java` (new)

## What changed

1. Reworked the player from a utility/dashboard layout into an illustration-first book reader.
2. Illustration now receives most vertical space using layout weights instead of a fixed 175dp image height.
3. Connection/debug status is visually quieter in healthy states; errors remain prominent.
4. Added a persistent discoverability hint that the child can talk/interject while narration is running.
5. `onUserInterrupted()` now produces an explicit LISTENING state instead of doing nothing.
6. Centralized transient player UI state in `StoryPlayerUiState`.
7. Simplified header controls and reduced visual competition from Edit / connection / emotion metadata.
8. Previous/next disabled state is explicit at story boundaries.
9. Kept the existing `StoryLiveClient` + `StoryPlaybackService` responsibilities intact to minimize merge risk.

## Merge notes

This package intentionally contains only changed/new source files, not a full repository clone.
Copy the files onto the matching paths in the current repo, then let the local agent resolve any changes made after the reviewed `main` revision.

Recommended validation:

```bash
./gradlew assembleDebug
```

Then test:

- open a saved story
- start/stop narration
- interrupt narration with speech
- prev/next page
- drag page SeekBar
- open Editor and return
- finish story and replay
- background/foreground the app

## Follow-up candidates for 1002+

- page-centric Story Editor with bottom filmstrip
- home/shelf hierarchy cleanup
- Room migration from one SharedPreferences JSON blob
- richer transcript/current-sentence highlighting if Gemini Live transcript timing is reliable
- service/session ownership refactor only after validating current background-play requirements
