package com.findmyfaculty.domain;
import javax.persistence.*;
import java.time.LocalTime;
@Entity
@Table(name="class_timetable_entry",indexes=@Index(name="idx_class_entry_section_day",columnList="section_id,day_name"))
public class ClassTimetableEntry {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(fetch=FetchType.EAGER,optional=false) @JoinColumn(name="section_id",nullable=false) public ClassSection section;
    @Column(name="day_name",nullable=false,length=12) public String day;
    @Column(name="period_number",nullable=false) public Integer period;
    @Column(name="start_time",nullable=false) public LocalTime startTime;
    @Column(name="end_time",nullable=false) public LocalTime endTime;
    @Column(nullable=false,length=240) public String subject;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "subject_id") public Subject subjectRef;
    @Column(length=180) public String room;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "classroom_id") public Classroom classroomRef;
    @Column(name="source_text",length=400) public String sourceText;
}
