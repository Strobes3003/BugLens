function ActivityTimeline({ activities = [] }) {
    if (activities.length === 0) {
        return (
            <div>
                <h3>Activity</h3>
                <p>No activity recorded yet.</p>
            </div>
        );
    }

    return (
        <section>
            <h2>Activity</h2>

            {activities.map((activity) => (
                <article key={activity.id}>
                    <strong>
                        {activity.actor ?? "System"}
                    </strong>

                    <p>
                        {activity.description}
                    </p>

                    {activity.createdAt && (
                        <small>
                            {activity.createdAt}
                        </small>
                    )}
                </article>
            ))}
        </section>
    );
}

export default ActivityTimeline;