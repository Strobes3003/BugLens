import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as intelligenceApi from "../api/intelligenceApi";
import { useActiveProject } from "../features/projects/hooks/useActiveProject";
import ProjectPicker from "../features/projects/components/ProjectPicker";
import { Spinner, ErrorState, EmptyState } from "../components/ui";

/**
 * Ranking is the backend's call, not the UI's: the list is rendered in the order the API
 * returns it, highest impact first with ties broken by age.
 */
function FixNextPage() {
    const {
        projects,
        activeProjectId,
        selectProject,
        isLoading: isProjectLoading,
        error: projectError,
    } = useActiveProject();

    const [ranked, setRanked] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = useCallback(() => {
        if (!activeProjectId) {
            setRanked([]);
            setIsLoading(false);
            return;
        }

        setIsLoading(true);
        setError(null);

        intelligenceApi
            .getFixNext(activeProjectId, 10)
            .then(setRanked)
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
                description="Create a project to see what to fix next."
            />
        );
    }

    return (
        <main>
            <header>
                <h1>Fix Next</h1>
                <p>What the team should pick up next, ranked by impact.</p>
                <ProjectPicker
                    projects={projects}
                    activeProjectId={activeProjectId}
                    onSelect={selectProject}
                />
            </header>

            {ranked.length === 0 ? (
                <EmptyState
                    title="Nothing ranked yet"
                    description="Impact scores appear once issues are created and updated."
                />
            ) : (
                <ol>
                    {ranked.map((entry) => (
                        <li key={entry.issueId}>
                            <Link to={`/issues/${entry.issueId}`}>
                                <strong>{entry.issueKey}</strong> {entry.title}
                            </Link>
                            <span> — impact {entry.impactScore}</span>
                            <span>
                                {" "}
                                ({entry.severity} severity, {entry.priority}{" "}
                                priority, {entry.status})
                            </span>
                        </li>
                    ))}
                </ol>
            )}
        </main>
    );
}

export default FixNextPage;
