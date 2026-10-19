package com.findmyfaculty.repo;

import com.findmyfaculty.domain.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByPublishedTrueOrderByCreatedAtDesc();
    @org.springframework.data.jpa.repository.Query("select a from Announcement a left join fetch a.createdBy order by a.createdAt desc")
    List<Announcement> findAllWithAuthor();
    @org.springframework.data.jpa.repository.Query("select a from Announcement a left join fetch a.createdBy where a.published = true order by a.createdAt desc")
    List<Announcement> findPublishedWithAuthor();
}
