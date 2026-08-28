import { useState } from "react";

function CommentSection({
                            comments = [],
                            onAddComment,
                            onEditComment,
                            onDeleteComment,
                        }) {
    const [text, setText] = useState("");

    const handleSubmit = (event) => {
        event.preventDefault();

        if (!text.trim()) {
            return;
        }

        onAddComment?.({
            content: text.trim(),
            visibility: "PUBLIC",
        });

        setText("");
    };

    return (
        <section>
            <h2>Comments</h2>

            {comments.length === 0 ? (
                <p>No comments yet.</p>
            ) : (
                comments.map((comment) => (
                    <article key={comment.id}>
                        <strong>
                            {comment.author?.name ??
                                comment.author ??
                                "User"}
                        </strong>

                        <p>
                            {comment.content ??
                                comment.text}
                        </p>

                        <button
                            type="button"
                            onClick={() =>
                                onEditComment?.(
                                    comment
                                )
                            }
                        >
                            Edit
                        </button>

                        <button
                            type="button"
                            onClick={() =>
                                onDeleteComment?.(
                                    comment.id
                                )
                            }
                        >
                            Delete
                        </button>
                    </article>
                ))
            )}

            <form onSubmit={handleSubmit}>
                <textarea
                    value={text}
                    onChange={(event) =>
                        setText(event.target.value)
                    }
                    placeholder="Write a comment..."
                />

                <button type="submit">
                    Add Comment
                </button>
            </form>
        </section>
    );
}

export default CommentSection;