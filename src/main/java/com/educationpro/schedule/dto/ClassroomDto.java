package com.educationpro.schedule.dto;

public record ClassroomDto(
    Long id,
    String name,
    String roomNumber,
    int capacity
) {}
