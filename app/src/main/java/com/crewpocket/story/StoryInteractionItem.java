package com.crewpocket.story;

import java.io.Serializable;

/**
 * 1004: Represents a single Q&A interaction between the child and the storyteller.
 */
public class StoryInteractionItem implements Serializable {
    public int pageIndex;
    public String childTranscript;
    public String teacherAnswer;
    public long timestamp;

    public StoryInteractionItem() {
        this.timestamp = System.currentTimeMillis();
    }

    public StoryInteractionItem(int pageIndex, String childTranscript, String teacherAnswer) {
        this.pageIndex = pageIndex;
        this.childTranscript = childTranscript != null ? childTranscript.trim() : "";
        this.teacherAnswer = teacherAnswer != null ? teacherAnswer.trim() : "";
        this.timestamp = System.currentTimeMillis();
    }
}
