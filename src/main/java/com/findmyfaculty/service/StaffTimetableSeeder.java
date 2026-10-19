package com.findmyfaculty.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyfaculty.domain.Faculty;
import com.findmyfaculty.domain.TimetableEntry;
import com.findmyfaculty.repo.FacultyRepository;
import com.findmyfaculty.repo.TimetableRepository;
import com.findmyfaculty.repo.UserRepository;
import com.findmyfaculty.domain.UserAccount;
import com.findmyfaculty.domain.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StaffTimetableSeeder implements ApplicationRunner {
    private final FacultyRepository facultyRepo; private final TimetableRepository timetableRepo; private final UserRepository userRepo; private final ObjectMapper mapper; private final PasswordEncoder encoder;
    @Value("${app.admin.username:}") private String adminUsername;
    @Value("${app.admin.password:}") private String adminPassword;
    public StaffTimetableSeeder(FacultyRepository f, TimetableRepository t, UserRepository u, ObjectMapper m, PasswordEncoder p){facultyRepo=f;timetableRepo=t;userRepo=u;mapper=m;encoder=p;}
    @Override @Transactional public void run(ApplicationArguments args) throws Exception {
        if(facultyRepo.count()==0) seedStaff();
        if(adminUsername!=null&&!adminUsername.trim().isEmpty()&&adminPassword!=null&&!adminPassword.isEmpty()&&!userRepo.existsByUsername(adminUsername)){
            if(adminPassword.length()<12)throw new IllegalStateException("ADMIN_PASSWORD must be at least 12 characters");
            UserAccount admin=new UserAccount();admin.username=adminUsername.trim();admin.passwordHash=encoder.encode(adminPassword);admin.role=Role.ADMIN;userRepo.save(admin);
        }
    }
    private void seedStaff() throws Exception {
        InputStream input=getClass().getResourceAsStream("/seed/staff-timetable.json");
        if(input==null)throw new IllegalStateException("Missing seed/staff-timetable.json");
        List<SeedFaculty> seed=mapper.readValue(input,new TypeReference<List<SeedFaculty>>(){});
        for(SeedFaculty raw:seed){
            Faculty f=new Faculty();f.sourceKey=raw.id;f.name=raw.name;f.courses=raw.courses==null?new ArrayList<>():raw.courses;
            f=facultyRepo.save(f);
            if(raw.schedule==null)continue;
            for(SeedDay day:raw.schedule)for(SeedSlot slot:day.slots){
                TimetableEntry e=new TimetableEntry();e.faculty=f;e.day=day.day.toUpperCase();e.period=slot.period;e.startTime=LocalTime.parse(slot.start);e.endTime=LocalTime.parse(slot.end);e.subject=slot.event;e.sourceFile="Staff Time table.pdf";timetableRepo.save(e);
            }
        }
    }
    public static class SeedFaculty { public String id; public String name; public String initials; public List<String> courses; public List<SeedDay> schedule; }
    public static class SeedDay { public String day; public List<SeedSlot> slots; }
    public static class SeedSlot { public Integer period; public String start; public String end; public String event; }
}
