package com.findmyfaculty.domain;
import javax.persistence.*;
@Entity
@Table(name="class_course",indexes=@Index(name="idx_class_course_section",columnList="section_id"))
public class ClassCourse {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="section_id",nullable=false) public ClassSection section;
    @Column(name="course_code",length=120) public String code;
    @Column(name="course_name",length=320) public String name;
    @Column(name="faculty_name",length=180) public String facultyName;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="subject_id") public Subject subjectRef;
}
