package com.findmyfaculty.repo;

import com.findmyfaculty.domain.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
