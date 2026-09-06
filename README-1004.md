# crew-story-1004

Implements the requested 1004 interactive reading session enhancements:

- **1 — Child Speech Transcript (小朋友語音轉文字即時字幕)**
- **2 — Teacher Answer Rendered as Text (波波老師互動回答文字呈現)**
- **3 — Reading Session Interaction History (當次共讀問答互動紀錄與瀏覽視窗)**

This package builds directly on top of 1001 (Player UI), 1002 (Push-to-Talk manual boundary), and 1003 (Filmstrip Editor & StoryStore Architecture).

## Key Changes & Architecture

### 1. Data Model
- `StoryInteractionItem.java`: Records `pageIndex`, `childTranscript`, `teacherAnswer`, and `timestamp`.

### 2. Live Client (`StoryLiveClient.java`)
- Captures realtime child speech transcripts from Gemini Live API `userTurn` and `inputAudioTranscription`.
- Accumulates streaming teacher answer text during child interaction turns.
- Notifies listener on transcript arrival (`onChildSpeechTranscript`), text answer (`onTeacherAnswerText`), and turn completion (`onInteractionCompleted`).

### 3. Player UI (`StoryPlayerActivity.java`)
- **Live Q&A Subtitle Card (`liveInteractionCard`)**: Real-time rendering of child query and storyteller response with visual role separation.
- **Session Interaction History Dialog (`showInteractionHistoryDialog`)**: Accessible via `💬 互動 (N)` in the reader header, showing all questions and warm answers across the reading session.
- Automatically updates count badges whenever an interaction turn concludes.

## Verification Checklist

1. Open a story in `StoryPlayerActivity`.
2. Tap `🎤 我要說話`, ask a question (e.g., "為什麼白雪公主會去森林？"), and tap `⏹️ 說完了`.
3. Verify that the child query appears in the Live Interaction card.
4. Verify that teacher's answer text streams/renders on screen while audio plays.
5. Tap `💬 互動 (1)` in the top header and verify the full Q&A entry with page tag is listed.
6. Advance pages and confirm story playback resumes smoothly.
