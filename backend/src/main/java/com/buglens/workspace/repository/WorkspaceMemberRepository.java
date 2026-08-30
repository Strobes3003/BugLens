package com.buglens.workspace.repository;

import com.buglens.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    @EntityGraph(attributePaths = {"workspace"})
    List<WorkspaceMember> findAllByUserIdOrderByJoinedAtAsc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<WorkspaceMember> findAllByWorkspaceIdOrderByJoinedAtAsc(Long workspaceId);

    /*
     * Derived on purpose. Spring Data runs this as a select followed by a remove per
     * row, which evicts the members from the persistence context; a bulk @Query delete
     * would go straight to SQL and leave them managed, which is the very thing that
     * breaks workspace deletion. See WorkspaceService#delete.
     */
    void deleteAllByWorkspaceId(Long workspaceId);
}
