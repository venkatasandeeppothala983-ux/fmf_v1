package com.findmyfaculty.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_request", indexes = {
    @Index(name = "idx_appointment_faculty_state", columnList = "faculty_id, state"),
    @Index(name = "idx_appointment_student", columnList = "student_id")
})
public class AppointmentRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    public Faculty faculty;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    public UserAccount student;
    @Column(name = "requested_start", nullable = false)
    public LocalDateTime requestedStart;
    @Column(name = "suggested_start")
    public LocalDateTime suggestedStart;
    @Column(nullable = false, length = 500)
    public String reason;
    @Column(name = "conflict_warning", length = 240)
    public String conflictWarning;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public AppointmentState state = AppointmentState.PENDING;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
