package com.matsuzaka.bigdata.dto;

import java.time.LocalDate;
import java.util.List;


public record QueryResult<T>(List<T> data, long executionTime) {}

