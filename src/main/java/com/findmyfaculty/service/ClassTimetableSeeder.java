package com.findmyfaculty.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE+1)
public class ClassTimetableSeeder implements ApplicationRunner {
    private final ClassSectionRepository sections;private final ClassTimetableRepository entries;private final ClassCourseRepository courses;private final ObjectMapper mapper;
    public ClassTimetableSeeder(ClassSectionRepository s,ClassTimetableRepository e,ClassCourseRepository c,ObjectMapper m){sections=s;entries=e;courses=c;mapper=m;}
    @Override @Transactional public void run(ApplicationArguments args)throws Exception{if(sections.count()>0)return;InputStream source=getClass().getResourceAsStream("/seed/class-timetable.json");if(source==null)throw new IllegalStateException("Missing seed/class-timetable.json");List<SeedSection> seeds=mapper.readValue(source,new TypeReference<List<SeedSection>>(){});for(SeedSection raw:seeds){ClassSection section=new ClassSection();section.sourceKey=raw.id;section.year=raw.year;section.semester=raw.semester;section.section=raw.section;section.classIncharge=raw.classIncharge;section.sourceFile=raw.source;section=sections.save(section);if(raw.schedule!=null)for(SeedDay day:raw.schedule)for(SeedSlot slot:day.slots){ClassTimetableEntry e=new ClassTimetableEntry();e.section=section;e.day=day.day;e.period=slot.period;e.startTime=LocalTime.parse(slot.start);e.endTime=LocalTime.parse(slot.end);e.subject=slot.subject;e.room=slot.room;e.sourceText=slot.sourceText;entries.save(e);}if(raw.courses!=null)for(SeedCourse course:raw.courses){ClassCourse c=new ClassCourse();c.section=section;c.code=course.code;c.name=course.name;c.facultyName=course.faculty;courses.save(c);}}}
    public static class SeedSection{public String id;public String year;public String semester;public String section;public String classIncharge;public String source;public List<SeedDay> schedule;public List<SeedCourse> courses;}
    public static class SeedDay{public String day;public List<SeedSlot> slots;}
    public static class SeedSlot{public Integer period;public String start;public String end;public String subject;public String room;public String sourceText;}
    public static class SeedCourse{public String code;public String name;public String faculty;}
}
