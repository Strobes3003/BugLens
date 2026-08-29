import { useEffect, useMemo, useState } from "react";
import dependencyApi from "../api/dependencyApi";
import mockIssues from "../features/issues/mockIssues";

const MOCK_DEPENDENCIES = [
    {
        from: "BL-101",
        to: "BL-107",
        relationship: "BLOCKS",
    },
    {
        from: "BL-107",
        to: "BL-103",
        relationship: "AFFECTS",
    },
    {
        from: "BL-103",
        to: "BL-109",
        relationship: "BLOCKS",
    },
    {
        from: "BL-109",
        to: "BL-104",
        relationship: "AFFECTS",
    },
];

function DependencyGraphPage() {
    const [dependencies, setDependencies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    /*
     * Project ID will eventually come from the workspace/project
     * context owned by Member 1.
     *
     * Keep this configurable rather than hardcoding backend logic.
     */
    const projectId = null;

    useEffect(() => {
        let mounted = true;

        const loadGraph = async () => {
            setLoading(true);
            setError("");

            /*
             * Until the project context/backend contract is connected,
             * use the existing mock graph.
             */
            if (!projectId) {
                if (mounted) {
                    setDependencies(MOCK_DEPENDENCIES);
                    setLoading(false);
                }
                return;
            }

            try {
                const data = await dependencyApi.getGraph(projectId);

                if (!mounted) return;

                /*
                 * Keep normalization lightweight.
                 * The exact backend response shape can be adjusted
                 * once Member 4 provides the final contract.
                 */
                const relationships =
                    Array.isArray(data)
                        ? data
                        : data?.relationships ||
                          data?.dependencies ||
                          [];

                setDependencies(relationships);
            } catch (err) {
                if (!mounted) return;

                setError(
                    err?.message ||
                    "Unable to load dependency graph."
                );
                setDependencies([]);
            } finally {
                if (mounted) {
                    setLoading(false);
                }
            }
        };

        loadGraph();

        return () => {
            mounted = false;
        };
    }, [projectId]);

    const getIssueTitle = (key) => {
        return (
            mockIssues.find(
                (issue) =>
                    issue.issueKey === key
            )?.title ?? key
        );
    };

    const normalizedDependencies = useMemo(() => {
        return dependencies.map((dependency) => ({
            from:
                dependency.from ??
                dependency.source ??
                dependency.issueKey ??
                dependency.fromIssueKey,
            to:
                dependency.to ??
                dependency.target ??
                dependency.dependencyIssueKey ??
                dependency.toIssueKey,
            relationship:
                dependency.relationship ??
                dependency.type ??
                "DEPENDENCY",
        }));
    }, [dependencies]);

    const issueCount = useMemo(() => {
        const keys = new Set();

        normalizedDependencies.forEach(
            ({ from, to }) => {
                if (from) keys.add(from);
                if (to) keys.add(to);
            }
        );

        return keys.size;
    }, [normalizedDependencies]);

    return (
        <div>
            <header>
                <h1>Dependency Graph</h1>

                <p>
                    Visualize relationships and dependency
                    impact between issues.
                </p>
            </header>

            {loading && (
                <section>
                    <p>Loading dependency graph...</p>
                </section>
            )}

            {error && (
                <section>
                    <h2>Unable to load graph</h2>
                    <p>{error}</p>
                </section>
            )}

            {!loading &&
                !error &&
                normalizedDependencies.length === 0 && (
                    <section>
                        <h2>No Dependencies</h2>
                        <p>
                            No dependency relationships have
                            been defined for this project.
                        </p>
                    </section>
                )}

            {!loading &&
                normalizedDependencies.length > 0 && (
                    <>
                        <section>
                            <h2>Dependency Relationships</h2>

                            {normalizedDependencies.map(
                                (dependency, index) => (
                                    <article
                                        key={`${dependency.from}-${dependency.to}-${index}`}
                                    >
                                        <div>
                                            <strong>
                                                {dependency.from}
                                            </strong>

                                            {" → "}

                                            <strong>
                                                {dependency.to}
                                            </strong>
                                        </div>

                                        <p>
                                            {getIssueTitle(
                                                dependency.from
                                            )}

                                            {" → "}

                                            {getIssueTitle(
                                                dependency.to
                                            )}
                                        </p>

                                        <span>
                                            {dependency.relationship}
                                        </span>
                                    </article>
                                )
                            )}
                        </section>

                        <section>
                            <h2>Dependency Analysis</h2>

                            <p>
                                Issues involved:{" "}
                                {issueCount}
                            </p>

                            <p>
                                Total relationships:{" "}
                                {
                                    normalizedDependencies.length
                                }
                            </p>

                            <p>
                                Impact analysis:{" "}
                                <strong>
                                    Pending backend analysis
                                </strong>
                            </p>

                            <p>
                                Cycle detection:{" "}
                                <strong>
                                    Pending backend analysis
                                </strong>
                            </p>
                        </section>
                    </>
                )}
        </div>
    );
}

export default DependencyGraphPage;
