package com.educationpro.coursenode;

import com.educationpro.coursenode.dto.CourseNodeDto;
import com.educationpro.coursenode.dto.CreateNodeRequest;
import com.educationpro.coursenode.dto.UpdateNodeRequest;
import com.educationpro.domain.CourseNode;
import com.educationpro.domain.NodeType;
import com.educationpro.domain.Role;
import com.educationpro.domain.User;
import com.educationpro.exception.BusinessException;
import com.educationpro.repository.CourseNodeRepository;
import com.educationpro.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CourseNodeServiceTest {

    @Mock CourseNodeRepository nodeRepo;
    @Mock UserRepository       userRepo;
    @InjectMocks CourseNodeService service;

    private User admin;
    private CourseNode parentNode;
    private CourseNode questionNode;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@educationpro.com");
        admin.setFullName("Super Admin");
        admin.setPasswordHash("hash");
        admin.setRole(Role.ADMIN);

        parentNode = new CourseNode();
        parentNode.setId(10L);
        parentNode.setType(NodeType.NODE);
        parentNode.setTitle("Mathematics");
        parentNode.setCreatedBy(admin);

        questionNode = new CourseNode();
        questionNode.setId(20L);
        questionNode.setType(NodeType.QUESTION);
        questionNode.setTitle("What is 2+2?");
        questionNode.setParentId(10L);
        questionNode.setCreatedBy(admin);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsMappedDtos() {
        given(nodeRepo.findAllByOrderBySortOrderAsc()).willReturn(List.of(parentNode, questionNode));

        List<CourseNodeDto> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(1).id()).isEqualTo(20L);
        assertThat(result.get(0).type()).isEqualTo("NODE");
        assertThat(result.get(1).type()).isEqualTo("QUESTION");
    }

    @Test
    void findAll_emptyRepo_returnsEmptyList() {
        given(nodeRepo.findAllByOrderBySortOrderAsc()).willReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    // ── addNode ───────────────────────────────────────────────────────────────

    @Test
    void addNode_rootNode_success() {
        CreateNodeRequest req = nodeReq(null, "Algebra", "NODE");
        CourseNode saved = nodeWithId(100L, null, NodeType.NODE, "Algebra");
        given(userRepo.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        given(nodeRepo.save(any())).willReturn(saved);

        CourseNodeDto dto = service.addNode(req, admin.getEmail());

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.type()).isEqualTo("NODE");
        assertThat(dto.title()).isEqualTo("Algebra");
        then(nodeRepo).should(never()).findById(any());
    }

    @Test
    void addNode_withValidParent_success() {
        CreateNodeRequest req = nodeReq(10L, "Sub-topic", "NODE");
        CourseNode saved = nodeWithId(101L, 10L, NodeType.NODE, "Sub-topic");
        given(nodeRepo.findById(10L)).willReturn(Optional.of(parentNode));
        given(userRepo.findByEmail(admin.getEmail())).willReturn(Optional.of(admin));
        given(nodeRepo.save(any())).willReturn(saved);

        CourseNodeDto dto = service.addNode(req, admin.getEmail());

        assertThat(dto.id()).isEqualTo(101L);
        assertThat(dto.parentId()).isEqualTo(10L);
    }

    @Test
    void addNode_parentIsQuestion_throwsBusinessException() {
        CreateNodeRequest req = nodeReq(20L, "Child of question", "NODE");
        given(nodeRepo.findById(20L)).willReturn(Optional.of(questionNode));

        assertThatThrownBy(() -> service.addNode(req, admin.getEmail()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot add children to a Question node");
    }

    @Test
    void addNode_parentNotFound_throwsEntityNotFound() {
        CreateNodeRequest req = nodeReq(999L, "Orphan", "NODE");
        given(nodeRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.addNode(req, admin.getEmail()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void addNode_creatorNotFound_throwsEntityNotFound() {
        CreateNodeRequest req = nodeReq(null, "New Root", "NODE");
        given(userRepo.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.addNode(req, "ghost@test.com"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ── updateNode ────────────────────────────────────────────────────────────

    @Test
    void updateNode_success() {
        UpdateNodeRequest req = updateReq("Algebra Updated");
        CourseNode updated = nodeWithId(10L, null, NodeType.NODE, "Algebra Updated");
        given(nodeRepo.findById(10L)).willReturn(Optional.of(parentNode));
        given(nodeRepo.save(any())).willReturn(updated);

        CourseNodeDto dto = service.updateNode(10L, req);

        assertThat(dto.title()).isEqualTo("Algebra Updated");
    }

    @Test
    void updateNode_notFound_throwsEntityNotFound() {
        given(nodeRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNode(999L, updateReq("X")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── deleteNode ────────────────────────────────────────────────────────────

    @Test
    void deleteNode_success() {
        given(nodeRepo.existsById(10L)).willReturn(true);

        service.deleteNode(10L);

        then(nodeRepo).should().deleteById(10L);
    }

    @Test
    void deleteNode_notFound_throwsEntityNotFound() {
        given(nodeRepo.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> service.deleteNode(999L))
                .isInstanceOf(EntityNotFoundException.class);
        then(nodeRepo).should(never()).deleteById(any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CreateNodeRequest nodeReq(Long parentId, String title, String type) {
        return new CreateNodeRequest(
            parentId, title, null, type, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null
        );
    }

    private UpdateNodeRequest updateReq(String title) {
        return new UpdateNodeRequest(
            title, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null
        );
    }

    private CourseNode nodeWithId(Long id, Long parentId, NodeType type, String title) {
        CourseNode n = new CourseNode();
        n.setId(id);
        n.setParentId(parentId);
        n.setType(type);
        n.setTitle(title);
        n.setCreatedBy(admin);
        return n;
    }
}
