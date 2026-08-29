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
        const message =
            await response.text();

        throw new Error(
            message || `Request failed: ${response.status}`
        );
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

export const issueApi = {
    getIssues: (projectId, params = {}) => {
        const query = new URLSearchParams(params);

        return request(
            `/projects/${projectId}/issues?${query.toString()}`
        );
    },

    getIssue: (issueId) => {
        return request(`/issues/${issueId}`);
    },

    createIssue: (issue) => {
        return request("/issues", {
            method: "POST",
            body: JSON.stringify(issue),
        });
    },

    updateIssue: (issueId, issue) => {
        return request(`/issues/${issueId}`, {
            method: "PUT",
            body: JSON.stringify(issue),
        });
    },

    deleteIssue: (issueId) => {
        return request(`/issues/${issueId}`, {
            method: "DELETE",
        });
    },

    assignIssue: (issueId, assigneeId) => {
        return request(
            `/issues/${issueId}/assignee`,
            {
                method: "PATCH",
                body: JSON.stringify({
                    assigneeId,
                }),
            }
        );
    },
};

export default issueApi;