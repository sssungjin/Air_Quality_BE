package com.sungjin.airquailitymonitordemo.controller;

import com.sungjin.airquailitymonitordemo.dto.request.sensordata.SensorDataRequestDto;
import com.sungjin.airquailitymonitordemo.entity.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/redis")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final RedisTemplate<String, SensorData> sensorDataRedisTemplate;

    @PostMapping("/")
    public ResponseEntity<String> testRedis(@RequestBody SensorDataRequestDto requestDto) {
        try {
            String key = "device:" + requestDto.deviceId() + ":latest";
            SensorData sensorData = SensorData.builder()
                    .deviceId(requestDto.deviceId())
                    .timestamp(requestDto.timestamp())
                    .pm25Value(requestDto.pm25Value())
                    .pm25Level(requestDto.pm25Level())
                    .pm10Value(requestDto.pm10Value())
                    .pm10Level(requestDto.pm10Level())
                    .temperature(requestDto.temperature())
                    .temperatureLevel(requestDto.temperatureLevel())
                    .humidity(requestDto.humidity())
                    .humidityLevel(requestDto.humidityLevel())
                    .co2Value(requestDto.co2Value())
                    .co2Level(requestDto.co2Level())
                    .vocValue(requestDto.vocValue())
                    .vocLevel(requestDto.vocLevel())
                    .latitude(requestDto.latitude())
                    .longitude(requestDto.longitude())
                    .rawData(requestDto.rawData())
                    .build();

            log.info("Saving sensor data to Redis - Key: {}, Value: {}", key, sensorData);

            sensorDataRedisTemplate.opsForValue().set(key, sensorData, 24, TimeUnit.HOURS);

            // 저장 직후 확인
            SensorData savedData = sensorDataRedisTemplate.opsForValue().get(key);
            log.info("Retrieved sensor data from Redis - Key: {}, Value: {}", key, savedData);

            return ResponseEntity.ok("Sensor data saved to Redis with key: " + key);
        } catch (Exception e) {
            log.error("Error saving to Redis: ", e);
            return ResponseEntity.status(500)
                    .body("Error saving to Redis: " + e.getMessage());
        }
    }

    // 데이터 확인용 GET 엔드포인트 추가
    @GetMapping("/{deviceId}")
    public ResponseEntity<SensorData> getSensorData(@PathVariable String deviceId) {
        String key = "device:" + deviceId + ":latest";
        SensorData data = sensorDataRedisTemplate.opsForValue().get(key);

        if (data != null) {
            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sensor-data/{deviceId}")
    public ResponseEntity<SensorData> getLatestSensorData(@PathVariable String deviceId) {
        String key = "device:" + deviceId + ":latest";
        SensorData data = sensorDataRedisTemplate.opsForValue().get(key);
        return data != null ? ResponseEntity.ok(data) : ResponseEntity.notFound().build();
    }

    @GetMapping("/keys")
    public ResponseEntity<Set<String>> getAllKeys() {
        Set<String> keys = sensorDataRedisTemplate.keys("*");
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/monitor/sensor-data")
    public ResponseEntity<Map<String, Object>> monitorSensorData() {
        Map<String, Object> result = new HashMap<>();
        try {
            Set<String> keys = sensorDataRedisTemplate.keys("device:*:latest");
            result.put("totalKeys", keys.size());

            Map<String, SensorData> latestData = new HashMap<>();
            for (String key : keys) {
                SensorData data = sensorDataRedisTemplate.opsForValue().get(key);
                if (data != null) {
                    latestData.put(key, data);
                }
            }
            result.put("data", latestData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
