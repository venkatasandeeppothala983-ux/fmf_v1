package com.findmyfaculty.api;
import com.findmyfaculty.domain.ClassTimetableEntry;
import java.time.LocalTime;
public class ClassTimetableView {
    public Long id;public Long sectionId;public String section;public String day;public Integer period;public LocalTime startTime;public LocalTime endTime;public String subject;public String room;public String sourceText;
    public ClassTimetableView(ClassTimetableEntry e){id=e.id;sectionId=e.section.id;section=e.section.displayName();day=e.day;period=e.period;startTime=e.startTime;endTime=e.endTime;subject=e.subjectRef!=null?e.subjectRef.name:e.subject;room=e.classroomRef!=null?e.classroomRef.code:e.room;sourceText=e.sourceText;}
}
