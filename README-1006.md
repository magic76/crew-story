# crew-story-1006

First-run Content Experience + Story Context Engine v1.

Includes 10 bundled stories: 5 Crew Story originals and 5 newly written
adaptations based on old public-domain fairy-tale material.

Originals:
- 小恐龍不敢關燈
- 月亮掉進我的口袋
- 小狐狸第一次迷路
- 機器人為什麼不會哭
- 不想分享的小熊

New adaptations:
- 小紅帽：森林裡的選擇
- 青蛙與金球
- 四個不退休的音樂家
- 灰姑娘與午夜舞會
- 糖果屋外的麵包屑

Also adds:
- age / time / topic metadata
- built_in vs user ownership
- built-ins cannot be overwritten or deleted
- built-in -> Make my own copy
- first-run `和阿奇一起讀`
- StoryContext: currentEvent / revealedFacts / spoilerBoundary / interactionHints
- explicit anti-spoiler context sent to Archie
- old sample_snow_white filtered from shelf
- no third-party artwork; emoji covers only

Apply:
    python3 /path/to/crew-story-1006/apply-1006.py .
    ./gradlew assembleDebug

Validate push-to-talk, transcripts, Q&A history, built-in copy/edit behavior,
and ask Archie an early-page question to ensure it does not reveal later facts.

The classic prose in this package is newly written for Crew Story and does not
copy modern publisher translations or Disney dialogue. Review public-domain
status per distribution country before commercial release.
