package com.findmyfaculty.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_status_update", indexes = @Index(name = "idx_status_faculty_updated", columnList = "faculty_id, updated_at"))
public class FacultyStatusUpdate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    public Faculty faculty;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public StatusType status;
    @Column(length = 180)
    public String location;
    @Column(length = 240)
    public String note;
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;
    @Column(name = "expected_return")
    public LocalDateTime expectedReturn;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    public UserAccount updatedBy;
}
