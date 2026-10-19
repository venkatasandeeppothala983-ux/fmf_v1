package com.findmyfaculty.domain;

import javax.persistence.*;

@Entity
@Table(name = "subject", indexes = @Index(name = "idx_subject_department", columnList = "department_id"))
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(length = 40, unique = true) public String code;
    @Column(nullable = false, unique = true, length = 240) public String name;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "department_id") public Department department;
    @Column(nullable = false) public boolean active = true;
}
