package com.matsuzaka.bigdata.dto;

import java.time.LocalDate;
import java.util.List;

public record StudentCourseInfo(String courseTitle, int credit, LocalDate enrollmentDate) {}

