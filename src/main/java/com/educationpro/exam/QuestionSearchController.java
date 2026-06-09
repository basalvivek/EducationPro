package com.educationpro.exam;

import com.educationpro.domain.CourseNode;
import com.educationpro.domain.NodeType;
import com.educationpro.exam.dto.FilterOptionsDto;
import com.educationpro.exam.dto.QuestionSearchDto;
import com.educationpro.exam.dto.TreeNodeDto;
import com.educationpro.repository.CourseNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/question-search")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
@RequiredArgsConstructor
public class QuestionSearchController {

    private final CourseNodeRepository repo;

    /**
     * Returns direct NODE children for cascade dropdowns.
     * parentId absent → root level (Class).
     * parentId present → children of that node (Subject, Topic, etc.).
     */
    @GetMapping("/tree-nodes")
    public ResponseEntity<List<TreeNodeDto>> treeNodes(@RequestParam(required = false) Long parentId) {
        List<CourseNode> nodes = (parentId == null)
            ? repo.findByParentIdIsNullAndTypeOrderBySortOrderAsc(NodeType.NODE)
            : repo.findByParentIdAndTypeOrderBySortOrderAsc(parentId, NodeType.NODE);

        List<TreeNodeDto> dtos = nodes.stream()
            .map(n -> new TreeNodeDto(n.getId(), n.getTitle()))
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Searches QUESTION nodes.
     * nodeId → BFS to find all node IDs in subtree, return questions whose parentId is in that set.
     * complexity → optional additional filter.
     * No params → return all questions.
     */
    @GetMapping
    public ResponseEntity<List<QuestionSearchDto>> search(
            @RequestParam(required = false) Long nodeId,
            @RequestParam(required = false) String complexity) {

        List<CourseNode> allNodes = repo.findAllByOrderBySortOrderAsc();

        List<CourseNode> questions;
        if (nodeId != null) {
            Set<Long> subtreeIds = collectSubtreeIds(allNodes, nodeId);
            questions = allNodes.stream()
                .filter(n -> n.getType() == NodeType.QUESTION
                          && n.getParentId() != null
                          && subtreeIds.contains(n.getParentId()))
                .collect(Collectors.toList());
        } else {
            questions = allNodes.stream()
                .filter(n -> n.getType() == NodeType.QUESTION)
                .collect(Collectors.toList());
        }

        if (complexity != null && !complexity.isBlank()) {
            String c = complexity.trim();
            questions = questions.stream()
                .filter(n -> c.equals(n.getComplexity()))
                .collect(Collectors.toList());
        }

        List<QuestionSearchDto> dtos = questions.stream()
            .sorted(Comparator.comparing(CourseNode::getTitle, String.CASE_INSENSITIVE_ORDER))
            .map(n -> new QuestionSearchDto(
                n.getId(), n.getTitle(), n.getQuestionText(),
                n.getQuestionType(), n.getComplexity(),
                n.getMarks() != null ? n.getMarks() : 1,
                n.getClassName(), n.getSubject(), n.getExamBoard(),
                n.getTopic(), n.getSubTopic()))
            .toList();

        return ResponseEntity.ok(dtos);
    }

    /** Legacy filter-options endpoint kept for backward compat (unused by current UI). */
    @GetMapping("/filter-options")
    public ResponseEntity<FilterOptionsDto> filterOptions() {
        return ResponseEntity.ok(new FilterOptionsDto(
            repo.findDistinctClassNames(),
            repo.findDistinctSubjects(),
            repo.findDistinctExamBoards(),
            repo.findDistinctTopics(),
            repo.findDistinctSubTopics()
        ));
    }

    // ── BFS to collect all node IDs in the subtree rooted at rootId ───────────
    private Set<Long> collectSubtreeIds(List<CourseNode> allNodes, Long rootId) {
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (CourseNode n : allNodes) {
            if (n.getParentId() != null) {
                childrenMap.computeIfAbsent(n.getParentId(), k -> new ArrayList<>()).add(n.getId());
            }
        }
        Set<Long> result = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long curr = queue.poll();
            result.add(curr);
            List<Long> children = childrenMap.getOrDefault(curr, Collections.emptyList());
            queue.addAll(children);
        }
        return result;
    }
}
