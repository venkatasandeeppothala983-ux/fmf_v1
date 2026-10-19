package com.findmyfaculty.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcement", indexes = {
    @Index(name = "idx_announcement_publish_window", columnList = "published, starts_at, ends_at"),
    @Index(name = "idx_announcement_audience", columnList = "audience")
})
public class Announcement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 160) public String title;
    @Column(nullable = false, length = 4000) public String body;
    @Column(nullable = false, length = 16) public String audience = "ALL";
    @Column(nullable = false) public boolean published = true;
    @Column(name = "starts_at") public LocalDateTime startsAt;
    @Column(name = "ends_at") public LocalDateTime endsAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_user_id") public UserAccount createdBy;
    @Column(name = "created_at", nullable = false) public LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") public LocalDateTime updatedAt;
}
