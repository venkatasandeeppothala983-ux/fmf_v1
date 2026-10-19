package com.findmyfaculty.repo;
import com.findmyfaculty.domain.FacultyStatusUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface StatusRepository extends JpaRepository<FacultyStatusUpdate, Long> {
    Optional<FacultyStatusUpdate> findFirstByFaculty_IdOrderByUpdatedAtDesc(Long facultyId);
    List<FacultyStatusUpdate> findByFaculty_IdOrderByUpdatedAtDesc(Long facultyId);
}
