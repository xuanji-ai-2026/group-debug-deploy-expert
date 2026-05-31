package com.beijixing.bxuser.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordValidatorUtil {
    
    public PasswordValidationResult validate(String password) {
        List<String> errorMessages = new ArrayList<>();
        
        if (password == null) {
            errorMessages.add("密码不能为空");
            return PasswordValidationResult.builder()
                    .valid(false)
                    .errorMessages(errorMessages)
                    .build();
        }
        
        // 规则1: 最小长度8位
        if (password.length() < 8) {
            errorMessages.add("密码长度至少8位");
        }
        
        // 规则2: 必须包含大写字母
        if (!password.matches(".*[A-Z].*")) {
            errorMessages.add("密码必须包含至少1个大写字母");
        }
        
        // 规则3: 必须包含小写字母
        if (!password.matches(".*[a-z].*")) {
            errorMessages.add("密码必须包含至少1个小写字母");
        }
        
        // 规则4: 必须包含数字
        if (!password.matches(".*\\d.*")) {
            errorMessages.add("密码必须包含至少1个数字");
        }
        
        // 规则5: 必须包含特殊字符
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            errorMessages.add("密码必须包含至少1个特殊字符(!@#$%^&*等)");
        }
        
        return PasswordValidationResult.builder()
                .valid(errorMessages.isEmpty())
                .errorMessages(errorMessages)
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PasswordValidationResult {
        private boolean valid;
        private List<String> errorMessages;
        
        public String getErrorMessage() {
            return String.join("; ", errorMessages);
        }
    }
}
