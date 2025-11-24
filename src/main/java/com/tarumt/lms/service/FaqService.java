package com.tarumt.lms.service;

import com.tarumt.lms.model.Faq;
import com.tarumt.lms.repo.FaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<Faq> getAllActiveFaqs() {
        log.info("Fetching all active FAQs");
        return faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Faq> getFaqById(Long id) {
        log.info("Fetching FAQ by ID: {}", id);
        return faqRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Faq> searchActiveFaqs(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return getAllActiveFaqs();
        }
        log.info("Searching active FAQs with search term: {}", searchTerm);
        return faqRepository.searchActiveFaqs(searchTerm);
    }

    @Transactional(readOnly = true)
    public List<Faq> getActiveFaqsByCategory(String category) {
        if (category == null || category.isBlank()) {
            return getAllActiveFaqs();
        }
        log.info("Fetching active FAQs by category: {}", category);
        return faqRepository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);
    }

    @Transactional(readOnly = true)
    public List<Faq> findRelevantFaqs(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            // Return all FAQs if no query (ordered by display_order)
            return faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        }

        // Search FAQs based on user query
        List<Faq> matchingFaqs = faqRepository.searchActiveFaqs(userQuery);

        // If no matches found, try searching by individual keywords
        if (matchingFaqs.isEmpty()) {
            String[] keywords = userQuery.toLowerCase().split("\\s+");
            for (String keyword : keywords) {
                if (keyword.length() >= 3) {
                    List<Faq> keywordMatches = faqRepository.findByKeyword(keyword);
                    if (!keywordMatches.isEmpty()) {
                        matchingFaqs = keywordMatches;
                        break;
                    }
                }
            }
        }

        // Return all matching FAQs (no limit since we only have 10 FAQs total)
        // They are already ordered by displayOrder from the repository query
        return matchingFaqs;
    }
}

