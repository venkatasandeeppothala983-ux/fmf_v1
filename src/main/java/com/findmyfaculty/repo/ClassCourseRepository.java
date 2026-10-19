package com.findmyfaculty.repo;
import com.findmyfaculty.domain.ClassCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ClassCourseRepository extends JpaRepository<ClassCourse,Long>{List<ClassCourse> findBySection_IdOrderByNameAsc(Long id);}
