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

    private final UserRepository users;
    private final FacultyRepository faculty;
    private final com.findmyfaculty.repo.StudentProfileRepository profiles;
    private final PasswordEncoder encoder;

    public PreviewAccountSeeder(
            UserRepository users,
            FacultyRepository faculty,
            com.findmyfaculty.repo.StudentProfileRepository profiles,
            PasswordEncoder encoder) {
        this.users = users;
        this.faculty = faculty;
        this.profiles = profiles;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (studentUsername != null && !studentUsername.trim().isEmpty()) {
            String username = studentUsername.trim();
            create(username, studentPassword, Role.STUDENT, null, null);

            users.findByUsername(username)
                .filter(user -> user.role == Role.STUDENT)
                .ifPresent(user -> {
                    com.findmyfaculty.domain.StudentProfile sandeep =
                        profiles.findAll().stream()
                            .filter(profile ->
                                "11249A290".equalsIgnoreCase(profile.studentNumber))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                "Approved Sandeep roster record 11249A290 was not seeded"));

                    com.findmyfaculty.domain.StudentProfile previous =
                        profiles.findByUser_Id(user.id).orElse(null);

                    if (previous != null && !previous.id.equals(sandeep.id)) {
                        profiles.delete(previous);
                    }

                    if (sandeep.user != null && !sandeep.user.id.equals(user.id)) {
                        throw new IllegalStateException(
                            "Sandeep roster record is already linked to another user account");
                    }

                    sandeep.user = user;
                    profiles.save(sandeep);
                });
        }

        if (facultyUsername != null
                && !facultyUsername.trim().isEmpty()
                && !users.existsByUsername(facultyUsername.trim())) {
            Faculty target = faculty.findBySourceKey("staff-07")
                .orElseThrow(() -> new IllegalStateException(
                    "Real faculty record staff-07 was not seeded"));

            if (facultyPassword == null || facultyPassword.length() < 12) {
                throw new IllegalStateException(
                    "PREVIEW_FACULTY_PASSWORD must be at least 12 characters");
            }

            create(facultyUsername.trim(), facultyPassword, Role.FACULTY, target, null);
        }
    }

    private void create(
            String username,
            String password,
            Role role,
            Faculty profile,
            com.findmyfaculty.domain.ClassSection section) {
        if (username == null || username.trim().isEmpty()
                || users.existsByUsername(username)) {
            return;
        }

        if (password == null || password.length() < 12) {
            throw new IllegalStateException(
                "Preview passwords must be at least 12 characters");
        }

        UserAccount account = new UserAccount();
        account.username = username.trim();
        account.passwordHash = encoder.encode(password);
        account.role = role;
        account.faculty = profile;
        account.section = section;
        users.save(account);
    }
}
