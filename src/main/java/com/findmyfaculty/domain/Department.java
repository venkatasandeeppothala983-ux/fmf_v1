package com.findmyfaculty.domain;

import javax.persistence.*;

@Entity
@Table(name = "department", uniqueConstraints = {
    @UniqueConstraint(name = "uk_department_code", columnNames = "code"),
    @UniqueConstraint(name = "uk_department_name", columnNames = "name")
})
public class Department {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 24) public String code;
    @Column(nullable = false, length = 140) public String name;
    @Column(length = 500) public String description;
    @Column(nullable = false) public boolean active = true;
}
