import { useEffect, useState } from "react";
import intelligenceApi from "../api/intelligenceApi";

const RELEASES = [
    {
        id: null,
        name: "v2.4",
    },
    {
        id: null,
        name: "v2.5",
    },
];

function ReleaseRiskPage() {
    const [releaseRisk, setReleaseRisk] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    /*
     * Release IDs will eventually come from the project/release
     * context provided by the rest of the application.
     *
     * Keep these as placeholders until the final backend contract
     * and release/project context are connected.
     */


    useEffect(() => {
        let mounted = true;

        const loadReleaseRisk = async () => {
            setLoading(true);
            setError("");

            /*
             * No real release IDs are available yet.
             * Keep the page in a pending state rather than
             * calculating risk on the frontend.
             */
            if (
                RELEASES.every(
                    (release) => !release.id
                )
            ) {
                if (mounted) {
                    setReleaseRisk([]);
                    setLoading(false);
                }

                return;
            }

            try {
                const results =
                    await Promise.all(
                        RELEASES
                            .filter(
                                (release) =>
                                    release.id
                            )
                            .map(async (release) => {
                                const data =
                                    await intelligenceApi.getReleaseRisk(
                                        release.id
                                    );

                                return {
                                    ...release,
                                    ...data,
                                };
                            })
                    );

                if (!mounted) return;

                setReleaseRisk(results);
            } catch (err) {
                if (!mounted) return;

                setError(
                    err?.message ||
                    "Unable to load release risk."
                );
                setReleaseRisk([]);
            } finally {
                if (mounted) {
                    setLoading(false);
                }
            }
        };

        loadReleaseRisk();

        return () => {
            mounted = false;
        };
    }, []);

    const getRiskLevel = (score) => {
        if (typeof score !== "number") {
            return "Pending";
        }

        if (score >= 70) {
            return "CRITICAL";
        }

        if (score >= 40) {
            return "HIGH";
        }

        if (score >= 20) {
            return "MEDIUM";
        }

        return "LOW";
    };

    return (
        <div>
            <header>
                <h1>Release Risk</h1>

                <p>
                    Analyze issue risk before releasing a
                    project version.
                </p>
            </header>

            {loading && (
                <section>
                    <p>
                        Loading release risk...
                    </p>
                </section>
            )}

            {error && (
                <section>
                    <h2>
                        Unable to load release risk
                    </h2>

                    <p>{error}</p>
                </section>
            )}

            {!loading &&
                !error &&
                releaseRisk.length === 0 && (
                    <section>
                        <h2>Release Risk Analysis</h2>

                        <p>
                            Release risk analysis is
                            pending backend intelligence.
                        </p>

                        {RELEASES.map((release) => (
                            <article
                                key={release.name}
                            >
                                <h3>
                                    {release.name}
                                </h3>

                                <p>
                                    Risk Score:{" "}
                                    <strong>
                                        Pending
                                    </strong>
                                </p>

                                <p>
                                    Risk Level:{" "}
                                    <strong>
                                        Pending
                                    </strong>
                                </p>

                                <p>
                                    Issues:{" "}
                                    <strong>
                                        Pending
                                    </strong>
                                </p>
                            </article>
                        ))}
                    </section>
                )}

            {!loading &&
                !error &&
                releaseRisk.length > 0 && (
                    <section>
                        <h2>Release Risk Analysis</h2>

                        {releaseRisk.map((release) => {
                            const score =
                                release.riskScore ??
                                release.score;

                            const level =
                                release.riskLevel ??
                                getRiskLevel(score);

                            return (
                                <article
                                    key={
                                        release.id ??
                                        release.name
                                    }
                                >
                                    <h3>
                                        {release.name}
                                    </h3>

                                    <p>
                                        Risk Score:{" "}
                                        <strong>
                                            {typeof score ===
                                            "number"
                                                ? `${score}/100`
: "Pending"}
</strong>
</p>

<p>
    Risk Level:{" "}
    <strong>
        {level}
    </strong>
</p>

<p>
    Issues:{" "}
    <strong>
        {release.issueCount ??
            "Pending"}
    </strong>
</p>
</article>
);
})}
</section>
)}
</div>
);
}

export default ReleaseRiskPage;