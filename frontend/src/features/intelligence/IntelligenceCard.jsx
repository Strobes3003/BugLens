import "../issues/issue-detail.css";

/**
 * A single headline number. Deliberately not a chart: one value with no series and
 * no time axis reads faster as a stat tile, and the meaning never rests on color.
 */
function IntelligenceCard({ title, value, description, status }) {
    return (
        <article className="idp-stat">
            <h3 className="idp-label">{title}</h3>

            <p className="idp-stat-value">{value}</p>

            {status && <p className="idp-stat-desc">Status: {status}</p>}

            {description && <p className="idp-stat-desc">{description}</p>}
        </article>
    );
}

export default IntelligenceCard;
