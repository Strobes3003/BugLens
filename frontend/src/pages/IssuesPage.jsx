import { useState } from "react";
import mockIssues from "../features/issues/mockIssues";
import IssueFilters from "../features/issues/IssueFilters";
import IssueTable from "../features/issues/IssueTable";
import IssuePagination from "../features/issues/IssuePagination";

function IssuesPage() {
    const [issues, setIssues] = useState(mockIssues);

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

    const issuesPerPage = 10;

    const filteredIssues = issues.filter((issue) => {
        const query = searchQuery.toLowerCase().trim();

        const matchesSearch =
            !query ||
            issue.title.toLowerCase().includes(query) ||
            issue.issueKey.toLowerCase().includes(query);

        const matchesStatus =
            !statusFilter || issue.status === statusFilter;

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
            severity: {
                CRITICAL: 4,
                HIGH: 3,
                MEDIUM: 2,
                LOW: 1,
            },
            priority: {
                HIGH: 3,
                MEDIUM: 2,
                LOW: 1,
            },
        };

        if (rankings[sortField]) {
            const comparison =
                (rankings[sortField][a[sortField]] ?? 0) -
                (rankings[sortField][b[sortField]] ?? 0);

            return sortDirection === "asc"
                ? -comparison
                : comparison;
        }

        const valueA = String(a[sortField] ?? "");
        const valueB = String(b[sortField] ?? "");

        const comparison = valueA.localeCompare(
            valueB,
            undefined,
            { sensitivity: "base" }
        );

        return sortDirection === "asc"
            ? comparison
            : -comparison;
    });

    const totalPages = Math.max(
        1,
        Math.ceil(sortedIssues.length / issuesPerPage)
    );

    const safeCurrentPage = Math.min(
        currentPage,
        totalPages
    );

    const startIndex =
        (safeCurrentPage - 1) * issuesPerPage;

    const paginatedIssues = sortedIssues.slice(
        startIndex,
        startIndex + issuesPerPage
    );

    const resetPage = () => {
        setCurrentPage(1);
    };

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
        setCurrentPage(1);

        if (sortField === field) {
            setSortDirection((current) =>
                current === "asc" ? "desc" : "asc"
            );
        } else {
            setSortField(field);
            setSortDirection("asc");
        }
    };

    const handlePageChange = (page) => {
        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
        }
    };

    const handleCreateIssue = (issueData) => {
        const nextNumber =
            Math.max(
                ...issues.map((issue) => issue.id),
                0
            ) + 1;

        const newIssue = {
            id: nextNumber,
            issueKey: `BL-${100 + nextNumber}`,
            ...issueData,
        };

        setIssues((current) => [newIssue, ...current]);
        setShowCreateForm(false);
        setCurrentPage(1);
    };

    const handleUpdateIssue = (issueData) => {
        setIssues((current) =>
            current.map((issue) =>
                issue.id === editingIssue.id
                    ? { ...issue, ...issueData }
                    : issue
            )
        );

        setEditingIssue(null);
    };

    return (
        <div>
            <header>
                <h1>Issues</h1>
                <p>Track, manage, and prioritize project issues.</p>
            </header>

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

            {showCreateForm && (
                <IssueForm
                    title="Create Issue"
                    onSubmit={handleCreateIssue}
                    onClose={() => setShowCreateForm(false)}
                />
            )}

            {editingIssue && (
                <IssueForm
                    title="Edit Issue"
                    issue={editingIssue}
                    onSubmit={handleUpdateIssue}
                    onClose={() => setEditingIssue(null)}
                />
            )}
        </div>
    );
}

function IssueForm({ title, issue, onSubmit, onClose }) {
    const [form, setForm] = useState({
        title: issue?.title ?? "",
        status: issue?.status ?? "OPEN",
        severity: issue?.severity ?? "MEDIUM",
        priority: issue?.priority ?? "MEDIUM",
        assignee: issue?.assignee ?? "Unassigned",
        component: issue?.component ?? "Authentication",
        release: issue?.release ?? "Unreleased",
        description: issue?.description ?? "",
    });

    const handleChange = (field, value) => {
        setForm((current) => ({
            ...current,
            [field]: value,
        }));
    };

    const handleSubmit = (event) => {
        event.preventDefault();

        if (!form.title.trim()) {
            return;
        }

        onSubmit({
            ...form,
            title: form.title.trim(),
        });
    };

    return (
        <div>
            <div>
                <h2>{title}</h2>

                <form onSubmit={handleSubmit}>
                    <div>
                        <label>Title</label>
                        <input
                            value={form.title}
                            onChange={(event) =>
                                handleChange(
                                    "title",
                                    event.target.value
                                )
                            }
                            placeholder="Issue title"
                        />
                    </div>

                    <div>
                        <label>Description</label>
                        <textarea
                            value={form.description}
                            onChange={(event) =>
                                handleChange(
                                    "description",
                                    event.target.value
                                )
                            }
                            placeholder="Describe the issue..."
                        />
                    </div>

                    <div>
                        <label>Status</label>
                        <select
                            value={form.status}
                            onChange={(event) =>
                                handleChange(
                                    "status",
                                    event.target.value
                                )
                            }
                        >
                            <option value="OPEN">Open</option>
                            <option value="IN PROGRESS">
                                In Progress
                            </option>
                            <option value="IN REVIEW">
                                In Review
                            </option>
                            <option value="RESOLVED">
                                Resolved
                            </option>
                        </select>
                    </div>

                    <div>
                        <label>Severity</label>
                        <select
                            value={form.severity}
                            onChange={(event) =>
                                handleChange(
                                    "severity",
                                    event.target.value
                                )
                            }
                        >
                            <option value="CRITICAL">Critical</option>
                            <option value="HIGH">High</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="LOW">Low</option>
                        </select>
                    </div>

                    <div>
                        <label>Priority</label>
                        <select
                            value={form.priority}
                            onChange={(event) =>
                                handleChange(
                                    "priority",
                                    event.target.value
                                )
                            }
                        >
                            <option value="HIGH">High</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="LOW">Low</option>
                        </select>
                    </div>

                    <div>
                        <label>Assignee</label>
                        <select
                            value={form.assignee}
                            onChange={(event) =>
                                handleChange(
                                    "assignee",
                                    event.target.value
                                )
                            }
                        >
                            <option value="Rahul">Rahul</option>
                            <option value="Priya">Priya</option>
                            <option value="Arjun">Arjun</option>
                            <option value="Unassigned">
                                Unassigned
                            </option>
                        </select>
                    </div>

                    <div>
                        <label>Component</label>
                        <input
                            value={form.component}
                            onChange={(event) =>
                                handleChange(
                                    "component",
                                    event.target.value
                                )
                            }
                        />
                    </div>

                    <div>
                        <label>Release</label>
                        <input
                            value={form.release}
                            onChange={(event) =>
                                handleChange(
                                    "release",
                                    event.target.value
                                )
                            }
                        />
                    </div>

                    <button type="submit">
                        {issue ? "Save Changes" : "Create Issue"}
                    </button>

                    <button type="button" onClick={onClose}>
                        Cancel
                    </button>
                </form>
            </div>
        </div>
    );
}

export default IssuesPage;