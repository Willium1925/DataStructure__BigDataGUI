package com.matsuzaka.bigdata.config;

import com.github.javafaker.Faker;
import com.matsuzaka.bigdata.entity.Course;
import com.matsuzaka.bigdata.entity.Enrollment;
import com.matsuzaka.bigdata.entity.Student;
import com.matsuzaka.bigdata.entity.Teacher;
import com.matsuzaka.bigdata.repository.CourseRepository;
import com.matsuzaka.bigdata.repository.EnrollmentRepository;
import com.matsuzaka.bigdata.repository.StudentRepository;
import com.matsuzaka.bigdata.repository.TeacherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;

    public DataInitializer(JdbcTemplate jdbcTemplate, StudentRepository studentRepository, CourseRepository courseRepository, TeacherRepository teacherRepository, EnrollmentRepository enrollmentRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 檢查 enrollment 資料表是否有資料，來決定是否需要生成
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM enrollment", Integer.class);
        if (count != null && count > 0) {
            System.out.println("資料庫已有資料，跳過資料生成程序。");
            return;
        }

        System.out.println("資料庫為空，開始生成大量測試資料...");
        long startTime = System.currentTimeMillis();

        Faker faker = new Faker(new Locale("zh-TW"));
        Random random = new Random();

        // 1. 生成 10,000 學生
        System.out.println("正在生成 10,000 筆學生資料...");
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            Student s = new Student();
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            s.setName(firstName + " " + lastName);
            s.setBirth(faker.date().birthday(18, 25).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            // 【修正】透過在 email 中加入迴圈索引 i 來確保唯一性
            s.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@example.com");
            students.add(s);
        }
        List<Student> savedStudents = studentRepository.saveAllAndFlush(students);
        System.out.println("學生資料生成完畢。");

        // 2. 生成 1,000 課程
        System.out.println("正在生成 1,000 筆課程資料...");
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Course c = new Course();
            c.setTitle(faker.educator().course() + " #" + i); // 課程名稱也加上唯一後綴避免重複
            c.setCredit(random.nextInt(1, 5));
            courses.add(c);
        }
        List<Course> savedCourses = courseRepository.saveAllAndFlush(courses);
        System.out.println("課程資料生成完畢。");

        // 3. 生成 100 老師
        System.out.println("正在生成 100 筆教師資料...");
        List<Teacher> teachers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Teacher t = new Teacher();
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            t.setName(firstName + " " + lastName);
            // 【修正】透過在 email 中加入迴圈索引 i 並使用不同域名來確保唯一性
            t.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@example-teacher.com");
            teachers.add(t);
        }
        teacherRepository.saveAllAndFlush(teachers);
        System.out.println("教師資料生成完畢。");

        // 4. 生成 1,000,000 筆選課紀錄
        System.out.println("正在生成 1,000,000 筆選課紀錄，請稍候...");
        int batchSize = 1000; // 每 1000 筆存一次
        List<Enrollment> enrollmentBatch = new ArrayList<>();
        for (int i = 1; i <= 1_000_000; i++) {
            Enrollment e = new Enrollment();
            e.setStudentId(savedStudents.get(random.nextInt(savedStudents.size())).getId());
            e.setCourseId(savedCourses.get(random.nextInt(savedCourses.size())).getId());
            e.setEnrollmentDate(faker.date().past(365 * 3, TimeUnit.DAYS).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            enrollmentBatch.add(e);

            if (i % batchSize == 0) {
                enrollmentRepository.saveAll(enrollmentBatch);
                enrollmentBatch.clear();
                if (i % 100000 == 0) {
                    System.out.printf("...已生成 %d 筆選課紀錄%n", i);
                }
            }
        }
        if (!enrollmentBatch.isEmpty()) {
            enrollmentRepository.saveAll(enrollmentBatch);
        }
        System.out.println("選課紀錄生成完畢。");

        long endTime = System.currentTimeMillis();
        System.out.printf("所有資料生成完畢，總耗時: %.2f 分鐘%n", (endTime - startTime) / 60000.0);
    }
}