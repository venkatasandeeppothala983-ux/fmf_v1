package com.findmyfaculty.service;

import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.AppointmentRepository;
import com.findmyfaculty.repo.TimetableRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments; private final TimetableRepository timetable;
    public AppointmentService(AppointmentRepository a,TimetableRepository t){appointments=a;timetable=t;}
    public boolean conflicts(Faculty faculty,LocalDateTime start){String day=start.getDayOfWeek().name();LocalDateTime end=start.plusMinutes(30);return timetable.findByFaculty_IdAndDayIgnoreCaseOrderByStartTimeAsc(faculty.id,day).stream().anyMatch(e->start.toLocalTime().isBefore(e.endTime)&&end.toLocalTime().isAfter(e.startTime));}
    public List<AppointmentRequest> forFaculty(Long id){return appointments.findByFaculty_IdOrderByCreatedAtDesc(id);}
    public List<AppointmentRequest> forStudent(Long id){return appointments.findByStudent_IdOrderByCreatedAtDesc(id);}
    public AppointmentRequest require(Long id){return appointments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Appointment request not found"));}
    public AppointmentRequest save(AppointmentRequest a){return appointments.save(a);}
    public List<AppointmentRequest> all(){return appointments.findAllByOrderByCreatedAtDesc();}
}
