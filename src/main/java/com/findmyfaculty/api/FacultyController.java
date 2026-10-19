package com.findmyfaculty.api;

import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.*;
import com.findmyfaculty.service.AvailabilityService;
import com.findmyfaculty.service.AvailabilityWindowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class FacultyController {
    private final FacultyRepository facultyRepo;private final TimetableRepository timetableRepo;private final AvailabilityService availability;private final AvailabilityWindowService windows;
    private final ClassSectionRepository sections;private final ClassTimetableRepository classTimetable;private final ClassCourseRepository classCourses;
    public FacultyController(FacultyRepository f,TimetableRepository t,AvailabilityService a,AvailabilityWindowService w,ClassSectionRepository s,ClassTimetableRepository ct,ClassCourseRepository cc){facultyRepo=f;timetableRepo=t;availability=a;windows=w;sections=s;classTimetable=ct;classCourses=cc;}
    @GetMapping("/health") public Map<String,String> health(){return Collections.singletonMap("status","ok");}
    @GetMapping("/faculty") public List<FacultyView> search(@RequestParam(required=false) String q,@RequestParam(required=false) String department,@RequestParam(required=false) String room){String query=norm(q),dept=norm(department),rm=norm(room);return facultyRepo.findAll().stream().filter(f->!Boolean.FALSE.equals(f.active)).filter(f->matches(f,query,dept,rm)).map(availability::view).collect(Collectors.toList());}
    private boolean matches(Faculty f,String q,String dept,String room){boolean matchesQuery=q.isEmpty()||norm(f.name).contains(q)||f.courses.stream().anyMatch(c->norm(c).contains(q))||f.subjects.stream().anyMatch(s->norm(s.name).contains(q))||timetableRepo.findByFaculty_IdOrderByDayAscStartTimeAsc(f.id).stream().anyMatch(e->norm(e.subject).contains(q));String departmentName=f.departmentRef!=null?f.departmentRef.name:f.department;boolean matchesDept=dept.isEmpty()||(departmentName!=null&&norm(departmentName).contains(dept));boolean matchesRoom=room.isEmpty()||(f.cabin!=null&&norm(f.cabin).contains(room))||timetableRepo.findByFaculty_IdOrderByDayAscStartTimeAsc(f.id).stream().anyMatch(e->e.room!=null&&norm(e.room).contains(room));return matchesQuery&&matchesDept&&matchesRoom;}
    private String norm(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
    @GetMapping("/faculty/{id}") public FacultyView byId(@PathVariable Long id){return availability.view(requireFaculty(id));}
    @GetMapping("/faculty/{id}/status") public FacultyView status(@PathVariable Long id){return availability.view(requireFaculty(id));}
    @GetMapping("/faculty/{id}/timetable") public List<TimetableView> timetable(@PathVariable Long id,@RequestParam(required=false) String day){requireFaculty(id);List<TimetableEntry> entries=day==null||day.trim().isEmpty()?timetableRepo.findByFaculty_IdOrderByDayAscStartTimeAsc(id):timetableRepo.findByFaculty_IdAndDayIgnoreCaseOrderByStartTimeAsc(id,day);return entries.stream().sorted(Comparator.comparingInt((TimetableEntry e)->dayIndex(e.day)).thenComparing(e->e.startTime)).map(TimetableView::new).collect(Collectors.toList());}
    @GetMapping("/faculty/{id}/alternatives") public List<PotentialOpeningView> alternatives(@PathVariable Long id,@RequestParam(defaultValue="30") int durationMinutes){return windows.find(requireFaculty(id),durationMinutes);}
    @GetMapping("/sections") public List<ClassSectionView> findSections(@RequestParam(required=false) String q){String query=norm(q);return sections.findAll().stream().filter(s->query.isEmpty()||norm(s.year+" "+s.semester+" "+s.section+" "+s.classIncharge).contains(query)||classCourses.findBySection_IdOrderByNameAsc(s.id).stream().anyMatch(c->norm(c.name+" "+c.code+" "+c.facultyName).contains(query))).map(this::sectionView).collect(Collectors.toList());}
    @GetMapping("/sections/{id}") public ClassSectionView section(@PathVariable Long id){return sectionView(requireSection(id));}
    @GetMapping("/sections/{id}/timetable") public List<ClassTimetableView> sectionTimetable(@PathVariable Long id,@RequestParam(required=false) String day){requireSection(id);List<ClassTimetableEntry> entries=day==null||day.trim().isEmpty()?classTimetable.findBySection_IdOrderByDayAscStartTimeAsc(id):classTimetable.findBySection_IdAndDayIgnoreCaseOrderByStartTimeAsc(id,day);return entries.stream().sorted(Comparator.comparingInt((ClassTimetableEntry e)->dayIndex(e.day)).thenComparing(e->e.startTime)).map(ClassTimetableView::new).collect(Collectors.toList());}
    @GetMapping("/rooms/{room}/schedule") public List<Map<String,Object>> room(@PathVariable String room,@RequestParam String day){List<Map<String,Object>> out=new ArrayList<>();timetableRepo.findByDayIgnoreCaseAndRoomIgnoreCase(day,room).forEach(e->{Map<String,Object> m=new LinkedHashMap<>();m.put("kind","faculty");m.put("faculty",e.faculty.name);m.put("subject",e.subject);m.put("day",e.day);m.put("startTime",e.startTime);m.put("endTime",e.endTime);m.put("room",e.room);out.add(m);});classTimetable.findByDayIgnoreCaseAndRoomIgnoreCase(day,room).forEach(e->{Map<String,Object> m=new LinkedHashMap<>();m.put("kind","class section");m.put("section",e.section.displayName());m.put("subject",e.subject);m.put("day",e.day);m.put("startTime",e.startTime);m.put("endTime",e.endTime);m.put("room",e.room);out.add(m);});return out;}
    private int dayIndex(String day){try{return java.time.DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT)).getValue();}catch(Exception ex){return 8;}}
    private ClassSectionView sectionView(ClassSection s){return new ClassSectionView(s,classCourses.findBySection_IdOrderByNameAsc(s.id).stream().map(ClassCourseView::new).collect(Collectors.toList()));}
    private Faculty requireFaculty(Long id){Faculty f=facultyRepo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty record not found"));if(Boolean.FALSE.equals(f.active))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty record not found");return f;}
    private ClassSection requireSection(Long id){return sections.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Class section not found"));}
}
