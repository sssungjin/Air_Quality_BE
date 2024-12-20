package com.sungjin.airquailitymonitordemo.entity;

import com.sungjin.airquailitymonitordemo.entity.enums.TransmissionMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "device")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {
    @Id
    private String deviceId;

    @Column(length = 50)
    private String placeType;

    @Column
    private Integer floorLevel;

    @Column
    private String description;

    @Column
    private String userName;

    @Column
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_mode")
    private TransmissionMode transmissionMode = TransmissionMode.REALTIME;

    @Column(name = "upload_interval")
    private Integer uploadInterval; // 추가된 필드

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}