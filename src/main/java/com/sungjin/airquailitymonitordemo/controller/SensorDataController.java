package com.sungjin.airquailitymonitordemo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sungjin.airquailitymonitordemo.dto.request.sensordata.SensorDataRequestDto;
import com.sungjin.airquailitymonitordemo.dto.request.sensordata.SensorDataSearchRequestDto;
import com.sungjin.airquailitymonitordemo.dto.response.sensordata.SensorDataUploadResponseDto;
import com.sungjin.airquailitymonitordemo.dto.response.sensordata.SensorDataResponseDto;
import com.sungjin.airquailitymonitordemo.service.SensorDataService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sensor-data")
@Slf4j
@RequiredArgsConstructor
public class SensorDataController {
    private final SensorDataService sensorDataService;
    private List<SensorDataRequestDto> dataList;
    private final ObjectMapper objectMapper;

    @PostMapping("/search")
    public ResponseEntity<Page<SensorDataResponseDto>> searchSensorData(
            @RequestBody SensorDataSearchRequestDto searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp,desc") String[] sort
    ) {
        Sort.Direction direction = sort[1].equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        Page<SensorDataResponseDto> result = sensorDataService.searchSensorData(searchRequest, pageRequest);
        return ResponseEntity.ok(result);
    }



    @GetMapping("/history/{deviceId}")
    public ResponseEntity<Page<SensorDataResponseDto>> getDeviceSensorHistory(
            @PathVariable String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").ascending());
            Page<SensorDataResponseDto> history = sensorDataService.getDeviceSensorHistory(
                    deviceId, startDate, endDate, pageRequest);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching device sensor history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<String> testEndpoint(@RequestBody SensorDataRequestDto dto) {
        log.info("Received: {}", dto);
        return ResponseEntity.ok("Success");
    }

    @PostMapping("/batch")
    public ResponseEntity<SensorDataUploadResponseDto> uploadBatchData(@RequestBody String rawData) {
        log.info("Received raw batch sensor data upload request");
        try {
            int processedCount = sensorDataService.processBatchData(rawData);
            return ResponseEntity.ok(new SensorDataUploadResponseDto(
                    true,
                    String.format("Successfully processed %d sensor data entries", processedCount),
                    processedCount
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SensorDataUploadResponseDto(false, e.getMessage(), 0));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new SensorDataUploadResponseDto(false, e.getMessage(), 0));
        } catch (Exception e) {
            log.error("Error processing batch sensor data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SensorDataUploadResponseDto(false, "Internal server error", 0));
        }
    }

    @PostMapping("/upload-csv")
    public ResponseEntity<SensorDataUploadResponseDto> uploadCsv(@RequestParam("file") MultipartFile file) {
        log.info("Received CSV upload request");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new SensorDataUploadResponseDto(
                    false, "File is empty", 0
            ));
        }

        try {
            // 파일 처리 및 저장 로직 호출
            int processedCount = sensorDataService.processCsvFile(file);
            return ResponseEntity.ok(new SensorDataUploadResponseDto(
                    true,
                    String.format("Successfully processed %d sensor data entries", processedCount),
                    processedCount
            ));
        } catch (Exception e) {
            log.error("Error processing CSV file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new SensorDataUploadResponseDto(
                    false, "Internal server error", 0
            ));
        }
    }

    private byte[] convertRawData(JsonNode rawArray) {
        if (rawArray == null || !rawArray.isArray()) {
            return new byte[0];
        }

        byte[] rawData = new byte[rawArray.size()];
        for (int i = 0; i < rawArray.size(); i++) {
            rawData[i] = (byte) rawArray.get(i).asInt();
        }
        return rawData;
    }

}
