package com.tarumt.lms.service;

import com.tarumt.lms.model.Course;
import com.tarumt.lms.model.Enrollment;
import com.tarumt.lms.model.Faq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Database Context Service - Provides curated LMS data to the AI chat experience.
 * This implementation focuses on the course catalog so the chatbot can answer
 * "what can I learn" and "which course fits me" style questions.
 * Also includes FAQ context for navigation and system usage questions.
 * Includes student enrollment details when the user is a student.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseContextService {

    private final CourseService courseService;
    private final FaqService faqService;
    private final EnrollmentService enrollmentService;

    private static final int MAX_RECOMMENDED_COURSES = 8;
    private static final int MAX_CATEGORY_HIGHLIGHTS = 5;
    private static final int MAX_INSTRUCTOR_HIGHLIGHTS = 5;

    /**
     * Build a database context string tailored to the incoming query so the LLM
     * understands which courses, categories, and instructors are available.
     */
    public String getDatabaseContext(String userQuery, String userId, String userRole) {
        String normalizedQuery = userQuery == null ? "" : userQuery.trim();
        String queryLower = normalizedQuery.toLowerCase(Locale.ENGLISH);

        StringBuilder context = new StringBuilder();

        context.append("DATABASE SCHEMA:\n");
        context.append("- Courses: course_id, title, description, status, level, duration, instructor_id, category_id\n");
        context.append("- CourseCategories: category_id, name, description\n");
        context.append("- Instructors: instructor_id, name, specialization, experience, status\n\n");

        List<Course> activeCourses = fetchActiveCourses();
        if (activeCourses.isEmpty()) {
            context.append("ACTIVE COURSE INVENTORY:\n");
            context.append("No active courses are currently published in the LMS catalog.\n");
            return context.toString();
        }

        appendInventoryOverview(context, activeCourses);
        appendCategoryHighlights(context, activeCourses);
        appendInstructorHighlights(context, activeCourses);

        if (shouldProvideRecommendations(queryLower)) {
            List<Course> targeted = findMatchingCourses(activeCourses, queryLower);
            if (targeted.isEmpty()) {
                context.append("\nAVAILABLE ACTIVE COURSES:\n");
                context.append("IMPORTANT: Display each course with line breaks. Format:\n");
                context.append("COURSE: [Name]\n");
                context.append("  - Category: [Category]\n");
                context.append("  - Instructor: [Instructor]\n");
                context.append("(Leave a blank line between courses)\n\n");
                targeted = activeCourses.stream()
                        .sorted(Comparator.comparing(this::resolveTitle, String.CASE_INSENSITIVE_ORDER))
                        .limit(MAX_RECOMMENDED_COURSES)
                        .collect(Collectors.toList());
            } else {
                context.append("\nCOURSE RECOMMENDATIONS BASED ON USER QUERY:\n");
                context.append("IMPORTANT: Display each course with line breaks. Format:\n");
                context.append("COURSE: [Name]\n");
                context.append("  - Category: [Category]\n");
                context.append("  - Instructor: [Instructor]\n");
                context.append("(Leave a blank line between courses)\n\n");
            }

            targeted.forEach(course -> context.append(formatCourseLine(course)));
        }

        // Add FAQ context for navigation and system usage questions
        appendFaqContext(context, normalizedQuery, queryLower);

        // Add student enrollment context if user is a student and asking about enrollments
        if (userId != null && !userId.isBlank() && "STUDENT".equalsIgnoreCase(userRole)) {
            if (isEnrollmentRelatedQuery(queryLower)) {
                appendEnrollmentContext(context, userId, queryLower);
            }
        }

        return context.toString();
    }

    private List<Course> fetchActiveCourses() {
        try {
            return courseService.getAllActiveCourses();
        } catch (Exception e) {
            log.error("Unable to fetch active courses for AI chat context", e);
            return Collections.emptyList();
        }
    }

    private void appendInventoryOverview(StringBuilder context, List<Course> activeCourses) {
        context.append("ACTIVE COURSE INVENTORY:\n");
        context.append("Total active courses: ").append(activeCourses.size()).append("\n");

        Map<String, Long> statusCounts = activeCourses.stream()
                .collect(Collectors.groupingBy(this::resolveStatus, Collectors.counting()));
        if (!statusCounts.isEmpty()) {
            context.append("Status breakdown:\n");
            statusCounts.forEach((status, count) ->
                    context.append("- ").append(status).append(": ").append(count).append("\n"));
        }
        context.append("\n");
    }

    private void appendCategoryHighlights(StringBuilder context, List<Course> courses) {
        Map<String, Long> categoryCounts = courses.stream()
                .collect(Collectors.groupingBy(this::resolveCategoryName, Collectors.counting()));

        if (categoryCounts.isEmpty()) {
            return;
        }

        context.append("TOP COURSE CATEGORIES:\n");
        categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(MAX_CATEGORY_HIGHLIGHTS)
                .forEach(entry -> context.append("- ")
                        .append(entry.getKey())
                        .append(" → ")
                        .append(entry.getValue())
                        .append(" active course(s)\n"));
        context.append("\n");
    }

    private void appendInstructorHighlights(StringBuilder context, List<Course> courses) {
        Map<String, Long> instructorCounts = courses.stream()
                .collect(Collectors.groupingBy(this::resolveInstructorName, Collectors.counting()));

        if (instructorCounts.isEmpty()) {
            return;
        }

        context.append("INSTRUCTOR HIGHLIGHTS:\n");
        instructorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(MAX_INSTRUCTOR_HIGHLIGHTS)
                .forEach(entry -> context.append("- ")
                        .append(entry.getKey())
                        .append(" teaches ")
                        .append(entry.getValue())
                        .append(" active course(s)\n"));
        context.append("\n");
    }

    private List<Course> findMatchingCourses(List<Course> courses, String queryLower) {
        if (queryLower == null || queryLower.isBlank()) {
            return Collections.emptyList();
        }

        String[] tokens = queryLower.split("[^a-z0-9]+");
        List<String> keywords = Arrays.stream(tokens)
                .filter(token -> token.length() >= 3)
                .distinct()
                .collect(Collectors.toList());

        if (keywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<CourseScore> scored = new ArrayList<>();
        for (Course course : courses) {
            int score = scoreCourse(course, keywords);
            if (score > 0) {
                scored.add(new CourseScore(course, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        return scored.stream()
                .map(cs -> cs.course)
                .limit(MAX_RECOMMENDED_COURSES)
                .collect(Collectors.toList());
    }

    private int scoreCourse(Course course, List<String> keywords) {
        String searchable = buildSearchableText(course);
        int score = 0;
        for (String keyword : keywords) {
            if (!keyword.isBlank() && searchable.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private String buildSearchableText(Course course) {
        return (resolveTitle(course) + " "
                + resolveCategoryName(course) + " "
                + resolveInstructorName(course) + " "
                + resolveDescription(course))
                .toLowerCase(Locale.ENGLISH);
    }

    private String formatCourseLine(Course course) {
        StringBuilder line = new StringBuilder();
        line.append("COURSE: ").append(resolveTitle(course)).append("\n");
        line.append("  - Category: ").append(resolveCategoryName(course)).append("\n");
        line.append("  - Instructor: ").append(resolveInstructorName(course)).append("\n");
        String status = resolveStatus(course);
        if (!status.isBlank() && !status.equalsIgnoreCase("ACTIVE")) {
            line.append("  - Status: ").append(status).append("\n");
        }
        return line.append("\n").toString();
    }

    private boolean shouldProvideRecommendations(String queryLower) {
        if (queryLower == null || queryLower.isBlank()) {
            return true;
        }
        return queryLower.contains("course")
                || queryLower.contains("class")
                || queryLower.contains("subject")
                || queryLower.contains("learn")
                || queryLower.contains("enroll")
                || queryLower.contains("recommend")
                || queryLower.contains("suggest")
                || queryLower.contains("interested");
    }

    private String resolveTitle(Course course) {
        String title = invokeStringGetter(course, "getTitle", "getName");
        if (!title.isBlank()) {
            return title;
        }
        String code = invokeStringGetter(course, "getCourseCode");
        if (!code.isBlank()) {
            return "Course " + code;
        }
        String id = invokeStringGetter(course, "getCourseId");
        return id.isBlank() ? "Untitled course" : "Course #" + id;
    }

    private String resolveDescription(Course course) {
        return invokeStringGetter(course, "getDescription", "getShortDescription", "getOverview");
    }

    private String resolveStatus(Course course) {
        String status = invokeStringGetter(course, "getStatus");
        return status.isBlank() ? "ACTIVE" : status.toUpperCase(Locale.ENGLISH);
    }

    private String resolveCategoryName(Course course) {
        String direct = invokeStringGetter(course, "getCategoryName", "getCategoryLabel");
        if (!direct.isBlank()) {
            return direct;
        }

        Object category = invokeObjectGetter(course, "getCategory", "getCourseCategory");
        if (category != null) {
            String nested = invokeStringGetter(category, "getCategoryName", "getName", "getTitle");
            if (!nested.isBlank()) {
                return nested;
            }
        }
        return "General";
    }

    private String resolveInstructorName(Course course) {
        String direct = invokeStringGetter(course, "getInstructorName", "getInstructorFullName");
        if (!direct.isBlank()) {
            return direct;
        }

        Object instructor = invokeObjectGetter(course, "getInstructor");
        if (instructor != null) {
            String nested = invokeStringGetter(instructor, "getName", "getFullName");
            if (!nested.isBlank()) {
                return nested;
            }
            String id = invokeStringGetter(instructor, "getInstructorId");
            if (!id.isBlank()) {
                return "Instructor #" + id;
            }
        }
        return "Instructor (TBA)";
    }

    private Object invokeObjectGetter(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (Exception ignored) {
                // try next method name
            }
        }
        return null;
    }

    private String invokeStringGetter(Object target, String... methodNames) {
        if (target == null || methodNames == null) {
            return "";
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof String str && !str.isBlank()) {
                    return str.trim();
                } else if (value != null && !(value instanceof String)) {
                    return value.toString();
                }
            } catch (NoSuchMethodException ignored) {
                // move to next
            } catch (Exception e) {
                log.debug("Unable to read method {} on {}", methodName, target.getClass().getSimpleName(), e);
            }
        }
        return "";
    }

    /**
     * Append relevant FAQ context to the database context string.
     * This helps the AI chatbot answer navigation and system usage questions.
     */
    private void appendFaqContext(StringBuilder context, String userQuery, String queryLower) {
        try {
            // Check if the query seems FAQ-related (navigation, how-to, system usage)
            if (isFaqRelatedQuery(queryLower)) {
                List<Faq> relevantFaqs = faqService.findRelevantFaqs(userQuery);

                if (!relevantFaqs.isEmpty()) {
                    context.append("\n\n=== FREQUENTLY ASKED QUESTIONS (FAQs) ===\n");
                    context.append("The following FAQs may help answer the user's question:\n\n");

                    for (int i = 0; i < relevantFaqs.size(); i++) {
                        Faq faq = relevantFaqs.get(i);
                        context.append(String.format("%d. Q: %s\n", i + 1, faq.getQuestion()));
                        context.append(String.format("   A: %s\n\n", faq.getAnswer()));
                    }

                    context.append("Use these FAQs to provide accurate, step-by-step guidance for navigation and system usage questions.\n");
                }
            }
        } catch (Exception e) {
            log.error("Error fetching FAQ context for AI chat", e);
            // Don't fail the entire context building if FAQ fetch fails
        }
    }

    /**
     * Determine if the user query is related to FAQs (navigation, how-to, system usage).
     */
    private boolean isFaqRelatedQuery(String queryLower) {
        if (queryLower == null || queryLower.isBlank()) {
            return false;
        }

        // Keywords that suggest FAQ-related questions
        String[] faqKeywords = {
                "how", "where", "what", "when", "why", "can i", "how do i", "how can i",
                "navigate", "navigation", "find", "access", "view", "see", "show",
                "change", "update", "edit", "modify", "profile", "password", "settings",
                "enroll", "enrollment", "register", "course", "courses", "history",
                "logout", "login", "sign", "contact", "support", "help", "guide",
                "step", "steps", "instructions", "tutorial", "way", "method"
        };

        for (String keyword : faqKeywords) {
            if (queryLower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine if the user query is related to enrollments (my courses, enrolled courses, progress, etc.).
     */
    private boolean isEnrollmentRelatedQuery(String queryLower) {
        if (queryLower == null || queryLower.isBlank()) {
            return false;
        }

        // Keywords that suggest enrollment-related questions
        String[] enrollmentKeywords = {
                "my courses", "my course", "enrolled", "enrollment", "enrollments",
                "what courses", "which courses", "courses i", "courses am i",
                "progress", "my progress", "course progress", "completion",
                "currently enrolled", "active courses", "taking", "registered"
        };

        for (String keyword : enrollmentKeywords) {
            if (queryLower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Append student enrollment context to the database context string.
     * This helps the AI chatbot answer questions about the student's enrolled courses.
     */
    private void appendEnrollmentContext(StringBuilder context, String userId, String queryLower) {
        try {
            Long studentId = parseStudentId(userId);
            if (studentId == null) {
                log.warn("Unable to parse studentId from userId: {}", userId);
                return;
            }

            // Fetch all enrollments for the student
            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByStudentId(studentId);

            if (enrollments.isEmpty()) {
                context.append("\n\n=== STUDENT ENROLLMENT INFORMATION ===\n");
                context.append("The student is not currently enrolled in any courses.\n");
                return;
            }

            // Filter by status if query mentions specific status
            List<Enrollment> filteredEnrollments = enrollments;
            if (queryLower.contains("completed") || queryLower.contains("finished")) {
                filteredEnrollments = enrollmentService.getEnrollmentsByStudentIdAndStatus(studentId, "Completed");
            } else if (queryLower.contains("withdrawn") || queryLower.contains("dropped")) {
                filteredEnrollments = enrollmentService.getEnrollmentsByStudentIdAndStatus(studentId, "Withdrawn");
            } else if (queryLower.contains("enrolled") || queryLower.contains("active") || queryLower.contains("current")) {
                filteredEnrollments = enrollmentService.getEnrollmentsByStudentIdAndStatus(studentId, "Enrolled");
            }

            if (filteredEnrollments.isEmpty()) {
                context.append("\n\n=== STUDENT ENROLLMENT INFORMATION ===\n");
                context.append("No enrollments found matching the query criteria.\n");
                return;
            }

            context.append("\n\n=== STUDENT ENROLLMENT INFORMATION ===\n");
            context.append("The student is enrolled in the following courses:\n\n");

            for (int i = 0; i < filteredEnrollments.size(); i++) {
                Enrollment enrollment = filteredEnrollments.get(i);
                Course course = enrollment.getCourse();

                context.append(String.format("%d. COURSE: %s\n", i + 1, resolveTitle(course)));
                context.append(String.format("   - Category: %s\n", resolveCategoryName(course)));
                context.append(String.format("   - Instructor: %s\n", resolveInstructorName(course)));

                String status = enrollment.getStatus();
                if (status != null && !status.isBlank()) {
                    context.append(String.format("   - Enrollment Status: %s\n", status));
                }

                Integer progress = enrollment.getProgress();
                if (progress != null) {
                    context.append(String.format("   - Progress: %d%%\n", progress));
                }

                context.append("\n");
            }

            context.append("Use this enrollment information to answer questions about the student's enrolled courses, progress, and course status.\n");

        } catch (Exception e) {
            log.error("Error fetching enrollment context for AI chat (studentId={})", userId, e);
            // Don't fail the entire context building if enrollment fetch fails
        }
    }

    /**
     * Parse student ID from userId string.
     */
    private Long parseStudentId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            log.warn("Unable to parse studentId from userId: {}", userId);
            return null;
        }
    }

    private static class CourseScore {
        private final Course course;
        private final int score;

        private CourseScore(Course course, int score) {
            this.course = course;
            this.score = score;
        }
    }
}
