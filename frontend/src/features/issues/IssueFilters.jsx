function IssueFilters({
                          searchQuery,
                          onSearchChange,
                          statusFilter,
                          onStatusChange,
                          severityFilter,
                          onSeverityChange,
                          priorityFilter,
                          onPriorityChange,
                          assigneeFilter,
                          onAssigneeChange,
                          onCreateIssue,
                      }) {
    return (
        <>
            <div>
                <input
                    type="text"
                    placeholder="Search issues..."
                    value={searchQuery}
                    onChange={(event) =>
                        onSearchChange(event.target.value)
                    }
                />

                <button
                    type="button"
                    onClick={onCreateIssue}
                >
                    Create Issue
                </button>
            </div>

            <div>
                <select
                    value={statusFilter}
                    onChange={(event) =>
                        onStatusChange(event.target.value)
                    }
                >
                    <option value="">All Statuses</option>
                    <option value="OPEN">Open</option>
                    <option value="IN PROGRESS">
                        In Progress
                    </option>
                    <option value="IN REVIEW">
                        In Review
                    </option>
                    <option value="RESOLVED">Resolved</option>
                </select>

                <select
                    value={severityFilter}
                    onChange={(event) =>
                        onSeverityChange(event.target.value)
                    }
                >
                    <option value="">All Severities</option>
                    <option value="CRITICAL">Critical</option>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                </select>

                <select
                    value={priorityFilter}
                    onChange={(event) =>
                        onPriorityChange(event.target.value)
                    }
                >
                    <option value="">All Priorities</option>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                </select>

                <select
                    value={assigneeFilter}
                    onChange={(event) =>
                        onAssigneeChange(event.target.value)
                    }
                >
                    <option value="">All Assignees</option>
                    <option value="Rahul">Rahul</option>
                    <option value="Priya">Priya</option>
                    <option value="Arjun">Arjun</option>
                    <option value="Unassigned">
                        Unassigned
                    </option>
                </select>
            </div>
        </>
    );
}

export default IssueFilters;