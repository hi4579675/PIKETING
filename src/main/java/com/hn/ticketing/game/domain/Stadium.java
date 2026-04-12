package com.hn.ticketing.game.domain;

import com.hn.ticketing.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// ── 구장 엔티티 ──
// 이 프로젝트는 대전 신구장(한화생명 이글스파크) 단일 구장.
//
// 왜 단일 구장인데 엔티티를 만드나:
//   하드코딩하면 구장 정보를 바꿀 때 코드 수정이 필요.
//   엔티티로 두면 DB INSERT 한 번으로 끝.
//   또한 Section이 stadium_id FK를 가지므로 정규화된 구조 유지.
@Entity
@Table(name = "stadium")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stadium extends BaseEntity {

    // ── 필드 ──

    // 구장 이름. 예: "대전 한화생명 이글스파크"
    // nullable = false → DB 레벨 NOT NULL 제약.
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // 구장 주소.
    // length = 200 → 주소는 길 수 있으므로 넉넉히.
    @Column(name = "address", nullable = false, length = 200)
    private String address;

    // ── 연관관계 ──

    // 이 구장에 속한 구역 목록.
    //   양방향 관계를 거는 이유:
    //   Stadium에서 구역 목록을 조회할 일이 있으므로 (좌석 배치도 등).
    @OneToMany(mappedBy = "stadium")
    private List<Section> sections = new ArrayList<>();

    // ── 생성자 ──
    @Builder
    private Stadium(String name, String address) {
        this.name = name;
        this.address = address;
    }
}
