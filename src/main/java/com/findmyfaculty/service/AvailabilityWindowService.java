package com.findmyfaculty.service;

import com.findmyfaculty.api.PotentialOpeningView;
import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.AppointmentRepository;
import com.findmyfaculty.repo.TimetableRepository;
import com.findmyfaculty.repo.StatusRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvailabilityWindowService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Kolkata");
    private final TimetableRepository timetable;private final AppointmentRepository appointments;private final StatusRepository statuses;
    public AvailabilityWindowService(TimetableRepository t,AppointmentRepository a,StatusRepository s){timetable=t;appointments=a;statuses=s;}
    public List<PotentialOpeningView> find(Faculty faculty,int minutes){
        int duration=Math.max(15,Math.min(120,minutes));LocalDateTime now=LocalDateTime.now(ZONE);LocalDate monday=now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));if(now.getDayOfWeek()==DayOfWeek.SUNDAY)monday=monday.plusWeeks(1);
        List<AppointmentRequest> requests=appointments.findByFaculty_IdOrderByCreatedAtDesc(faculty.id).stream().filter(a->a.state==AppointmentState.PENDING||a.state==AppointmentState.ACCEPTED).collect(Collectors.toList());
        FacultyStatusUpdate activeStatus=statuses.findFirstByFaculty_IdOrderByUpdatedAtDesc(faculty.id).orElse(null);LocalDateTime statusUntil=activeStatus==null?null:activeStatus.expectedReturn;boolean explicitUnavailable=activeStatus!=null&&activeStatus.status!=StatusType.AVAILABLE&&activeStatus.status!=StatusType.APPOINTMENT_ONLY;
        List<PotentialOpeningView> result=new ArrayList<>();
        for(int offset=0;offset<6&&result.size()<3;offset++){
            LocalDate date=monday.plusDays(offset);String day=date.getDayOfWeek().name();List<TimetableEntry> classes=timetable.findByFaculty_IdAndDayIgnoreCaseOrderByStartTimeAsc(faculty.id,day);
            LocalTime start=LocalTime.of(8,10),last=LocalTime.of(16,10);
            while(!start.plusMinutes(duration).isAfter(last)&&result.size()<3){final LocalTime candidate=start;LocalDateTime begins=date.atTime(candidate),ends=begins.plusMinutes(duration);if(begins.isAfter(now)&&!(candidate.isBefore(LocalTime.of(14,20))&&candidate.plusMinutes(duration).isAfter(LocalTime.of(13,20)))){
                boolean classConflict=classes.stream().anyMatch(e->candidate.isBefore(e.endTime)&&candidate.plusMinutes(duration).isAfter(e.startTime));
                boolean appointmentConflict=requests.stream().anyMatch(a->overlaps(begins,ends,a.requestedStart,a.requestedStart.plusMinutes(30)));
                boolean absentNow=explicitUnavailable&&statusUntil!=null&&statusUntil.isAfter(now)&&date.equals(now.toLocalDate())&&begins.isBefore(statusUntil);
                if(!classConflict&&!appointmentConflict&&!absentNow)result.add(new PotentialOpeningView(begins,duration));
            }start=start.plusMinutes(30);}
        }
        return result;
    }
    private boolean overlaps(LocalDateTime a,LocalDateTime b,LocalDateTime c,LocalDateTime d){return a.isBefore(d)&&b.isAfter(c);}
}
