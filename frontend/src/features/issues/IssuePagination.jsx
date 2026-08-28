function IssuePagination({ totalIssues }) {
    return (
        <div>
            <span>Showing {totalIssues} issues</span>

            <div>
                <button type="button">Previous</button>
                <button type="button">1</button>
                <button type="button">2</button>
                <button type="button">Next</button>
            </div>
        </div>
    );
}

export default IssuePagination;