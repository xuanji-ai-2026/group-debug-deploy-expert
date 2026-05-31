package com.beijixing.bxuser.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class DeviceFingerprintUtil {
    
    public String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        
        String fingerprintData = String.format("%s|%s|%s", 
                StringUtils.hasText(userAgent) ? userAgent : "",
                StringUtils.hasText(acceptLanguage) ? acceptLanguage : "",
                StringUtils.hasText(acceptEncoding) ? acceptEncoding : "");
        
        return hashFingerprint(fingerprintData);
    }
    
    public String generateFingerprint(String userAgent, String acceptLanguage) {
        String fingerprintData = String.format("%s|%s", 
                StringUtils.hasText(userAgent) ? userAgent : "",
                StringUtils.hasText(acceptLanguage) ? acceptLanguage : "");
        
        return hashFingerprint(fingerprintData);
    }
    
    private String hashFingerprint(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return data;
        }
    }
    
    public String extractDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        }
        return "Desktop";
    }
    
    public String extractOS(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("windows")) return "Windows";
        if (userAgent.contains("mac")) return "MacOS";
        if (userAgent.contains("linux")) return "Linux";
        if (userAgent.contains("android")) return "Android";
        if (userAgent.contains("iphone") || userAgent.contains("ipad")) return "iOS";
        return "Unknown";
    }
    
    public String extractBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("chrome") && !userAgent.contains("edg")) return "Chrome";
        if (userAgent.contains("firefox")) return "Firefox";
        if (userAgent.contains("safari") && !userAgent.contains("chrome")) return "Safari";
        if (userAgent.contains("edg")) return "Edge";
        if (userAgent.contains("opera")) return "Opera";
        return "Unknown";
    }
}
