package com.sungjin.airquailitymonitordemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sungjin.airquailitymonitordemo.dto.request.sensordata.SensorDataSearchRequestDto;
import com.sungjin.airquailitymonitordemo.dto.response.sensordata.SensorDataResponseDto;
import com.sungjin.airquailitymonitordemo.entity.Project;
import com.sungjin.airquailitymonitordemo.entity.SensorData;
import com.sungjin.airquailitymonitordemo.dto.request.sensordata.SensorDataRequestDto;
import com.sungjin.airquailitymonitordemo.exception.InvalidSearchCriteriaException;
import com.sungjin.airquailitymonitordemo.repository.ProjectRepository;
import com.sungjin.airquailitymonitordemo.repository.SensorDataRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SensorDataService {

    private final RedisTemplate<String, SensorData> sensorDataRedisTemplate;
    private final SensorDataRepository sensorDataRepository;
    private final KafkaTemplate<String, SensorData> kafkaTemplate;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;


    @Value("${kafka.topic.sensor-data}")
    private String topicName;

    // Redis의 데이터를 mysql에 저장
    @Scheduled(fixedRate = 5000)
    public void processRedisData() {
        try {
            log.info("Starting scheduled process at: {}", LocalDateTime.now());

            // 1. 일반 데이터 키 패턴으로 변경
            Set<String> keys = sensorDataRedisTemplate.keys("device:*:data:*");
            log.info("Found {} Redis keys", keys != null ? keys.size() : 0);

            if (keys != null && !keys.isEmpty()) {
                int processedCount = 0;

                for (String key : keys) {
                    SensorData data = sensorDataRedisTemplate.opsForValue().get(key);
                    if (data != null) {
                        // MySQL 저장
                        try {
                            sensorDataRepository.save(data);
                            processedCount++;
                            log.info("Saved to MySQL - Device: {}, Time: {}",
                                    data.getDeviceId(), data.getTimestamp());
                        } catch (Exception e) {
                            log.error("MySQL save failed - Device: {}, Error: {}",
                                    data.getDeviceId(), e.getMessage());
                            continue;
                        }

                        // Kafka 전송
                        try {
                            sendToKafka(data);
                        } catch (Exception e) {
                            log.warn("Kafka send failed - Device: {}", data.getDeviceId());

                            continue;
                        }

                        // Redis에서 삭제
                        sensorDataRedisTemplate.delete(key);
                    }
                }

                log.info("Processed {} records at {}", processedCount, LocalDateTime.now());
            }

        } catch (Exception e) {
            log.error("Error in scheduled process: {}", e.getMessage(), e);
        }
    }

    /**
     * 센서 데이터 처리 (cache redis)
     * @param requestDto 센서 데이터 요청 DTO
     * @return 센서 데이터
     */
    public SensorData processSensorData(SensorDataRequestDto requestDto) {
        SensorData sensorData = convertToEntity(requestDto);

        log.info("timestamp: {}", sensorData.getTimestamp());

        // Redis에만 캐시
        try {
            cacheLatestData(sensorData);
            log.info("Successfully cached sensor data for device: {}", sensorData.getDeviceId());
        } catch (Exception e) {
            log.error("Error caching sensor data for device: {}", sensorData.getDeviceId(), e);
        }

        return sensorData;
    }

    /**
     * Redis에 최신 데이터 캐시 (저장)
     * @param data 센서 데이터
     */
    private void cacheLatestData(SensorData data) {
        String deviceId = data.getDeviceId();
        if (deviceId == null) {
            log.warn("DeviceId is null, skipping Redis cache");
            return;
        }

        try {
            // 1. 모든 데이터용 키 (타임스탬프 포함)
            String dataKey = String.format("device:%s:data:%s",
                    deviceId, data.getTimestamp().toString());

            // 2. 최신 데이터용 키
            String latestKey = String.format("device:%s:latest", deviceId);

            // 일반 데이터는 5초 동안만 보관
            sensorDataRedisTemplate.opsForValue()
                    .set(dataKey, data, 5, TimeUnit.SECONDS);

            // latest 데이터는 24시간 보관
            sensorDataRedisTemplate.opsForValue()
                    .set(latestKey, data, 24, TimeUnit.HOURS);

            log.info("Cached sensor data - Device: {}, Time: {}",
                    deviceId, data.getTimestamp());

        } catch (Exception e) {
            log.error("Error caching sensor data for device: {}", deviceId, e);
        }
    }

    private void sendToKafka(SensorData sensorData) {
        try {
            Message<SensorData> message = MessageBuilder
                .withPayload(sensorData)
                .setHeader(KafkaHeaders.TOPIC, topicName)
                .setHeader(KafkaHeaders.KEY, sensorData.getDeviceId())
                .build();

            kafkaTemplate.send(message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent to Kafka: {}", sensorData.getDeviceId());
                    } else {
                        log.error("Kafka send failed: {}", ex.getMessage());
                    }
                });
        } catch (Exception e) {
            log.error("Error sending to Kafka", e);
            throw e;
        }
    }

    private SensorData convertToEntity(SensorDataRequestDto dto) {
        log.info("Converting to entity: {}", dto);

        return SensorData.builder()
                .deviceId(dto.deviceId())
                .timestamp(dto.timestamp())
                .pm25Value(dto.pm25Value())
                .pm25Level(dto.pm25Level())
                .pm10Value(dto.pm10Value())
                .pm10Level(dto.pm10Level())
                .temperature(dto.temperature())
                .temperatureLevel(dto.temperatureLevel())
                .humidity(dto.humidity())
                .humidityLevel(dto.humidityLevel())
                .co2Value(dto.co2Value())
                .co2Level(dto.co2Level())
                .vocValue(dto.vocValue())
                .vocLevel(dto.vocLevel())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .rawData(dto.rawData())
                .build();
    }

    public SensorData getLatestSensorData(String deviceId) {
        String redisKey = "device:" + deviceId + ":latest";
        SensorData data = sensorDataRedisTemplate.opsForValue().get(redisKey);

        if (data == null) {
            // Redis에 없으면 DB에서 조회
            data = sensorDataRepository.findFirstByDeviceIdOrderByTimestampDesc(deviceId)
                    .orElse(null);

            // 있으면 Redis에 캐시
            if (data != null) {
                cacheLatestData(data);
            }
        }

        return data;
    }

    public Page<SensorDataResponseDto> searchSensorData(
            SensorDataSearchRequestDto searchRequest,
            PageRequest pageRequest
    ) {
        validateSearchCriteria(searchRequest);

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (searchRequest.dateRange().exists()) {
            startDateTime = searchRequest.dateRange().startDate().atStartOfDay();
            endDateTime = searchRequest.dateRange().endDate().atTime(23, 59, 59);
        }

        Double latStart = null;
        Double latEnd = null;
        Double longStart = null;
        Double longEnd = null;

        if (searchRequest.location().exists()) {
            latStart = searchRequest.location().getLatitudeStart();
            latEnd = searchRequest.location().getLatitudeEnd();
            longStart = searchRequest.location().getLongitudeStart();
            longEnd = searchRequest.location().getLongitudeEnd();
        }

        Page<SensorData> sensorDataPage = sensorDataRepository.findBySearchCriteria(
                latStart,
                latEnd,
                longStart,
                longEnd,
                startDateTime,
                endDateTime,
                pageRequest
        );

        return sensorDataPage.map(SensorDataResponseDto::fromEntity);
    }

    private void validateSearchCriteria(SensorDataSearchRequestDto searchRequest) {
        if (!searchRequest.hasValidCriteria()) {
            throw new InvalidSearchCriteriaException("Invalid search criteria provided");
        }

        if (!searchRequest.location().isValid()) {
            throw new InvalidSearchCriteriaException("Both latitude and longitude must be provided together");
        }

        if (!searchRequest.dateRange().isValid()) {
            throw new InvalidSearchCriteriaException("Both start date and end date must be provided together");
        }

        if (!searchRequest.dateRange().isValidRange()) {
            throw new InvalidSearchCriteriaException("Start date must be before end date");
        }

        if (!searchRequest.location().exists() && !searchRequest.dateRange().exists()) {
            throw new InvalidSearchCriteriaException("At least one search criteria (location or date) must be provided");
        }
    }



    @Transactional
    public int processBatchData(String rawData) throws JsonProcessingException {
        try {
            JsonNode arrayNode = objectMapper.readTree(rawData);
            if (!arrayNode.isArray()) {
                throw new IllegalArgumentException("Expected JSON array");
            }

            List<SensorDataRequestDto> dataList = new ArrayList<>();

            for (JsonNode payload : arrayNode) {
                JsonNode data = payload.get("data");
                JsonNode location = payload.get("location");
                if (data == null) {
                    throw new IllegalArgumentException("Missing 'data' field in payload");
                }

                String deviceId = payload.get("deviceId").asText();
                Long projectId = payload.get("projectId").asLong();


                String timestampStr = payload.get("timestamp").asText();
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr.replace("Z", ""));
//                LocalDateTime timestamp = Instant.parse(payload.get("timestamp").asText())
//                        .atZone(ZoneId.of("Asia/Seoul"))
//                        .toLocalDateTime();

                Double latitude = location != null && location.has("latitude") ? location.get("latitude").asDouble() : null;
                Double longitude = location != null && location.has("longitude") ? location.get("longitude").asDouble() : null;

                SensorDataRequestDto sensorDataDto = buildSensorDataDto(
                        deviceId, projectId, timestamp, data, latitude, longitude
                );

                dataList.add(sensorDataDto);
            }

            return saveBatchData(dataList);  // 메서드 이름 변경
        } catch (Exception e) {
            log.error("Error processing batch data", e);
            throw e;
        }
    }

    @Transactional
    public int saveBatchData(List<SensorDataRequestDto> dataList) {  // 기존 processBatchData 메서드 이름 변경
        List<SensorData> sensorDataList = new ArrayList<>();

        for (SensorDataRequestDto dto : dataList) {
            try {
                Project project = projectRepository.findById(dto.projectId())
                        .orElseThrow(() -> new EntityNotFoundException("Project not found: " + dto.projectId()));

                SensorData sensorData = SensorData.builder()
                        .deviceId(dto.deviceId())
                        .project(project)
                        .timestamp(dto.timestamp())
                        .pm25Value(dto.pm25Value())
                        .pm25Level(dto.pm25Level())
                        .pm10Value(dto.pm10Value())
                        .pm10Level(dto.pm10Level())
                        .temperature(dto.temperature())
                        .temperatureLevel(dto.temperatureLevel())
                        .humidity(dto.humidity())
                        .humidityLevel(dto.humidityLevel())
                        .co2Value(dto.co2Value())
                        .co2Level(dto.co2Level())
                        .vocValue(dto.vocValue())
                        .vocLevel(dto.vocLevel())
                        .latitude(dto.latitude())
                        .longitude(dto.longitude())
                        .rawData(dto.rawData())
                        .build();

                sensorDataList.add(sensorData);
            } catch (Exception e) {
                log.error("Error processing sensor data entry: {}", dto, e);
                throw e;  // 예외를 상위로 전파
            }
        }

        if (!sensorDataList.isEmpty()) {
            sensorDataRepository.saveAll(sensorDataList);
        }

        return sensorDataList.size();
    }

    private SensorDataRequestDto buildSensorDataDto(
            String deviceId, Long projectId, LocalDateTime timestamp,
            JsonNode data, Double latitude, Double longitude) {
        return SensorDataRequestDto.builder()
                .deviceId(deviceId)
                .projectId(projectId)
                .timestamp(timestamp)
                .pm25Value(data.get("pm25").get("value").asDouble())
                .pm25Level(data.get("pm25").get("level").asInt())
                .pm10Value(data.get("pm10").get("value").asDouble())
                .pm10Level(data.get("pm10").get("level").asInt())
                .temperature(data.get("temperature").get("value").asDouble())
                .temperatureLevel(data.get("temperature").get("level").asInt())
                .humidity(data.get("humidity").get("value").asDouble())
                .humidityLevel(data.get("humidity").get("level").asInt())
                .co2Value(data.get("co2").get("value").asDouble())
                .co2Level(data.get("co2").get("level").asInt())
                .vocValue(data.get("voc").get("value").asDouble())
                .vocLevel(data.get("voc").get("level").asInt())
                .latitude(latitude)
                .longitude(longitude)
                .rawData(data.has("_raw") ? convertRawData(data.get("_raw")) : null)
                .build();
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


    public Page<SensorDataResponseDto> getDeviceSensorHistory(
            String deviceId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            PageRequest pageRequest) {

        Specification<SensorData> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deviceId"), deviceId));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sensorDataRepository.findAll(spec, pageRequest)
                .map(SensorDataResponseDto::fromEntity);
    }

    public int processCsvFile(MultipartFile file) throws IOException {
        List<SensorDataRequestDto> sensorDataList = parseCsvFile(file);

        // Convert DTOs to Entities
        List<SensorData> entities = sensorDataList.stream()
                .map(this::convertToEntityFile)
                .toList();

        sensorDataRepository.saveAll(entities);
        return entities.size();
    }

    private List<SensorDataRequestDto> parseCsvFile(MultipartFile file) throws IOException {
        List<SensorDataRequestDto> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine(); // Read header line
            if (headerLine == null || headerLine.isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty or invalid");
            }

            String[] headers = headerLine.split(",");
            Map<String, Integer> headerIndexMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndexMap.put(headers[i].trim(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length < 17) { // Validate column count
                    log.warn("Invalid CSV line: {}", line);
                    continue;
                }

                try {
                    SensorDataRequestDto dto = SensorDataRequestDto.builder()
                            .timestamp(OffsetDateTime.parse(columns[headerIndexMap.get("timestamp")]).toLocalDateTime())
                            .deviceId(columns[headerIndexMap.get("deviceId")])
                            .projectId(Long.parseLong(columns[headerIndexMap.get("projectId")]))
                            .pm25Value(Double.parseDouble(columns[headerIndexMap.get("pm25Value")]))
                            .pm25Level(Integer.parseInt(columns[headerIndexMap.get("pm25Level")]))
                            .pm10Value(Double.parseDouble(columns[headerIndexMap.get("pm10Value")]))
                            .pm10Level(Integer.parseInt(columns[headerIndexMap.get("pm10Level")]))
                            .temperature(Double.parseDouble(columns[headerIndexMap.get("temperature")]))
                            .temperatureLevel(Integer.parseInt(columns[headerIndexMap.get("temperatureLevel")]))
                            .humidity(Double.parseDouble(columns[headerIndexMap.get("humidity")]))
                            .humidityLevel(Integer.parseInt(columns[headerIndexMap.get("humidityLevel")]))
                            .co2Value(Double.parseDouble(columns[headerIndexMap.get("co2Value")]))
                            .co2Level(Integer.parseInt(columns[headerIndexMap.get("co2Level")]))
                            .vocValue(Double.parseDouble(columns[headerIndexMap.get("vocValue")]))
                            .vocLevel(Integer.parseInt(columns[headerIndexMap.get("vocLevel")]))
                            .latitude(Double.parseDouble(columns[headerIndexMap.get("latitude")]))
                            .longitude(Double.parseDouble(columns[headerIndexMap.get("longitude")]))
                            .rawData(line.getBytes(StandardCharsets.UTF_8)) // Save raw line for debugging
                            .build();

                    dataList.add(dto);
                } catch (Exception e) {
                    log.warn("Failed to parse CSV line: {}", line, e);
                }
            }
        }

        return dataList;
    }

    private SensorData convertToEntityFile(SensorDataRequestDto dto) {
        return SensorData.builder()
                .timestamp(dto.timestamp())
                .deviceId(dto.deviceId())
                .project(Project.builder().projectId(dto.projectId()).build()) // projectId를 기준으로 참조 생성
                .pm25Value(dto.pm25Value())
                .pm25Level(dto.pm25Level())
                .pm10Value(dto.pm10Value())
                .pm10Level(dto.pm10Level())
                .temperature(dto.temperature())
                .temperatureLevel(dto.temperatureLevel())
                .humidity(dto.humidity())
                .humidityLevel(dto.humidityLevel())
                .co2Value(dto.co2Value())
                .co2Level(dto.co2Level())
                .vocValue(dto.vocValue())
                .vocLevel(dto.vocLevel())
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .rawData(dto.rawData())
                .build();
    }
}