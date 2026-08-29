import { useCallback, useEffect, useState } from "react";
import * as intelligenceApi from "../api/intelligenceApi";
import * as releaseApi from "../api/releaseApi";
import { useActiveProject } from "../features/projects/hooks/useActiveProject";
import ProjectPicker from "../features/projects/components/ProjectPicker";
import { Spinner, ErrorState, EmptyState } from "../components/ui";

function riskLabel(score) {
    if (score >= 70) return "High";
    if (score >= 30) return "Moderate";
    return "Low";
}

function ReleaseRiskPage() {
    const {
        projects,
        activeProjectId,
        selectProject,
        isLoading: isProjectLoading,
        error: projectError,
    } = useActiveProject();

    const [risks, setRisks] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = useCallback(() => {
        if (!activeProjectId) {
            setRisks([]);
            setIsLoading(false);
            return;
        }

        setIsLoading(true);
        setError(null);

        releaseApi
            .getReleases(activeProjectId)
            .then((releases) =>
                /*
                 * One risk request per release. allSettled so a release whose score cannot be
                 * read does not hide the others.
                 */
                Promise.allSettled(
                    releases.map((release) =>
                        intelligenceApi.getReleaseRisk(release.id)
                    )
                ).then((results) =>
                    releases.map((release, index) => ({
                        release,
                        risk:
                            results[index].status === "fulfilled"
                                ? results[index].value
                                : null,
                    }))
                )
            )
            .then(setRisks)
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
                description="Create a project to assess release risk."
            />
        );
    }

    return (
        <main>
            <header>
                <h1>Release Risk</h1>
                <p>How risky each release looks right now.</p>
                <ProjectPicker
                    projects={projects}
                    activeProjectId={activeProjectId}
                    onSelect={selectProject}
                />
            </header>

            {risks.length === 0 ? (
                <EmptyState
                    title="No releases"
                    description="Create a release in this project to track its risk."
                />
            ) : (
                <table>
                    <thead>
                        <tr>
                            <th>Release</th>
                            <th>Status</th>
                            <th>Target date</th>
                            <th>Risk</th>
                        </tr>
                    </thead>
                    <tbody>
                        {risks.map(({ release, risk }) => (
                            <tr key={release.id}>
                                <td>{release.name}</td>
                                <td>{release.status}</td>
                                <td>{release.targetDate ?? "—"}</td>
                                <td>
                                    {risk
                                        ? `${risk.riskScore} (${riskLabel(
                                              risk.riskScore
                                          )})`
                                        : "Unavailable"}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </main>
    );
}

export default ReleaseRiskPage;
