package com.findmyfaculty.api;

import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.FacultyRepository;
import com.findmyfaculty.repo.StatusRepository;
import com.findmyfaculty.repo.UserRepository;
import com.findmyfaculty.service.AvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/api/faculty/{facultyId}/status")
public class StatusController {
    private final FacultyRepository facultyRepo; private final StatusRepository statusRepo; private final UserRepository userRepo; private final AvailabilityService availability;
    public StatusController(FacultyRepository f,StatusRepository s,UserRepository u,AvailabilityService a){facultyRepo=f;statusRepo=s;userRepo=u;availability=a;}
    @PostMapping public FacultyView update(@PathVariable Long facultyId,@Valid @RequestBody StatusInput input,Authentication authentication){
        UserAccount actor=userRepo.findByUsername(authentication.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Faculty faculty=facultyRepo.findById(facultyId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found"));
        if(actor.role!=Role.ADMIN&&(actor.role!=Role.FACULTY||actor.faculty==null||!actor.faculty.id.equals(facultyId)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned faculty member or an admin may update this status");
        StatusType type;String raw=input.status.trim().toUpperCase(Locale.ROOT);try{type="UNAVAILABLE".equals(raw)?StatusType.ABSENT:StatusType.valueOf(raw);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose Available, In a Meeting, On Duty, or Unavailable");}
        if(type!=StatusType.AVAILABLE&&type!=StatusType.MEETING&&type!=StatusType.ON_DUTY&&type!=StatusType.ABSENT)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose Available, In a Meeting, On Duty, or Unavailable");
        LocalDateTime now=LocalDateTime.now(availability.zone()),until;
        try{until=input.expectedReturn==null||input.expectedReturn.trim().isEmpty()?now.plusHours(1):LocalDateTime.parse(input.expectedReturn);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"expectedReturn must use ISO local date-time, for example 2026-10-18T14:30:00");}
        if(!until.isAfter(now))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"expectedReturn must be in the future");
        if(until.isAfter(now.plusDays(1)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status expiry cannot be more than 24 hours away");
        FacultyStatusUpdate update=new FacultyStatusUpdate();update.faculty=faculty;update.status=type;update.location=trim(input.location,180);update.note=trim(input.note,240);update.updatedAt=now;update.expectedReturn=until;update.updatedBy=actor;statusRepo.save(update);
        return availability.view(faculty);
    }
    @DeleteMapping public FacultyView clear(@PathVariable Long facultyId,Authentication authentication){
        UserAccount actor=userRepo.findByUsername(authentication.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Faculty faculty=facultyRepo.findById(facultyId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found"));
        if(actor.role!=Role.ADMIN&&(actor.role!=Role.FACULTY||actor.faculty==null||!actor.faculty.id.equals(facultyId)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only the assigned faculty member or an admin may update this status");
        statusRepo.findFirstByFaculty_IdOrderByUpdatedAtDesc(facultyId).ifPresent(update->{update.expectedReturn=LocalDateTime.now(availability.zone());statusRepo.save(update);});
        return availability.view(faculty);
    }
    private String trim(String s,int max){if(s==null)return null;s=s.trim();if(s.isEmpty())return null;return s.length()>max?s.substring(0,max):s;}
}
