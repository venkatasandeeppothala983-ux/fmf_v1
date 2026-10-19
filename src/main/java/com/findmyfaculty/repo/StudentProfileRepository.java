package com.findmyfaculty.repo;

import com.findmyfaculty.domain.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser_Id(Long userId);
    Optional<StudentProfile> findByUser_Username(String username);
    boolean existsByStudentNumberIgnoreCase(String studentNumber);
    long countByActiveTrue();
}
