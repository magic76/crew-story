# crew-story-1005

Built against the current GitHub `main` inspected on 2026-09-06.

Observed source SHAs:
- StoryPlayerActivity.java: `ca81a5ffad6a56b5cecf6e6b6b9f2ddc4d25aaa2`
- StoryLiveClient.java: `45a0e52190aef8b4f9dd4e63362cbbae803b5392`
- StoryPlaybackService.java: `3851f9d5e43d19b7b9deb62d4d58840ca3316b08`
- StoryPlayerUiState.java: `addb0567f36a4ccbe2a1a945f09fe890a6aeb769`

Current GitHub main already contains manual push-to-talk, child transcription,
Archie-answer transcription, an in-player interaction card and in-memory Q&A
history. This package preserves those features.

It also confirms the earlier StorySession / ReadingPosition / ConversationTurn
proposal is not currently in GitHub main, so 1005 does not pretend it has
already been merged.

## Changes

- Chinese identity: `阿奇`
- English identity: `Archie`
- Role: `故事夥伴 / Story Companion`
- Adds `ArchiePersona.java` as the single persona contract.
- Gemini Live prompt now explicitly makes Archie a companion, not a teacher.
- Removes habitual teacher-like praise/instruction behavior from persona.
- Push-to-talk becomes `跟阿奇說話 / Talk to Archie`.
- Listening becomes `阿奇在聽… / Archie is listening…`.
- Second tap becomes `我說完了 / I'm done`.
- Adds real `RESPONDING` UI mode.
- Fixes the current bug where "answering" was passed through READY.detail but
  READY ignored the detail.
- READY becomes visually quiet.
- Technical websocket/setup statuses are no longer surfaced to children.
- Q&A transcript/history labels use `阿奇 / Archie`.
- Background notification becomes `阿奇說故事中`.
- Main title becomes `Crew Story · 阿奇 / Crew Story · Archie`.
- Story generation wording is aligned with the Archie identity.

## Apply

From the repo root:

```bash
python3 /path/to/crew-story-1005/apply-1005.py .
./gradlew assembleDebug
```

The updater checks important anchors and stops if your local code has diverged
too much, rather than silently making a partial merge.

## Validate

1. Open a story and confirm no visible `波波老師`, `Teacher`, or generic
   `AI 說書人` remains in the touched flows.
2. Tap `跟阿奇說話`.
3. Confirm narration stops and UI says `阿奇在聽…`.
4. Tap `我說完了`.
5. Confirm UI immediately becomes `阿奇正在回答…`.
6. Confirm answer transcript label is `阿奇`.
7. Open Q&A history and confirm the same naming.
8. Background the app and confirm notification says `阿奇說故事中`.
9. Switch UI to English and confirm the visible identity is `Archie`.

## Not included

Persistent reading-session storage is deliberately not bundled here because
those session classes are not present on the actual GitHub main inspected for
this package. That should be added after 1005 against the real merged tree.
