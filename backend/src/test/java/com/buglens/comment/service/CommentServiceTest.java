package com.buglens.comment.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.comment.dto.request.CreateCommentRequest;
import com.buglens.comment.dto.request.UpdateCommentRequest;
import com.buglens.comment.dto.response.CommentResponse;
import com.buglens.comment.entity.Comment;
import com.buglens.comment.exception.CommentAccessDeniedException;
import com.buglens.comment.exception.CommentIssueNotFoundException;
import com.buglens.comment.exception.CommentNotFoundException;
import com.buglens.comment.exception.InvalidCommentBodyException;
import com.buglens.comment.repository.CommentRepository;
import com.buglens.component.entity.Component;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceTest {

    private static final Long ISSUE_ID = 5L;
    private static final Long COMMENT_ID = 9L;
    private static final Long AUTHOR_ID = 7L;
    private static final Long OTHER_USER_ID = 8L;
    private static final Long MANAGER_ID = 11L;
    private static final Long WORKSPACE_ID = 3L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private Workspace workspace;

    @Mock
    private Project project;

    @Mock
    private Component component;

    @Mock
    private Issue issue;

    private CommentService commentService;
    private User author;
    private User otherUser;
    private User manager;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository, issueRepository, userRepository, workspaceAccessService
        );

        author = userWithId(AUTHOR_ID, "Author", "author@buglens.test");
        otherUser = userWithId(OTHER_USER_ID, "Other", "other@buglens.test");
        manager = userWithId(MANAGER_ID, "Manager", "manager@buglens.test");

        lenient().when(workspace.getId()).thenReturn(WORKSPACE_ID);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(component.getProject()).thenReturn(project);
        lenient().when(issue.getId()).thenReturn(ISSUE_ID);
        lenient().when(issue.getComponent()).thenReturn(component);
        lenient().when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));
        lenient().when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));

        // Only MANAGER_ID passes the manager check.
        lenient().doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMemberManager(WORKSPACE_ID, AUTHOR_ID);
        lenient().doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMemberManager(WORKSPACE_ID, OTHER_USER_ID);
    }

    @Test
    void addCommentAssociatesIssueAndAuthor() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        CommentResponse response = commentService.addComment(
                ISSUE_ID, new CreateCommentRequest("  Looks like a session bug.  "), AUTHOR_ID
        );

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        Comment saved = captor.getValue();

        assertEquals(issue, saved.getIssue());
        assertEquals(author, saved.getAuthor());
        assertEquals("Looks like a session bug.", saved.getBody());
        assertFalse(saved.isEdited());
        assertEquals(ISSUE_ID, response.issueId());
        assertEquals(AUTHOR_ID, response.author().id());
        assertFalse(response.isEdited());
    }

    @Test
    void addCommentRejectsBlankBody() {
        assertThrows(
                InvalidCommentBodyException.class,
                () -> commentService.addComment(ISSUE_ID, new CreateCommentRequest("   "), AUTHOR_ID)
        );
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addCommentRejectsMissingIssue() {
        when(issueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                CommentIssueNotFoundException.class,
                () -> commentService.addComment(404L, new CreateCommentRequest("Body"), AUTHOR_ID)
        );
    }

    @Test
    void addCommentRejectsNonMember() {
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMember(WORKSPACE_ID, AUTHOR_ID);

        assertThrows(
                CommentAccessDeniedException.class,
                () -> commentService.addComment(ISSUE_ID, new CreateCommentRequest("Body"), AUTHOR_ID)
        );
    }

    @Test
    void listReturnsCommentsChronologically() {
        when(commentRepository.findAllByIssueIdOrderByCreatedAtAsc(ISSUE_ID))
                .thenReturn(List.of(comment(author, "first"), comment(author, "second")));

        List<CommentResponse> responses = commentService.listForIssue(ISSUE_ID, AUTHOR_ID);

        assertEquals(2, responses.size());
        assertEquals("first", responses.get(0).body());
        assertEquals("second", responses.get(1).body());
    }

    @Test
    void authorCanEditCommentAndIsEditedBecomesTrue() {
        Comment existing = comment(author, "original");
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existing));

        CommentResponse response = commentService.updateComment(
                COMMENT_ID, new UpdateCommentRequest("  corrected body  "), AUTHOR_ID
        );

        assertEquals("corrected body", existing.getBody());
        assertTrue(existing.isEdited());
        assertTrue(response.isEdited());
        assertEquals("corrected body", response.body());
    }

    @Test
    void nonAuthorCannotEditComment() {
        Comment existing = comment(author, "original");
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existing));

        assertThrows(
                CommentAccessDeniedException.class,
                () -> commentService.updateComment(COMMENT_ID, new UpdateCommentRequest("hijack"), OTHER_USER_ID)
        );
        assertEquals("original", existing.getBody());
        assertFalse(existing.isEdited());
    }

    @Test
    void managerCannotEditAnotherUsersComment() {
        Comment existing = comment(author, "original");
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existing));

        assertThrows(
                CommentAccessDeniedException.class,
                () -> commentService.updateComment(COMMENT_ID, new UpdateCommentRequest("edited"), MANAGER_ID)
        );
        assertEquals("original", existing.getBody());
    }

    @Test
    void authorCanDeleteOwnComment() {
        Comment existing = comment(author, "mine");
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existing));

        commentService.deleteComment(COMMENT_ID, AUTHOR_ID);

        verify(commentRepository).delete(existing);
    }

    @Test
    void managerCanDeleteAnotherUsersComment() {
        Comment existing = comment(author, "not mine");
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existing));

        commentService.deleteComment(COMMENT_ID, MANAGER_ID);

        verify(commentRepository).delete(existing);
    }

    @Test
    void nonAuthorNonManagerCannotDeleteComment() {
        Comment existing = comment(author, "not yours");
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existing));

        assertThrows(
                CommentAccessDeniedException.class,
                () -> commentService.deleteComment(COMMENT_ID, OTHER_USER_ID)
        );
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsMissingComment() {
        when(commentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(404L, AUTHOR_ID));
    }

    private Comment comment(User commentAuthor, String body) {
        return new Comment(issue, commentAuthor, body);
    }

    private User userWithId(Long id, String name, String email) {
        User user = new User(name, email, "hash", null);
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return user;
    }
}
