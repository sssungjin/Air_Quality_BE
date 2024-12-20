package com.sungjin.airquailitymonitordemo.controller;

import com.sungjin.airquailitymonitordemo.dto.request.project.ProjectEditRequestDto;
import com.sungjin.airquailitymonitordemo.dto.response.project.ProjectListResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.project.ProjectResponseDto;
import com.sungjin.airquailitymonitordemo.service.ProjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{projectId}")
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

    @PostMapping("/register")
    public ResponseEntity<ProjectResponseDto> registerProject(@RequestBody ProjectEditRequestDto request) {
        try {
            ProjectResponseDto response = projectService.registerProject(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in registerProject: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}