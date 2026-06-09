package com.educationpro.coursenode;

import com.educationpro.coursenode.dto.CourseNodeDto;
import com.educationpro.coursenode.dto.CreateNodeRequest;
import com.educationpro.coursenode.dto.UpdateNodeRequest;
import com.educationpro.domain.CourseNode;
import com.educationpro.domain.NodeType;
import com.educationpro.domain.User;
import com.educationpro.exception.BusinessException;
import com.educationpro.repository.CourseNodeRepository;
import com.educationpro.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseNodeService {

    private final CourseNodeRepository nodeRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<CourseNodeDto> findAll() {
        return nodeRepo.findAllByOrderBySortOrderAsc()
                .stream().map(CourseNodeMapper::toDto).toList();
    }

    public CourseNodeDto addNode(CreateNodeRequest req, String creatorEmail) {
        if (req.parentId() != null) {
            CourseNode parent = nodeRepo.findById(req.parentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent node not found"));
            if (parent.getType() == NodeType.QUESTION) {
                throw new BusinessException("Cannot add children to a Question node.");
            }
        }
        User creator = userRepo.findByEmail(creatorEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + creatorEmail));
        CourseNode node = CourseNodeMapper.fromRequest(req, creator);
        return CourseNodeMapper.toDto(nodeRepo.save(node));
    }

    public CourseNodeDto updateNode(Long id, UpdateNodeRequest req) {
        CourseNode node = nodeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: " + id));
        CourseNodeMapper.applyUpdate(node, req);
        return CourseNodeMapper.toDto(nodeRepo.save(node));
    }

    public void deleteNode(Long id) {
        if (!nodeRepo.existsById(id)) {
            throw new EntityNotFoundException("Node not found: " + id);
        }
        nodeRepo.deleteById(id);
    }
}
