package com.findmyfaculty.repo;
import com.findmyfaculty.domain.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findBySourceKey(String sourceKey);
}
