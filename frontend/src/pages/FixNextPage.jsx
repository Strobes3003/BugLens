import { useEffect, useState } from "react";
import intelligenceApi from "../api/intelligenceApi";
import mockIssues from "../features/issues/mockIssues";

function FixNextPage() {
    const [recommendation, setRecommendation] =
        useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    /*
     * Project ID will eventually come from the project/workspace
     * context owned by Member 1.
     *
     * Until that integration is available, the page uses
     * mock issue data so the frontend can be completed first.
     */
    const projectId = null;

    useEffect(() => {
        let mounted = true;

        const loadRecommendation = async () => {
            setLoading(true);
            setError("");

            /*
             * Backend integration will be enabled once the
             * final project context and API contract are available.
             */
            if (!projectId) {
                if (mounted) {
                    setRecommendation({
                        issue: mockIssues[0],
                        reason:
                            "Recommendation will be provided by backend intelligence.",
                    });
                    setLoading(false);
                }

                return;
            }

            try {
                const data =
                    await intelligenceApi.getFixNext(
                        projectId
                    );

                if (!mounted) return;

                setRecommendation(data);
            } catch (err) {
                if (!mounted) return;

                setError(
                    err?.message ||
                    "Unable to load Fix Next recommendation."
                );
                setRecommendation(null);
            } finally {
                if (mounted) {
                    setLoading(false);
                }
            }
        };

        loadRecommendation();

        return () => {
            mounted = false;
        };
    }, [projectId]);

    const issue = recommendation?.issue;

    return (
        <div>
            <header>
                <h1>Fix Next</h1>

                <p>
                    Identify the issue that should be
                    prioritized for fixing next.
                </p>
            </header>

            {loading && (
                <section>
                    <p>
                        Loading recommendation...
                    </p>
                </section>
            )}

            {error && (
                <section>
                    <h2>
                        Unable to load recommendation
                    </h2>

                    <p>{error}</p>
                </section>
            )}

            {!loading &&
                !error &&
                recommendation && (
                    <>
                        <section>
                            <h2>Recommended Issue</h2>

                            {issue ? (
                                <>
                                    <h3>
                                        {issue.issueKey}
                                    </h3>

                                    <p>
                                        {issue.title}
                                    </p>

                                    <p>
                                        Status:{" "}
                                        {issue.status}
                                    </p>

                                    <p>
                                        Severity:{" "}
                                        {issue.severity}
                                    </p>

                                    <p>
                                        Priority:{" "}
                                        {issue.priority}
                                    </p>

                                    <p>
                                        Assignee:{" "}
                                        {issue.assignee ??
                                            "Unassigned"}
                                    </p>
                                </>
                            ) : (
                                <p>
                                    No issue recommendation
                                    is currently available.
                                </p>
                            )}
                        </section>

                        <section>
                            <h2>Why This Issue?</h2>

                            <p>
                                {recommendation.reason ??
                                    "Backend intelligence analysis is pending."}
                            </p>
                        </section>

                        <section>
                            <h2>Recommendation Score</h2>

                            <p>
                                {recommendation.score ??
                                    "Pending backend analysis"}
                            </p>
                        </section>
                    </>
                )}

            {!loading &&
                !error &&
                !recommendation && (
                    <section>
                        <h2>No Recommendation</h2>

                        <p>
                            There is currently no issue
                            recommended for the next fix.
                        </p>
                    </section>
                )}
        </div>
    );
}

export default FixNextPage;
