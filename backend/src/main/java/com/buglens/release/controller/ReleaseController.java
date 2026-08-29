package com.buglens.release.controller;

import com.buglens.common.security.UserPrincipal;
import com.buglens.release.dto.request.CreateReleaseRequest;
import com.buglens.release.dto.request.UpdateReleaseRequest;
import com.buglens.release.dto.response.ReleaseResponse;
import com.buglens.release.service.ReleaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/releases")
public class ReleaseController {

    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @GetMapping
    public List<ReleaseResponse> list(
            @RequestParam Long projectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return releaseService.listForProject(projectId, principal.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReleaseResponse create(
            @Valid @RequestBody CreateReleaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return releaseService.create(request, principal.getId());
    }

    @GetMapping("/{releaseId}")
    public ReleaseResponse getById(
            @PathVariable Long releaseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return releaseService.getById(releaseId, principal.getId());
    }

    @PatchMapping("/{releaseId}")
    public ReleaseResponse update(
            @PathVariable Long releaseId,
            @Valid @RequestBody UpdateReleaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return releaseService.update(releaseId, request, principal.getId());
    }

    @DeleteMapping("/{releaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long releaseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        releaseService.delete(releaseId, principal.getId());
    }
}
