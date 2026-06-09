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

    @GetMapping("/approvals")
    public String approvals() {
        return "admin/approvals";
    }

    @GetMapping("/teachers")
    public String teachers() {
        return "admin/teachers";
    }

    @GetMapping("/teachers/register")
    public String teacherRegister() {
        return "admin/teacher-register";
    }

    @GetMapping("/students")
    public String students() {
        return "admin/students";
    }

    @GetMapping("/students/register")
    public String studentRegister() {
        return "admin/student-register";
    }

    @GetMapping("/assignments")
    public String assignments() {
        return "admin/assignments";
    }
}
