package com.findmyfaculty.service;

import com.findmyfaculty.api.RoomAvailabilityView;
import com.findmyfaculty.domain.ClassTimetableEntry;
import com.findmyfaculty.domain.Classroom;
import com.findmyfaculty.domain.TimetableEntry;
import com.findmyfaculty.repo.ClassTimetableRepository;
import com.findmyfaculty.repo.ClassroomRepository;
import com.findmyfaculty.repo.TimetableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RoomAvailabilityService {
    private final ClassroomRepository classrooms;
    private final TimetableRepository staffTimetables;
    private final ClassTimetableRepository classTimetables;
    private final ZoneId zone;

    public RoomAvailabilityService(
            ClassroomRepository classrooms,
            TimetableRepository staffTimetables,
            ClassTimetableRepository classTimetables,
            @Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.classrooms = classrooms;
        this.staffTimetables = staffTimetables;
        this.classTimetables = classTimetables;
        this.zone = ZoneId.of(timezone);
    }

    public List<RoomAvailabilityView> currentSnapshot() {
        ZonedDateTime now = ZonedDateTime.now(zone);
        return snapshotFor(now.getDayOfWeek(), now.toLocalTime());
    }

    List<RoomAvailabilityView> snapshotFor(DayOfWeek day, LocalTime now) {
        String dayName = day.name();
        List<TimetableEntry> staffRows = staffTimetables.findAll();
        List<ClassTimetableEntry> classRows = classTimetables.findAll();

        return classrooms.findAll().stream()
                .filter(room -> room.active)
                .sorted(Comparator.comparing(
                        room -> room.code == null ? "" : room.code.toLowerCase(Locale.ROOT)))
                .map(room -> buildRoomView(room, dayName, now, staffRows, classRows))
                .collect(Collectors.toList());
    }

    private RoomAvailabilityView buildRoomView(
            Classroom room,
            String day,
            LocalTime now,
            List<TimetableEntry> staffRows,
            List<ClassTimetableEntry> classRows) {
        RoomAvailabilityView view = new RoomAvailabilityView();
        view.roomId = room.id;
        view.roomCode = room.code;
        view.building = room.building;
        view.capacity = room.capacity;
        view.day = day;
        view.checkedAt = now;

        String key = roomKey(room.code);
        boolean ambiguous = room.code != null && room.code.contains("/");
        List<RoomAvailabilityView.ScheduleItem> allRoomRows = new ArrayList<>();
        List<RoomAvailabilityView.ScheduleItem> todaysRows = new ArrayList<>();

        for (TimetableEntry entry : staffRows) {
            if (!key.isEmpty()
                    && key.equals(roomKey(entry.classroomRef != null
                            ? entry.classroomRef.code : entry.room))) {
                RoomAvailabilityView.ScheduleItem item = fromStaff(entry);
                allRoomRows.add(item);
                if (day.equalsIgnoreCase(entry.day)) {
                    todaysRows.add(item);
                }
            }
        }

        for (ClassTimetableEntry entry : classRows) {
            if (!key.isEmpty()
                    && key.equals(roomKey(entry.classroomRef != null
                            ? entry.classroomRef.code : entry.room))) {
                RoomAvailabilityView.ScheduleItem item = fromClass(entry);
                allRoomRows.add(item);
                if (day.equalsIgnoreCase(entry.day)) {
                    todaysRows.add(item);
                }
            }
        }

        todaysRows.sort(Comparator.comparing(item -> item.startTime));
        view.today = todaysRows;
        view.currentUses = todaysRows.stream()
                .filter(item -> !now.isBefore(item.startTime) && now.isBefore(item.endTime))
                .collect(Collectors.toList());
        view.nextUse = todaysRows.stream()
                .filter(item -> item.startTime.isAfter(now))
                .min(Comparator.comparing(item -> item.startTime))
                .orElse(null);

        if (ambiguous) {
            view.status = "UNKNOWN";
            view.statusLabel = "Room needs confirmation";
            view.explanation =
                    "This room record combines multiple room names, so it cannot be matched to one physical room.";
        } else if (!view.currentUses.isEmpty()) {
            view.status = "SCHEDULED_NOW";
            view.statusLabel = "Scheduled now";
            view.explanation =
                    "A class timetable lists this room at the current time. This is not a live occupancy reading.";
        } else if (!allRoomRows.isEmpty()) {
            view.status = "NO_CLASS_LISTED_NOW";
            view.statusLabel = "No class listed now";
            view.explanation =
                    "No timetable entry lists this room at the current time. The portal cannot verify physical occupancy.";
        } else {
            view.status = "UNKNOWN";
            view.statusLabel = "Unknown";
            view.explanation =
                    "No timetable entries are recorded for this room, so its schedule-based status cannot be determined.";
        }

        return view;
    }

    private RoomAvailabilityView.ScheduleItem fromStaff(TimetableEntry entry) {
        RoomAvailabilityView.ScheduleItem item = new RoomAvailabilityView.ScheduleItem();
        item.kind = "Faculty timetable";
        item.subject = entry.subjectRef != null ? entry.subjectRef.name : entry.subject;
        item.faculty = entry.faculty == null ? null : entry.faculty.name;
        item.period = entry.period;
        item.startTime = entry.startTime;
        item.endTime = entry.endTime;
        return item;
    }

    private RoomAvailabilityView.ScheduleItem fromClass(ClassTimetableEntry entry) {
        RoomAvailabilityView.ScheduleItem item = new RoomAvailabilityView.ScheduleItem();
        item.kind = "Class timetable";
        item.subject = entry.subjectRef != null ? entry.subjectRef.name : entry.subject;
        item.section = entry.section == null ? null : entry.section.displayName();
        item.period = entry.period;
        item.startTime = entry.startTime;
        item.endTime = entry.endTime;
        return item;
    }

    private String roomKey(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}