package com.findmyfaculty.repo;
import com.findmyfaculty.domain.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ClassSectionRepository extends JpaRepository<ClassSection,Long>{Optional<ClassSection> findBySourceKey(String sourceKey);}
