package com.findmyfaculty.api;

import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.AppointmentRepository;
import com.findmyfaculty.repo.FacultyRepository;
import com.findmyfaculty.repo.UserRepository;
import com.findmyfaculty.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AppointmentController {
    private final AppointmentService service;private final FacultyRepository facultyRepo;private final UserRepository userRepo;private final AppointmentRepository appointmentRepo;private final com.findmyfaculty.repo.PortalNotificationRepository notifications;
    public AppointmentController(AppointmentService s,FacultyRepository f,UserRepository u,AppointmentRepository a,com.findmyfaculty.repo.PortalNotificationRepository n){service=s;facultyRepo=f;userRepo=u;appointmentRepo=a;notifications=n;}
    @PostMapping("/appointments") @ResponseStatus(HttpStatus.CREATED)
    public AppointmentView request(@Valid @RequestBody AppointmentInput input,Authentication auth){
        UserAccount student=current(auth);if(student.role!=Role.STUDENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only students can request appointments");
        Faculty faculty=facultyRepo.findById(input.facultyId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found"));if(Boolean.FALSE.equals(faculty.active))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty not found");
        LocalDateTime requested=parse(input.requestedStart,"requestedStart");if(!requested.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"requestedStart must be in the future");
        AppointmentRequest a=new AppointmentRequest();a.faculty=faculty;a.student=student;a.requestedStart=requested;a.reason=input.reason.trim();a.createdAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));a.state=AppointmentState.PENDING;
        if(service.conflicts(faculty,requested))a.conflictWarning="Timetable shows a teaching period overlapping this requested time.";
        AppointmentRequest saved=service.save(a);userRepo.findByFaculty_Id(faculty.id).ifPresent(account->notifyUser(account,"New meeting request","A student requested a meeting.","/faculty/appointments"));return new AppointmentView(saved);
    }
    @GetMapping("/appointments/mine") public List<AppointmentView> mine(Authentication auth){UserAccount user=current(auth);List<AppointmentRequest> rows=user.role==Role.ADMIN?service.all():user.role==Role.FACULTY&&user.faculty!=null?service.forFaculty(user.faculty.id):service.forStudent(user.id);return rows.stream().map(AppointmentView::new).collect(Collectors.toList());}
    @GetMapping("/faculty/{facultyId}/appointments") public List<AppointmentView> facultyRequests(@PathVariable Long facultyId,Authentication auth){UserAccount actor=current(auth);if(actor.role==Role.FACULTY&&(actor.faculty==null||!actor.faculty.id.equals(facultyId)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Faculty may view only their own requests");if(actor.role!=Role.FACULTY&&actor.role!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN);return service.forFaculty(facultyId).stream().map(AppointmentView::new).collect(Collectors.toList());}
    @PutMapping("/appointments/{id}/decision") public AppointmentView decide(@PathVariable Long id,@Valid @RequestBody DecisionInput input,Authentication auth){
        UserAccount actor=current(auth);AppointmentRequest a=service.require(id);if(actor.role==Role.FACULTY&&(actor.faculty==null||!actor.faculty.id.equals(a.faculty.id)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Faculty may decide only on their own requests");if(actor.role!=Role.FACULTY&&actor.role!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(a.state!=AppointmentState.PENDING)throw new ResponseStatusException(HttpStatus.CONFLICT,"This request has already been handled");String d=input.decision.trim().toUpperCase(Locale.ROOT);
        if("ACCEPTED".equals(d)){if(service.conflicts(a.faculty,a.requestedStart))throw new ResponseStatusException(HttpStatus.CONFLICT,"The request overlaps a scheduled class. Suggest another time instead.");a.state=AppointmentState.ACCEPTED;}
        else if("REJECTED".equals(d)){a.state=AppointmentState.REJECTED;}
        else if("SUGGESTED".equals(d)){if(input.suggestedStart==null||input.suggestedStart.trim().isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"suggestedStart is required for SUGGESTED");LocalDateTime suggestion=parse(input.suggestedStart,"suggestedStart");if(!suggestion.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"suggestedStart must be in the future");if(service.conflicts(a.faculty,suggestion))throw new ResponseStatusException(HttpStatus.CONFLICT,"The suggested time overlaps a scheduled class");a.suggestedStart=suggestion;a.state=AppointmentState.SUGGESTED;}
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"decision must be ACCEPTED, REJECTED, or SUGGESTED");a.updatedAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));AppointmentRequest saved=service.save(a);notifyUser(a.student,"Meeting request updated","Your meeting request is now "+a.state.name().toLowerCase(Locale.ROOT)+".","/student/appointments");return new AppointmentView(saved);
    }
    @PostMapping("/appointments/{id}/confirm-suggestion") public AppointmentView confirmSuggestion(@PathVariable Long id,Authentication auth){UserAccount student=current(auth);AppointmentRequest a=service.require(id);if(student.role!=Role.STUDENT||!a.student.id.equals(student.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN);if(a.state!=AppointmentState.SUGGESTED||a.suggestedStart==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"There is no suggested time to confirm");if(!a.suggestedStart.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))))throw new ResponseStatusException(HttpStatus.CONFLICT,"The suggested time has passed");if(service.conflicts(a.faculty,a.suggestedStart))throw new ResponseStatusException(HttpStatus.CONFLICT,"The suggested time now conflicts with the timetable");a.requestedStart=a.suggestedStart;a.state=AppointmentState.ACCEPTED;a.updatedAt=LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));AppointmentRequest saved=service.save(a);userRepo.findByFaculty_Id(a.faculty.id).ifPresent(account->notifyUser(account,"Meeting time confirmed","The student confirmed the suggested meeting time.","/faculty/appointments"));return new AppointmentView(saved);}
    private UserAccount current(Authentication auth){return userRepo.findByUsername(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
    private void notifyUser(UserAccount recipient,String title,String message,String target){com.findmyfaculty.domain.PortalNotification n=new com.findmyfaculty.domain.PortalNotification();n.user=recipient;n.title=title;n.message=message;n.targetPath=target;notifications.save(n);}
    private LocalDateTime parse(String value,String field){try{return LocalDateTime.parse(value);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,field+" must be ISO local date-time, e.g. 2026-10-20T14:30:00");}}
}
