package com.buglens.activity.controller;

import com.buglens.activity.dto.response.ActivityLogResponse;
import com.buglens.activity.entity.ActivityAction;
import com.buglens.activity.exception.ActivityAccessDeniedException;
import com.buglens.activity.exception.ActivityIssueNotFoundException;
import com.buglens.activity.service.ActivityLogService;
import com.buglens.auth.entity.User;
import com.buglens.common.security.JwtAuthenticationFilter;
import com.buglens.common.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ActivityLogController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityLogService activityLogService;

    private final UserPrincipal principal =
            UserPrincipal.from(new User("Actor", "actor@buglens.test", "hash", null));

    @Test
    void returnsChronologicalHistory() throws Exception {
        when(activityLogService.listForIssue(eq(5L), any())).thenReturn(List.of(
                entry(1L, ActivityAction.ISSUE_CREATED, "created issue BL-13", "2026-08-29T10:00:00Z"),
                entry(2L, ActivityAction.STATUS_CHANGED, "changed status from OPEN to IN_PROGRESS",
                        "2026-08-29T10:05:00Z")
        ));

        mockMvc.perform(get("/api/issues/5/activity").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].actionType").value("ISSUE_CREATED"))
                .andExpect(jsonPath("$[0].description").value("created issue BL-13"))
                .andExpect(jsonPath("$[0].actor.name").value("Actor"))
                .andExpect(jsonPath("$[1].actionType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].description").value("changed status from OPEN to IN_PROGRESS"));
    }

    @Test
    void returnsEmptyListWhenNoActivity() throws Exception {
        when(activityLogService.listForIssue(eq(5L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/issues/5/activity").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void returns404WhenIssueMissing() throws Exception {
        when(activityLogService.listForIssue(eq(404L), any()))
                .thenThrow(new ActivityIssueNotFoundException(404L));

        mockMvc.perform(get("/api/issues/404/activity").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Issue not found"));
    }

    @Test
    void returns403ForNonMember() throws Exception {
        when(activityLogService.listForIssue(eq(5L), any()))
                .thenThrow(new ActivityAccessDeniedException());

        mockMvc.perform(get("/api/issues/5/activity").with(user(principal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    private ActivityLogResponse entry(Long id, ActivityAction action, String description, String at) {
        return new ActivityLogResponse(
                id,
                5L,
                new ActivityLogResponse.ActivityActor(7L, "Actor"),
                action,
                description,
                Instant.parse(at)
        );
    }
}
