package com.tarumt.lms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "coursecategory")
@NoArgsConstructor
@AllArgsConstructor
public class CourseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_category_id")
    private Long courseCategoryId;

    @Column(name = "category_type", nullable = false, length = 100)
    private String categoryType;

    @Column(name = "interest", length = 100)
    private String interest;
}

