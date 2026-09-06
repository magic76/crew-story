# crew-story-1002

Manual interruption / push-to-talk interaction for Crew Story.

## Why

The current `StoryLiveClient` already contains an `AudioRecord` implementation, but the
normal player flow does not provide a reliable user entry point into microphone streaming.
At the same time, leaving voice activity detection responsible for interruption makes a
children's story player vulnerable to TV audio, family conversation, coughing, and other
room noise.

1002 changes the interaction contract:

1. Teacher narrates with the microphone OFF.
2. Child taps **🎤 我要說話**.
3. Current teacher audio is flushed immediately.
4. Client sends Gemini Live `activityStart`.
5. PCM 16kHz microphone audio streams only while the child-turn is active.
6. Child taps **⏹️ 說完了**.
7. Microphone closes and client sends `activityEnd`.
8. Gemini answers the child.
9. After the answer finishes, the client explicitly asks Gemini to naturally return to the
   interrupted page instead of auto-advancing.

## Included

### Full replacement

`app/src/main/java/com/crewpocket/story/StoryLiveClient.java`

This is based on the reviewed current `main` version and includes:

- server automatic VAD disabled
- explicit `activityStart` / `activityEnd`
- `beginUserTurn()`
- `endUserTurn()`
- mic lifecycle cleanup
- child answer state
- no auto page advance during the child question/answer turn
- same-page narration resume after the answer

### Small UI patch

`patches/StoryPlayerActivity-1002.patch`

This intentionally stays as a patch because `crew-story-1001` may already have changed the
player layout locally. Your local agent should merge the behavior into whichever 1001
player UI is currently present.

## Important merge behavior

If 1001 has already been merged, do **not** replace the whole player with the old GitHub
main player. Instead:

- keep the 1001 illustration-first player
- add one primary child interaction button
- wire it to `beginUserTurn()` / `endUserTurn()`
- map `onUserInterrupted()` to the listening state
- disable the button while Gemini is answering

Recommended button states:

- `🎤 我要說話`
- `⏹️ 說完了`
- `⏳ 波波老師回答中…`
- back to `🎤 我要說話`

## Gemini Live protocol

1002 uses true manual activity boundaries:

```json
"realtimeInputConfig": {
  "automaticActivityDetection": {
    "disabled": true
  },
  "activityHandling": "START_OF_ACTIVITY_INTERRUPTS"
}
```

Button press:

```json
{
  "realtimeInput": {
    "activityStart": {}
  }
}
```

Button release / "說完了":

```json
{
  "realtimeInput": {
    "activityEnd": {}
  }
}
```

This is intentionally different from `audioStreamEnd`, which is meant for configurations
where automatic activity detection remains enabled.

## Validation

Run:

```bash
./gradlew assembleDebug
```

Manual test:

1. Open a story and let the teacher narrate.
2. Make noise near the phone without touching the talk button.
   - Expected: teacher continues; no interruption.
3. Tap `我要說話` while the teacher is speaking.
   - Expected: teacher audio stops immediately.
   - UI becomes `正在聽你說話`.
4. Ask a question.
5. Tap `說完了`.
   - Expected: mic closes and teacher answers.
6. Wait for teacher answer to finish.
   - Expected: narration returns to the same page instead of moving to the next page.
7. Repeat several times.
8. Test pause/resume, page jump, app background/foreground, and story replay.

## Known limitation

The app does not currently track word/sentence-level playback position. After answering a
child, Gemini is therefore instructed to resume "near the interrupted position" using its
conversation context. It is not sample-accurate. A future version can use output
transcription timestamps to track the exact narration sentence.

## Suggested next version

`crew-story-1003`

- show the child's recognized transcript on screen
- show the teacher's answer as text so it is not forgotten
- optional one-tap "再說一次"
- optional auto-end after local silence detection, while still requiring the button to
  begin a child turn
