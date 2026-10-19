package com.findmyfaculty.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faculty")
public class Faculty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "source_key", nullable = false, unique = true, length = 40)
    public String sourceKey;
    @Column(nullable = false, length = 160)
    public String name;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    public Department departmentRef;
    /** Compatibility label for legacy imported profiles; new edits should link departmentRef. */
    @Column(length = 140)
    public String department;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "faculty_subject", joinColumns = @JoinColumn(name = "faculty_id"), inverseJoinColumns = @JoinColumn(name = "subject_id"), uniqueConstraints = @UniqueConstraint(name = "uk_faculty_subject", columnNames = {"faculty_id", "subject_id"}))
    public java.util.Set<Subject> subjects = new java.util.LinkedHashSet<>();
    @Column(nullable = true)
    public Boolean active = true;
    @Column(length = 100)
    public String cabin;
    @Column(length = 180)
    public String email;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "faculty_course", joinColumns = @JoinColumn(name = "faculty_id"))
    @Column(name = "course_name", length = 240)
    public List<String> courses = new ArrayList<>();
}
