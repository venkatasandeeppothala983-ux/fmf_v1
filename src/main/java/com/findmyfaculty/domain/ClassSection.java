package com.findmyfaculty.domain;
import javax.persistence.*;
@Entity
@Table(name="class_section",uniqueConstraints=@UniqueConstraint(name="uk_section_source",columnNames="source_key"))
public class ClassSection {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(name="source_key",nullable=false,length=40) public String sourceKey;
    @Column(name="year_label",nullable=false,length=40) public String year;
    @Column(name="semester_label",nullable=false,length=40) public String semester;
    @Column(name="section_label",nullable=false,length=60) public String section;
    @Column(name="class_incharge",length=180) public String classIncharge;
    @Column(name="source_file",length=180) public String sourceFile="Class Time Table Jul 2026.pdf";
    public String displayName(){return year+" Year · "+semester+" Semester · "+section;}
}
