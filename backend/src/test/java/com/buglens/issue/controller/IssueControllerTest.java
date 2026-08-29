package com.buglens.issue.controller;

import com.buglens.common.security.JwtAuthenticationFilter;
import com.buglens.common.security.UserPrincipal;
import com.buglens.auth.entity.User;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.exception.IssueNotFoundException;
import com.buglens.issue.service.IssueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = IssueController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private IssueService issueService;

    private final UserPrincipal principal =
            UserPrincipal.from(new User("Reporter", "reporter@buglens.test", "hash", null));

    @Test
    void createReturns201AndMappedBody() throws Exception {
        when(issueService.create(any(), any())).thenReturn(sampleResponse());

        Map<String, Object> body = Map.of(
                "title", "Login fails after password reset",
                "description", "Steps attached.",
                "priority", "HIGH",
                "severity", "CRITICAL",
                "componentId", 10,
                "releaseId", 20
        );

        mockMvc.perform(post("/api/issues")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.issueKey").value("BL-13"))
                .andExpect(jsonPath("$.title").value("Login fails after password reset"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.projectKey").value("BL"))
                .andExpect(jsonPath("$.componentName").value("Authentication"))
                .andExpect(jsonPath("$.releaseName").value("v2.4"))
                .andExpect(jsonPath("$.reporterName").value("Reporter"));
    }

    @Test
    void createReturns400WhenTitleBlank() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "   ",
                "priority", "HIGH",
                "severity", "CRITICAL",
                "componentId", 10
        );

        mockMvc.perform(post("/api/issues")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createReturns400WhenComponentIdMissing() throws Exception {
        Map<String, Object> body = Map.of(
                "title", "Missing component",
                "priority", "HIGH",
                "severity", "CRITICAL"
        );

        mockMvc.perform(post("/api/issues")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.componentId").exists());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(issueService.getById(eq(404L), any())).thenThrow(new IssueNotFoundException(404L));

        mockMvc.perform(get("/api/issues/404").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Issue not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getByIdReturns200() throws Exception {
        when(issueService.getById(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/issues/1").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueKey").value("BL-13"));
    }

    @Test
    void getByKeyReturns200() throws Exception {
        when(issueService.getByKey(eq("BL-13"), any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/issues/key/BL-13").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueKey").value("BL-13"));
    }

    @Test
    void patchIgnoresStatusFieldInRequestBody() throws Exception {
        when(issueService.updateDetails(eq(1L), any(), any())).thenReturn(sampleResponse());

        String body = """
                {"title":"Renamed","status":"CLOSED"}
                """;

        mockMvc.perform(patch("/api/issues/1")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    private IssueResponse sampleResponse() {
        return new IssueResponse(
                1L,
                "BL-13",
                "Login fails after password reset",
                "Steps attached.",
                IssueStatus.OPEN,
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
