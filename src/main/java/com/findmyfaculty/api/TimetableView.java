package com.findmyfaculty.api;
import com.findmyfaculty.domain.TimetableEntry;
import java.time.LocalTime;
public class TimetableView {
    public Long id;
    public String day;
    public Integer period;
    public LocalTime startTime;
    public LocalTime endTime;
    public String subject;
    public String room;
    public TimetableView(TimetableEntry e) { id=e.id; day=e.day; period=e.period; startTime=e.startTime; endTime=e.endTime; subject=e.subjectRef!=null?e.subjectRef.name:e.subject; room=e.classroomRef!=null?e.classroomRef.code:e.room; }
}
