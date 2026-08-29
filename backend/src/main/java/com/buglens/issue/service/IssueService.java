package com.buglens.issue.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.component.entity.Component;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.event.domain.IssueCreatedEvent;
import com.buglens.event.domain.IssueUpdatedEvent;
import com.buglens.issue.dto.request.CreateIssueRequest;
import com.buglens.issue.dto.request.UpdateIssueRequest;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.exception.CrossProjectIssueException;
import com.buglens.issue.exception.InvalidIssueFieldException;
import com.buglens.issue.exception.IssueAccessDeniedException;
import com.buglens.issue.exception.IssueAssigneeNotFoundException;
import com.buglens.issue.exception.IssueComponentNotFoundException;
import com.buglens.issue.exception.IssueKeyNotFoundException;
import com.buglens.issue.exception.IssueNotFoundException;
import com.buglens.issue.exception.IssueProjectNotFoundException;
import com.buglens.issue.exception.IssueReleaseNotFoundException;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.entity.Release;
import com.buglens.release.repository.ReleaseRepository;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class IssueService {

    private final IssueRepository issueRepository;
    private final ComponentRepository componentRepository;
    private final ReleaseRepository releaseRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final IssueKeyGenerator issueKeyGenerator;
    private final WorkspaceAccessService workspaceAccessService;
    private final ApplicationEventPublisher eventPublisher;

    public IssueService(
            IssueRepository issueRepository,
            ComponentRepository componentRepository,
            ReleaseRepository releaseRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            IssueKeyGenerator issueKeyGenerator,
            WorkspaceAccessService workspaceAccessService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.issueRepository = issueRepository;
        this.componentRepository = componentRepository;
        this.releaseRepository = releaseRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.issueKeyGenerator = issueKeyGenerator;
        this.workspaceAccessService = workspaceAccessService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public IssueResponse create(CreateIssueRequest request, Long actorId) {
        Component component = requireComponent(request.componentId());
        Project project = component.getProject();
        requireMember(project, actorId);

        Release release = resolveRelease(request.releaseId(), project);
        User assignee = resolveAssignee(request.assigneeId());
        User reporter = requireUser(actorId);

        Issue issue = new Issue(
                issueKeyGenerator.nextKey(project),
                normalizeTitle(request.title()),
                normalizeDescription(request.description()),
                IssueStatus.OPEN,
                request.priority(),
                request.severity(),
                component,
                release,
                reporter,
                assignee
        );
        Issue saved = issueRepository.save(issue);
        eventPublisher.publishEvent(
                IssueCreatedEvent.of(saved.getId(), actorId, saved.getIssueKey(), saved.getTitle())
        );
        return IssueResponse.from(saved);
    }

    public IssueResponse getById(Long issueId, Long actorId) {
        Issue issue = getIssue(issueId);
        requireMember(issue.getComponent().getProject(), actorId);
        return IssueResponse.from(issue);
    }

    public IssueResponse getByKey(String issueKey, Long actorId) {
        Issue issue = issueRepository.findByIssueKey(issueKey)
                .orElseThrow(() -> new IssueKeyNotFoundException(issueKey));
        requireMember(issue.getComponent().getProject(), actorId);
        return IssueResponse.from(issue);
    }

    public List<IssueResponse> listForProject(Long projectId, Long actorId) {
        Project project = requireProject(projectId);
        requireMember(project, actorId);
        return issueRepository.findAllByComponentProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(IssueResponse::from)
                .toList();
    }

    public List<IssueResponse> listForComponent(Long projectId, Long componentId, Long actorId) {
        Project project = requireProject(projectId);
        requireMember(project, actorId);

        Component component = requireComponent(componentId);
        requireSameProject(project, component.getProject(), "component");

        return issueRepository.findAllByComponentIdOrderByCreatedAtDesc(componentId)
                .stream()
                .map(IssueResponse::from)
                .toList();
    }

    @Transactional
    public IssueResponse updateDetails(Long issueId, UpdateIssueRequest request, Long actorId) {
        Issue issue = getIssue(issueId);
        Project project = issue.getComponent().getProject();
        requireMember(project, actorId);

        IssueSnapshot before = IssueSnapshot.of(issue);

        Component component = issue.getComponent();
        if (request.componentId() != null) {
            component = requireComponent(request.componentId());
            requireSameProject(project, component.getProject(), "component");
        }

        Release release = resolveReleaseUpdate(request, project);

        issue.updateDetails(
                request.title() == null ? null : normalizeTitle(request.title()),
                normalizeDescription(request.description()),
                request.priority(),
                request.severity(),
                component,
                release
        );

        if (Boolean.TRUE.equals(request.clearRelease())) {
            issue.moveToBacklog();
        }
        if (Boolean.TRUE.equals(request.clearAssignee())) {
            issue.assignTo(null);
        } else if (request.assigneeId() != null) {
            issue.assignTo(resolveAssignee(request.assigneeId()));
        }

        List<String> changedFields = before.diff(issue);
        if (!changedFields.isEmpty()) {
            eventPublisher.publishEvent(IssueUpdatedEvent.of(issue.getId(), actorId, changedFields));
        }

        return IssueResponse.from(issue);
    }

    /**
     * Values captured before a detail update, so the emitted event can name what actually
     * changed rather than reporting a generic edit. A request that sets a field to its existing
     * value produces no event at all.
     */
    private record IssueSnapshot(
            String title,
            String description,
            IssuePriority priority,
            IssueSeverity severity,
            Long componentId,
            Long releaseId,
            Long assigneeId
    ) {

        static IssueSnapshot of(Issue issue) {
            return new IssueSnapshot(
                    issue.getTitle(),
                    issue.getDescription(),
                    issue.getPriority(),
                    issue.getSeverity(),
                    idOf(issue.getComponent() == null ? null : issue.getComponent().getId()),
                    idOf(issue.getRelease() == null ? null : issue.getRelease().getId()),
                    idOf(issue.getAssignee() == null ? null : issue.getAssignee().getId())
            );
        }

        private static Long idOf(Long id) {
            return id;
        }

        List<String> diff(Issue after) {
            List<String> changed = new ArrayList<>();
            addIfChanged(changed, "title", title, after.getTitle());
            addIfChanged(changed, "description", description, after.getDescription());
            addIfChanged(changed, "priority", priority, after.getPriority());
            addIfChanged(changed, "severity", severity, after.getSeverity());
            addIfChanged(changed, "component", componentId,
                    after.getComponent() == null ? null : after.getComponent().getId());
            addIfChanged(changed, "release", releaseId,
                    after.getRelease() == null ? null : after.getRelease().getId());
            addIfChanged(changed, "assignee", assigneeId,
                    after.getAssignee() == null ? null : after.getAssignee().getId());
            return changed;
        }

        private static void addIfChanged(List<String> changed, String field, Object before, Object after) {
            if (!Objects.equals(before, after)) {
                changed.add(field);
            }
        }
    }

    @Transactional
    public void delete(Long issueId, Long actorId) {
        Issue issue = getIssue(issueId);
        requireManager(issue.getComponent().getProject(), actorId);
        issueRepository.delete(issue);
    }

    private Release resolveReleaseUpdate(UpdateIssueRequest request, Project project) {
        if (Boolean.TRUE.equals(request.clearRelease())) {
            if (request.releaseId() != null) {
                throw new CrossProjectIssueException(
                        "Cannot set a release and clear it in the same request"
                );
            }
            return null;
        }
        return resolveRelease(request.releaseId(), project);
    }

    private Release resolveRelease(Long releaseId, Project project) {
        if (releaseId == null) {
            return null;
        }
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IssueReleaseNotFoundException(releaseId));
        requireSameProject(project, release.getProject(), "release");
        return release;
    }

    private User resolveAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new IssueAssigneeNotFoundException(assigneeId));
    }

    private void requireSameProject(Project expected, Project actual, String field) {
        if (!expected.getId().equals(actual.getId())) {
            throw new CrossProjectIssueException(
                    "Issue " + field + " must belong to project " + expected.getId()
            );
        }
    }

    private Issue getIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));
    }

    private Component requireComponent(Long componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new IssueComponentNotFoundException(componentId));
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IssueProjectNotFoundException(projectId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IssueAssigneeNotFoundException(userId));
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new IssueAccessDeniedException();
        }
    }

    private void requireManager(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMemberManager(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new IssueAccessDeniedException();
        }
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? null : title.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new InvalidIssueFieldException("title");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
