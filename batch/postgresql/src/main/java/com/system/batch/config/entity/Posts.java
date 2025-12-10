package com.system.batch.config.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 엔티티 - 검열 대상
 */
@Entity
@Table(name = "posts")
@NamedQuery(
        name = "Post.findByReportsReportedAtBetween",
        query = "SELECT p FROM Posts p JOIN FETCH p.reports r WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime"
)
@Getter
public class Posts {
    @Id
    private Long id;
    private String title;         // 게시물 제목
    private String content;       // 게시물 내용
    private String writer;        // 작성자

    @OneToMany(mappedBy = "posts")
    private List<Reports> reports = new ArrayList<>();
}