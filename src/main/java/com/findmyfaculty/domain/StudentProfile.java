package com.findmyfaculty.domain;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_profile", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_number", columnNames = "student_number"),
    @UniqueConstraint(name = "uk_student_user", columnNames = "user_account_id")
}, indexes = @Index(name = "idx_student_department_year", columnList = "department_id, year_label"))
public class StudentProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @OneToOne(fetch = FetchType.EAGER, optional = true) @JoinColumn(name = "user_account_id") public UserAccount user;
    @Column(name = "full_name", length = 160) public String fullName;
    @Column(name = "student_number", nullable = false, length = 40) public String studentNumber;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "department_id") public Department department;
    @Column(name = "year_label", length = 40) public String yearLabel;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "section_id") public ClassSection section;
    @Column(length = 30) public String phone;
    @Column(nullable = false) public boolean active = true;
    @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt = LocalDateTime.now();
}
