package com.sungjin.airquailitymonitordemo.service;

import com.sungjin.airquailitymonitordemo.dto.request.project.ProjectEditRequestDto;
import com.sungjin.airquailitymonitordemo.dto.request.project.ProjectRegistrationRequestDto;
import com.sungjin.airquailitymonitordemo.dto.response.project.ProjectListResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.project.ProjectResponseDto;
import com.sungjin.airquailitymonitordemo.entity.Project;
import com.sungjin.airquailitymonitordemo.entity.User;
import com.sungjin.airquailitymonitordemo.exception.ServiceException;
import com.sungjin.airquailitymonitordemo.repository.ProjectRepository;
import com.sungjin.airquailitymonitordemo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectListResponseDto getProjectList() {
        try {
            List<ProjectResponseDto> projects = projectRepository.findAll()
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return new ProjectListResponseDto(projects);
        } catch (Exception e) {
            log.error("Error while fetching project list: ", e);
            throw new ServiceException("Failed to fetch project list", e);
        }
    }

    public ProjectResponseDto getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID: " + projectId));

        return convertToDto(project);
    }

    private ProjectResponseDto convertToDto(Project project) {
        return new ProjectResponseDto(
                project.getProjectId(),
                project.getTitle(),
                project.getDescription(),
                project.getCreatedAt()
        );
    }

    public ProjectResponseDto getProjectByDeviceId(String deviceId) {
        Project project = projectRepository.findByDevicesDeviceId(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found for device ID: " + deviceId));

        return convertToDto(project);
    }

    public ProjectResponseDto updateProject(Long projectId, ProjectEditRequestDto request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID: " + projectId));

        // setter 대신 엔티티의 메서드 호출
        project.updateProjectDetails(request.projectName(), request.description(), request.collectionMethod());

        Project updatedProject = projectRepository.save(project);
        return convertToDto(updatedProject);
    }

    // 프로젝트 등록
    public ProjectResponseDto registerProject(ProjectRegistrationRequestDto request, Long userId) {
        User pm = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        Project project = Project.builder()
                .pm(pm)
                .nationCode(request.nationCode())
                .title(request.title())
                .description(request.description())
                .startDate(LocalDateTime.parse(request.startDate()))
                .endDate(LocalDateTime.parse(request.endDate()))
                .termsOfUse(request.termOfUse())
                .additionalTerms(request.additionalConsent())
                .collectionMethod(request.collectionMethod())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Project savedProject = projectRepository.save(project);
        return convertToDto(savedProject);
    }

}