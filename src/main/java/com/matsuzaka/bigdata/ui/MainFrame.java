package com.matsuzaka.bigdata.ui;

import com.matsuzaka.bigdata.dto.CourseStudentInfo;
import com.matsuzaka.bigdata.dto.PopularCourseInfo;
import com.matsuzaka.bigdata.dto.QueryResult;
import com.matsuzaka.bigdata.dto.StudentCourseInfo;
import com.matsuzaka.bigdata.service.PerformanceService;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import org.springframework.stereotype.Component;


@Component
public class MainFrame extends JFrame {

    private final PerformanceService performanceService;
    private final JTextArea resultArea;
    private final JTextField studentIdField;
    private final JTextField courseIdField;

    public MainFrame(PerformanceService performanceService) {
        this.performanceService = performanceService;

        setTitle("資料庫 vs 記憶體 效能比較工具");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Layout ---
        setLayout(new BorderLayout(10, 10));

        // Result Area
        resultArea = new JTextArea("查詢結果將會顯示在這裡...\n");
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(3, 1, 5, 5));

        // Function 1
        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel1.setBorder(BorderFactory.createTitledBorder("功能 1: 查詢學生修課 (ID: 1-10000)"));
        studentIdField = new JTextField("1", 5);
        JButton btn1 = new JButton("查詢");
        panel1.add(new JLabel("學生 ID:"));
        panel1.add(studentIdField);
        panel1.add(btn1);
        controlPanel.add(panel1);

        // Function 2
        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel2.setBorder(BorderFactory.createTitledBorder("功能 2: 查詢課程修課學生 (ID: 1-1000)"));
        courseIdField = new JTextField("1", 5);
        JButton btn2 = new JButton("查詢");
        panel2.add(new JLabel("課程 ID:"));
        panel2.add(courseIdField);
        panel2.add(btn2);
        controlPanel.add(panel2);

        // Function 3
        JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel3.setBorder(BorderFactory.createTitledBorder("功能 3: 查詢 Top 10 熱門課程"));
        JButton btn3 = new JButton("查詢");
        panel3.add(btn3);
        controlPanel.add(panel3);

        add(controlPanel, BorderLayout.NORTH);

        // --- Action Listeners ---
        btn1.addActionListener(e -> findCoursesByStudent());
        btn2.addActionListener(e -> findStudentsByCourse());
        btn3.addActionListener(e -> findTopCourses());
    }

    private void findCoursesByStudent() {
        try {
            long studentId = Long.parseLong(studentIdField.getText());

            QueryResult<StudentCourseInfo> dbResult = performanceService.findCoursesByStudentId_DB(studentId);
            QueryResult<StudentCourseInfo> memResult = performanceService.findCoursesByStudentId_InMemory(studentId);

            StringBuilder sb = new StringBuilder();
            sb.append("\n=======================================================\n");
            sb.append(String.format("查詢學生 ID: %d 的修課紀錄\n", studentId));
            sb.append("-------------------------------------------------------\n");
            sb.append(String.format("直接查詢資料庫：找到 %d 筆紀錄，耗時: %d ms\n", dbResult.data().size(), dbResult.executionTime()));
            sb.append(String.format("查詢記憶體物件：找到 %d 筆紀錄，耗時: %d ms\n", memResult.data().size(), memResult.executionTime()));
            sb.append("-------------------------------------------------------\n");

            // 只顯示部分結果避免洗版
            int limit = Math.min(memResult.data().size(), 5);
            if (limit > 0) {
                sb.append("結果預覽 (最多顯示 5 筆):\n");
                for(int i = 0; i<limit; i++){
                    StudentCourseInfo info = memResult.data().get(i);
                    sb.append(String.format("- %s (學分: %d, 修課日期: %s)\n", info.courseTitle(), info.credit(), info.enrollmentDate()));
                }
            }
            resultArea.append(sb.toString());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的學生 ID!");
        }
    }

    private void findStudentsByCourse() {
        try {
            long courseId = Long.parseLong(courseIdField.getText());

            QueryResult<CourseStudentInfo> dbResult = performanceService.findStudentsByCourseId_DB(courseId);
            QueryResult<CourseStudentInfo> memResult = performanceService.findStudentsByCourseId_InMemory(courseId);

            StringBuilder sb = new StringBuilder();
            sb.append("\n=======================================================\n");
            sb.append(String.format("查詢課程 ID: %d 的修課學生\n", courseId));
            sb.append("-------------------------------------------------------\n");
            sb.append(String.format("直接查詢資料庫：找到 %d 筆紀錄，耗時: %d ms\n", dbResult.data().size(), dbResult.executionTime()));
            sb.append(String.format("查詢記憶體物件：找到 %d 筆紀錄，耗時: %d ms\n", memResult.data().size(), memResult.executionTime()));
            sb.append("-------------------------------------------------------\n");

            int limit = Math.min(memResult.data().size(), 5);
            if (limit > 0) {
                sb.append("結果預覽 (最多顯示 5 筆):\n");
                for(int i = 0; i<limit; i++){
                    CourseStudentInfo info = memResult.data().get(i);
                    sb.append(String.format("- %s (%s, 修課日期: %s)\n", info.studentName(), info.email(), info.enrollmentDate()));
                }
            }
            resultArea.append(sb.toString());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效的課程 ID!");
        }
    }

    private void findTopCourses() {
        QueryResult<PopularCourseInfo> dbResult = performanceService.findTop10PopularCourses_DB();
        QueryResult<PopularCourseInfo> memResult = performanceService.findTop10PopularCourses_InMemory();

        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================\n");
        sb.append("查詢 Top 10 熱門課程\n");
        sb.append("-------------------------------------------------------\n");
        sb.append(String.format("直接查詢資料庫：耗時: %d ms\n", dbResult.executionTime()));
        sb.append(String.format("查詢記憶體物件：耗時: %d ms\n", memResult.executionTime()));
        sb.append("-------------------------------------------------------\n");

        if (!memResult.data().isEmpty()) {
            sb.append("熱門課程列表:\n");
            for (PopularCourseInfo info : memResult.data()) {
                sb.append(String.format("- %s (修課人數: %d)\n", info.courseTitle(), info.enrollmentCount()));
            }
        }
        resultArea.append(sb.toString());
    }
}
