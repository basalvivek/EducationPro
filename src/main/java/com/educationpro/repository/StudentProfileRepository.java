package com.educationpro.repository;

import com.educationpro.domain.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    @Query("SELECT s FROM StudentProfile s LEFT JOIN FETCH s.user ORDER BY s.createdAt DESC")
    List<StudentProfile> findAllWithUser();
}
