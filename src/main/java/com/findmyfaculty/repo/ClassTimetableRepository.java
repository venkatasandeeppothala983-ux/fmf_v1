package com.findmyfaculty.repo;
import com.findmyfaculty.domain.ClassTimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ClassTimetableRepository extends JpaRepository<ClassTimetableEntry,Long>{List<ClassTimetableEntry> findBySection_IdOrderByDayAscStartTimeAsc(Long id);List<ClassTimetableEntry> findBySection_IdAndDayIgnoreCaseOrderByStartTimeAsc(Long id,String day);List<ClassTimetableEntry> findByDayIgnoreCaseAndRoomIgnoreCase(String day,String room);}
