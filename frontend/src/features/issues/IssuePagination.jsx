function IssuePagination({
                             currentPage,
                             totalPages,
                             totalIssues,
                             issuesPerPage,
                             onPageChange,
                         }) {
    const start =
        totalIssues === 0
            ? 0
            : (currentPage - 1) * issuesPerPage + 1;

    const end = Math.min(
        currentPage * issuesPerPage,
        totalIssues
    );

    const pages = [];

    for (let page = 1; page <= totalPages; page += 1) {
        pages.push(page);
    }

    return (
        <div>
            <span>
                Showing {start}-{end} of {totalIssues} issues
            </span>

            <div>
                <button
                    type="button"
                    disabled={currentPage === 1}
                    onClick={() =>
                        onPageChange(currentPage - 1)
                    }
                >
                    Previous
                </button>

                {pages.map((page) => (
                    <button
                        key={page}
                        type="button"
                        disabled={page === currentPage}
                        onClick={() => onPageChange(page)}
                    >
                        {page}
                    </button>
                ))}

                <button
                    type="button"
                    disabled={currentPage === totalPages}
                    onClick={() =>
                        onPageChange(currentPage + 1)
                    }
                >
                    Next
                </button>
            </div>
        </div>
    );
}

export default IssuePagination;