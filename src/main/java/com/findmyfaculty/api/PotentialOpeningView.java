package com.findmyfaculty.api;
import java.time.LocalDateTime;
public class PotentialOpeningView {
    public LocalDateTime start;
    public LocalDateTime end;
    public String basis="TIMETABLE_GAP";
    public String certainty="UNCONFIRMED";
    public String note="No class or appointment is listed for this time; faculty presence is not confirmed.";
    public PotentialOpeningView(LocalDateTime start,int minutes){this.start=start;this.end=start.plusMinutes(minutes);}
}
