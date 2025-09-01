package com.matsuzaka.bigdata.service;

import com.matsuzaka.bigdata.dto.CourseStudentInfo;
import com.matsuzaka.bigdata.dto.PopularCourseInfo;
import com.matsuzaka.bigdata.dto.QueryResult;
import com.matsuzaka.bigdata.dto.StudentCourseInfo;
import com.matsuzaka.bigdata.entity.Course;
import com.matsuzaka.bigdata.entity.Enrollment;
import com.matsuzaka.bigdata.entity.Student;
import com.matsuzaka.bigdata.repository.CourseRepository;
import com.matsuzaka.bigdata.repository.EnrollmentRepository;
import com.matsuzaka.bigdata.repository.StudentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PerformanceService {
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    // === 物件化後端資料結構 ===
    // 這些 Map 就是我們預先載入到記憶體中的資料快取
    // 這是實現「物件化後端資料結構」的核心

    /**
     * 儲存所有學生資料，Key 為 student.id，Value 為 Student 物件
     * 作用：可以透過 O(1) 的時間複雜度快速查找學生詳細資訊
     */
    private Map<Long, Student> studentMap;

    /**
     * 儲存所有課程資料，Key 為 course.id，Value 為 Course 物件
     * 作用：可以透過 O(1) 的時間複雜度快速查找課程詳細資訊
     */
    private Map<Long, Course> courseMap;

    /**
     * 以學生ID為索引，儲存該學生的所有選課紀錄
     * Key: student.id
     * Value: List<Enrollment>，該學生的所有選課紀錄
     * 作用：這是針對「功能1」的優化，可以 O(1) 找到某學生的所有選課，不需遍歷全部紀錄
     */
    private Map<Long, List<Enrollment>> enrollmentsByStudent;

    /**
     * 以課程ID為索引，儲存該課程的所有選課紀錄
     * Key: course.id
     * Value: List<Enrollment>，該課程的所有選課紀錄
     * 作用：這是針對「功能2」和「功能3」的優化，可以 O(1) 找到某課程的所有學生
     */
    private Map<Long, List<Enrollment>> enrollmentsByCourse;


    public PerformanceService(EnrollmentRepository enrollmentRepository, StudentRepository studentRepository, CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * @PostConstruct
     * Spring Boot 在完成此 Service 的建構後，會自動執行此方法
     * 我們在這裡將所有資料從資料庫載入到記憶體中的 Map
     */
    @PostConstruct
    public void initializeInMemoryCache() {
        System.out.println("開始初始化記憶體快取...");
        long startTime = System.currentTimeMillis();

        // 1. 載入所有學生和課程資料，並轉換成 Map
        studentMap = studentRepository.findAll().stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        courseMap = courseRepository.findAll().stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        // 2. 載入所有選課紀錄
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();

        // 3. 建立索引
        // 使用 groupingBy 一次性建立兩個索引 Map，效率更高
        enrollmentsByStudent = new HashMap<>();
        enrollmentsByCourse = new HashMap<>();

        for (Enrollment e : allEnrollments) {
            enrollmentsByStudent.computeIfAbsent(e.getStudentId(), k -> new ArrayList<>()).add(e);
            enrollmentsByCourse.computeIfAbsent(e.getCourseId(), k -> new ArrayList<>()).add(e);
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("記憶體快取初始化完成。共載入 %d 筆選課紀錄，耗時: %d ms%n", allEnrollments.size(), (endTime - startTime));
    }


    // --- 功能 1: 根據學生ID查詢課程 ---

    public QueryResult<StudentCourseInfo> findCoursesByStudentId_DB(Long studentId) {
        // 暖機
        for (int i = 0; i < 5; i++) {
            enrollmentRepository.findCourseDetailsByStudentIdNative(studentId);
        }
        long startTime = System.nanoTime();
        List<Object[]> results = enrollmentRepository.findCourseDetailsByStudentIdNative(studentId);
        long endTime = System.nanoTime();

        List<StudentCourseInfo> dtoList = results.stream()
                .map(res -> new StudentCourseInfo((String) res[0], (Integer) res[1], ((java.sql.Date) res[2]).toLocalDate()))
                .collect(Collectors.toList());
        return new QueryResult<>(dtoList, (endTime - startTime) / 1_000_000);
    }

    public QueryResult<StudentCourseInfo> findCoursesByStudentId_InMemory(Long studentId) {
        // 暖機
        for (int i = 0; i < 5; i++) {
            enrollmentsByStudent.getOrDefault(studentId, Collections.emptyList());
        }

        long startTime = System.nanoTime();
        // 核心查詢邏輯
        List<Enrollment> studentEnrollments = enrollmentsByStudent.getOrDefault(studentId, Collections.emptyList());
        List<StudentCourseInfo> resultList = new ArrayList<>();
        if (!studentEnrollments.isEmpty()) {
            for (Enrollment enrollment : studentEnrollments) {
                Course course = courseMap.get(enrollment.getCourseId());
                if (course != null) {
                    resultList.add(new StudentCourseInfo(course.getTitle(), course.getCredit(), enrollment.getEnrollmentDate()));
                }
            }
        }
        // 核心查詢邏輯結束
        long endTime = System.nanoTime();
        return new QueryResult<>(resultList, (endTime - startTime) / 1_000_000);
    }

    // --- 功能 2: 根據課程ID查詢學生 ---

    public QueryResult<CourseStudentInfo> findStudentsByCourseId_DB(Long courseId) {
        // 暖機
        for (int i = 0; i < 5; i++) {
            enrollmentRepository.findStudentDetailsByCourseIdNative(courseId);
        }
        long startTime = System.nanoTime();
        List<Object[]> results = enrollmentRepository.findStudentDetailsByCourseIdNative(courseId);
        long endTime = System.nanoTime();
        List<CourseStudentInfo> dtoList = results.stream()
                .map(res -> new CourseStudentInfo((String) res[0], (String) res[1], ((java.sql.Date) res[2]).toLocalDate()))
                .collect(Collectors.toList());
        return new QueryResult<>(dtoList, (endTime - startTime) / 1_000_000);
    }

    public QueryResult<CourseStudentInfo> findStudentsByCourseId_InMemory(Long courseId) {
        // 暖機
        for (int i = 0; i < 5; i++) {
            enrollmentsByCourse.getOrDefault(courseId, Collections.emptyList());
        }
        long startTime = System.nanoTime();
        // 核心查詢邏輯
        List<Enrollment> courseEnrollments = enrollmentsByCourse.getOrDefault(courseId, Collections.emptyList());
        List<CourseStudentInfo> resultList = new ArrayList<>();
        if (!courseEnrollments.isEmpty()) {
            for (Enrollment enrollment : courseEnrollments) {
                Student student = studentMap.get(enrollment.getStudentId());
                if (student != null) {
                    resultList.add(new CourseStudentInfo(student.getName(), student.getEmail(), enrollment.getEnrollmentDate()));
                }
            }
        }
        // 核心查詢邏輯結束
        long endTime = System.nanoTime();
        return new QueryResult<>(resultList, (endTime - startTime) / 1_000_000);
    }

    // --- 功能 3: 查詢最熱門的10門課程 ---

    public QueryResult<PopularCourseInfo> findTop10PopularCourses_DB() {
        // 暖機
        for (int i = 0; i < 5; i++) {
            enrollmentRepository.findTop10PopularCoursesNative();
        }
        long startTime = System.nanoTime();
        List<Object[]> results = enrollmentRepository.findTop10PopularCoursesNative();
        long endTime = System.nanoTime();
        List<PopularCourseInfo> dtoList = results.stream()
                .map(res -> {
                    Long courseId = ((Number) res[0]).longValue();
                    String title = courseMap.get(courseId).getTitle();
                    long count = ((Number) res[1]).longValue();
                    return new PopularCourseInfo(title, count);
                })
                .collect(Collectors.toList());
        return new QueryResult<>(dtoList, (endTime - startTime) / 1_000_000);
    }

    public QueryResult<PopularCourseInfo> findTop10PopularCourses_InMemory() {
        // 暖機
        for (int i = 0; i < 5; i++) {
            enrollmentsByCourse.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                    .limit(10)
                    .toList();
        }

        long startTime = System.nanoTime();
        // 核心查詢邏輯: 遍歷 enrollmentsByCourse 這個 Map，找出 list size 最大的前10名
        List<PopularCourseInfo> resultList = enrollmentsByCourse.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .limit(10)
                .map(entry -> {
                    Course course = courseMap.get(entry.getKey());
                    long count = entry.getValue().size();
                    return new PopularCourseInfo(course.getTitle(), count);
                })
                .collect(Collectors.toList());
        // 核心查詢邏輯結束
        long endTime = System.nanoTime();
        return new QueryResult<>(resultList, (endTime - startTime) / 1_000_000);
    }
}
