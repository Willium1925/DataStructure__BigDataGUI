package com.matsuzaka.bigdata.dto;

import java.time.LocalDate;
import java.util.List;

public record CourseStudentInfo(String studentName, String email, LocalDate enrollmentDate) {}

