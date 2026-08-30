import { useState } from "react";
import { Badge, Button } from "../../components/ui";
import "../issues/issue-detail.css";

function DependencyList({
                            dependencies = [],
                            availableIssues = [],
                            currentIssueId,
                            onRemove,
                            onAdd,
                        }) {
    const [showAddForm, setShowAddForm] = useState(false);
    const [selectedIssueId, setSelectedIssueId] = useState("");
    const [relationship, setRelationship] = useState("BLOCKS");
    const [isAdding, setIsAdding] = useState(false);

    const otherIssues = availableIssues.filter(
        (issue) => String(issue.id) !== String(currentIssueId)
    );

    const handleAdd = async () => {
        if (!selectedIssueId || !onAdd) return;

        setIsAdding(true);

        try {
            await onAdd(
                relationship === "BLOCKS"
                    ? {
                        blockerIssueId: currentIssueId,
                        blockedIssueId: Number(selectedIssueId),
                    }
                    : {
                        blockerIssueId: Number(selectedIssueId),
                        blockedIssueId: currentIssueId,
                    }
            );

            setSelectedIssueId("");
            setShowAddForm(false);
        } finally {
            setIsAdding(false);
        }
    };

    return (
        <section className="idp-card">
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: "12px",
                }}
            >
                <h2 className="idp-section-title">Dependencies</h2>

                {otherIssues.length > 0 && (
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => setShowAddForm((value) => !value)}
                    >
                        {showAddForm ? "Cancel" : "Add Dependency"}
                    </Button>
                )}
            </div>

            {showAddForm && (
                <div
                    style={{
                        display: "grid",
                        gap: "10px",
                        marginTop: "12px",
                        marginBottom: "16px",
                    }}
                >
                    <label>
                        Issue
                        <select
                            value={selectedIssueId}
                            onChange={(event) =>
                                setSelectedIssueId(event.target.value)
                            }
                            style={{
                                display: "block",
                                width: "100%",
                                marginTop: "6px",
                                padding: "8px",
                            }}
                        >
                            <option value="">Select an issue</option>

                            {otherIssues.map((issue) => (
                                <option key={issue.id} value={issue.id}>
                                    {issue.issueKey} — {issue.title}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label>
                        Relationship
                        <select
                            value={relationship}
                            onChange={(event) =>
                                setRelationship(event.target.value)
                            }
                            style={{
                                display: "block",
                                width: "100%",
                                marginTop: "6px",
                                padding: "8px",
                            }}
                        >
                            <option value="BLOCKS">
                                This issue blocks the selected issue
                            </option>
                            <option value="BLOCKED_BY">
                                Selected issue blocks this issue
                            </option>
                        </select>
                    </label>

                    <div>
                        <Button
                            type="button"
                            size="sm"
                            disabled={!selectedIssueId || isAdding}
                            onClick={handleAdd}
                        >
                            {isAdding ? "Adding..." : "Add Dependency"}
                        </Button>
                    </div>
                </div>
            )}

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
                                    onClick={() =>
                                        onRemove?.(dependency.id)
                                    }
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