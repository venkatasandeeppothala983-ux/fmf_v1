package com.findmyfaculty.service;

import com.findmyfaculty.api.RoomAvailabilityView;
import com.findmyfaculty.domain.Classroom;
import com.findmyfaculty.domain.TimetableEntry;
import com.findmyfaculty.repo.ClassTimetableRepository;
import com.findmyfaculty.repo.ClassroomRepository;
import com.findmyfaculty.repo.TimetableRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomAvailabilityServiceTest {
    @Test void distinguishesScheduledNoListedClassAndMissingRoomTimetable() {
        Classroom scheduledRoom = room(1L, "DELL Lab");
        Classroom roomWithPastEntry = room(2L, "Computer Lab -2");
        Classroom roomWithoutTimetable = room(3L, "Architecture Computer Lab-4");

        TimetableEntry active = slot("DELL Lab", "TUESDAY", 10, 10, 11, 10, "NLP Lab");
        TimetableEntry past = slot("Computer Lab -2", "TUESDAY", 9, 10, 10, 10, "Computer Networks Lab");

        ClassroomRepository classrooms = mock(ClassroomRepository.class);
        TimetableRepository staff = mock(TimetableRepository.class);
        ClassTimetableRepository classes = mock(ClassTimetableRepository.class);
        when(classrooms.findAll()).thenReturn(Arrays.asList(scheduledRoom, roomWithPastEntry, roomWithoutTimetable));
        when(staff.findAll()).thenReturn(Arrays.asList(active, past));
        when(classes.findAll()).thenReturn(Collections.emptyList());

        RoomAvailabilityService service = new RoomAvailabilityService(classrooms, staff, classes, "Asia/Kolkata");
        List<RoomAvailabilityView> result = service.snapshotFor(DayOfWeek.TUESDAY, LocalTime.of(10, 30));

        assertEquals("UNKNOWN", result.get(0).status); // Architecture Computer Lab-4 has no timetable rows.
        assertEquals("NO_CLASS_LISTED_NOW", result.get(1).status); // Its listed period has ended.
        assertEquals("SCHEDULED_NOW", result.get(2).status);
        assertEquals(1, result.get(2).currentUses.size());
    }

    @Test void treatsPeriodEndAsOutsideTheScheduledInterval() {
        Classroom room = room(1L, "DELL Lab");
        TimetableEntry slot = slot("DELL Lab", "TUESDAY", 10, 10, 11, 10, "NLP Lab");
        ClassroomRepository classrooms = mock(ClassroomRepository.class);
        TimetableRepository staff = mock(TimetableRepository.class);
        ClassTimetableRepository classes = mock(ClassTimetableRepository.class);
        when(classrooms.findAll()).thenReturn(Collections.singletonList(room));
        when(staff.findAll()).thenReturn(Collections.singletonList(slot));
        when(classes.findAll()).thenReturn(Collections.emptyList());

        RoomAvailabilityService service = new RoomAvailabilityService(classrooms, staff, classes, "Asia/Kolkata");
        List<RoomAvailabilityView> result = service.snapshotFor(DayOfWeek.TUESDAY, LocalTime.of(11, 10));

        assertEquals("NO_CLASS_LISTED_NOW", result.get(0).status);
        assertEquals(0, result.get(0).currentUses.size());
    }

    private Classroom room(Long id, String code) {
        Classroom room = new Classroom();
        room.id = id;
        room.code = code;
        room.active = true;
        return room;
    }

    private TimetableEntry slot(String room, String day, int startHour, int startMinute,
                                int endHour, int endMinute, String subject) {
        TimetableEntry entry = new TimetableEntry();
        entry.room = room;
        entry.day = day;
        entry.period = 1;
        entry.startTime = LocalTime.of(startHour, startMinute);
        entry.endTime = LocalTime.of(endHour, endMinute);
        entry.subject = subject;
        return entry;
    }
}
