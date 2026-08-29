package com.buglens.workspace.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.workspace.dto.request.AddWorkspaceMemberRequest;
import com.buglens.workspace.dto.request.CreateWorkspaceRequest;
import com.buglens.workspace.dto.request.UpdateWorkspaceRequest;
import com.buglens.workspace.dto.request.UpdateWorkspaceMemberRequest;
import com.buglens.workspace.dto.response.WorkspaceMemberResponse;
import com.buglens.workspace.dto.response.WorkspaceResponse;
import com.buglens.workspace.dto.response.WorkspaceSummaryResponse;
import com.buglens.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public List<WorkspaceSummaryResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return workspaceService.listForUser(principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workspaceService.create(request, principal.getId());
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getById(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workspaceService.getById(workspaceId, principal.getId());
    }

    @PatchMapping("/{workspaceId}")
    public WorkspaceResponse update(
            @PathVariable Long workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workspaceService.update(workspaceId, request, principal.getId());
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        workspaceService.delete(workspaceId, principal.getId());
    }

    @PostMapping("/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceMemberResponse addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workspaceService.addMember(workspaceId, request, principal.getId());
    }

    @PatchMapping("/{workspaceId}/members/{userId}")
    public WorkspaceMemberResponse updateMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateWorkspaceMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return workspaceService.updateMember(workspaceId, userId, request, principal.getId());
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long workspaceId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        workspaceService.removeMember(workspaceId, userId, principal.getId());
    }
}
