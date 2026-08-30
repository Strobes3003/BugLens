import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import * as issueApi from "../api/issueApi";
import * as commentApi from "../api/commentApi";
import * as activityApi from "../api/activityApi";
import * as dependencyApi from "../api/dependencyApi";
import * as intelligenceApi from "../api/intelligenceApi";
import * as workflowApi from "../api/workflowApi";
import { Spinner, ErrorState, Badge } from "../components/ui";
import WorkflowControls from "../features/workflow/WorkflowControls";
import CommentSection from "../features/comments/CommentSection";
import ActivityTimeline from "../features/activity/ActivityTimeline";
import DependencyList from "../features/dependencies/DependencyList";
import IntelligenceCard from "../features/intelligence/IntelligenceCard";
import {
    humanize,
    statusVariant,
    levelVariant,
} from "../features/issues/issueBadges";
import "../features/issues/issue-detail.css";

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

/** A labelled row in the metadata card. Values that are badges pass `badge`. */
function MetaRow({ label, children }) {
    return (
        <div className="idp-meta-row">
            <span className="idp-label">{label}</span>
            {children}
        </div>
    );
}

function IssueDetailPage() {
    const { issueId } = useParams();

    const [issue, setIssue] = useState(null);
    const [transitions, setTransitions] = useState([]);
    const [comments, setComments] = useState([]);
    const [activities, setActivities] = useState([]);
    const [dependencies, setDependencies] = useState([]);
    const [availableIssues, setAvailableIssues] = useState([]);
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
                    loadedIssue.projectId
                        ? issueApi.getIssues(loadedIssue.projectId)
                        : Promise.resolve([]),
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
                    const loadedProjectIssues = value(5);

                    setAvailableIssues(
                        Array.isArray(loadedProjectIssues)
                            ? loadedProjectIssues
                            : loadedProjectIssues?.content ?? []
                    );

                    setAnalysis(value(4));
                    setHealth(value(6));
                    setRisk(value(7));
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
    const handleAddDependency = ({
                                     blockerIssueId,
                                     blockedIssueId,
                                 }) => {
        setActionError(null);

        return dependencyApi
            .addDependency(blockerIssueId, blockedIssueId)
            .then(load)
            .catch((err) => {
                setActionError(err.message);
                throw err;
            });
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
            {actionError && <ErrorState message={actionError} />}

            <div className="idp-grid">
                <div className="idp-col">
                    <header className="idp-card">
                        <p className="idp-key">{issue.issueKey}</p>
                        <h1 className="idp-title">{issue.title}</h1>
                    </header>

                    <section className="idp-card">
                        <h2 className="idp-section-title">Description</h2>
                        {issue.description ? (
                            <p className="idp-description">
                                {issue.description}
                            </p>
                        ) : (
                            <p className="idp-empty">
                                No description was provided for this issue.
                            </p>
                        )}
                    </section>

                    <WorkflowControls
                        status={issue.status}
                        transitions={transitions}
                        onTransition={handleTransition}
                    />

                    <DependencyList
                        dependencies={dependencies}
                        availableIssues={availableIssues}
                        currentIssueId={issue.id}
                        onRemove={handleRemoveDependency}
                        onAdd={handleAddDependency}
                    />

                    <section className="idp-card">
                        <h2 className="idp-section-title">Intelligence</h2>

                        <div className="idp-stat-grid">
                            <IntelligenceCard
                                title="Blast Radius"
                                value={
                                    analysis
                                        ? String(analysis.blastRadius)
                                        : "Unavailable"
                                }
                                description={
                                    analysis?.hasBottleneck
                                        ? "This issue is a bottleneck — several issues wait on it directly."
                                        : "Issues downstream of this one, at any depth."
                                }
                            />

                            <IntelligenceCard
                                title="Component Health"
                                value={
                                    health
                                        ? String(health.healthScore)
                                        : "Unavailable"
                                }
                                description={
                                    health
                                        ? `Health of ${health.componentName}.`
                                        : "Not calculated yet."
                                }
                            />

                            <IntelligenceCard
                                title="Release Risk"
                                value={
                                    risk
                                        ? String(risk.riskScore)
                                        : "Not in a release"
                                }
                                description={
                                    risk
                                        ? `Risk for ${risk.releaseName}.`
                                        : "This issue is in the backlog."
                                }
                            />
                        </div>
                    </section>

                    <div className="idp-card idp-feed">
                        <CommentSection
                            comments={comments}
                            onAddComment={handleAddComment}
                            onDeleteComment={handleDeleteComment}
                        />
                    </div>

                    <div className="idp-card idp-feed">
                        <ActivityTimeline activities={activities} />
                    </div>
                </div>

                <aside className="idp-side">
                    <div className="idp-card">
                        <h2 className="idp-section-title">Details</h2>

                        <MetaRow label="Status">
                            <span>
                                <Badge variant={statusVariant(issue.status)}>
                                    {humanize(issue.status)}
                                </Badge>
                            </span>
                        </MetaRow>

                        <MetaRow label="Severity">
                            <span>
                                <Badge variant={levelVariant(issue.severity)}>
                                    {humanize(issue.severity)}
                                </Badge>
                            </span>
                        </MetaRow>

                        <MetaRow label="Priority">
                            <span>
                                <Badge variant={levelVariant(issue.priority)}>
                                    {humanize(issue.priority)}
                                </Badge>
                            </span>
                        </MetaRow>

                        <MetaRow label="Assignee">
                            <span className="idp-value">
                                {issue.assigneeName ?? "Unassigned"}
                            </span>
                        </MetaRow>

                        <MetaRow label="Component">
                            <span className="idp-value">
                                {issue.componentName ?? "—"}
                            </span>
                        </MetaRow>

                        <MetaRow label="Release">
                            <span className="idp-value">
                                {issue.releaseName ?? "Backlog"}
                            </span>
                        </MetaRow>
                    </div>
                </aside>
            </div>
        </main>
    );
}

export default IssueDetailPage;
