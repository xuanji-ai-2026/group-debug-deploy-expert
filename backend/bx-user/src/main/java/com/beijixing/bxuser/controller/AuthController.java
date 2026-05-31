package com.beijixing.bxuser.controller;

import com.beijixing.bxuser.dto.LoginRequest;
import com.beijixing.bxuser.dto.RegisterRequest;
import com.beijixing.bxuser.entity.User;
import com.beijixing.bxuser.mapper.UserMapper;
import com.beijixing.bxuser.service.EmailService;
import com.beijixing.bxuser.service.TencentSmsService;
import com.beijixing.bxuser.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TencentSmsService tencentSmsService;

    @Autowired
    private EmailService emailService;

    /**
     * 传统密码登录（保留兼容）
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "请求参数不能为空"));
        }
        if (request.getPhone() == null || request.getPhone().isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "手机号不能为空"));
        }
        log.info("登录请求: phone={}, type={}", request.getPhone(), request.getLoginType());

        User user = userMapper.findByPhone(request.getPhone()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok().body(Map.of("code", 401, "message", "手机号或密码错误"));
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return ResponseEntity.ok().body(Map.of("code", 403, "message", "账号已被禁用"));
        }

        boolean passwordMatch = false;
        if (request.getPassword() != null && user.getPassword() != null) {
            passwordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        }

        if (!passwordMatch) {
            return ResponseEntity.ok().body(Map.of("code", 401, "message", "手机号或密码错误"));
        }

        return buildLoginResponse(user);
    }

    /**
     * 验证码登录（手机号）- 统一接口，自动判断注册/已登录用户
     * 如果是新用户返回 isNewUser=true，前端引导设置密码
     * 如果是老用户直接登录成功
     */
    @PostMapping("/login-sms")
    public ResponseEntity<?> loginWithSmsCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String code = request.get("code");

        log.info("验证码登录请求: phone={}", phone);

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "手机号不能为空"));
        }

        if (code == null || code.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "验证码不能为空"));
        }

        if (!tencentSmsService.verifyCode(phone, code, "LOGIN")) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "验证码错误或已过期"));
        }

        User user = userMapper.findByPhone(phone).orElse(null);

        if (user == null) {
            log.info("新用户检测: phone={}", phone);

            Map<String, Object> newData = new HashMap<>();
            newData.put("token", "");
            newData.put("refresh_token", "");
            newData.put("user", Map.of("phone", phone));
            newData.put("isNewUser", true);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "验证成功，请设置密码完成注册");
            result.put("data", newData);

            return ResponseEntity.ok(result);
        } else {
            // 已注册用户，直接登录
            if (user.getStatus() != null && user.getStatus() == 0) {
                return ResponseEntity.ok().body(Map.of("code", 403, "message", "账号已被禁用"));
            }

            log.info("验证码登录成功: id={}, phone={}", user.getId(), user.getPhone());
            return buildLoginResponse(user);
        }
    }

    /**
     * 注册并登录（新用户设置密码后调用）
     * 完成注册并自动登录返回token
     */
    @PostMapping("/register-login")
    public ResponseEntity<?> registerAndLogin(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String password = request.get("password");

        log.info("注册并登录请求: phone={}", phone);

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "手机号不能为空"));
        }

        if (password == null || password.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "密码不能为空"));
        }

        if (password.length() < 6) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "密码长度至少6位"));
        }

        User existingUser = userMapper.findByPhone(phone).orElse(null);
        if (existingUser != null) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "该手机号已注册，请直接登录"));
        }

        // 创建新用户
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickName("用户" + phone.substring(7));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        log.info("用户注册并登录成功: id={}, phone={}", user.getId(), user.getPhone());

        return buildLoginResponse(user);
    }

    /**
     * 邮箱验证码登录 - 支持邮箱注册和登录
     */
    @PostMapping("/login-email")
    public ResponseEntity<?> loginWithEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        log.info("邮箱登录请求: email={}", email);

        if (email == null || email.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "邮箱不能为空"));
        }

        if (code == null || code.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "验证码不能为空"));
        }

        // 验证邮箱验证码
        if (!emailService.verifyCode(email, code, "LOGIN")) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "验证码错误或已过期"));
        }

        User user = userMapper.findByEmail(email).orElse(null);

        if (user == null) {
            log.info("新邮箱用户检测: email={}", email);

            Map<String, Object> newData = new HashMap<>();
            newData.put("token", "");
            newData.put("refresh_token", "");
            newData.put("user", Map.of("email", email));
            newData.put("isNewUser", true);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("message", "验证成功，请设置密码完成注册");
            result.put("data", newData);

            return ResponseEntity.ok(result);
        } else {
            if (user.getStatus() != null && user.getStatus() == 0) {
                return ResponseEntity.ok().body(Map.of("code", 403, "message", "账号已被禁用"));
            }

            log.info("邮箱登录成功: id={}, email={}", user.getId(), user.getEmail());
            return buildLoginResponse(user);
        }
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendSmsCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");

        log.info("📱 发送短信验证码请求: phone={}", phone);

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "手机号不能为空"));
        }

        boolean sendResult = tencentSmsService.sendVerificationCode(phone, "LOGIN");
        
        if (!sendResult) {
            return ResponseEntity.ok().body(Map.of("code", 500, "message", "短信发送失败，请稍后重试"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "验证码已发送");
        result.put("expireSeconds", 300); // 5分钟有效期

        return ResponseEntity.ok(result);
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-email-code")
    public ResponseEntity<?> sendEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        log.info("发送邮箱验证码请求: email={}", email);

        if (email == null || email.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "邮箱不能为空"));
        }

        // 邮箱格式简单校验
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "邮箱格式不正确"));
        }

        // 调用真实邮件服务发送验证码
        boolean success = emailService.sendVerificationCode(email, "LOGIN");

        if (!success) {
            log.error("❌ 邮箱验证码发送失败: email={}", email);
            return ResponseEntity.ok().body(Map.of("code", 500, "message", "验证码发送失败，请稍后重试"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "邮箱验证码已发送");
        result.put("expireSeconds", 300);

        return ResponseEntity.ok(result);
    }

    /**
     * 传统注册接口（保留兼容）
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("注册请求: phone={}", request.getPhone());

        User existingUser = userMapper.findByPhone(request.getPhone()).orElse(null);
        if (existingUser != null) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "手机号已被注册"));
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickName(request.getNickName() != null ? request.getNickName() : "用户" + request.getPhone().substring(7));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        log.info("用户注册成功: id={}, phone={}", user.getId(), user.getPhone());

        return ResponseEntity.ok(Map.of("code", 200, "message", "注册成功", "userId", user.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        log.info("Token刷新请求: token存在={}", refreshToken != null);

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", 400, "message", "缺少refreshToken"));
        }

        try {
            if (!jwtUtils.validateToken(refreshToken)) {
                return ResponseEntity.ok().body(Map.of("code", 401, "message", "refreshToken无效或已过期"));
            }

            String phone = jwtUtils.getPhoneFromToken(refreshToken);
            User user = userMapper.findByPhone(phone).orElse(null);

            if (user == null) {
                return ResponseEntity.ok().body(Map.of("code", 404, "message", "用户不存在"));
            }

            if (user.getStatus() != null && user.getStatus() == 0) {
                return ResponseEntity.ok().body(Map.of("code", 403, "message", "账号已被禁用"));
            }

            log.info("Token刷新成功: phone={}", phone);
            return buildLoginResponse(user);
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage(), e);
            return ResponseEntity.ok().body(Map.of("code", 500, "message", "Token刷新失败: " + e.getMessage()));
        }
    }

    /**
     * 构建统一的登录响应
     */
    private ResponseEntity<?> buildLoginResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getPhone());

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getId());
        userData.put("phone", user.getPhone());
        userData.put("nickname", user.getNickName());
        userData.put("avatar", user.getAvatar());
        userData.put("roleType", user.getRoleType());
        userData.put("status", user.getStatus());
        userData.put("tenantId", user.getTenantId());

        Map<String, Object> loginData = new HashMap<>();
        loginData.put("token", accessToken);
        loginData.put("refresh_token", refreshToken);
        loginData.put("user", userData);
        loginData.put("isNewUser", false);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "登录成功");
        result.put("data", loginData);

        return ResponseEntity.ok(result);
    }

    /**
     * SMS服务诊断端点（调试用）
     */
    @GetMapping("/sms-status")
    public ResponseEntity<?> checkSmsStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", System.currentTimeMillis());
        status.put("smsClientInitialized", tencentSmsService.isSmsClientInitialized());
        status.put("config", tencentSmsService.getConfigStatus());
        return ResponseEntity.ok(status);
    }

    /**
     * 登出接口（移动端需要）
     * 清除服务端Token缓存（如有），返回登出成功
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("用户登出请求");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("Token已失效(登出): token前缀={}", token.length() > 10 ? token.substring(0, 10) + "..." : "N/A");
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "登出成功"));
    }

    @PostMapping("/reset-test-password")
    public ResponseEntity<?> resetTestPassword(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String newPassword = request.getOrDefault("password", "Test123456");
        
        log.warn("⚠️ [TEST] 重置测试用户密码: phone={}", phone);
        
        User user = userMapper.findByPhone(phone).orElse(null);
        if (user == null) {
            return ResponseEntity.ok().body(Map.of("code", 404, "message", "用户不存在"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        
        log.info("✅ [TEST] 密码重置成功: phone={}, id={}", phone, user.getId());
        return ResponseEntity.ok(Map.of("code", 0, "message", "密码重置成功", "newPassword", newPassword));
    }
}
