package com.hn.ticketing.game.domain;

// ── 좌석 등급 enum ──
// Section(구역)마다 하나의 등급이 붙는다.
// 한화생명 볼파크 기준으로 대표적인 등급만 정의.
// 등급별 기본 가격은 여기가 아니라 GameSeat에서 경기마다 따로 매긴다.
// (같은 테이블석이라도 한국시리즈와 평일 경기의 가격이 다르니까)
public enum SeatGrade {

    CENTRAL("중앙 지정석"),
    CENTRAL_TABLE("중앙 탁자석"),
    CENTRAL_WHEELCHAIR("중앙 휠체어석"),
    CATCHER_BEHIND("포수 후면석"),
    INFIELD_A("내야 지정석A"),
    INFIELD_B("내야 지정석B"),
    INFIELD_TABLE("내야 탁자석(4층)"),
    INFIELD_WHEELCHAIR("내야 휠체어석"),
    SPLASH_JACUZZI("스플래쉬 자쿠지(인피니티 풀)"),
    SPLASH_CARAVAN("스플래쉬 카라반(인피니티 풀)"),
    CASS_ZONE("카스존(응원단석)"),
    INNINGS_VIP("이닝스 VIP 바 & 룸/테라스"),
    SKY_BOX("스카이박스");

    private final String displayName;

    // ── 생성자 ──
    // enum 생성자는 private이 기본. 외부에서 new 불가.
    SeatGrade(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
}
