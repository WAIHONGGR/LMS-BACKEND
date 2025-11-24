package com.tarumt.lms.repo;

import com.tarumt.lms.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    /**
     * Find all active FAQs ordered by display order
     */
    List<Faq> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Find FAQs by category
     */
    List<Faq> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(String category);

    /**
     * Search FAQs by question or answer containing the search term (case-insensitive)
     */
    @Query("SELECT f FROM Faq f WHERE f.isActive = true AND " +
            "(LOWER(f.question) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(f.answer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(f.keywords) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY f.displayOrder ASC")
    List<Faq> searchActiveFaqs(@Param("searchTerm") String searchTerm);

    /**
     * Find FAQs by keywords (searches in keywords field)
     */
    @Query("SELECT f FROM Faq f WHERE f.isActive = true AND " +
            "LOWER(f.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY f.displayOrder ASC")
    List<Faq> findByKeyword(@Param("keyword") String keyword);
}

