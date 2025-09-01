package com.matsuzaka.bigdata.repository;

import com.matsuzaka.bigdata.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // 功能 3: 原生 SQL 查詢找出最熱門的前10門課程
    @Query(value = "SELECT course_id, COUNT(*) as count FROM enrollment GROUP BY course_id ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTop10PopularCoursesNative();

    // 功能 1: 原生 SQL 查詢某學生所有修課紀錄
    @Query(value = "SELECT c.title, c.credit, e.enrollment_date " +
            "FROM enrollment e JOIN course c ON e.course_id = c.id " +
            "WHERE e.student_id = ?1", nativeQuery = true)
    List<Object[]> findCourseDetailsByStudentIdNative(Long studentId);

    // 功能 2: 原生 SQL 查詢某課程所有修課學生
    @Query(value = "SELECT s.name, s.email, e.enrollment_date " +
            "FROM enrollment e JOIN student s ON e.student_id = s.id " +
            "WHERE e.course_id = ?1", nativeQuery = true)
    List<Object[]> findStudentDetailsByCourseIdNative(Long courseId);
}
