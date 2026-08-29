function DependencyList({
                            dependencies = [],
                            onRemove,
                        }) {
    return (
        <section>
            <h2>Dependencies</h2>

            {dependencies.length === 0 ? (
                <p>No dependencies.</p>
            ) : (
                dependencies.map((dependency) => (
                    <article key={dependency.id}>
                        <strong>
                            {dependency.issueKey ??
                                dependency.targetIssueKey}
                        </strong>

                        <p>
                            {dependency.type ??
                                "DEPENDS_ON"}
                        </p>

                        <button
                            type="button"
                            onClick={() =>
                                onRemove?.(
                                    dependency.id
                                )
                            }
                        >
                            Remove
                        </button>
                    </article>
                ))
            )}
        </section>
    );
}

export default DependencyList;