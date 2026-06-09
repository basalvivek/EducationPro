package com.educationpro.exam;

import com.educationpro.domain.*;
import com.educationpro.exception.BusinessException;
import com.educationpro.exam.dto.*;
import com.educationpro.repository.CourseNodeRepository;
import com.educationpro.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock ExamRepository         examRepo;
    @Mock ExamQuestionRepository examQRepo;
    @Mock CourseNodeRepository   nodeRepo;
    @Mock UserRepository         userRepo;
    @InjectMocks ExamService service;

    private User    admin;
    private Exam    draftExam;
    private CourseNode questionNode;
    private ExamQuestion examQ1;
    private ExamQuestion examQ2;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@educationpro.com");
        admin.setFullName("Super Admin");
        admin.setPasswordHash("hash");
        admin.setRole(Role.ADMIN);

        draftExam = new Exam();
        draftExam.setId(1L);
        draftExam.setName("Algebra Quiz");
        draftExam.setStatus("DRAFT");
        draftExam.setTimeLimitMinutes(60);
        draftExam.setCreatedBy(admin);

        questionNode = new CourseNode();
        questionNode.setId(100L);
        questionNode.setType(NodeType.QUESTION);
        questionNode.setTitle("What is 2+2?");
        questionNode.setMarks((short) 2);
        questionNode.setCreatedBy(admin);

        examQ1 = makeExamQ(1L, draftExam, questionNode, 0, null);

        CourseNode q2 = new CourseNode();
        q2.setId(101L);
        q2.setType(NodeType.QUESTION);
        q2.setTitle("What is 3+3?");
        q2.setMarks((short) 3);
        q2.setCreatedBy(admin);
        examQ2 = makeExamQ(2L, draftExam, q2, 1, null);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_success_returnsDraftExam() {
        CreateExamRequest req = new CreateExamRequest("Algebra Quiz", null, 45, null, false, false);
        given(userRepo.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        given(examRepo.save(any())).willReturn(draftExam);

        ExamDto dto = service.create(req, admin.getEmail());

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Algebra Quiz");
        assertThat(dto.status()).isEqualTo("DRAFT");
        assertThat(dto.questions()).isEmpty();
    }

    @Test
    void create_zeroTimelimit_defaultsTo60() {
        CreateExamRequest req = new CreateExamRequest("Quiz", null, 0, null, false, false);
        given(userRepo.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        ArgumentCaptor<Exam> cap = ArgumentCaptor.forClass(Exam.class);
        given(examRepo.save(cap.capture())).willReturn(draftExam);

        service.create(req, admin.getEmail());

        assertThat(cap.getValue().getTimeLimitMinutes()).isEqualTo(60);
    }

    @Test
    void create_creatorNotFound_throwsEntityNotFound() {
        CreateExamRequest req = new CreateExamRequest("Quiz", null, 30, null, false, false);
        given(userRepo.findByEmail("ghost@x.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req, "ghost@x.com"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_success() {
        given(examRepo.existsById(1L)).willReturn(true);
        service.delete(1L);
        then(examRepo).should().deleteById(1L);
    }

    @Test
    void delete_notFound_throwsEntityNotFound() {
        given(examRepo.existsById(999L)).willReturn(false);
        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
        then(examRepo).should(never()).deleteById(any());
    }

    // ── addQuestion ───────────────────────────────────────────────────────────

    @Test
    void addQuestion_success_appendsAndRecalcMarks() {
        AddQuestionRequest req = new AddQuestionRequest(100L, null);
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.existsByExamIdAndQuestionId(1L, 100L)).willReturn(false);
        given(nodeRepo.findById(100L)).willReturn(Optional.of(questionNode));
        given(examQRepo.findByExamIdOrderByPositionAsc(1L))
                .willReturn(List.of())     // before save
                .willReturn(List.of(examQ1)); // recalc + findById call
        given(examRepo.save(any())).willReturn(draftExam);
        given(examQRepo.save(any())).willReturn(examQ1);

        ExamDto dto = service.addQuestion(1L, req);

        then(examQRepo).should().save(any(ExamQuestion.class));
        assertThat(dto).isNotNull();
    }

    @Test
    void addQuestion_duplicate_throwsBusinessException() {
        AddQuestionRequest req = new AddQuestionRequest(100L, null);
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.existsByExamIdAndQuestionId(1L, 100L)).willReturn(true);

        assertThatThrownBy(() -> service.addQuestion(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already added");
    }

    @Test
    void addQuestion_notQuestionNode_throwsBusinessException() {
        CourseNode topicNode = new CourseNode();
        topicNode.setId(50L);
        topicNode.setType(NodeType.NODE);
        topicNode.setTitle("Topic");
        topicNode.setCreatedBy(admin);

        AddQuestionRequest req = new AddQuestionRequest(50L, null);
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.existsByExamIdAndQuestionId(1L, 50L)).willReturn(false);
        given(nodeRepo.findById(50L)).willReturn(Optional.of(topicNode));

        assertThatThrownBy(() -> service.addQuestion(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only QUESTION nodes");
    }

    @Test
    void addQuestion_examNotFound_throwsEntityNotFound() {
        given(examRepo.findById(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.addQuestion(999L, new AddQuestionRequest(1L, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── reorder ───────────────────────────────────────────────────────────────

    @Test
    void reorder_success_updatesPositions() {
        given(examQRepo.findByExamIdOrderByPositionAsc(1L))
                .willReturn(List.of(examQ1, examQ2))
                .willReturn(List.of(examQ2, examQ1)); // findById call
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.saveAll(any())).willReturn(List.of(examQ2, examQ1));

        // reverse order: q2=pos0, q1=pos1
        service.reorder(1L, List.of(101L, 100L));

        assertThat(examQ2.getPosition()).isEqualTo(0);
        assertThat(examQ1.getPosition()).isEqualTo(1);
        then(examQRepo).should().saveAll(anyList());
    }

    @Test
    void reorder_wrongCount_throwsBusinessException() {
        given(examQRepo.findByExamIdOrderByPositionAsc(1L)).willReturn(List.of(examQ1, examQ2));

        assertThatThrownBy(() -> service.reorder(1L, List.of(100L))) // only 1 of 2
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reorder list must contain all 2");
    }

    @Test
    void reorder_unknownQuestionId_throwsBusinessException() {
        given(examQRepo.findByExamIdOrderByPositionAsc(1L)).willReturn(List.of(examQ1, examQ2));

        assertThatThrownBy(() -> service.reorder(1L, List.of(100L, 999L))) // 999 not in exam
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not part of this exam");
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    void submit_adminRole_setsApproved() {
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.findByExamIdOrderByPositionAsc(1L)).willReturn(List.of());
        ArgumentCaptor<Exam> cap = ArgumentCaptor.forClass(Exam.class);
        given(examRepo.save(cap.capture())).willReturn(draftExam);

        service.submit(1L, true);

        assertThat(cap.getValue().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void submit_nonAdminRole_setsPendingApproval() {
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.findByExamIdOrderByPositionAsc(1L)).willReturn(List.of());
        ArgumentCaptor<Exam> cap = ArgumentCaptor.forClass(Exam.class);
        given(examRepo.save(cap.capture())).willReturn(draftExam);

        service.submit(1L, false);

        assertThat(cap.getValue().getStatus()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void submit_nonDraftExam_throwsBusinessException() {
        draftExam.setStatus("APPROVED");
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));

        assertThatThrownBy(() -> service.submit(1L, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only DRAFT exams");
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    void approve_pendingExam_setsApproved() {
        draftExam.setStatus("PENDING_APPROVAL");
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.findByExamIdOrderByPositionAsc(1L)).willReturn(List.of());
        ArgumentCaptor<Exam> cap = ArgumentCaptor.forClass(Exam.class);
        given(examRepo.save(cap.capture())).willReturn(draftExam);

        service.approve(1L);

        assertThat(cap.getValue().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void approve_draftExam_throwsBusinessException() {
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam)); // status=DRAFT

        assertThatThrownBy(() -> service.approve(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING_APPROVAL");
    }

    // ── recalcTotalMarks (via addQuestion) ────────────────────────────────────

    @Test
    void recalcTotalMarks_usesMarkOverrideWhenSet() {
        examQ1.setMarksOverride(5);
        AddQuestionRequest req = new AddQuestionRequest(100L, 5);
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.existsByExamIdAndQuestionId(1L, 100L)).willReturn(false);
        given(nodeRepo.findById(100L)).willReturn(Optional.of(questionNode));
        given(examQRepo.findByExamIdOrderByPositionAsc(1L))
                .willReturn(List.of())
                .willReturn(List.of(examQ1));
        ArgumentCaptor<Exam> cap = ArgumentCaptor.forClass(Exam.class);
        given(examRepo.save(cap.capture())).willReturn(draftExam);
        given(examQRepo.save(any())).willReturn(examQ1);

        service.addQuestion(1L, req);

        assertThat(cap.getValue().getTotalMarks()).isEqualTo(5);
    }

    @Test
    void recalcTotalMarks_fallsBackToQuestionMarks() {
        examQ1.setMarksOverride(null);  // no override → use question.marks = 2
        AddQuestionRequest req = new AddQuestionRequest(100L, null);
        given(examRepo.findById(1L)).willReturn(Optional.of(draftExam));
        given(examQRepo.existsByExamIdAndQuestionId(1L, 100L)).willReturn(false);
        given(nodeRepo.findById(100L)).willReturn(Optional.of(questionNode));
        given(examQRepo.findByExamIdOrderByPositionAsc(1L))
                .willReturn(List.of())
                .willReturn(List.of(examQ1));
        ArgumentCaptor<Exam> cap = ArgumentCaptor.forClass(Exam.class);
        given(examRepo.save(cap.capture())).willReturn(draftExam);
        given(examQRepo.save(any())).willReturn(examQ1);

        service.addQuestion(1L, req);

        assertThat(cap.getValue().getTotalMarks()).isEqualTo(2); // question.marks
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ExamQuestion makeExamQ(Long id, Exam exam, CourseNode question, int position, Integer override) {
        ExamQuestion eq = new ExamQuestion();
        eq.setId(id);
        eq.setExam(exam);
        eq.setQuestion(question);
        eq.setPosition(position);
        eq.setMarksOverride(override);
        return eq;
    }
}
