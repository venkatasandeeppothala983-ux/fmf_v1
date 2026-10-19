package com.findmyfaculty.repo;
import com.findmyfaculty.domain.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Set;
public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findByFaculty_IdOrderByDayAscStartTimeAsc(Long facultyId);
    List<TimetableEntry> findByFaculty_IdAndDayIgnoreCaseOrderByStartTimeAsc(Long facultyId, String day);
    List<TimetableEntry> findByDayIgnoreCaseAndRoomIgnoreCase(String day, String room);
    @Modifying @Query("delete from TimetableEntry t where t.faculty.id in :facultyIds")
    void deleteForFacultyIds(Set<Long> facultyIds);
}
