package com.sungjin.airquailitymonitordemo.dto.response.sensordata;

import com.sungjin.airquailitymonitordemo.entity.SensorData;
import java.time.LocalDateTime;

public record SensorDataResponseDto(
        String deviceId,
        LocalDateTime timestamp,
        Double pm25Value,
        Integer pm25Level,
        Double pm10Value,
        Integer pm10Level,
        Double temperature,
        Integer temperatureLevel,
        Double humidity,
        Integer humidityLevel,
        Double co2Value,
        Integer co2Level,
        Double vocValue,
        Integer vocLevel,
        Double latitude,
        Double longitude
) {
    public static SensorDataResponseDto fromEntity(SensorData entity) {
        return new SensorDataResponseDto(
                entity.getDeviceId(),
                entity.getTimestamp(),
                entity.getPm25Value(),
                entity.getPm25Level(),
                entity.getPm10Value(),
                entity.getPm10Level(),
                entity.getTemperature(),
                entity.getTemperatureLevel(),
                entity.getHumidity(),
                entity.getHumidityLevel(),
                entity.getCo2Value(),
                entity.getCo2Level(),
                entity.getVocValue(),
                entity.getVocLevel(),
                entity.getLatitude(),
                entity.getLongitude()
        );
    }
}