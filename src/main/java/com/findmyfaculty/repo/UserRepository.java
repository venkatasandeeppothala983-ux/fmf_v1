package com.findmyfaculty.repo;
import com.findmyfaculty.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByFaculty_Id(Long facultyId);
    Optional<UserAccount> findByFaculty_Id(Long facultyId);
    java.util.List<UserAccount> findByRoleOrderByUsernameAsc(com.findmyfaculty.domain.Role role);
}
