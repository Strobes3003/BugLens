import { useState } from "react";
import { Button } from "../../components/ui";

function CommentSection({
    comments = [],
    onAddComment,
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

                        <div className="idp-comment-actions">
                            <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() => onDeleteComment?.(comment.id)}
                            >
                                Delete
                            </Button>
                        </div>
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

                <Button type="submit" variant="primary">
                    Add Comment
                </Button>
            </form>
        </section>
    );
}

export default CommentSection;
