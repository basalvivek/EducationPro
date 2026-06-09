package com.educationpro.exam;

import com.educationpro.domain.CourseNode;
import com.educationpro.domain.Exam;
import com.educationpro.domain.ExamQuestion;
import com.educationpro.domain.NodeType;
import com.educationpro.domain.User;
import com.educationpro.exception.BusinessException;
import com.educationpro.exam.dto.*;
import com.educationpro.repository.CourseNodeRepository;
import com.educationpro.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepo;
    private final ExamQuestionRepository examQRepo;
    private final CourseNodeRepository nodeRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<ExamSummaryDto> findAll() {
        return examRepo.findAllOrderByCreatedAtDesc().stream()
            .map(e -> new ExamSummaryDto(
                e.getId(), e.getName(), e.getStatus(),
                e.getTimeLimitMinutes(), e.getTotalMarks(),
                examQRepo.findByExamIdOrderByPositionAsc(e.getId()).size()))
            .toList();
    }

    @Transactional(readOnly = true)
    public ExamDto findById(Long id) {
        Exam exam = examRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + id));
        List<ExamQuestionDto> qs = examQRepo.findByExamIdOrderByPositionAsc(id)
            .stream().map(this::toExamQuestionDto).toList();
        return toDto(exam, qs);
    }

    public ExamDto create(CreateExamRequest req, String creatorEmail) {
        User creator = userRepo.findByEmail(creatorEmail)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + creatorEmail));
        Exam exam = new Exam();
        exam.setName(req.name());
        exam.setDescription(req.description());
        exam.setTimeLimitMinutes(req.timeLimitMinutes() > 0 ? req.timeLimitMinutes() : 60);
        exam.setPassMark(req.passMark());
        exam.setShuffleQuestions(req.shuffleQuestions());
        exam.setShuffleOptions(req.shuffleOptions());
        exam.setCreatedBy(creator);
        return toDto(examRepo.save(exam), List.of());
    }

    public ExamDto update(Long id, UpdateExamRequest req) {
        Exam exam = examRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + id));
        exam.setName(req.name());
        exam.setDescription(req.description());
        exam.setTimeLimitMinutes(req.timeLimitMinutes() > 0 ? req.timeLimitMinutes() : 60);
        exam.setTotalMarks(req.totalMarks());
        exam.setPassMark(req.passMark());
        exam.setShuffleQuestions(req.shuffleQuestions());
        exam.setShuffleOptions(req.shuffleOptions());
        List<ExamQuestionDto> qs = examQRepo.findByExamIdOrderByPositionAsc(id)
            .stream().map(this::toExamQuestionDto).toList();
        return toDto(examRepo.save(exam), qs);
    }

    public void delete(Long id) {
        if (!examRepo.existsById(id)) throw new EntityNotFoundException("Exam not found: " + id);
        examRepo.deleteById(id);
    }

    public ExamDto addQuestion(Long examId, AddQuestionRequest req) {
        Exam exam = examRepo.findById(examId)
            .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
        if (examQRepo.existsByExamIdAndQuestionId(examId, req.questionId())) {
            throw new BusinessException("Question already added to this exam.");
        }
        CourseNode question = nodeRepo.findById(req.questionId())
            .orElseThrow(() -> new EntityNotFoundException("Question not found: " + req.questionId()));
        if (question.getType() != NodeType.QUESTION) {
            throw new BusinessException("Only QUESTION nodes can be added to an exam.");
        }

        List<ExamQuestion> existing = examQRepo.findByExamIdOrderByPositionAsc(examId);
        ExamQuestion eq = new ExamQuestion();
        eq.setExam(exam);
        eq.setQuestion(question);
        eq.setPosition(existing.size());
        eq.setMarksOverride(req.marksOverride());
        examQRepo.save(eq);

        recalcTotalMarks(exam, examId);
        return findById(examId);
    }

    public ExamDto removeQuestion(Long examId, Long questionId) {
        examQRepo.deleteByExamIdAndQuestionId(examId, questionId);
        Exam exam = examRepo.findById(examId)
            .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
        recalcTotalMarks(exam, examId);
        return findById(examId);
    }

    public ExamDto reorder(Long examId, List<Long> orderedQuestionIds) {
        List<ExamQuestion> eqs = examQRepo.findByExamIdOrderByPositionAsc(examId);
        if (orderedQuestionIds.size() != eqs.size()) {
            throw new BusinessException("Reorder list must contain all " + eqs.size() + " question(s).");
        }
        Set<Long> examQIds = eqs.stream()
            .map(e -> e.getQuestion().getId())
            .collect(Collectors.toSet());
        for (Long qid : orderedQuestionIds) {
            if (!examQIds.contains(qid)) {
                throw new BusinessException("Question " + qid + " is not part of this exam.");
            }
        }
        for (int i = 0; i < orderedQuestionIds.size(); i++) {
            final Long qid = orderedQuestionIds.get(i);
            final int  pos = i;
            eqs.stream()
               .filter(e -> e.getQuestion().getId().equals(qid))
               .findFirst()
               .ifPresent(e -> e.setPosition(pos));
        }
        examQRepo.saveAll(eqs);
        return findById(examId);
    }

    public ExamDto submit(Long examId, boolean isAdmin) {
        Exam exam = examRepo.findById(examId)
            .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
        if (!"DRAFT".equals(exam.getStatus())) {
            throw new BusinessException("Only DRAFT exams can be submitted.");
        }
        exam.setStatus(isAdmin ? "APPROVED" : "PENDING_APPROVAL");
        List<ExamQuestionDto> qs = examQRepo.findByExamIdOrderByPositionAsc(examId)
            .stream().map(this::toExamQuestionDto).toList();
        return toDto(examRepo.save(exam), qs);
    }

    public ExamDto approve(Long examId) {
        Exam exam = examRepo.findById(examId)
            .orElseThrow(() -> new EntityNotFoundException("Exam not found: " + examId));
        if (!"PENDING_APPROVAL".equals(exam.getStatus())) {
            throw new BusinessException("Only PENDING_APPROVAL exams can be approved.");
        }
        exam.setStatus("APPROVED");
        List<ExamQuestionDto> qs = examQRepo.findByExamIdOrderByPositionAsc(examId)
            .stream().map(this::toExamQuestionDto).toList();
        return toDto(examRepo.save(exam), qs);
    }

    @Transactional(readOnly = true)
    public List<ExamSummaryDto> findPendingApproval() {
        return examRepo.findByStatusWithCreator("PENDING_APPROVAL").stream()
            .map(e -> new ExamSummaryDto(
                e.getId(), e.getName(), e.getStatus(),
                e.getTimeLimitMinutes(), e.getTotalMarks(),
                examQRepo.findByExamIdOrderByPositionAsc(e.getId()).size(),
                e.getCreatedBy().getFullName()))
            .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void recalcTotalMarks(Exam exam, Long examId) {
        int total = examQRepo.findByExamIdOrderByPositionAsc(examId).stream()
            .mapToInt(eq -> {
                if (eq.getQuestion() == null) return 0;
                return eq.getMarksOverride() != null
                    ? eq.getMarksOverride()
                    : (eq.getQuestion().getMarks() != null ? eq.getQuestion().getMarks() : 1);
            })
            .sum();
        exam.setTotalMarks(total);
        examRepo.save(exam);
    }

    private ExamQuestionDto toExamQuestionDto(ExamQuestion eq) {
        CourseNode q = eq.getQuestion();
        int marks = eq.getMarksOverride() != null
            ? eq.getMarksOverride()
            : (q.getMarks() != null ? q.getMarks() : 1);
        return new ExamQuestionDto(
            eq.getId(), q.getId(), q.getTitle(),
            q.getQuestionText(), q.getQuestionType(),
            q.getComplexity(), marks, eq.getPosition());
    }

    private ExamDto toDto(Exam e, List<ExamQuestionDto> qs) {
        return new ExamDto(
            e.getId(), e.getName(), e.getDescription(),
            e.getTimeLimitMinutes(), e.getTotalMarks(), e.getPassMark(),
            e.isShuffleQuestions(), e.isShuffleOptions(), e.getStatus(), qs);
    }
}
