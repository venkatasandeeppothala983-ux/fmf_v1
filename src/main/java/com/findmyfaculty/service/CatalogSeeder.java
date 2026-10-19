package com.findmyfaculty.service;

import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class CatalogSeeder implements ApplicationRunner {
    private final FacultyRepository faculty;
    private final TimetableRepository staffSlots;
    private final ClassCourseRepository classCourses;
    private final ClassTimetableRepository classSlots;
    private final SubjectRepository subjects;
    private final ClassroomRepository rooms;

    public CatalogSeeder(FacultyRepository faculty, TimetableRepository staffSlots, ClassCourseRepository classCourses,
                         ClassTimetableRepository classSlots, SubjectRepository subjects, ClassroomRepository rooms) {
        this.faculty = faculty; this.staffSlots = staffSlots; this.classCourses = classCourses;
        this.classSlots = classSlots; this.subjects = subjects; this.rooms = rooms;
    }

    @Override @Transactional public void run(ApplicationArguments args) {
        for (Faculty f : faculty.findAll()) {
            if (f.active == null) f.active = true;
            if (f.courses != null) for (String name : f.courses) {
                Subject subject = subjectFor(name);
                if (subject != null) f.subjects.add(subject);
            }
            faculty.save(f);
        }
        for (TimetableEntry entry : staffSlots.findAll()) {
            entry.subjectRef = subjectFor(entry.subject);
            entry.classroomRef = roomFor(entry.room);
            staffSlots.save(entry);
        }
        for (ClassCourse course : classCourses.findAll()) {
            course.subjectRef = subjectFor(course.name);
            classCourses.save(course);
        }
        for (ClassTimetableEntry entry : classSlots.findAll()) {
            entry.subjectRef = subjectFor(entry.subject);
            entry.classroomRef = roomFor(entry.room);
            classSlots.save(entry);
        }
    }

    private Subject subjectFor(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return subjects.findByNameIgnoreCase(name.trim()).orElseGet(() -> {
            Subject subject = new Subject(); subject.name = name.trim(); subject.active = true;
            return subjects.save(subject);
        });
    }

    private Classroom roomFor(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        return rooms.findByCodeIgnoreCase(code.trim()).orElseGet(() -> {
            Classroom room = new Classroom(); room.code = code.trim(); room.active = true;
            return rooms.save(room);
        });
    }
}
