import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import * as issueApi from "../api/issueApi";
import * as commentApi from "../api/commentApi";
import * as activityApi from "../api/activityApi";
import * as dependencyApi from "../api/dependencyApi";
import * as intelligenceApi from "../api/intelligenceApi";
import * as workflowApi from "../api/workflowApi";
import { Spinner, ErrorState } from "../components/ui";
import WorkflowControls from "../features/workflow/WorkflowControls";
import CommentSection from "../features/comments/CommentSection";
import ActivityTimeline from "../features/activity/ActivityTimeline";
import DependencyList from "../features/dependencies/DependencyList";
import IntelligenceCard from "../features/intelligence/IntelligenceCard";

const humanize = (status) =>
    status
        .split("_")
        .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
        .join(" ");

/** Both directions flattened into the rows DependencyList renders. */
function toDependencyRows(dependencies, issueId) {
    if (!dependencies) return [];

    const blockedBy = (dependencies.blockedBy ?? []).map((other) => ({
        id: `${other.id}-${issueId}`,
        blockerId: other.id,
        blockedId: issueId,
        issueKey: other.issueKey,
        targetIssueKey: dependencies.issueKey,
        type: "BLOCKED BY",
    }));

    const blocking = (dependencies.blocking ?? []).map((other) => ({
        id: `${issueId}-${other.id}`,
        blockerId: issueId,
        blockedId: other.id,
        issueKey: dependencies.issueKey,
        targetIssueKey: other.issueKey,
        type: "BLOCKS",
    }));

    return [...blockedBy, ...blocking];
}

function IssueDetailPage() {
    const { issueId } = useParams();

    const [issue, setIssue] = useState(null);
    const [transitions, setTransitions] = useState([]);
    const [comments, setComments] = useState([]);
    const [activities, setActivities] = useState([]);
    const [dependencies, setDependencies] = useState([]);
    const [analysis, setAnalysis] = useState(null);
    const [health, setHealth] = useState(null);
    const [risk, setRisk] = useState(null);

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [actionError, setActionError] = useState(null);

    const load = useCallback(() => {
        setIsLoading(true);
        setError(null);

        issueApi
            .getIssue(issueId)
            .then((loadedIssue) => {
                setIssue(loadedIssue);

                /*
                 * Everything below hangs off the issue and is independent of the others, so a
                 * single failure should not blank the page. allSettled keeps whatever loaded.
                 */
                return Promise.allSettled([
                    workflowApi.getTransitions(issueId),
                    commentApi.getComments(issueId),
                    activityApi.getActivity(issueId),
                    dependencyApi.getDependencies(issueId),
                    dependencyApi.getDependencyAnalysis(issueId),
                    loadedIssue.componentId
                        ? intelligenceApi.getComponentHealth(
                              loadedIssue.componentId
                          )
                        : Promise.resolve(null),
                    loadedIssue.releaseId
                        ? intelligenceApi.getReleaseRisk(loadedIssue.releaseId)
                        : Promise.resolve(null),
                ]).then((results) => {
                    const value = (index) =>
                        results[index].status === "fulfilled"
                            ? results[index].value
                            : null;

                    const workflow = value(0);
                    setTransitions(
                        (workflow?.allowedTransitions ?? []).map((status) => ({
                            id: status,
                            name: humanize(status),
                        }))
                    );

                    setComments(
                        (value(1) ?? []).map((comment) => ({
                            id: comment.id,
                            author: comment.author?.name ?? "Unknown",
                            content: comment.body,
                            text: comment.body,
                        }))
                    );

                    setActivities(
                        (value(2) ?? []).map((entry) => ({
                            id: entry.id,
                            actor: entry.actor?.name ?? "Unknown",
                            description: entry.description,
                            createdAt: new Date(
                                entry.createdAt
                            ).toLocaleString(),
                        }))
                    );

                    setDependencies(toDependencyRows(value(3), loadedIssue.id));
                    setAnalysis(value(4));
                    setHealth(value(5));
                    setRisk(value(6));
                });
            })
            .catch((err) => setError(err.message))
            .finally(() => setIsLoading(false));
    }, [issueId]);

    useEffect(() => {
        load();
    }, [load]);

    const handleTransition = (transition) => {
        setActionError(null);
        workflowApi
            .transitionIssue(issueId, transition.id)
            .then(load)
            .catch((err) => setActionError(err.message));
    };

    const handleAddComment = (comment) => {
        setActionError(null);
        commentApi
            .createComment(issueId, comment.content ?? comment.text)
            .then(load)
            .catch((err) => setActionError(err.message));
    };

    const handleDeleteComment = (commentId) => {
        setActionError(null);
        commentApi
            .deleteComment(commentId)
            .then(load)
            .catch((err) => setActionError(err.message));
    };

    const handleRemoveDependency = (dependencyId) => {
        const row = dependencies.find((item) => item.id === dependencyId);
        if (!row) return;

        setActionError(null);
        dependencyApi
            .removeDependency(row.blockerId, row.blockedId)
            .then(load)
            .catch((err) => setActionError(err.message));
    };

    if (isLoading) {
        return <Spinner size={24} />;
    }

    if (error) {
        return <ErrorState message={error} onRetry={load} />;
    }

    if (!issue) {
        return <ErrorState message="Issue not found." />;
    }

    return (
        <main>
            <header>
                <p>{issue.issueKey}</p>
                <h1>{issue.title}</h1>
                <p>Issue details and project activity</p>
            </header>

            {actionError && <ErrorState message={actionError} />}

            <section>
                <h2>Issue Information</h2>

                <div>
                    <strong>Status</strong>
                    <p>{issue.status}</p>
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
                    <p>{issue.assigneeName ?? "Unassigned"}</p>
                </div>

                <div>
                    <strong>Component</strong>
                    <p>{issue.componentName}</p>
                </div>

                <div>
                    <strong>Release</strong>
                    <p>{issue.releaseName ?? "Backlog"}</p>
                </div>

                {issue.description && (
                    <div>
                        <strong>Description</strong>
                        <p>{issue.description}</p>
                    </div>
                )}
            </section>

            <WorkflowControls
                status={issue.status}
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
                    title="Blast Radius"
                    value={
                        analysis ? String(analysis.blastRadius) : "Unavailable"
                    }
                    description={
                        analysis?.hasBottleneck
                            ? "This issue is a bottleneck — several issues wait on it directly."
                            : "Issues downstream of this one, at any depth."
                    }
                />

                <IntelligenceCard
                    title="Component Health"
                    value={health ? String(health.healthScore) : "Unavailable"}
                    description={
                        health
                            ? `Health of ${health.componentName}.`
                            : "Not calculated yet."
                    }
                />

                <IntelligenceCard
                    title="Release Risk"
                    value={risk ? String(risk.riskScore) : "Not in a release"}
                    description={
                        risk
                            ? `Risk for ${risk.releaseName}.`
                            : "This issue is in the backlog."
                    }
                />
            </section>

            <CommentSection
                comments={comments}
                onAddComment={handleAddComment}
                onDeleteComment={handleDeleteComment}
            />

            <ActivityTimeline activities={activities} />
        </main>
    );
}

export default IssueDetailPage;
