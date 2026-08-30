import { Badge, Button } from "../../components/ui";
import "../issues/issue-detail.css";

function DependencyList({ dependencies = [], onRemove }) {
    return (
        <section className="idp-card">
            <h2 className="idp-section-title">Dependencies</h2>

            {dependencies.length === 0 ? (
                <p className="idp-empty">
                    This issue neither blocks nor waits on anything.
                </p>
            ) : (
                <div className="idp-dep-list">
                    {dependencies.map((dependency) => (
                        <article key={dependency.id} className="idp-dep-row">
                            <span className="idp-dep-key">
                                {dependency.issueKey ??
                                    dependency.targetIssueKey}
                            </span>

                            <Badge variant="default">
                                {dependency.type ?? "DEPENDS ON"}
                            </Badge>

                            <span className="idp-dep-actions">
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => onRemove?.(dependency.id)}
                                >
                                    Remove
                                </Button>
                            </span>
                        </article>
                    ))}
                </div>
            )}
        </section>
    );
}

export default DependencyList;
