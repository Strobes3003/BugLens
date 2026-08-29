package com.buglens.intelligence.controller;

import com.buglens.auth.entity.User;
import com.buglens.common.security.JwtAuthenticationFilter;
import com.buglens.common.security.UserPrincipal;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.intelligence.entity.ComponentHealth;
import com.buglens.intelligence.entity.IssueImpact;
import com.buglens.intelligence.entity.ReleaseRisk;
import com.buglens.intelligence.exception.IntelligenceAccessDeniedException;
import com.buglens.intelligence.exception.IntelligenceProjectNotFoundException;
import com.buglens.intelligence.exception.IntelligenceReleaseNotFoundException;
import com.buglens.intelligence.service.IntelligenceService;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = IntelligenceController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class IntelligenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntelligenceService intelligenceService;

    private final UserPrincipal principal =
            UserPrincipal.from(new User("Actor", "actor@buglens.test", "hash", null));

    @Test
    void fixNextReturnsRankedIssues() throws Exception {
        when(intelligenceService.getFixNext(eq(5L), anyInt(), any()))
                .thenReturn(List.of(impact("BL-3", 90), impact("BL-1", 70)));

        mockMvc.perform(get("/api/projects/5/fix-next").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].issueKey").value("BL-3"))
                .andExpect(jsonPath("$[0].impactScore").value(90))
                .andExpect(jsonPath("$[1].issueKey").value("BL-1"))
                .andExpect(jsonPath("$[1].impactScore").value(70))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void fixNextDefaultsToTenAndHonoursExplicitLimit() throws Exception {
        when(intelligenceService.getFixNext(eq(5L), anyInt(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/5/fix-next").with(user(principal)))
                .andExpect(status().isOk());
        verify(intelligenceService).getFixNext(eq(5L), eq(10), any());

        mockMvc.perform(get("/api/projects/5/fix-next?limit=3").with(user(principal)))
                .andExpect(status().isOk());
        verify(intelligenceService).getFixNext(eq(5L), eq(3), any());
    }

    @Test
    void fixNextReturns404ForUnknownProject() throws Exception {
        when(intelligenceService.getFixNext(eq(404L), anyInt(), any()))
                .thenThrow(new IntelligenceProjectNotFoundException(404L));

        mockMvc.perform(get("/api/projects/404/fix-next").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void componentHealthReturnsScore() throws Exception {
        when(intelligenceService.getComponentHealth(eq(10L), any()))
                .thenReturn(new ComponentHealth(component(), 45));

        mockMvc.perform(get("/api/components/10/health").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthScore").value(45))
                .andExpect(jsonPath("$.componentName").value("Auth"))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    void componentHealthReturns403ForNonMember() throws Exception {
        when(intelligenceService.getComponentHealth(eq(10L), any()))
                .thenThrow(new IntelligenceAccessDeniedException());

        mockMvc.perform(get("/api/components/10/health").with(user(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void releaseRiskReturnsScore() throws Exception {
        when(intelligenceService.getReleaseRisk(eq(20L), any()))
                .thenReturn(new ReleaseRisk(release(), 70));

        mockMvc.perform(get("/api/releases/20/risk").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(70))
                .andExpect(jsonPath("$.releaseName").value("v2.4"));
    }

    @Test
    void releaseRiskReturns404WhenMissing() throws Exception {
        when(intelligenceService.getReleaseRisk(eq(404L), any()))
                .thenThrow(new IntelligenceReleaseNotFoundException(404L));

        mockMvc.perform(get("/api/releases/404/risk").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Release not found"));
    }

    private IssueImpact impact(String key, int score) {
        Issue issue = new Issue(
                key, "Issue " + key, null, IssueStatus.OPEN,
                IssuePriority.HIGH, IssueSeverity.HIGH, component(), null, null, null
        );
        return new IssueImpact(issue, score);
    }

    private Component component() {
        Project project = new Project(null, "BugLens", "BL", null, ProjectStatus.ACTIVE);
        return new Component(project, "Auth", null, ComponentStatus.ACTIVE);
    }

    private Release release() {
        Project project = new Project(null, "BugLens", "BL", null, ProjectStatus.ACTIVE);
        return new Release(project, "v2.4", null, ReleaseStatus.ACTIVE, null);
    }
}
