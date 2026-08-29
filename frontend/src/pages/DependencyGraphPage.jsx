import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as dependencyApi from "../api/dependencyApi";
import { useActiveProject } from "../features/projects/hooks/useActiveProject";
import ProjectPicker from "../features/projects/components/ProjectPicker";
import { Spinner, ErrorState, EmptyState } from "../components/ui";

/**
 * Renders every edge in the project in one request. The backend rejects cycles when an edge is
 * created, so this list is always a directed acyclic graph.
 */
function DependencyGraphPage() {
    const {
        projects,
        activeProjectId,
        selectProject,
        isLoading: isProjectLoading,
        error: projectError,
    } = useActiveProject();

    const [edges, setEdges] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = useCallback(() => {
        if (!activeProjectId) {
            setEdges([]);
            setIsLoading(false);
            return;
        }

        setIsLoading(true);
        setError(null);

        dependencyApi
            .getProjectGraph(activeProjectId)
            .then(setEdges)
            .catch((err) => setError(err.message))
            .finally(() => setIsLoading(false));
    }, [activeProjectId]);

    useEffect(() => {
        load();
    }, [load]);

    if (isProjectLoading || isLoading) {
        return <Spinner size={24} />;
    }

    if (projectError || error) {
        return <ErrorState message={projectError || error} onRetry={load} />;
    }

    if (!activeProjectId) {
        return (
            <EmptyState
                title="No project selected"
                description="Create a project to map its dependencies."
            />
        );
    }

    const blockerCounts = edges.reduce((counts, edge) => {
        const key = edge.blockingIssue.issueKey;
        counts[key] = (counts[key] ?? 0) + 1;
        return counts;
    }, {});

    return (
        <main>
            <header>
                <h1>Dependencies</h1>
                <p>Every blocking relationship in this project.</p>
                <ProjectPicker
                    projects={projects}
                    activeProjectId={activeProjectId}
                    onSelect={selectProject}
                />
            </header>

            {edges.length === 0 ? (
                <EmptyState
                    title="No dependencies"
                    description="Link issues from an issue's detail page to build the graph."
                />
            ) : (
                <table>
                    <thead>
                        <tr>
                            <th>Blocking</th>
                            <th />
                            <th>Blocked</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {edges.map((edge) => (
                            <tr key={edge.id}>
                                <td>
                                    <Link
                                        to={`/issues/${edge.blockingIssue.id}`}
                                    >
                                        {edge.blockingIssue.issueKey}
                                    </Link>
                                    {blockerCounts[
                                        edge.blockingIssue.issueKey
                                    ] >= 3 && <span> ⚠ bottleneck</span>}
                                </td>
                                <td>blocks</td>
                                <td>
                                    <Link
                                        to={`/issues/${edge.blockedIssue.id}`}
                                    >
                                        {edge.blockedIssue.issueKey}
                                    </Link>
                                </td>
                                <td>{edge.blockedIssue.status}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </main>
    );
}

export default DependencyGraphPage;
