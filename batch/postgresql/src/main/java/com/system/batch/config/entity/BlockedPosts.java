package com.system.batch.config.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 차단된 게시글 - 처형 결과 보고서
 */
@Entity
@Getter
@Table(name = "blocked_posts")
public class BlockedPosts {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blocked_posts_id_seq")
    @SequenceGenerator(name = "blocked_posts_id_seq", sequenceName = "blocked_posts_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    private String writer;
    private String title;

    @Column(name = "report_count")
    private int reportCount;

    @Column(name = "block_score")
    private double blockScore;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Builder
    public BlockedPosts(Long postId, String writer, String title,
                       int reportCount, double blockScore, LocalDateTime blockedAt) {
        this.postId = postId;
        this.writer = writer;
        this.title = title;
        this.reportCount = reportCount;
        this.blockScore = blockScore;
        this.blockedAt = blockedAt;
    }
}
