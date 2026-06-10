package com.educationpro.admin;

import com.educationpro.admin.dto.AssignmentResultDto;
import com.educationpro.admin.dto.AssignmentSessionDetailDto;
import com.educationpro.admin.dto.SaveAssignmentRequest;
import com.educationpro.domain.*;
import com.educationpro.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentSessionRepository sessionRepo;
    private final AssignmentGroupRepository groupRepo;
    private final AssignmentTeacherMappingRepository mappingRepo;
    private final CourseNodeRepository courseNodeRepo;
    private final UserRepository userRepo;

    public AssignmentResultDto save(SaveAssignmentRequest req, String adminEmail) {
        User admin = userRepo.findByEmail(adminEmail)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + adminEmail));

        CourseNode courseNode = courseNodeRepo.findById(req.getCourseNodeId())
            .orElseThrow(() -> new EntityNotFoundException("Course node not found: " + req.getCourseNodeId()));

        CourseNode scopeNode = req.getScopeNodeId() != null
            ? courseNodeRepo.findById(req.getScopeNodeId()).orElse(null)
            : null;

        AssignmentSession session = new AssignmentSession();
        session.setCourseNode(courseNode);
        session.setScopeNode(scopeNode);
        session.setScopeLevel(req.getScopeLevel() != null ? req.getScopeLevel() : "course");
        session.setMaxPerGroup(req.getMaxPerGroup() > 0 ? req.getMaxPerGroup() : 30);
        session.setStatus("SAVED".equals(req.getStatus()) ? "SAVED" : "DRAFT");
        session.setCreatedBy(admin);
        session = sessionRepo.save(session);

        Map<Integer, AssignmentGroup> localToGroup = new HashMap<>();
        if (req.getGroups() != null) {
            for (SaveAssignmentRequest.GroupRequest gr : req.getGroups()) {
                AssignmentGroup g = new AssignmentGroup();
                g.setSession(session);
                g.setName(gr.getName());
                g.setDescription(gr.getDescription());
                g.setPeriod(gr.getPeriod());
                if (gr.getStudentIds() != null) g.setStudentProfileIds(gr.getStudentIds());
                g = groupRepo.save(g);
                if (gr.getLocalId() != null) localToGroup.put(gr.getLocalId(), g);
            }
        }

        if (req.getTeacherAssignments() != null) {
            for (SaveAssignmentRequest.TeacherAssignmentRequest ta : req.getTeacherAssignments()) {
                AssignmentTeacherMapping m = new AssignmentTeacherMapping();
                m.setSession(session);
                m.setTeacherProfileId(ta.getTeacherId());
                if (ta.getGroupLocalId() != null) m.setGroup(localToGroup.get(ta.getGroupLocalId()));
                mappingRepo.save(m);
            }
        }

        String msg = "SAVED".equals(session.getStatus())
            ? "Assignment saved successfully"
            : "Assignment saved as draft";
        return new AssignmentResultDto(session.getId(), session.getStatus(), msg);
    }

    @Transactional(readOnly = true)
    public Optional<AssignmentSessionDetailDto> getLatestSession() {
        return sessionRepo.findTopByOrderByIdDesc().map(session -> {
            List<AssignmentGroup> groups = groupRepo.findBySession_Id(session.getId());
            List<AssignmentTeacherMapping> mappings = mappingRepo.findBySession_Id(session.getId());

            List<AssignmentSessionDetailDto.GroupDetail> groupDetails = groups.stream()
                .map(g -> new AssignmentSessionDetailDto.GroupDetail(
                    g.getId(), g.getName(), g.getDescription(), g.getPeriod(),
                    g.getStudentProfileIds()))
                .toList();

            List<AssignmentSessionDetailDto.TeacherAssignmentDetail> teacherDetails = mappings.stream()
                .map(m -> new AssignmentSessionDetailDto.TeacherAssignmentDetail(
                    m.getTeacherProfileId(),
                    m.getGroup() != null ? m.getGroup().getId() : null))
                .toList();

            return new AssignmentSessionDetailDto(
                session.getId(),
                session.getCourseNode().getId(),
                session.getScopeNode() != null ? session.getScopeNode().getId() : null,
                session.getScopeLevel(),
                session.getMaxPerGroup(),
                session.getStatus(),
                groupDetails,
                teacherDetails);
        });
    }
}
