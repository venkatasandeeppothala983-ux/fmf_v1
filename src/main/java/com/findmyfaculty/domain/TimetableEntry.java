package com.findmyfaculty.domain;

import javax.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_entry", indexes = {
    @Index(name = "idx_timetable_faculty_day", columnList = "faculty_id, day_name"),
    @Index(name = "idx_timetable_day_time", columnList = "day_name, start_time")
})
public class TimetableEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    public Faculty faculty;
    @Column(name = "day_name", nullable = false, length = 12)
    public String day;
    @Column(name = "period_number", nullable = false)
    public Integer period;
    @Column(name = "start_time", nullable = false)
    public LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    public LocalTime endTime;
    @Column(name = "subject_label", nullable = false, length = 240)
    public String subject;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "subject_id") public Subject subjectRef;
    @Column(length = 100)
    public String room;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "classroom_id") public Classroom classroomRef;
    @Column(name = "source_file", length = 180)
    public String sourceFile = "Staff Time table.pdf";
}
