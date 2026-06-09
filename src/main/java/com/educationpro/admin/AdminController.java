package com.educationpro.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/courses/design")
    public String courseDesigner() {
        return "admin/course-designer";
    }

    @GetMapping("/exams/builder")
    public String examBuilder() {
        return "admin/exam-builder";
    }
}
