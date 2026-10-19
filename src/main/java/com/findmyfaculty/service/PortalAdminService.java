package com.findmyfaculty.service;

import com.findmyfaculty.api.AdminInputs;
import com.findmyfaculty.api.ClassTimetableView;
import com.findmyfaculty.api.FacultyView;
import com.findmyfaculty.api.TimetableView;
import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortalAdminService {
    private final UserRepository users;
    private final FacultyRepository faculty;
    private final DepartmentRepository departments;
    private final StudentProfileRepository students;
    private final ClassSectionRepository sections;
    private final SubjectRepository subjects;
    private final ClassroomRepository rooms;
    private final TimetableRepository staffSlots;
    private final ClassTimetableRepository classSlots;
    private final AnnouncementRepository announcements;
    private final AppointmentRepository appointments;
    private final PasswordEncoder passwords;

    public PortalAdminService(UserRepository users, FacultyRepository faculty, DepartmentRepository departments,
                              StudentProfileRepository students, ClassSectionRepository sections, SubjectRepository subjects,
                              ClassroomRepository rooms, TimetableRepository staffSlots, ClassTimetableRepository classSlots,
                              AnnouncementRepository announcements, AppointmentRepository appointments, PasswordEncoder passwords) {
        this.users=users; this.faculty=faculty; this.departments=departments; this.students=students; this.sections=sections;
        this.subjects=subjects; this.rooms=rooms; this.staffSlots=staffSlots; this.classSlots=classSlots;
        this.announcements=announcements; this.appointments=appointments; this.passwords=passwords;
    }

    public Map<String,Object> summary() {
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("faculty",faculty.count()); m.put("activeFaculty",faculty.findAll().stream().filter(f->!Boolean.FALSE.equals(f.active)).count());
        m.put("students",students.count()); m.put("activeStudents",students.countByActiveTrue());
        m.put("users",users.count()); m.put("departments",departments.count()); m.put("subjects",subjects.count());
        m.put("classrooms",rooms.count()); m.put("appointments",appointments.count());
        m.put("pendingAppointments",appointments.findAll().stream().filter(a->a.state==AppointmentState.PENDING).count());
        m.put("announcements",announcements.count()); return m;
    }

    public List<Map<String,Object>> departments() { return departments.findAll().stream().sorted(Comparator.comparing(d->d.name.toLowerCase(Locale.ROOT))).map(this::departmentView).collect(Collectors.toList()); }
    @Transactional public Map<String,Object> saveDepartment(Long id, AdminInputs.DepartmentInput in) {
        Department d=id==null?new Department():departments.findById(id).orElseThrow(()->notFound("Department not found"));
        String code=required(in.code,"code"),name=required(in.name,"name");
        if(departments.existsByCodeIgnoreCase(code)&&!code.equalsIgnoreCase(d.code))throw conflict("Department code already exists");
        if(departments.existsByNameIgnoreCase(name)&&!name.equalsIgnoreCase(d.name))throw conflict("Department name already exists");
        d.code=code.toUpperCase(Locale.ROOT);d.name=name;d.description=trim(in.description,500);if(in.active!=null)d.active=in.active;
        return departmentView(departments.save(d));
    }
    @Transactional public void deleteDepartment(Long id) { Department d=department(id);d.active=false;departments.save(d); }

    public List<Map<String,Object>> students() { return students.findAll().stream().sorted(Comparator.comparing(s->s.studentNumber.toLowerCase(Locale.ROOT))).map(this::studentView).collect(Collectors.toList()); }
    @Transactional public Map<String,Object> saveStudent(Long id, AdminInputs.StudentInput in) {
        StudentProfile p=id==null?new StudentProfile():students.findById(id).orElseThrow(()->notFound("Student not found"));
        String username=trim(in.username,80);String studentNumber=required(in.studentNumber,"studentNumber").trim();
        UserAccount account=p.user;
        if(id==null){
            username=required(username,"username");
            if(in.password==null||in.password.length()<12)throw bad("A password of at least 12 characters is required");
            if(users.existsByUsername(username))throw conflict("Username already exists");
            account=new UserAccount();account.username=username;account.passwordHash=passwords.encode(in.password);account.role=Role.STUDENT;account.enabled=true;p.user=account;
        } else if(account==null){
            boolean hasUsername=username!=null,hasPassword=in.password!=null&&!in.password.isEmpty();
            if(hasUsername||hasPassword){
                if(!hasUsername||!hasPassword||in.password.length()<12)throw bad("Provide both a username and a password of at least 12 characters to enable student login");
                if(users.existsByUsername(username))throw conflict("Username already exists");
                account=new UserAccount();account.username=username;account.passwordHash=passwords.encode(in.password);account.role=Role.STUDENT;p.user=account;
            }
        } else {
            if(username!=null&&!username.equals(account.username)){
                if(users.existsByUsername(username))throw conflict("Username already exists");
                account.username=username;
            }
            if(in.password!=null&&!in.password.isEmpty())account.passwordHash=passwords.encode(in.password);
        }
        boolean duplicate=students.findAll().stream().anyMatch(other->!Objects.equals(other.id,p.id)&&other.studentNumber.equalsIgnoreCase(studentNumber));
        if(duplicate)throw conflict("Student ID already exists");
        p.fullName=trim(in.fullName,160);p.studentNumber=studentNumber;p.department=in.departmentId==null?null:department(in.departmentId);
        p.yearLabel=trim(in.yearLabel,40);p.section=in.sectionId==null?null:sections.findById(in.sectionId).orElseThrow(()->notFound("Section not found"));p.phone=trim(in.phone,30);
        if(in.active!=null)p.active=in.active;
        if(account!=null){account.enabled=p.active;account.role=Role.STUDENT;users.save(account);}
        p.updatedAt=LocalDateTime.now();return studentView(students.save(p));
    }
    @Transactional public void deactivateStudent(Long id) { StudentProfile p=students.findById(id).orElseThrow(()->notFound("Student not found"));p.active=false;if(p.user!=null){p.user.enabled=false;users.save(p.user);}p.updatedAt=LocalDateTime.now();students.save(p); }

    public List<Map<String,Object>> faculty() { return faculty.findAll().stream().sorted(Comparator.comparing(f->f.name.toLowerCase(Locale.ROOT))).map(this::facultyMap).collect(Collectors.toList()); }
    @Transactional public Map<String,Object> saveFaculty(Long id, AdminInputs.FacultyInput in) {
        Faculty f=id==null?new Faculty():faculty.findById(id).orElseThrow(()->notFound("Faculty not found"));
        f.name=required(in.name,"name");f.cabin=trim(in.cabin,100);f.email=trim(in.email,180);
        f.departmentRef=in.departmentId==null?null:department(in.departmentId);f.department=f.departmentRef==null?null:f.departmentRef.name;
        if(in.subjectIds!=null){Set<Subject> linked=new LinkedHashSet<>();for(Long subjectId:in.subjectIds)linked.add(subject(subjectId));f.subjects=linked;}
        if(id==null){f.sourceKey="ADMIN-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT);f.courses=new ArrayList<>();f.active=true;}
        if(in.active!=null)f.active=in.active;
        Faculty saved=faculty.save(f);users.findByFaculty_Id(saved.id).ifPresent(u->{u.enabled=!Boolean.FALSE.equals(saved.active);users.save(u);});
        return facultyMap(saved);
    }
    @Transactional public void deactivateFaculty(Long id) { Faculty f=faculty(id);f.active=false;faculty.save(f);users.findByFaculty_Id(id).ifPresent(u->{u.enabled=false;users.save(u);}); }

    public List<Map<String,Object>> subjects() { return subjects.findAll().stream().sorted(Comparator.comparing(s->s.name.toLowerCase(Locale.ROOT))).map(this::subjectView).collect(Collectors.toList()); }
    @Transactional public Map<String,Object> saveSubject(Long id,AdminInputs.SubjectInput in) {
        Subject s=id==null?new Subject():subject(id);String name=required(in.name,"name").trim();
        subjects.findByNameIgnoreCase(name).filter(found->!Objects.equals(found.id,s.id)).ifPresent(found->{throw conflict("Subject name already exists");});
        s.name=name;s.code=trim(in.code,40);s.department=in.departmentId==null?null:department(in.departmentId);if(in.active!=null)s.active=in.active;
        return subjectView(subjects.save(s));
    }
    @Transactional public void deactivateSubject(Long id){Subject s=subject(id);s.active=false;subjects.save(s);}

    public List<Map<String,Object>> classrooms(){return rooms.findAll().stream().sorted(Comparator.comparing(r->r.code.toLowerCase(Locale.ROOT))).map(this::roomView).collect(Collectors.toList());}
    @Transactional public Map<String,Object> saveClassroom(Long id,AdminInputs.ClassroomInput in){
        Classroom r=id==null?new Classroom():room(id);String code=required(in.code,"code").trim();
        rooms.findByCodeIgnoreCase(code).filter(found->!Objects.equals(found.id,r.id)).ifPresent(found->{throw conflict("Classroom code already exists");});
        r.code=code;r.building=trim(in.building,120);r.capacity=in.capacity;if(in.active!=null)r.active=in.active;return roomView(rooms.save(r));
    }
    @Transactional public void deactivateClassroom(Long id){Classroom r=room(id);r.active=false;rooms.save(r);}

    public List<Map<String,Object>> accounts(){return users.findAll().stream().sorted(Comparator.comparing(u->u.username.toLowerCase(Locale.ROOT))).map(this::accountView).collect(Collectors.toList());}
    @Transactional public Map<String,Object> setAccountEnabled(Long id,boolean enabled){UserAccount u=users.findById(id).orElseThrow(()->notFound("Account not found"));u.enabled=enabled;users.save(u);if(u.role==Role.STUDENT)students.findByUser_Id(u.id).ifPresent(p->{p.active=enabled;p.updatedAt=LocalDateTime.now();students.save(p);});return accountView(u);}
    @Transactional public void resetPassword(Long id,String password){UserAccount u=users.findById(id).orElseThrow(()->notFound("Account not found"));u.passwordHash=passwords.encode(password);users.save(u);}

    public List<Map<String,Object>> staffTimetables(){return staffSlots.findAll().stream().sorted(Comparator.comparingInt((TimetableEntry t)->dayIndex(t.day)).thenComparing(t->t.startTime)).map(this::staffSlotView).collect(Collectors.toList());}
    @Transactional public Map<String,Object> saveStaffSlot(Long id,AdminInputs.TimetableInput in){
        TimetableEntry e=id==null?new TimetableEntry():staffSlots.findById(id).orElseThrow(()->notFound("Timetable entry not found"));
        Faculty f=faculty(in.facultyId);if(Boolean.FALSE.equals(f.active))throw bad("Cannot schedule inactive faculty");
        String day=day(in.day);LocalTime start=time(in.startTime),end=time(in.endTime);if(!start.isBefore(end))throw bad("End time must be after start time");
        Subject subject=resolveSubject(in.subjectId,in.subject);Classroom room=resolveRoom(in.classroomId,in.room);
        for(TimetableEntry other:staffSlots.findByFaculty_IdAndDayIgnoreCaseOrderByStartTimeAsc(f.id,day))if(!Objects.equals(other.id,e.id)&&overlap(start,end,other.startTime,other.endTime))throw conflict("Faculty timetable overlaps another entry");
        if(room!=null)for(TimetableEntry other:staffSlots.findAll())if(!Objects.equals(other.id,e.id)&&other.day.equalsIgnoreCase(day)&&sameRoom(room,other.classroomRef,other.room)&&overlap(start,end,other.startTime,other.endTime))throw conflict("Classroom is already assigned during this time");
        e.faculty=f;e.day=day;e.period=in.period;e.startTime=start;e.endTime=end;e.subjectRef=subject;e.subject=subject.name;e.classroomRef=room;e.room=room==null?trim(in.room,100):room.code;e.sourceFile=id==null?"Admin managed":"Admin managed";
        return staffSlotView(staffSlots.save(e));
    }
    @Transactional public void deleteStaffSlot(Long id){staffSlots.delete(staffSlots.findById(id).orElseThrow(()->notFound("Timetable entry not found")));}

    public List<Map<String,Object>> classTimetables(){return classSlots.findAll().stream().sorted(Comparator.comparingInt((ClassTimetableEntry t)->dayIndex(t.day)).thenComparing(t->t.startTime)).map(this::classSlotView).collect(Collectors.toList());}
    @Transactional public Map<String,Object> saveClassSlot(Long id,AdminInputs.ClassTimetableInput in){
        ClassTimetableEntry e=id==null?new ClassTimetableEntry():classSlots.findById(id).orElseThrow(()->notFound("Class timetable entry not found"));
        ClassSection section=sections.findById(in.sectionId).orElseThrow(()->notFound("Section not found"));String day=day(in.day);LocalTime start=time(in.startTime),end=time(in.endTime);if(!start.isBefore(end))throw bad("End time must be after start time");
        Subject subject=resolveSubject(in.subjectId,in.subject);Classroom room=resolveRoom(in.classroomId,in.room);
        for(ClassTimetableEntry other:classSlots.findBySection_IdOrderByDayAscStartTimeAsc(section.id))if(!Objects.equals(other.id,e.id)&&other.day.equalsIgnoreCase(day)&&overlap(start,end,other.startTime,other.endTime))throw conflict("Class section timetable overlaps another entry");
        if(room!=null)for(ClassTimetableEntry other:classSlots.findAll())if(!Objects.equals(other.id,e.id)&&other.day.equalsIgnoreCase(day)&&sameRoom(room,other.classroomRef,other.room)&&overlap(start,end,other.startTime,other.endTime))throw conflict("Classroom is already assigned during this time");
        e.section=section;e.day=day;e.period=in.period;e.startTime=start;e.endTime=end;e.subjectRef=subject;e.subject=subject.name;e.classroomRef=room;e.room=room==null?trim(in.room,180):room.code;e.sourceText="Admin managed timetable entry";
        return classSlotView(classSlots.save(e));
    }
    @Transactional public void deleteClassSlot(Long id){classSlots.delete(classSlots.findById(id).orElseThrow(()->notFound("Class timetable entry not found")));}

    @Transactional(readOnly=true) public List<Map<String,Object>> allAnnouncements(){return announcements.findAllWithAuthor().stream().sorted(Comparator.comparing((Announcement a)->a.createdAt,Comparator.nullsLast(Comparator.reverseOrder()))).map(this::announcementView).collect(Collectors.toList());}
    @Transactional public Map<String,Object> saveAnnouncement(Long id,AdminInputs.AnnouncementInput in,String username){
        Announcement a=id==null?new Announcement():announcements.findById(id).orElseThrow(()->notFound("Announcement not found"));
        a.title=required(in.title,"title");a.body=required(in.body,"body");String audience=in.audience==null?"ALL":in.audience.toUpperCase(Locale.ROOT);if(!Arrays.asList("ALL","STUDENT","FACULTY").contains(audience))throw bad("Audience must be ALL, STUDENT, or FACULTY");a.audience=audience;
        if(in.published!=null)a.published=in.published;a.startsAt=parseDate(in.startsAt);a.endsAt=parseDate(in.endsAt);if(a.startsAt!=null&&a.endsAt!=null&&!a.endsAt.isAfter(a.startsAt))throw bad("End time must be after start time");
        if(id==null)a.createdBy=users.findByUsername(username).orElse(null);a.updatedAt=LocalDateTime.now();return announcementView(announcements.save(a));
    }
    @Transactional public void deleteAnnouncement(Long id){announcements.delete(announcements.findById(id).orElseThrow(()->notFound("Announcement not found")));}

    public Map<String,Object> report(){
        Map<String,Object> root=new LinkedHashMap<>();root.put("summary",summary());
        List<Map<String,Object>> byDepartment=new ArrayList<>();for(Department d:departments.findAll()){
            Map<String,Object> row=new LinkedHashMap<>();row.put("department",d.name);
            row.put("faculty",faculty.findAll().stream().filter(f->f.departmentRef!=null&&f.departmentRef.id.equals(d.id)&&!Boolean.FALSE.equals(f.active)).count());
            row.put("students",students.findAll().stream().filter(s->s.department!=null&&s.department.id.equals(d.id)&&s.active).count());byDepartment.add(row);
        }root.put("byDepartment",byDepartment);return root;
    }

    private Department department(Long id){return departments.findById(id).orElseThrow(()->notFound("Department not found"));}
    private Faculty faculty(Long id){return faculty.findById(id).orElseThrow(()->notFound("Faculty not found"));}
    private Subject subject(Long id){return subjects.findById(id).orElseThrow(()->notFound("Subject not found"));}
    private Classroom room(Long id){return rooms.findById(id).orElseThrow(()->notFound("Classroom not found"));}
    private Subject resolveSubject(Long id,String name){if(id!=null)return subject(id);String n=required(name,"subject");return subjects.findByNameIgnoreCase(n).orElseGet(()->{Subject s=new Subject();s.name=n;s.active=true;return subjects.save(s);});}
    private Classroom resolveRoom(Long id,String code){if(id!=null)return room(id);if(code==null||code.trim().isEmpty())return null;return rooms.findByCodeIgnoreCase(code.trim()).orElseGet(()->{Classroom r=new Classroom();r.code=code.trim();return rooms.save(r);});}
    private Map<String,Object> departmentView(Department d){Map<String,Object> m=new LinkedHashMap<>();m.put("id",d.id);m.put("code",d.code);m.put("name",d.name);m.put("description",d.description);m.put("active",d.active);return m;}
    private Map<String,Object> studentView(StudentProfile s){Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.id);m.put("userId",s.user==null?null:s.user.id);m.put("username",s.user==null?null:s.user.username);m.put("fullName",s.fullName);m.put("studentNumber",s.studentNumber);m.put("departmentId",s.department==null?null:s.department.id);m.put("department",s.department==null?null:s.department.name);m.put("yearLabel",s.yearLabel);m.put("sectionId",s.section==null?null:s.section.id);m.put("section",s.section==null?null:s.section.displayName());m.put("phone",s.phone);m.put("active",s.active&&(s.user==null||s.user.enabled));m.put("loginEnabled",s.user!=null&&s.user.enabled);return m;}
    private Map<String,Object> facultyMap(Faculty f){Map<String,Object> m=new LinkedHashMap<>();m.put("id",f.id);m.put("sourceKey",f.sourceKey);m.put("name",f.name);m.put("departmentId",f.departmentRef==null?null:f.departmentRef.id);m.put("department",f.departmentRef==null?f.department:f.departmentRef.name);m.put("cabin",f.cabin);m.put("email",f.email);m.put("active",!Boolean.FALSE.equals(f.active));m.put("courses",new FacultyView(f).courses);m.put("subjectIds",f.subjects.stream().map(s->s.id).collect(Collectors.toList()));m.put("accountEnabled",users.findByFaculty_Id(f.id).map(u->u.enabled).orElse(false));return m;}
    private Map<String,Object> subjectView(Subject s){Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.id);m.put("code",s.code);m.put("name",s.name);m.put("departmentId",s.department==null?null:s.department.id);m.put("department",s.department==null?null:s.department.name);m.put("active",s.active);return m;}
    private Map<String,Object> roomView(Classroom r){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.id);m.put("code",r.code);m.put("building",r.building);m.put("capacity",r.capacity);m.put("active",r.active);return m;}
    private Map<String,Object> accountView(UserAccount u){Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.id);m.put("username",u.username);m.put("role",u.role.name());m.put("enabled",u.enabled);m.put("facultyId",u.faculty==null?null:u.faculty.id);m.put("facultyName",u.faculty==null?null:u.faculty.name);m.put("studentId",students.findByUser_Id(u.id).map(p->p.studentNumber).orElse(null));return m;}
    private Map<String,Object> staffSlotView(TimetableEntry e){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.id);m.put("facultyId",e.faculty.id);m.put("facultyName",e.faculty.name);m.put("day",e.day);m.put("period",e.period);m.put("startTime",e.startTime);m.put("endTime",e.endTime);m.put("subject",e.subjectRef==null?e.subject:e.subjectRef.name);m.put("subjectId",e.subjectRef==null?null:e.subjectRef.id);m.put("room",e.classroomRef==null?e.room:e.classroomRef.code);m.put("classroomId",e.classroomRef==null?null:e.classroomRef.id);return m;}
    private Map<String,Object> classSlotView(ClassTimetableEntry e){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.id);m.put("sectionId",e.section.id);m.put("section",e.section.displayName());m.put("day",e.day);m.put("period",e.period);m.put("startTime",e.startTime);m.put("endTime",e.endTime);m.put("subject",e.subjectRef==null?e.subject:e.subjectRef.name);m.put("subjectId",e.subjectRef==null?null:e.subjectRef.id);m.put("room",e.classroomRef==null?e.room:e.classroomRef.code);m.put("classroomId",e.classroomRef==null?null:e.classroomRef.id);return m;}
    private Map<String,Object> announcementView(Announcement a){Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.id);m.put("title",a.title);m.put("body",a.body);m.put("audience",a.audience);m.put("published",a.published);m.put("startsAt",a.startsAt);m.put("endsAt",a.endsAt);m.put("createdAt",a.createdAt);m.put("createdBy",a.createdBy==null?null:a.createdBy.username);return m;}
    private int dayIndex(String day){try{return DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT)).getValue();}catch(Exception ex){return 8;}}
    private boolean sameRoom(Classroom a,Classroom b,String legacy){return b!=null?Objects.equals(a.id,b.id):legacy!=null&&a.code.equalsIgnoreCase(legacy);}
    private boolean overlap(LocalTime a1,LocalTime a2,LocalTime b1,LocalTime b2){return a1.isBefore(b2)&&b1.isBefore(a2);}
    private String day(String s){String v=required(s,"day").toUpperCase(Locale.ROOT);try{DayOfWeek.valueOf(v);}catch(Exception ex){throw bad("Invalid weekday");}return v;}
    private LocalTime time(String s){try{return LocalTime.parse(required(s,"time"));}catch(Exception ex){throw bad("Time must use HH:mm or HH:mm:ss");}}
    private LocalDateTime parseDate(String s){if(s==null||s.trim().isEmpty())return null;try{return LocalDateTime.parse(s.trim());}catch(Exception ex){throw bad("Date/time must be an ISO local date-time");}}
    private String required(String s,String field){if(s==null||s.trim().isEmpty())throw bad(field+" is required");return s.trim();}
    private String trim(String s,int max){if(s==null||s.trim().isEmpty())return null;String v=s.trim();return v.length()>max?v.substring(0,max):v;}
    private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
    private ResponseStatusException conflict(String m){return new ResponseStatusException(HttpStatus.CONFLICT,m);}
    private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
