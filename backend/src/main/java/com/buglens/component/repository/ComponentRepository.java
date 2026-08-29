package com.buglens.component.repository;

import com.buglens.component.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComponentRepository extends JpaRepository<Component, Long> {

    List<Component> findAllByProjectIdOrderByNameAsc(Long projectId);

    boolean existsByProjectIdAndNameIgnoreCase(Long projectId, String name);
}
