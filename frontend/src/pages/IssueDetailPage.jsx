import { useState } from "react";
import { useParams } from "react-router-dom";
import mockIssues from "../features/issues/mockIssues";
import WorkflowControls from "../features/workflow/WorkflowControls";
import CommentSection from "../features/comments/CommentSection";
import ActivityTimeline from "../features/activity/ActivityTimeline";
import DependencyList from "../features/dependencies/DependencyList";
import IntelligenceCard from "../features/intelligence/IntelligenceCard";

function IssueDetailPage() {
    const { issueId } = useParams();

    const issue =
        mockIssues.find(
            (item) =>
                String(item.id) === String(issueId) ||
                item.issueKey === issueId
        ) || mockIssues[0];

    const [status, setStatus] =
        useState(issue.status);

    const [comments, setComments] = useState([]);

    const [dependencies, setDependencies] =
        useState([]);

    const [activities, setActivities] =
        useState([]);

    const transitions = [
        {
            id: "start",
            name: "Start Progress",
        },
        {
            id: "review",
            name: "Move to Review",
        },
        {
            id: "resolve",
            name: "Resolve",
        },
    ];

    const handleTransition = (transition) => {
        let nextStatus = status;

        if (transition.id === "start") {
            nextStatus = "IN PROGRESS";
        }

        if (transition.id === "review") {
            nextStatus = "IN REVIEW";
        }

        if (transition.id === "resolve") {
            nextStatus = "RESOLVED";
        }

        setStatus(nextStatus);

        setActivities((current) => [
            ...current,
            {
                id: Date.now(),
                actor: "You",
                description: `Changed status from ${status} to ${nextStatus}`,
                createdAt: new Date().toLocaleString(),
            },
        ]);
    };

    const handleAddComment = (comment) => {
        const newComment = {
            id: Date.now(),
            author: "You",
            content: comment.content,
        };

        setComments((current) => [
            ...current,
            newComment,
        ]);

        setActivities((current) => [
            ...current,
            {
                id: Date.now() + 1,
                actor: "You",
                description: "Added a comment",
                createdAt: new Date().toLocaleString(),
            },
        ]);
    };

    const handleDeleteComment = (commentId) => {
        setComments((current) =>
            current.filter(
                (comment) =>
                    comment.id !== commentId
            )
        );
    };

    const handleRemoveDependency = (dependencyId) => {
        setDependencies((current) =>
            current.filter(
                (dependency) =>
                    dependency.id !== dependencyId
            )
        );
    };

    return (
        <main>
            <header>
                <p>{issue.issueKey}</p>

                <h1>{issue.title}</h1>

                <p>
                    Issue details and project activity
                </p>
            </header>

            <section>
                <h2>Issue Information</h2>

                <div>
                    <strong>Status</strong>
                    <p>{status}</p>
                </div>

                <div>
                    <strong>Severity</strong>
                    <p>{issue.severity}</p>
                </div>

                <div>
                    <strong>Priority</strong>
                    <p>{issue.priority}</p>
                </div>

                <div>
                    <strong>Assignee</strong>
                    <p>{issue.assignee}</p>
                </div>
            </section>

            <WorkflowControls
                status={status}
                transitions={transitions}
                onTransition={handleTransition}
            />

            <DependencyList
                dependencies={dependencies}
                onRemove={handleRemoveDependency}
            />

            <section>
                <h2>Intelligence</h2>

                <IntelligenceCard
                    title="Impact Score"
                    value="Pending"
                    description="Calculated by the backend."
                />

                <IntelligenceCard
                    title="Component Health"
                    value="Pending"
                    description="Calculated by the backend."
                />

                <IntelligenceCard
                    title="Release Risk"
                    value="Pending"
                    description="Calculated by the backend."
                />
            </section>

            <CommentSection
                comments={comments}
                onAddComment={handleAddComment}
                onDeleteComment={handleDeleteComment}
            />

            <ActivityTimeline
                activities={activities}
            />
        </main>
    );
}

export default IssueDetailPage;