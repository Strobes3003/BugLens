function IssueTable({
                        issues,
                        sortField,
                        sortDirection,
                        onSort,
                        onEdit,
                    }) {
    const renderSortIndicator = (field) => {
        if (sortField !== field) {
            return "";
        }

        return sortDirection === "asc"
            ? " ↑"
            : " ↓";
    };

    if (issues.length === 0) {
        return (
            <div>
                <h3>No issues found</h3>
                <p>
                    Try changing your search or filters.
                </p>
            </div>
        );
    }

    return (
        <div>
            <table>
                <thead>
                <tr>
                    <th>
                        <button
                            type="button"
                            onClick={() =>
                                onSort("issueKey")
                            }
                        >
                            Issue
                            {renderSortIndicator(
                                "issueKey"
                            )}
                        </button>
                    </th>

                    <th>
                        <button
                            type="button"
                            onClick={() =>
                                onSort("status")
                            }
                        >
                            Status
                            {renderSortIndicator(
                                "status"
                            )}
                        </button>
                    </th>

                    <th>
                        <button
                            type="button"
                            onClick={() =>
                                onSort("severity")
                            }
                        >
                            Severity
                            {renderSortIndicator(
                                "severity"
                            )}
                        </button>
                    </th>

                    <th>
                        <button
                            type="button"
                            onClick={() =>
                                onSort("priority")
                            }
                        >
                            Priority
                            {renderSortIndicator(
                                "priority"
                            )}
                        </button>
                    </th>

                    <th>
                        <button
                            type="button"
                            onClick={() =>
                                onSort("assignee")
                            }
                        >
                            Assignee
                            {renderSortIndicator(
                                "assignee"
                            )}
                        </button>
                    </th>

                    <th>Actions</th>
                </tr>
                </thead>

                <tbody>
                {issues.map((issue) => (
                    <tr key={issue.id}>
                        <td>
                            <strong>
                                {issue.issueKey}
                            </strong>
                            <br />
                            {issue.title}
                        </td>

                        <td>{issue.status}</td>
                        <td>{issue.severity}</td>
                        <td>{issue.priority}</td>
                        <td>{issue.assignee}</td>

                        <td>
                            <button
                                type="button"
                                onClick={() =>
                                    onEdit?.(issue)
                                }
                            >
                                Edit
                            </button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}

export default IssueTable;