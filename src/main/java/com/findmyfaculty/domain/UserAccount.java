package com.findmyfaculty.domain;

import javax.persistence.*;

@Entity
@Table(name = "user_account")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 80)
    public String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Role role;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", unique = true)
    public Faculty faculty;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    public ClassSection section;
    @Column(name = "enabled", nullable = false)
    public boolean enabled = true;
}
