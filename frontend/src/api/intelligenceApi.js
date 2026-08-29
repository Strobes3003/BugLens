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

const intelligenceApi = {
    getImpactScore: (issueId) =>
        request(
            `/intelligence/issues/${issueId}/impact-score`
        ),

    getFixNext: (projectId) =>
        request(
            `/intelligence/projects/${projectId}/fix-next`
        ),

    getComponentHealth: (componentId) =>
        request(
            `/intelligence/components/${componentId}/health`
        ),

    getReleaseRisk: (releaseId) =>
        request(
            `/intelligence/releases/${releaseId}/risk`
        ),

    getDependencyAnalysis: (issueId) =>
        request(
            `/intelligence/issues/${issueId}/dependencies`
        ),
};

export default intelligenceApi;