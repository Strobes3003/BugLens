package com.buglens.dependency.controller;

import com.buglens.auth.entity.User;
import com.buglens.common.security.JwtAuthenticationFilter;
import com.buglens.common.security.UserPrincipal;
import com.buglens.dependency.dto.response.DependencyIssueSummary;
import com.buglens.dependency.dto.response.DependencyResponse;
import com.buglens.dependency.dto.response.IssueDependenciesResponse;
import com.buglens.dependency.exception.CycleDetectedException;
import com.buglens.dependency.exception.DependencyNotFoundException;
import com.buglens.dependency.exception.DuplicateDependencyException;
import com.buglens.dependency.exception.SelfDependencyException;
import com.buglens.dependency.service.DependencyService;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DependencyController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class DependencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DependencyService dependencyService;

    private final UserPrincipal principal =
            UserPrincipal.from(new User("Actor", "actor@buglens.test", "hash", null));

    @Test
    void createReturns201() throws Exception {
        when(dependencyService.addDependency(eq(1L), eq(2L), any())).thenReturn(
                new DependencyResponse(10L, summary(1L, "BL-1"), summary(2L, "BL-2"),
                        Instant.parse("2026-08-29T10:00:00Z"))
        );

        mockMvc.perform(post("/api/issues/1/dependencies")
                        .with(user(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedIssueId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.blockingIssue.issueKey").value("BL-1"))
                .andExpect(jsonPath("$.blockedIssue.issueKey").value("BL-2"));
    }

    @Test
    void createReturns422WithCyclePathOnCycle() throws Exception {
        when(dependencyService.addDependency(eq(3L), eq(1L), any()))
                .thenThrow(new CycleDetectedException(3L, 1L, List.of(1L, 2L, 3L)));

        mockMvc.perform(post("/api/issues/3/dependencies")
                        .with(user(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedIssueId\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Cycle detected"))
                .andExpect(jsonPath("$.cyclePath[0]").value(1))
                .andExpect(jsonPath("$.cyclePath[2]").value(3));
    }

    @Test
    void createReturns400OnSelfDependency() throws Exception {
        when(dependencyService.addDependency(eq(1L), eq(1L), any()))
                .thenThrow(new SelfDependencyException(1L));

        mockMvc.perform(post("/api/issues/1/dependencies")
                        .with(user(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedIssueId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid dependency"));
    }

    @Test
    void createReturns409OnDuplicate() throws Exception {
        when(dependencyService.addDependency(eq(1L), eq(2L), any()))
                .thenThrow(new DuplicateDependencyException(1L, 2L));

        mockMvc.perform(post("/api/issues/1/dependencies")
                        .with(user(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedIssueId\":2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createReturns400WhenBlockedIssueIdMissing() throws Exception {
        mockMvc.perform(post("/api/issues/1/dependencies")
                        .with(user(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.blockedIssueId").exists());
    }

    @Test
    void listReturnsBothDirections() throws Exception {
        when(dependencyService.getDependencies(eq(2L), any())).thenReturn(
                new IssueDependenciesResponse(2L, "BL-2",
                        List.of(summary(1L, "BL-1")),
                        List.of(summary(3L, "BL-3")))
        );

        mockMvc.perform(get("/api/issues/2/dependencies").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueKey").value("BL-2"))
                .andExpect(jsonPath("$.blockedBy[0].issueKey").value("BL-1"))
                .andExpect(jsonPath("$.blocking[0].issueKey").value("BL-3"));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(dependencyService).removeDependency(eq(1L), eq(2L), any());

        mockMvc.perform(delete("/api/issues/1/dependencies/2").with(user(principal)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenEdgeMissing() throws Exception {
        doThrow(new DependencyNotFoundException(1L, 2L))
                .when(dependencyService).removeDependency(eq(1L), eq(2L), any());

        mockMvc.perform(delete("/api/issues/1/dependencies/2").with(user(principal)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Dependency not found"));
    }

    private DependencyIssueSummary summary(Long id, String key) {
        return new DependencyIssueSummary(
                id, key, "Issue " + id, IssueStatus.OPEN, IssuePriority.HIGH, IssueSeverity.HIGH
        );
    }
}
