package com.findmyfaculty.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "portal_notification", indexes = @Index(name = "idx_notification_user_created", columnList = "user_account_id, created_at"))
public class PortalNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_account_id", nullable = false) public UserAccount user;
    @Column(nullable = false, length = 140) public String title;
    @Column(nullable = false, length = 1000) public String message;
    @Column(name = "target_path", length = 240) public String targetPath;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "read_at") public LocalDateTime readAt;
}
