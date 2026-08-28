package com.buglens.project.repository;

import com.buglens.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByWorkspaceIdOrderByNameAsc(Long workspaceId);

    List<Project> findAllByWorkspaceIdInOrderByNameAsc(Collection<Long> workspaceIds);

    Optional<Project> findByIdAndWorkspaceId(Long projectId, Long workspaceId);

    boolean existsByWorkspaceIdAndKeyIgnoreCase(Long workspaceId, String key);
}
