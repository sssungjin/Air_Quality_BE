package com.sungjin.airquailitymonitordemo.controller;

import com.sungjin.airquailitymonitordemo.dto.request.project.ProjectEditRequestDto;
import com.sungjin.airquailitymonitordemo.dto.request.project.ProjectRegistrationRequestDto;
import com.sungjin.airquailitymonitordemo.dto.response.project.ProjectListResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.project.ProjectResponseDto;
import com.sungjin.airquailitymonitordemo.service.ProjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.sungjin.airquailitymonitordemo.security.CustomUserPrincipal;

@RestController
@RequestMapping("/api/v1/projects")
@Slf4j
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping("")
    public ResponseEntity<ProjectListResponseDto> getProjectList() {
        try {
            ProjectListResponseDto response = projectService.getProjectList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getProjectList: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponseDto> getProject(@PathVariable Long projectId) {
        try {
            ProjectResponseDto response = projectService.getProject(projectId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Project not found with ID: {}", projectId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error in getProject: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<ProjectResponseDto> getProjectByDeviceId(@PathVariable String deviceId) {
        try {
            ProjectResponseDto response = projectService.getProjectByDeviceId(deviceId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Project not found for device ID: {}", deviceId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error in getProjectByDeviceId: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * 프로젝트 수정 API
     * Security: PM, ADMIN 권한 필요
     * @param projectId
     * @param request
     * @return
     */
    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('PM') or hasRole('ADMIN')")
    public ResponseEntity<ProjectResponseDto> updateProject(
            @PathVariable Long projectId,
            @RequestBody ProjectEditRequestDto request
    ) {
        try {
            ProjectResponseDto response = projectService.updateProject(projectId, request);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.error("Project not found with ID: {}", projectId, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating project with ID {}: ", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /*
     * 프로젝트 등록 API
     * Security: PM, ADMIN 권한 필요
     * @param request
     * @param userPrincipal
     * @return
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('PM') or hasRole('ADMIN')")
    public ResponseEntity<ProjectResponseDto> registerProject(
            @RequestBody ProjectRegistrationRequestDto request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        try {
            ProjectResponseDto response = projectService.registerProject(request, userPrincipal.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error in registerProject: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}