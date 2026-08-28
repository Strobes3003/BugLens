const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL ||
    "http://localhost:8080/api";

async function request(endpoint, options = {}) {
    const response = await fetch(
        `${API_BASE_URL}${endpoint}`,
        {
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {}),
            },
            ...options,
        }
    );

    if (!response.ok) {
        throw new Error(
            (await response.text()) ||
            `Request failed: ${response.status}`
        );
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

const workflowApi = {
    getStatuses: (projectId) =>
        request(
            `/projects/${projectId}/workflow/statuses`
        ),

    getTransitions: (issueId) =>
        request(
            `/issues/${issueId}/workflow/transitions`
        ),

    transitionIssue: (issueId, transitionId) =>
        request(
            `/issues/${issueId}/workflow/transitions/${transitionId}`,
            {
                method: "POST",
            }
        ),
};

export default workflowApi;