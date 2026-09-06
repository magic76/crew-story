package com.crewpocket.story;

public final class StoryPlayerUiState {
    public enum Mode {
        CONNECTING,
        READY,
        NARRATING,
        LISTENING,
        RESPONDING,
        PAUSED,
        ERROR,
        FINISHED,
        DISCONNECTED
    }

    public final Mode mode;
    public final String detail;

    private StoryPlayerUiState(Mode mode, String detail) {
        this.mode = mode;
        this.detail = detail == null ? "" : detail;
    }

    public static StoryPlayerUiState of(Mode mode) {
        return new StoryPlayerUiState(mode, "");
    }

    public static StoryPlayerUiState of(Mode mode, String detail) {
        return new StoryPlayerUiState(mode, detail);
    }
}
