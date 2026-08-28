function IssueTable({ issues }) {
    return (
        <div>
            <table>
                <thead>
                <tr>
                    <th>Issue</th>
                    <th>Status</th>
                    <th>Severity</th>
                    <th>Priority</th>
                    <th>Assignee</th>
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