package com.educationpro.repository;

import com.educationpro.domain.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    @Query("SELECT tp FROM TeacherProfile tp JOIN FETCH tp.user ORDER BY tp.createdAt DESC")
    List<TeacherProfile> findAllWithUser();

    Optional<TeacherProfile> findByUserId(Long userId);
}
