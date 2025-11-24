package com.tarumt.lms.repo;

import com.tarumt.lms.model.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {

    Optional<CourseCategory> findByCourseCategoryId(Long courseCategoryId);
}

