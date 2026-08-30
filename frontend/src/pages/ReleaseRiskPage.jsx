import { useCallback, useEffect, useState } from "react";
import * as intelligenceApi from "../api/intelligenceApi";
import * as releaseApi from "../api/releaseApi";
import { useActiveProject } from "../features/projects/hooks/useActiveProject";
import ProjectPicker from "../features/projects/components/ProjectPicker";
import PageHeader from "../components/common/PageHeader";
import { Spinner, ErrorState, EmptyState, Badge, Table } from "../components/ui";
import { humanize, formatDate } from "../utils/format";
import "../features/releases/release-risk.css";

/**
 * Label and color are decided together so the two can never disagree about where
 * a threshold sits. The badge always carries its word, so the level never rests
 * on color alone.
 */
function riskLevel(score) {
    if (score >= 70) return { label: "High", variant: "danger" };
    if (score >= 30) return { label: "Moderate", variant: "warning" };
    return { label: "Low", variant: "success" };
}

const COLUMNS = [
    {
        key: "release",
        header: "Release",
        render: (row) => (
            <span className="rr-release-name">{row.release.name}</span>
        ),
    },
    {
        key: "status",
        header: "Status",
        render: (row) => humanize(row.release.status),
    },
    {
        key: "targetDate",
        header: "Target date",
        render: (row) =>
            formatDate(row.release.targetDate) ?? (
                <span className="rr-muted">—</span>
            ),
    },
    {
        key: "risk",
        header: "Risk",
        render: (row) => {
            if (!row.risk) {
                return <span className="rr-muted">Unavailable</span>;
            }

            const level = riskLevel(row.risk.riskScore);
            return (
                <span className="rr-risk-cell">
                    <span className="rr-score">{row.risk.riskScore}</span>
                    <Badge variant={level.variant}>{level.label}</Badge>
                </span>
            );
        },
    },
];

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
                        id: release.id,
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
            <PageHeader
                title="Release Risk"
                subtitle="How risky each release looks right now."
                action={
                    <ProjectPicker
                        projects={projects}
                        activeProjectId={activeProjectId}
                        onSelect={selectProject}
                    />
                }
            />

            {risks.length === 0 ? (
                <EmptyState
                    title="No releases"
                    description="Create a release in this project to track its risk."
                />
            ) : (
                <Table columns={COLUMNS} data={risks} keyField="id" />
            )}
        </main>
    );
}

export default ReleaseRiskPage;
