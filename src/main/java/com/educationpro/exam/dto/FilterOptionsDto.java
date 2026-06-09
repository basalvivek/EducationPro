package com.educationpro.exam.dto;

import java.util.List;

public record FilterOptionsDto(
    List<String> classNames,
    List<String> subjects,
    List<String> examBoards,
    List<String> topics,
    List<String> subTopics
) {}
