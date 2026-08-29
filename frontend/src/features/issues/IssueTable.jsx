import { Link, useNavigate } from "react-router-dom";

function IssueTable({
                        issues,
                        sortField,
                        sortDirection,
                        onSort,
                        onEdit,
                    }) {
    const navigate = useNavigate();

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
                    <tr
                        key={issue.id}
                        onClick={() =>
                            navigate(`/issues/${issue.id}`)
                        }
                        style={{ cursor: "pointer" }}
                    >
                        <td>
                            {/*
                              * A real link as well as the row click: the row gives mouse users a
                              * large target, but a clickable <tr> is not reachable by keyboard.
                              */}
                            <Link
                                to={`/issues/${issue.id}`}
                                onClick={(event) =>
                                    event.stopPropagation()
                                }
                            >
                                <strong>
                                    {issue.issueKey}
                                </strong>
                            </Link>
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
                                onClick={(event) => {
                                    // Without this the row handler also fires and navigates away.
                                    event.stopPropagation();
                                    onEdit?.(issue);
                                }}
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