package com.educationpro.repository;

import com.educationpro.domain.CourseNode;
import com.educationpro.domain.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseNodeRepository extends JpaRepository<CourseNode, Long> {

    List<CourseNode> findAllByOrderBySortOrderAsc();

    // ── Hierarchy cascade dropdowns ───────────────────────────────────────────

    List<CourseNode> findByParentIdIsNullAndTypeOrderBySortOrderAsc(NodeType type);

    List<CourseNode> findByParentIdAndTypeOrderBySortOrderAsc(Long parentId, NodeType type);

    // ── Question picker search ────────────────────────────────────────────────

    @Query("""
        SELECT n FROM CourseNode n WHERE n.type = com.educationpro.domain.NodeType.QUESTION
        AND (:className IS NULL OR n.className = :className)
        AND (:subject   IS NULL OR n.subject   = :subject)
        AND (:examBoard IS NULL OR n.examBoard  = :examBoard)
        AND (:topic     IS NULL OR n.topic      = :topic)
        AND (:subTopic  IS NULL OR n.subTopic   = :subTopic)
        AND (:complexity IS NULL OR n.complexity = :complexity)
        ORDER BY n.title ASC
        """)
    List<CourseNode> searchQuestions(
        @Param("className")  String className,
        @Param("subject")    String subject,
        @Param("examBoard")  String examBoard,
        @Param("topic")      String topic,
        @Param("subTopic")   String subTopic,
        @Param("complexity") String complexity
    );

    @Query("SELECT DISTINCT n.className FROM CourseNode n WHERE n.type = com.educationpro.domain.NodeType.QUESTION AND n.className IS NOT NULL ORDER BY n.className ASC")
    List<String> findDistinctClassNames();

    @Query("SELECT DISTINCT n.subject FROM CourseNode n WHERE n.type = com.educationpro.domain.NodeType.QUESTION AND n.subject IS NOT NULL ORDER BY n.subject ASC")
    List<String> findDistinctSubjects();

    @Query("SELECT DISTINCT n.examBoard FROM CourseNode n WHERE n.type = com.educationpro.domain.NodeType.QUESTION AND n.examBoard IS NOT NULL ORDER BY n.examBoard ASC")
    List<String> findDistinctExamBoards();

    @Query("SELECT DISTINCT n.topic FROM CourseNode n WHERE n.type = com.educationpro.domain.NodeType.QUESTION AND n.topic IS NOT NULL ORDER BY n.topic ASC")
    List<String> findDistinctTopics();

    @Query("SELECT DISTINCT n.subTopic FROM CourseNode n WHERE n.type = com.educationpro.domain.NodeType.QUESTION AND n.subTopic IS NOT NULL ORDER BY n.subTopic ASC")
    List<String> findDistinctSubTopics();
}
