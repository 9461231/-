package com.muxu.supermarket.stocktake.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 盘点任务
 */
@Data
@Entity
@Table(name = "stocktake")
public class Stocktake {

    public static final int STATUS_IN_PROGRESS = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_CANCELLED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_no", nullable = false, length = 64, unique = true)
    private String taskNo;

    /** 0-进行中 1-已完成 2-已取消 */
    @Column(nullable = false)
    private Integer status = STATUS_IN_PROGRESS;

    @Column(length = 255)
    private String remark;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
