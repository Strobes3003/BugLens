package com.buglens.workspace.repository;

import com.buglens.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    boolean existsBySlugIgnoreCase(String slug);
}
