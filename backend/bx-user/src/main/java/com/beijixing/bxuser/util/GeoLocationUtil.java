package com.beijixing.bxuser.util;

import org.springframework.stereotype.Component;

@Component
public class GeoLocationUtil {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    /**
     * 计算两点之间的距离（公里）
     */
    public double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return -1;
        }
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
    
    /**
     * 根据IP获取地理位置（简化版，实际应调用IP库）
     */
    public GeoLocation getLocationByIp(String ip) {
        // 简化实现，实际应该调用IP2Region或其他IP库
        return GeoLocation.builder()
                .country("中国")
                .province("未知")
                .city("未知")
                .latitude(null)
                .longitude(null)
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GeoLocation {
        private String country;
        private String province;
        private String city;
        private Double latitude;
        private Double longitude;
    }
}
