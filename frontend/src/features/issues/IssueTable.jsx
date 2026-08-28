function IssueTable({
                        issues,
                        sortField,
                        sortDirection,
                        onSort,
                    }) {
    const renderSortIndicator = (field) => {
        if (sortField !== field) {
            return "";
        }

        return sortDirection === "asc" ? " ↑" : " ↓";
    };

    return (
        <div>
            <table>
                <thead>
                <tr>
                    <th>
                        <button onClick={() => onSort("issueKey")}>
                            Issue{renderSortIndicator("issueKey")}
                        </button>
                    </th>

                    <th>
                        <button onClick={() => onSort("status")}>
                            Status{renderSortIndicator("status")}
                        </button>
                    </th>

                    <th>
                        <button onClick={() => onSort("severity")}>
                            Severity{renderSortIndicator("severity")}
                        </button>
                    </th>

                    <th>
                        <button onClick={() => onSort("priority")}>
                            Priority{renderSortIndicator("priority")}
                        </button>
                    </th>

                    <th>
                        <button onClick={() => onSort("assignee")}>
                            Assignee{renderSortIndicator("assignee")}
                        </button>
                    </th>
                </tr>
                </thead>

                <tbody>
                {issues.map((issue) => (
                    <tr key={issue.id}>
                        <td>
                            <strong>{issue.issueKey}</strong>
                            <br />
                            {issue.title}
                        </td>

                        <td>{issue.status}</td>
                        <td>{issue.severity}</td>
                        <td>{issue.priority}</td>
                        <td>{issue.assignee}</td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}

export default IssueTable;