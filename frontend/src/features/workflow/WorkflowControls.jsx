import { Badge, Button } from "../../components/ui";
import { humanize, statusVariant } from "../issues/issueBadges";
import "../issues/issue-detail.css";

function WorkflowControls({ status, transitions = [], onTransition }) {
    return (
        <section className="idp-card">
            <h2 className="idp-section-title">Workflow</h2>

            <div className="idp-current-status">
                <span className="idp-label">Current status</span>
                <Badge variant={statusVariant(status)}>
                    {humanize(status)}
                </Badge>
            </div>

            {transitions.length === 0 ? (
                <p className="idp-empty">
                    No transitions are available from this status.
                </p>
            ) : (
                <div className="idp-actions">
                    {transitions.map((transition) => (
                        <Button
                            key={transition.id}
                            type="button"
                            variant="secondary"
                            size="sm"
                            onClick={() => onTransition?.(transition)}
                        >
                            {transition.name}
                        </Button>
                    ))}
                </div>
            )}
        </section>
    );
}

export default WorkflowControls;
