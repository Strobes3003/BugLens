package com.buglens.dependency.controller;

import com.buglens.auth.entity.User;
import com.buglens.common.security.JwtAuthenticationFilter;
import com.buglens.common.security.UserPrincipal;
import com.buglens.dependency.dto.response.DependencyAnalysisResponse;
import com.buglens.dependency.dto.response.DependencyIssueSummary;
import com.buglens.dependency.exception.DependencyAccessDeniedException;
import com.buglens.dependency.exception.DependencyIssueNotFoundException;
import com.buglens.dependency.service.DependencyAnalysisService;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DependencyAnalysisController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class DependencyAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DependencyAnalysisService dependencyAnalysisService;

    private final UserPrincipal principal =
            UserPrincipal.from(new User("Actor", "actor@buglens.test", "hash", null));

    @Test
    void returnsFullAnalysisShape() throws Exception {
        when(dependencyAnalysisService.analyze(eq(1L), any())).thenReturn(
                new DependencyAnalysisResponse(
                        1L, "BL-1", 3, 1,
                        List.of(summary(9L, "BL-9")),
                        List.of(summary(2L, "BL-2"), summary(3L, "BL-3")),
                        false
                )
        );

        mockMvc.perform(get("/api/issues/1/dependency-analysis").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueId").value(1))
                .andExpect(jsonPath("$.issueKey").value("BL-1"))
                .andExpect(jsonPath("$.blastRadius").value(3))
                .andExpect(jsonPath("$.totalBlockers").value(1))
                .andExpect(jsonPath("$.directBlockers.length()").value(1))
                .andExpect(jsonPath("$.directBlockers[0].issueKey").value("BL-9"))
                .andExpect(jsonPath("$.directBlocked.length()").value(2))
                .andExpect(jsonPath("$.directBlocked[1].issueKey").value("BL-3"))
                .andExpect(jsonPath("$.hasBottleneck").value(false));
    }

    @Test
    void reportsBottleneckFlag() throws Exception {
        when(dependencyAnalysisService.analyze(eq(1L), any())).thenReturn(
                new DependencyAnalysisResponse(
                        1L, "BL-1", 5, 0,
                        List.of(),
                        List.of(summary(2L, "BL-2"), summary(3L, "BL-3"), summary(4L, "BL-4")),
                        true
                )
        );

        mockMvc.perform(get("/api/issues/1/dependency-analysis").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasBottleneck").value(true))
                .andExpect(jsonPath("$.blastRadius").value(5));
    }

    @Test
    void returnsZeroesForIsolatedIssue() throws Exception {
        when(dependencyAnalysisService.analyze(eq(1L), any())).thenReturn(
                new DependencyAnalysisResponse(1L, "BL-1", 0, 0, List.of(), List.of(), false)
        );

        mockMvc.perform(get("/api/issues/1/dependency-analysis").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blastRadius").value(0))
                .andExpect(jsonPath("$.directBlocked.length()").value(0));
    }

    @Test
    void returns404WhenIssueMissing() throws Exception {
        when(dependencyAnalysisService.analyze(eq(404L), any()))
                .thenThrow(new DependencyIssueNotFoundException(404L));

        mockMvc.perform(get("/api/issues/404/dependency-analysis").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Issue not found"));
    }

    @Test
    void returns403ForNonMember() throws Exception {
        when(dependencyAnalysisService.analyze(eq(1L), any()))
                .thenThrow(new DependencyAccessDeniedException());

        mockMvc.perform(get("/api/issues/1/dependency-analysis").with(user(principal)))
                .andExpect(status().isForbidden());
    }

    private DependencyIssueSummary summary(Long id, String key) {
        return new DependencyIssueSummary(
                id, key, "Issue " + id, IssueStatus.OPEN, IssuePriority.HIGH, IssueSeverity.HIGH
        );
    }
}
