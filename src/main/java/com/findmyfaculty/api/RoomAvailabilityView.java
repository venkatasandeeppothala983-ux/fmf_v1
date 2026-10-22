package com.findmyfaculty.api;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Schedule-based room information; it is not a live sensor reading. */
public class RoomAvailabilityView {
    public Long roomId;
    public String roomCode;
    public String building;
    public Integer capacity;
    public String status;
    public String statusLabel;
    public String explanation;
    public String day;
    public LocalTime checkedAt;
    public List<ScheduleItem> currentUses = new ArrayList<>();
    public ScheduleItem nextUse;
    public List<ScheduleItem> today = new ArrayList<>();

    public static class ScheduleItem {
        public String kind;
        public String subject;
        public String section;
        public String faculty;
        public Integer period;
        public LocalTime startTime;
        public LocalTime endTime;
    }
}
