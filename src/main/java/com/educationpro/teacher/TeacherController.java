package com.educationpro.teacher;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "teacher/dashboard";
    }

    @GetMapping("/courses/design")
    public String courseDesigner() {
        return "teacher/course-designer";
    }

    @GetMapping("/exams/builder")
    public String examBuilder() {
        return "teacher/exam-builder";
    }
}
