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

const dependencyApi = {
    getDependencies: (issueId) =>
        request(
            `/issues/${issueId}/dependencies`
        ),

    addDependency: (
        issueId,
        dependencyIssueId,
        type = "BLOCKS"
    ) =>
        request(
            `/issues/${issueId}/dependencies`,
            {
                method: "POST",
                body: JSON.stringify({
                    dependencyIssueId,
                    type,
                }),
            }
        ),

    removeDependency: (dependencyId) =>
        request(
            `/dependencies/${dependencyId}`,
            {
                method: "DELETE",
            }
        ),

    getGraph: (projectId) =>
        request(
            `/projects/${projectId}/dependencies/graph`
        ),

    getImpact: (issueId) =>
        request(
            `/issues/${issueId}/dependencies/impact`
        ),
};

export default dependencyApi;