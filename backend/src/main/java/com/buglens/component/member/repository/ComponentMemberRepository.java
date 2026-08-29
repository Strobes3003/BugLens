package com.buglens.component.member.repository;

import com.buglens.component.member.entity.ComponentMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComponentMemberRepository extends JpaRepository<ComponentMember, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<ComponentMember> findAllByComponentIdOrderByAssignedAtAsc(Long componentId);

    Optional<ComponentMember> findByComponentIdAndUserId(Long componentId, Long userId);

    boolean existsByComponentIdAndUserId(Long componentId, Long userId);
}
