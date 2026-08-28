function WorkflowControls({
                              status,
                              transitions = [],
                              onTransition,
                          }) {
    return (
        <section>
            <h2>Workflow</h2>

            <p>
                Current status:{" "}
                <strong>{status}</strong>
            </p>

            {transitions.length === 0 ? (
                <p>No available transitions.</p>
            ) : (
                transitions.map((transition) => (
                    <button
                        key={transition.id}
                        type="button"
                        onClick={() =>
                            onTransition?.(
                                transition
                            )
                        }
                    >
                        {transition.name}
                    </button>
                ))
            )}
        </section>
    );
}

export default WorkflowControls;