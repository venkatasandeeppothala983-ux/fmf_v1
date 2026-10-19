package com.findmyfaculty.domain;

import javax.persistence.*;

@Entity
@Table(name = "classroom", uniqueConstraints = @UniqueConstraint(name = "uk_classroom_code", columnNames = "code"))
public class Classroom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 80) public String code;
    @Column(length = 120) public String building;
    public Integer capacity;
    @Column(nullable = false) public boolean active = true;
}
