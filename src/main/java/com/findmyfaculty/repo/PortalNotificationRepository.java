package com.findmyfaculty.repo;

import com.findmyfaculty.domain.PortalNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PortalNotificationRepository extends JpaRepository<PortalNotification, Long> {
    List<PortalNotification> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<PortalNotification> findByIdAndUser_Id(Long id, Long userId);
    long countByUser_IdAndReadAtIsNull(Long userId);
}
