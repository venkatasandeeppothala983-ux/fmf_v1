package com.findmyfaculty.api;

import com.findmyfaculty.domain.Faculty;
import com.findmyfaculty.repo.FacultyRepository;
import com.findmyfaculty.service.AvailabilityService;
import com.findmyfaculty.service.PortalAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminManagementController {
    private final PortalAdminService service;private final AvailabilityService availability;private final FacultyRepository faculty;
    public AdminManagementController(PortalAdminService service,AvailabilityService availability,FacultyRepository faculty){this.service=service;this.availability=availability;this.faculty=faculty;}

    @GetMapping("/reports/overview") public Map<String,Object> report(){return service.report();}
    @GetMapping("/departments") public List<Map<String,Object>> departments(){return service.departments();}
    @PostMapping("/departments") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createDepartment(@Valid @RequestBody AdminInputs.DepartmentInput in){return service.saveDepartment(null,in);}
    @PutMapping("/departments/{id}") public Map<String,Object> updateDepartment(@PathVariable Long id,@Valid @RequestBody AdminInputs.DepartmentInput in){return service.saveDepartment(id,in);}
    @DeleteMapping("/departments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteDepartment(@PathVariable Long id){service.deleteDepartment(id);}

    @GetMapping("/students") public List<Map<String,Object>> students(){return service.students();}
    @PostMapping("/students") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createStudent(@Valid @RequestBody AdminInputs.StudentInput in){return service.saveStudent(null,in);}
    @PutMapping("/students/{id}") public Map<String,Object> updateStudent(@PathVariable Long id,@Valid @RequestBody AdminInputs.StudentInput in){return service.saveStudent(id,in);}
    @DeleteMapping("/students/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteStudent(@PathVariable Long id){service.deactivateStudent(id);}

    @GetMapping("/faculty") public List<Map<String,Object>> faculty(){return service.faculty();}
    @PostMapping("/faculty") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createFaculty(@Valid @RequestBody AdminInputs.FacultyInput in){return service.saveFaculty(null,in);}
    @PutMapping("/faculty/{id}") public Map<String,Object> updateFaculty(@PathVariable Long id,@Valid @RequestBody AdminInputs.FacultyInput in){return service.saveFaculty(id,in);}
    @DeleteMapping("/faculty/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteFaculty(@PathVariable Long id){service.deactivateFaculty(id);}

    @GetMapping("/subjects") public List<Map<String,Object>> subjects(){return service.subjects();}
    @PostMapping("/subjects") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createSubject(@Valid @RequestBody AdminInputs.SubjectInput in){return service.saveSubject(null,in);}
    @PutMapping("/subjects/{id}") public Map<String,Object> updateSubject(@PathVariable Long id,@Valid @RequestBody AdminInputs.SubjectInput in){return service.saveSubject(id,in);}
    @DeleteMapping("/subjects/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSubject(@PathVariable Long id){service.deactivateSubject(id);}

    @GetMapping("/classrooms") public List<Map<String,Object>> classrooms(){return service.classrooms();}
    @PostMapping("/classrooms") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createClassroom(@Valid @RequestBody AdminInputs.ClassroomInput in){return service.saveClassroom(null,in);}
    @PutMapping("/classrooms/{id}") public Map<String,Object> updateClassroom(@PathVariable Long id,@Valid @RequestBody AdminInputs.ClassroomInput in){return service.saveClassroom(id,in);}
    @DeleteMapping("/classrooms/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteClassroom(@PathVariable Long id){service.deactivateClassroom(id);}

    @GetMapping("/accounts") public List<Map<String,Object>> accounts(){return service.accounts();}
    @PatchMapping("/accounts/{id}/active") public Map<String,Object> setAccountEnabled(@PathVariable Long id,@Valid @RequestBody AdminInputs.AccountStateInput in){return service.setAccountEnabled(id,in.enabled);}
    @PutMapping("/accounts/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT) public void resetPassword(@PathVariable Long id,@Valid @RequestBody AdminInputs.PasswordResetInput in){service.resetPassword(id,in.password);}

    @GetMapping("/timetables/faculty") public List<Map<String,Object>> facultyTimetables(){return service.staffTimetables();}
    @PostMapping("/timetables/faculty") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createFacultySlot(@Valid @RequestBody AdminInputs.TimetableInput in){return service.saveStaffSlot(null,in);}
    @PutMapping("/timetables/faculty/{id}") public Map<String,Object> updateFacultySlot(@PathVariable Long id,@Valid @RequestBody AdminInputs.TimetableInput in){return service.saveStaffSlot(id,in);}
    @DeleteMapping("/timetables/faculty/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteFacultySlot(@PathVariable Long id){service.deleteStaffSlot(id);}

    @GetMapping("/timetables/classes") public List<Map<String,Object>> classTimetables(){return service.classTimetables();}
    @PostMapping("/timetables/classes") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createClassSlot(@Valid @RequestBody AdminInputs.ClassTimetableInput in){return service.saveClassSlot(null,in);}
    @PutMapping("/timetables/classes/{id}") public Map<String,Object> updateClassSlot(@PathVariable Long id,@Valid @RequestBody AdminInputs.ClassTimetableInput in){return service.saveClassSlot(id,in);}
    @DeleteMapping("/timetables/classes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteClassSlot(@PathVariable Long id){service.deleteClassSlot(id);}

    @GetMapping("/announcements") public List<Map<String,Object>> announcements(){return service.allAnnouncements();}
    @PostMapping("/announcements") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createAnnouncement(@Valid @RequestBody AdminInputs.AnnouncementInput in,Authentication auth){return service.saveAnnouncement(null,in,auth.getName());}
    @PutMapping("/announcements/{id}") public Map<String,Object> updateAnnouncement(@PathVariable Long id,@Valid @RequestBody AdminInputs.AnnouncementInput in,Authentication auth){return service.saveAnnouncement(id,in,auth.getName());}
    @DeleteMapping("/announcements/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteAnnouncement(@PathVariable Long id){service.deleteAnnouncement(id);}

    @GetMapping("/availability") public List<FacultyView> availability(){return faculty.findAll().stream().filter(f->!Boolean.FALSE.equals(f.active)).map(availability::view).collect(Collectors.toList());}
}
