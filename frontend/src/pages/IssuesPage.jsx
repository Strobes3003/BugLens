import { useCallback, useEffect, useState } from "react";
import * as issueApi from "../api/issueApi";
import * as componentApi from "../api/componentApi";
import * as releaseApi from "../api/releaseApi";
import { useActiveProject } from "../features/projects/hooks/useActiveProject";
import ProjectPicker from "../features/projects/components/ProjectPicker";
import { Spinner, ErrorState, EmptyState } from "../components/ui";
import IssueFilters from "../features/issues/IssueFilters";
import IssueTable from "../features/issues/IssueTable";
import IssuePagination from "../features/issues/IssuePagination";

/**
 * The table and filters were written against a flat mock shape, so the backend response is
 * adapted here rather than rewriting those components: the API returns assigneeName/componentName
 * where they expect assignee/component.
 */
function toRow(issue) {
    return {
        ...issue,
        assignee: issue.assigneeName ?? "Unassigned",
        component: issue.componentName ?? "",
        release: issue.releaseName ?? "Backlog",
    };
}

function IssuesPage() {
    const {
        projects,
        activeProjectId,
        selectProject,
        isLoading: isProjectLoading,
        error: projectError,
    } = useActiveProject();

    const [issues, setIssues] = useState([]);
    const [components, setComponents] = useState([]);
    const [releases, setReleases] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("");
    const [severityFilter, setSeverityFilter] = useState("");
    const [priorityFilter, setPriorityFilter] = useState("");
    const [assigneeFilter, setAssigneeFilter] = useState("");

    const [sortField, setSortField] = useState("");
    const [sortDirection, setSortDirection] = useState("asc");

    const [currentPage, setCurrentPage] = useState(1);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [editingIssue, setEditingIssue] = useState(null);
    const [saveError, setSaveError] = useState(null);

    const issuesPerPage = 10;

    const loadIssues = useCallback(() => {
        if (!activeProjectId) {
            setIssues([]);
            setIsLoading(false);
            return;
        }

        setIsLoading(true);
        setError(null);

        Promise.all([
            issueApi.getIssues(activeProjectId),
            componentApi.getComponents(activeProjectId),
            releaseApi.getReleases(activeProjectId),
        ])
            .then(([loadedIssues, loadedComponents, loadedReleases]) => {
                setIssues(loadedIssues.map(toRow));
                setComponents(loadedComponents);
                setReleases(loadedReleases);
            })
            .catch((err) => setError(err.message))
            .finally(() => setIsLoading(false));
    }, [activeProjectId]);

    useEffect(() => {
        loadIssues();
    }, [loadIssues]);

    const filteredIssues = issues.filter((issue) => {
        const query = searchQuery.toLowerCase().trim();

        const matchesSearch =
            !query ||
            issue.title.toLowerCase().includes(query) ||
            issue.issueKey.toLowerCase().includes(query);

        const matchesStatus = !statusFilter || issue.status === statusFilter;
        const matchesSeverity =
            !severityFilter || issue.severity === severityFilter;
        const matchesPriority =
            !priorityFilter || issue.priority === priorityFilter;
        const matchesAssignee =
            !assigneeFilter || issue.assignee === assigneeFilter;

        return (
            matchesSearch &&
            matchesStatus &&
            matchesSeverity &&
            matchesPriority &&
            matchesAssignee
        );
    });

    const sortedIssues = [...filteredIssues].sort((a, b) => {
        if (!sortField) return 0;

        const rankings = {
            severity: { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 },
            priority: { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 },
        };

        if (rankings[sortField]) {
            const comparison =
                (rankings[sortField][a[sortField]] ?? 0) -
                (rankings[sortField][b[sortField]] ?? 0);

            return sortDirection === "asc" ? -comparison : comparison;
        }

        const valueA = String(a[sortField] ?? "");
        const valueB = String(b[sortField] ?? "");

        const comparison = valueA.localeCompare(valueB, undefined, {
            sensitivity: "base",
        });

        return sortDirection === "asc" ? comparison : -comparison;
    });

    const totalPages = Math.max(
        1,
        Math.ceil(sortedIssues.length / issuesPerPage)
    );

    const safeCurrentPage = Math.min(currentPage, totalPages);
    const startIndex = (safeCurrentPage - 1) * issuesPerPage;
    const paginatedIssues = sortedIssues.slice(
        startIndex,
        startIndex + issuesPerPage
    );

    const resetPage = () => setCurrentPage(1);

    const handleSearchChange = (value) => {
        setSearchQuery(value);
        resetPage();
    };

    const handleStatusChange = (value) => {
        setStatusFilter(value);
        resetPage();
    };

    const handleSeverityChange = (value) => {
        setSeverityFilter(value);
        resetPage();
    };

    const handlePriorityChange = (value) => {
        setPriorityFilter(value);
        resetPage();
    };

    const handleAssigneeChange = (value) => {
        setAssigneeFilter(value);
        resetPage();
    };

    const handleSort = (field) => {
        if (sortField === field) {
            setSortDirection((current) =>
                current === "asc" ? "desc" : "asc"
            );
            return;
        }
        setSortField(field);
        setSortDirection("asc");
    };

    const handlePageChange = (page) => {
        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
        }
    };

    const handleCreateIssue = (payload) => {
        setSaveError(null);
        issueApi
            .createIssue(payload)
            .then(() => {
                setShowCreateForm(false);
                setCurrentPage(1);
                loadIssues();
            })
            .catch((err) => setSaveError(err.message));
    };

    const handleUpdateIssue = (payload) => {
        setSaveError(null);
        issueApi
            .updateIssue(editingIssue.id, payload)
            .then(() => {
                setEditingIssue(null);
                loadIssues();
            })
            .catch((err) => setSaveError(err.message));
    };

    if (isProjectLoading || isLoading) {
        return <Spinner size={24} />;
    }

    if (projectError || error) {
        return (
            <ErrorState message={projectError || error} onRetry={loadIssues} />
        );
    }

    if (!activeProjectId) {
        return (
            <EmptyState
                title="No project selected"
                description="Create a project in this workspace to start tracking issues."
            />
        );
    }

    return (
        <div>
            <header>
                <h1>Issues</h1>
                <p>Track, manage, and prioritize project issues.</p>
                <ProjectPicker
                    projects={projects}
                    activeProjectId={activeProjectId}
                    onSelect={selectProject}
                />
            </header>

            {saveError && <ErrorState message={saveError} />}

            <IssueFilters
                searchQuery={searchQuery}
                onSearchChange={handleSearchChange}
                statusFilter={statusFilter}
                onStatusChange={handleStatusChange}
                severityFilter={severityFilter}
                onSeverityChange={handleSeverityChange}
                priorityFilter={priorityFilter}
                onPriorityChange={handlePriorityChange}
                assigneeFilter={assigneeFilter}
                onAssigneeChange={handleAssigneeChange}
                onCreateIssue={() => setShowCreateForm(true)}
            />

            {sortedIssues.length === 0 ? (
                <EmptyState
                    title="No issues yet"
                    description="Nothing matches the current filters."
                />
            ) : (
                <>
                    <IssueTable
                        issues={paginatedIssues}
                        sortField={sortField}
                        sortDirection={sortDirection}
                        onSort={handleSort}
                        onEdit={setEditingIssue}
                    />

                    <IssuePagination
                        currentPage={safeCurrentPage}
                        totalPages={totalPages}
                        totalIssues={sortedIssues.length}
                        issuesPerPage={issuesPerPage}
                        onPageChange={handlePageChange}
                    />
                </>
            )}

            {showCreateForm && (
                <IssueForm
                    title="Create Issue"
                    components={components}
                    releases={releases}
                    onSubmit={handleCreateIssue}
                    onClose={() => setShowCreateForm(false)}
                />
            )}

            {editingIssue && (
                <IssueForm
                    title="Edit Issue"
                    issue={editingIssue}
                    components={components}
                    releases={releases}
                    onSubmit={handleUpdateIssue}
                    onClose={() => setEditingIssue(null)}
                />
            )}
        </div>
    );
}

/**
 * Sends componentId and releaseId, which is what the API accepts — the earlier form collected
 * component and release as free text, which the backend has no way to resolve.
 *
 * There is deliberately no status field: status is owned by the workflow engine and changes
 * through the transition controls on the issue detail page.
 */
function IssueForm({ title, issue, components, releases, onSubmit, onClose }) {
    const [form, setForm] = useState({
        title: issue?.title ?? "",
        description: issue?.description ?? "",
        severity: issue?.severity ?? "MEDIUM",
        priority: issue?.priority ?? "MEDIUM",
        componentId: issue?.componentId ?? components[0]?.id ?? "",
        releaseId: issue?.releaseId ?? "",
    });

    const handleChange = (field, value) => {
        setForm((current) => ({ ...current, [field]: value }));
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        onSubmit({
            title: form.title,
            description: form.description || null,
            severity: form.severity,
            priority: form.priority,
            componentId: Number(form.componentId),
            releaseId: form.releaseId ? Number(form.releaseId) : null,
        });
    };

    return (
        <div className="issue-form-backdrop">
            <form className="issue-form" onSubmit={handleSubmit}>
                <h2>{title}</h2>

                <label>
                    Title
                    <input
                        value={form.title}
                        onChange={(e) => handleChange("title", e.target.value)}
                        required
                        maxLength={200}
                    />
                </label>

                <label>
                    Description
                    <textarea
                        value={form.description}
                        onChange={(e) =>
                            handleChange("description", e.target.value)
                        }
                    />
                </label>

                <label>
                    Component
                    <select
                        value={form.componentId}
                        onChange={(e) =>
                            handleChange("componentId", e.target.value)
                        }
                        required
                    >
                        {components.map((component) => (
                            <option key={component.id} value={component.id}>
                                {component.name}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    Release
                    <select
                        value={form.releaseId}
                        onChange={(e) =>
                            handleChange("releaseId", e.target.value)
                        }
                    >
                        <option value="">Backlog</option>
                        {releases.map((release) => (
                            <option key={release.id} value={release.id}>
                                {release.name}
                            </option>
                        ))}
                    </select>
                </label>

                <label>
                    Severity
                    <select
                        value={form.severity}
                        onChange={(e) =>
                            handleChange("severity", e.target.value)
                        }
                    >
                        <option value="LOW">Low</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="HIGH">High</option>
                        <option value="CRITICAL">Critical</option>
                    </select>
                </label>

                <label>
                    Priority
                    <select
                        value={form.priority}
                        onChange={(e) =>
                            handleChange("priority", e.target.value)
                        }
                    >
                        <option value="LOW">Low</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="HIGH">High</option>
                        <option value="CRITICAL">Critical</option>
                    </select>
                </label>

                <div className="issue-form-actions">
                    <button type="submit">Save</button>
                    <button type="button" onClick={onClose}>
                        Cancel
                    </button>
                </div>
            </form>
        </div>
    );
}

export default IssuesPage;
