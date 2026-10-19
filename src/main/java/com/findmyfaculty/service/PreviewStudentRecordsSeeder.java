package com.findmyfaculty.service;

import com.findmyfaculty.domain.ClassSection;
import com.findmyfaculty.domain.Department;
import com.findmyfaculty.domain.StudentProfile;
import com.findmyfaculty.repo.ClassSectionRepository;
import com.findmyfaculty.repo.DepartmentRepository;
import com.findmyfaculty.repo.StudentProfileRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/** User-approved public-preview records; these are directory records, not login accounts. */
@Component
@Profile("preview")
@Order(Ordered.LOWEST_PRECEDENCE - 5)
public class PreviewStudentRecordsSeeder implements ApplicationRunner {
    private final StudentProfileRepository profiles;
    private final DepartmentRepository departments;
    private final ClassSectionRepository sections;

    public PreviewStudentRecordsSeeder(StudentProfileRepository profiles, DepartmentRepository departments,
                                       ClassSectionRepository sections) {
        this.profiles = profiles;
        this.departments = departments;
        this.sections = sections;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Department department = departments.findByCodeIgnoreCase("ADND").orElseGet(() -> {
            Department value = new Department();
            value.code = "ADND";
            value.name = "ADND"; // Keep the user-provided label; do not expand it without a verified name.
            value.active = true;
            return departments.save(value);
        });
        ClassSection section = sections.findBySourceKey("class-05")
            .orElseThrow(() -> new IllegalStateException("Preview S3 section class-05 was not seeded"));

        List<Record> records = Arrays.asList(
            new Record("11249A290", "VENKATA SANDEEP"),
            new Record("11249A301", "ADITYA"),
            new Record("11249A266", "YUGANDHAR"),
            new Record("11249A312", "ANIRUDH"),
            new Record("11249A435", "KOUSHIK")
        );
        for (Record record : records) {
            boolean exists = profiles.findAll().stream()
                .anyMatch(profile -> profile.studentNumber != null
                    && profile.studentNumber.equalsIgnoreCase(record.studentNumber));
            if (exists) continue;
            StudentProfile profile = new StudentProfile();
            profile.fullName = record.fullName;
            profile.studentNumber = record.studentNumber;
            profile.department = department;
            profile.yearLabel = "III Year";
            profile.section = section;
            profile.active = true;
            profiles.save(profile);
        }
    }

    private static final class Record {
        final String studentNumber;
        final String fullName;
        Record(String studentNumber, String fullName) {
            this.studentNumber = studentNumber;
            this.fullName = fullName;
        }
    }
}
