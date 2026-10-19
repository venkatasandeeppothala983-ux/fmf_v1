package com.findmyfaculty.api;

import com.findmyfaculty.domain.*;
import com.findmyfaculty.repo.AppointmentRepository;
import com.findmyfaculty.repo.FacultyRepository;
import com.findmyfaculty.repo.UserRepository;
import com.findmyfaculty.service.TimetableImportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;private final FacultyRepository faculty;private final com.findmyfaculty.repo.ClassSectionRepository sections;private final PasswordEncoder passwords;private final AppointmentRepository appointments;private final TimetableImportService importer;private final com.findmyfaculty.service.PortalAdminService portalAdmin;
    public AdminController(UserRepository u,FacultyRepository f,com.findmyfaculty.repo.ClassSectionRepository s,PasswordEncoder p,AppointmentRepository a,TimetableImportService i,com.findmyfaculty.service.PortalAdminService portalAdmin){users=u;faculty=f;sections=s;passwords=p;appointments=a;importer=i;this.portalAdmin=portalAdmin;}
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> createUser(@Valid @RequestBody CreateUserInput input){
        if(users.existsByUsername(input.username.trim()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Username already exists");Role role;try{role=Role.valueOf(input.role.trim().toUpperCase(Locale.ROOT));}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"role must be STUDENT or FACULTY");}
        if(role==Role.ADMIN)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Create the first admin via ADMIN_USERNAME/ADMIN_PASSWORD; admins should not be self-provisioned through this endpoint");
        UserAccount account=new UserAccount();account.username=input.username.trim();account.passwordHash=passwords.encode(input.password);account.role=role;
        if(role==Role.FACULTY){if(input.facultyId==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"facultyId is required for a faculty account");Faculty f=faculty.findById(input.facultyId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Faculty record not found"));if(Boolean.FALSE.equals(f.active))throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot create a login for an inactive faculty profile");if(users.existsByFaculty_Id(f.id))throw new ResponseStatusException(HttpStatus.CONFLICT,"A login is already linked to this faculty profile");account.faculty=f;}
        if(role==Role.STUDENT&&input.sectionId!=null)account.section=sections.findById(input.sectionId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Class section not found"));
        UserAccount saved=users.save(account);Map<String,Object> out=new LinkedHashMap<>();out.put("id",saved.id);out.put("username",saved.username);out.put("role",saved.role);out.put("facultyId",saved.faculty==null?null:saved.faculty.id);out.put("sectionId",saved.section==null?null:saved.section.id);return out;
    }
    @PostMapping("/timetable/import") public Map<String,Object> importTimetable(@RequestParam("file") MultipartFile file){if(file.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a CSV file");try{return importer.importCsv(file.getInputStream());}catch(ResponseStatusException ex){throw ex;}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Could not read timetable CSV: "+ex.getMessage());}}
    @GetMapping("/summary") public Map<String,Object> summary(){return portalAdmin.summary();}
}
