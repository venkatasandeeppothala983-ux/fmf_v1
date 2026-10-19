package com.findmyfaculty.repo;

import com.findmyfaculty.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
