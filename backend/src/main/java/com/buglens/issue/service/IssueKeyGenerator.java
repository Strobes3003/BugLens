package com.buglens.issue.service;

import com.buglens.issue.repository.IssueSequenceRepository;
import com.buglens.project.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class IssueKeyGenerator {

    private final IssueSequenceRepository issueSequenceRepository;

    public IssueKeyGenerator(IssueSequenceRepository issueSequenceRepository) {
        this.issueSequenceRepository = issueSequenceRepository;
    }

    /**
     * Builds the next key for a project, e.g. {@code BL-13} when {@code BL-12} was the last
     * number allocated. The numeric part is allocated atomically by the database.
     */
    public String nextKey(Project project) {
        long next = issueSequenceRepository.allocateNext(project.getId());
        return project.getKey() + "-" + next;
    }
}
