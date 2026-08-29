package com.buglens.workflow.controller;

import com.buglens.auth.entity.User;
import com.buglens.common.security.JwtAuthenticationFilter;
import com.buglens.common.security.UserPrincipal;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.exception.IssueNotFoundException;
import com.buglens.workflow.domain.IssueWorkflow;
import com.buglens.workflow.dto.response.AllowedTransitionsResponse;
import com.buglens.workflow.exception.InvalidStateTransitionException;
import com.buglens.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = WorkflowController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkflowService workflowService;

    private final UserPrincipal principal =
            UserPrincipal.from(new User("Reporter", "reporter@buglens.test", "hash", null));

    @Test
    void transitionReturns200OnLegalTarget() throws Exception {
        when(workflowService.transition(eq(1L), any(), any()))
                .thenReturn(responseWithStatus(IssueStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/issues/1/transitions")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueKey").value("BL-13"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void transitionReturns422OnIllegalTarget() throws Exception {
        when(workflowService.transition(eq(1L), any(), any())).thenThrow(
                new InvalidStateTransitionException(
                        IssueStatus.OPEN,
                        IssueStatus.RESOLVED,
                        List.of(IssueStatus.IN_PROGRESS)
                )
        );

        mockMvc.perform(post("/api/issues/1/transitions")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"RESOLVED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Invalid state transition"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.allowedTransitions[0]").value("IN_PROGRESS"));
    }

    @Test
    void transitionReturns400WhenTargetStatusMissing() throws Exception {
        mockMvc.perform(post("/api/issues/1/transitions")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"no target\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.targetStatus").exists());
    }

    @Test
    void transitionReturns400OnUnknownStatusValue() throws Exception {
        mockMvc.perform(post("/api/issues/1/transitions")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request"));
    }

    @Test
    void transitionReturns404WhenIssueMissing() throws Exception {
        when(workflowService.transition(eq(404L), any(), any()))
                .thenThrow(new IssueNotFoundException(404L));

        mockMvc.perform(post("/api/issues/404/transitions")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"IN_PROGRESS\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Issue not found"));
    }

    @ParameterizedTest(name = "GET transitions for {0}")
    @EnumSource(IssueStatus.class)
    void allowedTransitionsReturnsLegalOptionsForEachStatus(IssueStatus current) throws Exception {
        List<IssueStatus> allowed = IssueWorkflow.allowedFrom(current);
        when(workflowService.getAllowedTransitions(eq(1L), any()))
                .thenReturn(new AllowedTransitionsResponse(current, allowed));

        var result = mockMvc.perform(get("/api/issues/1/transitions").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value(current.name()))
                .andExpect(jsonPath("$.allowedTransitions.length()").value(allowed.size()));

        for (int index = 0; index < allowed.size(); index++) {
            result.andExpect(jsonPath("$.allowedTransitions[" + index + "]").value(allowed.get(index).name()));
        }
    }

    private IssueResponse responseWithStatus(IssueStatus status) {
        return new IssueResponse(
                1L,
                "BL-13",
                "Login fails after password reset",
                "Steps attached.",
                status,
                IssuePriority.HIGH,
                IssueSeverity.CRITICAL,
                100L,
                "BL",
                "BugLens",
                10L,
                "Authentication",
                20L,
                "v2.4",
                50L,
                "Reporter",
                null,
                null,
                Instant.parse("2026-08-29T10:00:00Z"),
                Instant.parse("2026-08-29T10:00:00Z")
        );
    }
}
