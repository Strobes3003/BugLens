function IntelligenceCard({
                              title,
                              value,
                              description,
                              status,
                          }) {
    return (
        <article>
            <h3>{title}</h3>

            <strong>{value}</strong>

            {status && (
                <p>Status: {status}</p>
            )}

            {description && (
                <p>{description}</p>
            )}
        </article>
    );
}

export default IntelligenceCard;