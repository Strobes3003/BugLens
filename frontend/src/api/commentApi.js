import { API_BASE_URL } from '../utils/constants';

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

const commentApi = {
    getComments: (issueId) =>
        request(`/issues/${issueId}/comments`),

    createComment: (issueId, data) =>
        request(`/issues/${issueId}/comments`, {
            method: "POST",
            body: JSON.stringify(data),
        }),

    updateComment: (commentId, data) =>
        request(`/comments/${commentId}`, {
            method: "PUT",
            body: JSON.stringify(data),
        }),

    deleteComment: (commentId) =>
        request(`/comments/${commentId}`, {
            method: "DELETE",
        }),
};

export default commentApi;