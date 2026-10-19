package com.findmyfaculty.repo;
import com.findmyfaculty.domain.AppointmentRequest;
import com.findmyfaculty.domain.AppointmentState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AppointmentRepository extends JpaRepository<AppointmentRequest, Long> {
    List<AppointmentRequest> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
    List<AppointmentRequest> findByFaculty_IdOrderByCreatedAtDesc(Long facultyId);
    List<AppointmentRequest> findByFaculty_IdAndStateOrderByRequestedStartAsc(Long facultyId, AppointmentState state);
    List<AppointmentRequest> findAllByOrderByCreatedAtDesc();
}
