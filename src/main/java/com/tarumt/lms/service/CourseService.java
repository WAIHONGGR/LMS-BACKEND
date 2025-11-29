package com.tarumt.lms.service;

//import com.tarumt.lms.dto.CreateCourseDTO;
//import com.tarumt.lms.dto.CreateLectureDTO;
//import com.tarumt.lms.dto.CreateModuleDTO;
//import com.tarumt.lms.dto.UpdateCourseDTO;
import com.tarumt.lms.model.Course;
import com.tarumt.lms.model.CourseCategory;
import com.tarumt.lms.model.Instructor;
//import com.tarumt.lms.model.Lecture;
//import com.tarumt.lms.model.Module;
import com.tarumt.lms.repo.CourseCategoryRepository;
import com.tarumt.lms.repo.CourseRepository;
import com.tarumt.lms.repo.InstructorRepository;
//import com.tarumt.lms.repo.LectureRepository;
//import com.tarumt.lms.repo.ModuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private CourseCategoryRepository courseCategoryRepository;

//    @Autowired
//    private ModuleRepository moduleRepository;
//
//    @Autowired
//    private LectureRepository lectureRepository;

    @Transactional(readOnly = true)
    public Optional<Course> getById(Long courseId) {
        log.info("Fetching course for courseId={}", courseId);
        return courseRepository.findByCourseId(courseId)
                .map(course -> {
                    log.info("Course found: id={}, title={}", course.getCourseId(), course.getTitle());
                    return course;
                })
                .or(() -> {
                    log.warn("Course not found for courseId={}", courseId);
                    return Optional.empty();
                });
    }

    @Transactional(readOnly = true)
    public List<Course> getByInstructorId(Long instructorId) {
        log.info("Fetching courses for instructorId={}", instructorId);
        List<Course> courses = courseRepository.findByInstructor_InstructorId(instructorId);
        log.info("Found {} courses for instructorId={}", courses.size(), instructorId);
        return courses;
    }

    @Transactional(readOnly = true)
    public List<Course> getByInstructorIdAndStatus(Long instructorId, String status) {
        log.info("Fetching courses for instructorId={}, status={}", instructorId, status);
        List<Course> courses = courseRepository.findByInstructor_InstructorIdAndStatus(instructorId, status);
        log.info("Found {} courses for instructorId={}, status={}", courses.size(), instructorId, status);
        return courses;
    }

    @Transactional(readOnly = true)
    public List<Course> getAllActiveCourses() {
        log.info("Fetching all active courses with instructor and category");
        // Only return courses that have both instructor and category (complete courses)
        List<Course> courses = courseRepository.findByStatusWithInstructorAndCategory("Active");
        log.info("Found {} complete active courses", courses.size());
        return courses;
    }

    @Transactional(readOnly = true)
    public List<Course> getByTitle(String title) {
        log.info("Fetching complete active courses with title containing: {}", title);
        // Only return complete active courses (with instructor and category)
        List<Course> courses = courseRepository.findByTitleContainingIgnoreCaseAndComplete(title);
        log.info("Found {} complete courses with title containing: {}", courses.size(), title);
        return courses;
    }

//    @Transactional
//    public Course createCourse(Long instructorId, CreateCourseDTO dto) {
//        log.info("Creating new course for instructorId={}, title={}", instructorId, dto.getTitle());
//
//        Instructor instructor = instructorRepository.findByInstructorId(instructorId)
//                .orElseThrow(() -> {
//                    log.warn("Instructor not found with ID {}", instructorId);
//                    return new RuntimeException("Instructor not found with ID " + instructorId);
//                });
//
//        Course course = new Course();
//        course.setTitle(dto.getTitle().trim());
//        course.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
//        course.setLevel(dto.getLevel().trim());
//        course.setStatus(dto.getStatus() != null && !dto.getStatus().isBlank() ? dto.getStatus() : "Active");
//        course.setCreditHour(dto.getCreditHour());
//        course.setImageUrl(dto.getImageUrl() != null && !dto.getImageUrl().isBlank() ? dto.getImageUrl().trim() : null);
//        course.setInstructor(instructor);
//
//        if (dto.getCourseCategoryId() != null) {
//            CourseCategory category = courseCategoryRepository.findByCourseCategoryId(dto.getCourseCategoryId())
//                    .orElseThrow(() -> {
//                        log.warn("Course category not found with ID {}", dto.getCourseCategoryId());
//                        return new RuntimeException("Course category not found with ID " + dto.getCourseCategoryId());
//                    });
//            course.setCourseCategory(category);
//        }
//
//        Course savedCourse = courseRepository.save(course);
//        log.info("Course created successfully: id={}, title={}", savedCourse.getCourseId(), savedCourse.getTitle());
//
//        // Create modules and lectures if provided
//        if (dto.getModules() != null && !dto.getModules().isEmpty()) {
//            log.info("Creating {} modules for course {}", dto.getModules().size(), savedCourse.getCourseId());
//            for (CreateModuleDTO moduleDTO : dto.getModules()) {
//                Module module = createModule(savedCourse, moduleDTO);
//                log.info("Module created: id={}, title={}", module.getModuleId(), module.getTitle());
//
//                // Create lectures for this module
//                if (moduleDTO.getLectures() != null && !moduleDTO.getLectures().isEmpty()) {
//                    log.info("Creating {} lectures for module {}", moduleDTO.getLectures().size(), module.getModuleId());
//                    for (CreateLectureDTO lectureDTO : moduleDTO.getLectures()) {
//                        Lecture lecture = createLecture(module, lectureDTO);
//                        log.info("Lecture created: id={}, title={}", lecture.getLectureId(), lecture.getTitle());
//                    }
//                }
//            }
//        }
//
//        return savedCourse;
//    }
//
//    @Transactional
//    private Module createModule(Course course, CreateModuleDTO moduleDTO) {
//        Module module = new Module();
//        module.setTitle(moduleDTO.getTitle().trim());
//        module.setLearningObj(moduleDTO.getLearningObjective() != null ? moduleDTO.getLearningObjective().trim() : null);
//        module.setCourse(course);
//
//        // Calculate module duration from lectures if not provided
//        Integer moduleDuration = moduleDTO.getModuleDuration();
//        if (moduleDuration == null && moduleDTO.getLectures() != null) {
//            moduleDuration = moduleDTO.getLectures().stream()
//                    .filter(lecture -> lecture.getDuration() != null)
//                    .mapToInt(CreateLectureDTO::getDuration)
//                    .sum();
//        }
//        module.setModuleDuration(moduleDuration != null ? moduleDuration : 0);
//
//        // curriculumItem can be left null for now
//        module.setCurriculumItem(null);
//
//        return moduleRepository.save(module);
//    }
//
//    @Transactional
//    private Lecture createLecture(Module module, CreateLectureDTO lectureDTO) {
//        Lecture lecture = new Lecture();
//        lecture.setTitle(lectureDTO.getTitle().trim());
//        lecture.setDescription(lectureDTO.getDescription() != null ? lectureDTO.getDescription().trim() : null);
//        lecture.setContentType(lectureDTO.getContentType() != null ? lectureDTO.getContentType().trim() : null);
//        lecture.setMediaUrl(lectureDTO.getMediaUrl() != null ? lectureDTO.getMediaUrl().trim() : null);
//        lecture.setArticleContent(lectureDTO.getArticleContent() != null ? lectureDTO.getArticleContent().trim() : null);
//        lecture.setModule(module);
//
//        return lectureRepository.save(lecture);
//    }
//
//    @Transactional
//    public Course updateCourse(Long courseId, Long instructorId, UpdateCourseDTO dto) {
//        log.info("Updating course for courseId={}, instructorId={}", courseId, instructorId);
//
//        Course course = courseRepository.findByCourseId(courseId)
//                .orElseThrow(() -> {
//                    log.warn("Course not found with ID {}", courseId);
//                    return new RuntimeException("Course not found with ID " + courseId);
//                });
//
//        // Verify instructor ownership
//        if (course.getInstructor() == null || !course.getInstructor().getInstructorId().equals(instructorId)) {
//            log.warn("Course {} does not belong to instructor {}", courseId, instructorId);
//            throw new RuntimeException("Course does not belong to this instructor");
//        }
//
//        course.setTitle(dto.getTitle().trim());
//        course.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
//        course.setLevel(dto.getLevel().trim());
//
//        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
//            course.setStatus(dto.getStatus());
//        }
//
//        course.setCreditHour(dto.getCreditHour());
//
//        if (dto.getImageUrl() != null) {
//            course.setImageUrl(dto.getImageUrl().isBlank() ? null : dto.getImageUrl().trim());
//        }
//
//        if (dto.getCourseCategoryId() != null) {
//            CourseCategory category = courseCategoryRepository.findByCourseCategoryId(dto.getCourseCategoryId())
//                    .orElseThrow(() -> {
//                        log.warn("Course category not found with ID {}", dto.getCourseCategoryId());
//                        return new RuntimeException("Course category not found with ID " + dto.getCourseCategoryId());
//                    });
//            course.setCourseCategory(category);
//        } else {
//            course.setCourseCategory(null);
//        }
//
//        Course updatedCourse = courseRepository.save(course);
//        log.info("Course updated successfully: id={}, title={}", updatedCourse.getCourseId(), updatedCourse.getTitle());
//
//        // Update modules and lectures if provided
//        if (dto.getModules() != null) {
//            log.info("Updating modules for course {}", updatedCourse.getCourseId());
//
//            // Delete all existing modules and lectures for this course
//            List<Module> existingModules = moduleRepository.findByCourse_CourseId(updatedCourse.getCourseId());
//            for (Module existingModule : existingModules) {
//                // Delete all lectures for this module first
//                List<Lecture> existingLectures = lectureRepository.findByModule_ModuleId(existingModule.getModuleId());
//                lectureRepository.deleteAll(existingLectures);
//                log.info("Deleted {} lectures for module {}", existingLectures.size(), existingModule.getModuleId());
//            }
//            // Delete all modules
//            moduleRepository.deleteAll(existingModules);
//            log.info("Deleted {} existing modules for course {}", existingModules.size(), updatedCourse.getCourseId());
//
//            // Create new modules and lectures
//            if (!dto.getModules().isEmpty()) {
//                log.info("Creating {} new modules for course {}", dto.getModules().size(), updatedCourse.getCourseId());
//                for (CreateModuleDTO moduleDTO : dto.getModules()) {
//                    Module module = createModule(updatedCourse, moduleDTO);
//                    log.info("Module created: id={}, title={}", module.getModuleId(), module.getTitle());
//
//                    // Create lectures for this module
//                    if (moduleDTO.getLectures() != null && !moduleDTO.getLectures().isEmpty()) {
//                        log.info("Creating {} lectures for module {}", moduleDTO.getLectures().size(), module.getModuleId());
//                        for (CreateLectureDTO lectureDTO : moduleDTO.getLectures()) {
//                            Lecture lecture = createLecture(module, lectureDTO);
//                            log.info("Lecture created: id={}, title={}", lecture.getLectureId(), lecture.getTitle());
//                        }
//                    }
//                }
//            }
//        }
//
//        return updatedCourse;
//    }

    @Transactional
    public void deleteCourse(Long courseId, Long instructorId) {
        log.info("Deleting course for courseId={}, instructorId={}", courseId, instructorId);

        Course course = courseRepository.findByCourseId(courseId)
                .orElseThrow(() -> {
                    log.warn("Course not found with ID {}", courseId);
                    return new RuntimeException("Course not found with ID " + courseId);
                });

        // Verify instructor ownership
        if (course.getInstructor() == null || !course.getInstructor().getInstructorId().equals(instructorId)) {
            log.warn("Course {} does not belong to instructor {}", courseId, instructorId);
            throw new RuntimeException("Course does not belong to this instructor");
        }

        courseRepository.delete(course);
        log.info("Course deleted successfully: id={}", courseId);
    }
}

