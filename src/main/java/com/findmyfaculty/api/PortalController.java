package com.findmyfaculty.api;

import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.*;
import com.findmyfaculty.service.AvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PortalController {
    private static final ZoneId ZONE=ZoneId.of("Asia/Kolkata");
    private final UserRepository users;private final StudentProfileRepository students;private final ClassTimetableRepository classSlots;
    private final TimetableRepository staffSlots;private final AnnouncementRepository announcements;private final PortalNotificationRepository notifications;
    private final AvailabilityService availability;
    public PortalController(UserRepository users,StudentProfileRepository students,ClassTimetableRepository classSlots,
                            TimetableRepository staffSlots,AnnouncementRepository announcements,PortalNotificationRepository notifications,
                            AvailabilityService availability){this.users=users;this.students=students;this.classSlots=classSlots;this.staffSlots=staffSlots;this.announcements=announcements;this.notifications=notifications;this.availability=availability;}

    @GetMapping("/students/me") public Map<String,Object> studentProfile(Authentication auth){
        UserAccount u=current(auth);requireRole(u,Role.STUDENT);StudentProfile p=students.findByUser_Id(u.id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile has not been assigned"));return studentView(p);
    }
    @PutMapping("/students/me") public Map<String,Object> updateStudentProfile(@Valid @RequestBody AdminInputs.ProfileInput input,Authentication auth){
        UserAccount u=current(auth);requireRole(u,Role.STUDENT);StudentProfile p=students.findByUser_Id(u.id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile has not been assigned"));
        p.phone=input.phone==null||input.phone.trim().isEmpty()?null:input.phone.trim();p.updatedAt=LocalDateTime.now(ZONE);return studentView(students.save(p));
    }
    @GetMapping("/students/me/timetable") public List<ClassTimetableView> studentTimetable(Authentication auth){
        UserAccount u=current(auth);requireRole(u,Role.STUDENT);StudentProfile p=students.findByUser_Id(u.id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile has not been assigned"));
        if(p.section==null)return Collections.emptyList();return classSlots.findBySection_IdOrderByDayAscStartTimeAsc(p.section.id).stream().sorted(Comparator.comparingInt((ClassTimetableEntry e)->dayIndex(e.day)).thenComparing(e->e.startTime)).map(ClassTimetableView::new).collect(Collectors.toList());
    }
    @GetMapping("/students/me/today") public Map<String,Object> studentToday(Authentication auth){
        UserAccount u=current(auth);requireRole(u,Role.STUDENT);StudentProfile p=students.findByUser_Id(u.id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile has not been assigned"));
        Map<String,Object> out=new LinkedHashMap<>();out.put("date",LocalDate.now(ZONE));out.put("timezone",ZONE.getId());
        if(p.section==null){out.put("scheduleState","NOT_ASSIGNED");out.put("current",null);out.put("next",null);return out;}
        LocalDateTime now=LocalDateTime.now(ZONE);List<ClassTimetableEntry> today=classSlots.findBySection_IdAndDayIgnoreCaseOrderByStartTimeAsc(p.section.id,now.getDayOfWeek().name());
        ClassTimetableEntry current=today.stream().filter(e->!now.toLocalTime().isBefore(e.startTime)&&now.toLocalTime().isBefore(e.endTime)).findFirst().orElse(null);
        ClassTimetableEntry next=today.stream().filter(e->e.startTime.isAfter(now.toLocalTime())).findFirst().orElse(null);
        out.put("scheduleState",today.isEmpty()?"NO_SOURCE_SCHEDULE":current!=null?"IN_CLASS":next!=null?"BETWEEN_PERIODS":"DAY_SCHEDULE_COMPLETE");
        out.put("current",current==null?null:new ClassTimetableView(current));out.put("next",next==null?null:new ClassTimetableView(next));return out;
    }
    @GetMapping("/faculty/me/timetable") public List<TimetableView> ownFacultyTimetable(@RequestParam(required=false) String day,Authentication auth){
        UserAccount u=current(auth);requireRole(u,Role.FACULTY);if(u.faculty==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty profile has not been linked");
        String requested=day==null||day.trim().isEmpty()?LocalDate.now(ZONE).getDayOfWeek().name():day.trim();
        return staffSlots.findByFaculty_IdAndDayIgnoreCaseOrderByStartTimeAsc(u.faculty.id,requested).stream().map(TimetableView::new).collect(Collectors.toList());
    }
    @GetMapping("/faculty/me/status") public FacultyView ownFacultyStatus(Authentication auth){UserAccount u=current(auth);requireRole(u,Role.FACULTY);if(u.faculty==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty profile has not been linked");return availability.view(u.faculty);}

    @GetMapping("/announcements") @Transactional(readOnly=true) public List<Map<String,Object>> visibleAnnouncements(Authentication auth){
        UserAccount u=current(auth);LocalDateTime now=LocalDateTime.now(ZONE);String role=u.role==Role.STUDENT?"STUDENT":u.role==Role.FACULTY?"FACULTY":"ADMIN";
        return announcements.findPublishedWithAuthor().stream().filter(a->"ALL".equalsIgnoreCase(a.audience)||role.equalsIgnoreCase(a.audience))
            .filter(a->a.startsAt==null||!a.startsAt.isAfter(now)).filter(a->a.endsAt==null||a.endsAt.isAfter(now)).map(this::announcementView).collect(Collectors.toList());
    }
    @GetMapping("/notifications") public Map<String,Object> myNotifications(Authentication auth){
        UserAccount u=current(auth);List<PortalNotification> rows=notifications.findByUser_IdOrderByCreatedAtDesc(u.id);Map<String,Object> out=new LinkedHashMap<>();out.put("unread",notifications.countByUser_IdAndReadAtIsNull(u.id));out.put("items",rows.stream().map(this::notificationView).collect(Collectors.toList()));return out;
    }
    @PostMapping("/notifications/{id}/read") public Map<String,Object> markRead(@PathVariable Long id,Authentication auth){
        UserAccount u=current(auth);PortalNotification n=notifications.findByIdAndUser_Id(id,u.id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found"));if(n.readAt==null)n.readAt=LocalDateTime.now(ZONE);return notificationView(notifications.save(n));
    }

    private int dayIndex(String day){try{return DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT)).getValue();}catch(Exception ex){return 8;}}
    private UserAccount current(Authentication auth){if(auth==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return users.findByUsername(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
    private void requireRole(UserAccount u,Role role){if(u.role!=role)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This endpoint is not available for your account role");if(!u.enabled)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Account is disabled");}
    private Map<String,Object> studentView(StudentProfile p){Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.id);m.put("username",p.user==null?null:p.user.username);m.put("fullName",p.fullName);m.put("studentNumber",p.studentNumber);m.put("department",p.department==null?null:p.department.name);m.put("departmentId",p.department==null?null:p.department.id);m.put("yearLabel",p.yearLabel);m.put("section",p.section==null?null:p.section.displayName());m.put("sectionId",p.section==null?null:p.section.id);m.put("phone",p.phone);m.put("active",p.active&&(p.user==null||p.user.enabled));return m;}
    private Map<String,Object> announcementView(Announcement a){Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.id);m.put("title",a.title);m.put("body",a.body);m.put("audience",a.audience);m.put("createdAt",a.createdAt);m.put("startsAt",a.startsAt);m.put("endsAt",a.endsAt);m.put("createdBy",a.createdBy==null?null:a.createdBy.username);return m;}
    private Map<String,Object> notificationView(PortalNotification n){Map<String,Object> m=new LinkedHashMap<>();m.put("id",n.id);m.put("title",n.title);m.put("message",n.message);m.put("targetPath",n.targetPath);m.put("createdAt",n.createdAt);m.put("readAt",n.readAt);m.put("read",n.readAt!=null);return m;}
}
