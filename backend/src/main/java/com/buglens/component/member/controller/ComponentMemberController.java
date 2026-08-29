package com.buglens.component.member.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.component.member.dto.request.AddComponentMemberRequest;
import com.buglens.component.member.dto.request.UpdateComponentMemberRequest;
import com.buglens.component.member.dto.response.ComponentMemberResponse;
import com.buglens.component.member.service.ComponentMemberService;
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
@RequestMapping("/api/components/{componentId}/members")
public class ComponentMemberController {

    private final ComponentMemberService componentMemberService;

    public ComponentMemberController(ComponentMemberService componentMemberService) {
        this.componentMemberService = componentMemberService;
    }

    @GetMapping
    public List<ComponentMemberResponse> list(
            @PathVariable Long componentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return componentMemberService.list(componentId, principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComponentMemberResponse add(
            @PathVariable Long componentId,
            @Valid @RequestBody AddComponentMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return componentMemberService.add(componentId, request, principal.getId());
    }

    @PatchMapping("/{userId}")
    public ComponentMemberResponse update(
            @PathVariable Long componentId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateComponentMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return componentMemberService.update(componentId, userId, request, principal.getId());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @PathVariable Long componentId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        componentMemberService.remove(componentId, userId, principal.getId());
    }
}
