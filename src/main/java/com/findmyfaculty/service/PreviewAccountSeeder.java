package com.findmyfaculty.service;

import com.findmyfaculty.domain.Faculty;
import com.findmyfaculty.domain.Role;
import com.findmyfaculty.domain.UserAccount;
import com.findmyfaculty.repo.FacultyRepository;
import com.findmyfaculty.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("preview")
@Order(Ordered.LOWEST_PRECEDENCE)
public class PreviewAccountSeeder implements ApplicationRunner {
    @Value("${app.preview.student-username:}") private String studentUsername;
    @Value("${app.preview.student-password:}") private String studentPassword;
    @Value("${app.preview.faculty-username:}") private String facultyUsername;
    @Value("${app.preview.faculty-password:}") private String facultyPassword;
    private final UserRepository users;private final FacultyRepository faculty;private final com.findmyfaculty.repo.ClassSectionRepository sections;private final com.findmyfaculty.repo.StudentProfileRepository profiles;private final PasswordEncoder encoder;
    public PreviewAccountSeeder(UserRepository u,FacultyRepository f,com.findmyfaculty.repo.ClassSectionRepository s,com.findmyfaculty.repo.StudentProfileRepository profiles,PasswordEncoder p){users=u;faculty=f;sections=s;this.profiles=profiles;encoder=p;}
    @Override public void run(ApplicationArguments args){
        com.findmyfaculty.domain.ClassSection studentSection=sections.findBySourceKey("class-05").orElse(null);
        create(studentUsername,studentPassword,Role.STUDENT,null,studentSection);
        if(studentUsername!=null&&!studentUsername.trim().isEmpty())users.findByUsername(studentUsername.trim()).filter(u->u.role==Role.STUDENT).ifPresent(u->{
            if(!profiles.findByUser_Id(u.id).isPresent()){
                com.findmyfaculty.domain.StudentProfile profile=new com.findmyfaculty.domain.StudentProfile();profile.user=u;profile.fullName="Preview Student";profile.studentNumber="DEMO-STU-001";profile.section=studentSection;profile.yearLabel=studentSection==null?null:studentSection.year+" Year";profiles.save(profile);
            }
        });
        if(facultyUsername!=null&&!facultyUsername.trim().isEmpty()&&!users.existsByUsername(facultyUsername)){
            Faculty target=faculty.findBySourceKey("staff-07").orElseThrow(()->new IllegalStateException("Real faculty record staff-07 was not seeded"));
            if(facultyPassword==null||facultyPassword.length()<12)throw new IllegalStateException("PREVIEW_FACULTY_PASSWORD must be at least 12 characters");
            create(facultyUsername,facultyPassword,Role.FACULTY,target,null);
        }
    }
    private void create(String username,String password,Role role,Faculty profile,com.findmyfaculty.domain.ClassSection section){

        if(username==null||username.trim().isEmpty()||users.existsByUsername(username))return;
        if(password==null||password.length()<12)throw new IllegalStateException("Preview passwords must be at least 12 characters");
        UserAccount u=new UserAccount();u.username=username.trim();u.passwordHash=encoder.encode(password);u.role=role;u.faculty=profile;u.section=section;users.save(u);
    }
}
