import { useState } from "react";
import mockIssues from "../features/issues/mockIssues";
import IssueFilters from "../features/issues/IssueFilters";
import IssueTable from "../features/issues/IssueTable";
import IssuePagination from "../features/issues/IssuePagination";

function IssuesPage() {
    const [searchQuery, setSearchQuery] = useState("");
    const [statusFilter, setStatusFilter] = useState("");
    const [severityFilter, setSeverityFilter] = useState("");
    const [priorityFilter, setPriorityFilter] = useState("");
    const [assigneeFilter, setAssigneeFilter] = useState("");

    const filteredIssues = mockIssues.filter((issue) => {
        const query = searchQuery.toLowerCase();

        const matchesSearch =
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

    return (
        <div>
            <div>
                <h1>Issues</h1>
                <p>Track, manage, and prioritize project issues.</p>
            </div>

            <IssueFilters
                searchQuery={searchQuery}
                onSearchChange={setSearchQuery}
                statusFilter={statusFilter}
                onStatusChange={setStatusFilter}
                severityFilter={severityFilter}
                onSeverityChange={setSeverityFilter}
                priorityFilter={priorityFilter}
                onPriorityChange={setPriorityFilter}
                assigneeFilter={assigneeFilter}
                onAssigneeChange={setAssigneeFilter}
            />

            {filteredIssues.length === 0 ? (
                <div>
                    <h2>No issues found</h2>
                    <p>Try changing your search or filters.</p>
                </div>
            ) : (
                <IssueTable issues={filteredIssues} />
            )}

            <IssuePagination totalIssues={filteredIssues.length} />
        </div>
    );
}

export default IssuesPage;