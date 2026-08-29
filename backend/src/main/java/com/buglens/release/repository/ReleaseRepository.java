package com.buglens.release.repository;

import com.buglens.release.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    List<Release> findAllByProjectIdOrderByReleaseDateDescNameAsc(Long projectId);

    boolean existsByProjectIdAndVersionIgnoreCase(Long projectId, String version);
}
