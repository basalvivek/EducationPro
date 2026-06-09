package com.educationpro.coursenode;

import com.educationpro.coursenode.dto.CourseNodeDto;
import com.educationpro.coursenode.dto.CreateNodeRequest;
import com.educationpro.coursenode.dto.UpdateNodeRequest;
import com.educationpro.domain.CourseNode;
import com.educationpro.domain.NodeType;
import com.educationpro.domain.User;

public final class CourseNodeMapper {

    private CourseNodeMapper() {}

    public static CourseNodeDto toDto(CourseNode n) {
        return new CourseNodeDto(
                n.getId(),
                n.getParentId(),
                n.getType().name(),
                n.getTitle(),
                n.getDescription(),
                n.getTagline(),

                // Question core
                n.getQuestionText(),
                n.getQuestionType(),
                n.getMarks(),
                n.getComplexity(),
                n.getExplanation(),

                // MCQ
                n.getOptions(),
                n.getCorrectIndex(),
                n.getCorrectIndices(),
                n.getPartialMarking(),

                // TRUE_FALSE
                n.getCorrectAnswer(),

                // SHORT_ANSWER / ESSAY
                n.getModelAnswer(),
                n.getMarkingScheme(),
                n.getWordLimit(),

                // CODE
                n.getCodeLanguage(),
                n.getStarterCode(),
                n.getExpectedOutput(),

                // IMAGE_BASED
                n.getImagePath(),
                n.getImageAlt(),
                n.getImageAnswerType(),

                // Question picker metadata
                n.getClassName(),
                n.getSubject(),
                n.getExamBoard(),
                n.getTopic(),
                n.getSubTopic(),

                // Audit/ordering
                n.getSortOrder()
        );
    }

    public static CourseNode fromRequest(CreateNodeRequest req, User creator) {
        CourseNode node = new CourseNode();
        node.setParentId(req.parentId());
        node.setType(NodeType.valueOf(req.type().toUpperCase()));
        node.setTitle(req.title());
        node.setDescription(req.description());
        node.setTagline(req.tagline());
        node.setCreatedBy(creator);

        // Picker metadata applies to all node types
        node.setClassName(req.className());
        node.setSubject(req.subject());
        node.setExamBoard(req.examBoard());
        node.setTopic(req.topic());
        node.setSubTopic(req.subTopic());

        if (NodeType.QUESTION == NodeType.valueOf(req.type().toUpperCase())) {
            node.setQuestionText(req.questionText());
            node.setQuestionType(req.questionType());
            if (req.marks() != null) node.setMarks(req.marks());
            node.setComplexity(req.complexity());
            node.setExplanation(req.explanation());

            // MCQ
            node.setOptions(req.options());
            node.setCorrectIndex(req.correctIndex());
            node.setCorrectIndices(req.correctIndices());
            node.setPartialMarking(req.partialMarking());

            // TRUE_FALSE
            node.setCorrectAnswer(req.correctAnswer());

            // SHORT_ANSWER / ESSAY
            node.setModelAnswer(req.modelAnswer());
            node.setMarkingScheme(req.markingScheme());
            if (req.wordLimit() != null) node.setWordLimit(req.wordLimit());

            // CODE
            node.setCodeLanguage(req.codeLanguage());
            node.setStarterCode(req.starterCode());
            node.setExpectedOutput(req.expectedOutput());

            // IMAGE_BASED
            node.setImagePath(req.imagePath());
            node.setImageAlt(req.imageAlt());
            node.setImageAnswerType(req.imageAnswerType());
        }
        return node;
    }

    public static void applyUpdate(CourseNode node, UpdateNodeRequest req) {
        node.setTitle(req.title());
        node.setDescription(req.description());
        node.setTagline(req.tagline());

        // Picker metadata applies to all node types
        node.setClassName(req.className());
        node.setSubject(req.subject());
        node.setExamBoard(req.examBoard());
        node.setTopic(req.topic());
        node.setSubTopic(req.subTopic());

        if (node.getType() == NodeType.QUESTION) {
            node.setQuestionText(req.questionText());
            node.setQuestionType(req.questionType());
            if (req.marks() != null) node.setMarks(req.marks());
            node.setComplexity(req.complexity());
            node.setExplanation(req.explanation());

            // MCQ
            node.setOptions(req.options());
            node.setCorrectIndex(req.correctIndex());
            node.setCorrectIndices(req.correctIndices());
            node.setPartialMarking(req.partialMarking());

            // TRUE_FALSE
            node.setCorrectAnswer(req.correctAnswer());

            // SHORT_ANSWER / ESSAY
            node.setModelAnswer(req.modelAnswer());
            node.setMarkingScheme(req.markingScheme());
            if (req.wordLimit() != null) node.setWordLimit(req.wordLimit());

            // CODE
            node.setCodeLanguage(req.codeLanguage());
            node.setStarterCode(req.starterCode());
            node.setExpectedOutput(req.expectedOutput());

            // IMAGE_BASED
            node.setImagePath(req.imagePath());
            node.setImageAlt(req.imageAlt());
            node.setImageAnswerType(req.imageAnswerType());
        }
    }
}
