package com.buglens.component.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.component.dto.request.CreateComponentRequest;
import com.buglens.component.dto.request.UpdateComponentRequest;
import com.buglens.component.dto.response.ComponentResponse;
import com.buglens.component.service.ComponentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/components")
public class ComponentController {

    private final ComponentService componentService;

    public ComponentController(ComponentService componentService) {
        this.componentService = componentService;
    }

    @GetMapping
    public List<ComponentResponse> list(
            @RequestParam Long projectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return componentService.listForProject(projectId, principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComponentResponse create(
            @Valid @RequestBody CreateComponentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return componentService.create(request, principal.getId());
    }

    @GetMapping("/{componentId}")
    public ComponentResponse getById(
            @PathVariable Long componentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return componentService.getById(componentId, principal.getId());
    }

    @PatchMapping("/{componentId}")
    public ComponentResponse update(
            @PathVariable Long componentId,
            @Valid @RequestBody UpdateComponentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return componentService.update(componentId, request, principal.getId());
    }
}
