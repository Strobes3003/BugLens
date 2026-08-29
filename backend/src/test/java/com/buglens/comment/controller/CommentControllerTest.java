package com.buglens.comment.controller;

import com.buglens.auth.entity.User;
import com.buglens.comment.dto.response.CommentResponse;
import com.buglens.comment.exception.CommentAccessDeniedException;
import com.buglens.comment.exception.CommentIssueNotFoundException;
import com.buglens.comment.exception.CommentNotFoundException;
import com.buglens.comment.service.CommentService;
import com.buglens.common.security.JwtAuthenticationFilter;
import com.buglens.common.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {CommentController.class, IssueCommentController.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    private final UserPrincipal principal =
            UserPrincipal.from(new User("Author", "author@buglens.test", "hash", null));

    @Test
    void createReturns201() throws Exception {
        when(commentService.addComment(eq(5L), any(), any())).thenReturn(sampleComment(false));

        mockMvc.perform(post("/api/issues/5/comments")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Looks like a session bug.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.issueId").value(5))
                .andExpect(jsonPath("$.body").value("Looks like a session bug."))
                .andExpect(jsonPath("$.isEdited").value(false))
                .andExpect(jsonPath("$.author.id").value(7))
                .andExpect(jsonPath("$.author.name").value("Author"));
    }

    @Test
    void createReturns400OnBlankBody() throws Exception {
        mockMvc.perform(post("/api/issues/5/comments")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.body").exists());
    }

    @Test
    void createReturns404WhenIssueMissing() throws Exception {
        when(commentService.addComment(eq(404L), any(), any()))
                .thenThrow(new CommentIssueNotFoundException(404L));

        mockMvc.perform(post("/api/issues/404/comments")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Body\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Issue not found"));
    }

    @Test
    void listReturns200WithComments() throws Exception {
        when(commentService.listForIssue(eq(5L), any()))
                .thenReturn(List.of(sampleComment(false), sampleComment(true)));

        mockMvc.perform(get("/api/issues/5/comments").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].isEdited").value(true));
    }

    @Test
    void updateReturns200AndFlagsEdited() throws Exception {
        when(commentService.updateComment(eq(9L), any(), any())).thenReturn(sampleComment(true));

        mockMvc.perform(patch("/api/comments/9")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"corrected\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEdited").value(true));
    }

    @Test
    void updateReturns403ForNonAuthor() throws Exception {
        when(commentService.updateComment(eq(9L), any(), any()))
                .thenThrow(new CommentAccessDeniedException("Only the comment author can edit this comment"));

        mockMvc.perform(patch("/api/comments/9")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"hijack\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void updateReturns400OnBlankBody() throws Exception {
        mockMvc.perform(patch("/api/comments/9")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.body").exists());
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(commentService).deleteComment(eq(9L), any());

        mockMvc.perform(delete("/api/comments/9").with(user(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(eq(9L), any());
    }

    @Test
    void deleteReturns403WhenNotAuthorOrManager() throws Exception {
        doThrow(new CommentAccessDeniedException("Only the comment author or a workspace manager can delete this comment"))
                .when(commentService).deleteComment(eq(9L), any());

        mockMvc.perform(delete("/api/comments/9").with(user(principal)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReturns404WhenCommentMissing() throws Exception {
        doThrow(new CommentNotFoundException(404L))
                .when(commentService).deleteComment(eq(404L), any());

        mockMvc.perform(delete("/api/comments/404").with(user(principal)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Comment not found"));
    }

    private CommentResponse sampleComment(boolean edited) {
        return new CommentResponse(
                9L,
                5L,
                new CommentResponse.CommentAuthor(7L, "Author", "author@buglens.test"),
                "Looks like a session bug.",
                edited,
                Instant.parse("2026-08-29T10:00:00Z"),
                Instant.parse("2026-08-29T10:05:00Z")
        );
    }
}
