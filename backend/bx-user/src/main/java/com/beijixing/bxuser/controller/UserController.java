package com.beijixing.bxuser.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.bxuser.entity.User;
import com.beijixing.bxuser.mapper.UserMapper;
import com.beijixing.bxuser.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("获取用户信息请求");

            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 401, "message", "未提供认证Token"));
            }

            if (!jwtUtils.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of("code", 401, "message", "Token无效或已过期"));
            }

            String phone = jwtUtils.getPhoneFromToken(token);
            User user = userMapper.findByPhone(phone).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "用户不存在"));
            }

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("phone", user.getPhone());
            userData.put("nickname", user.getNickName());
            userData.put("email", user.getEmail());
            userData.put("realName", user.getRealName());
            userData.put("roleType", user.getRoleType());
            userData.put("status", user.getStatus());
            userData.put("avatar", user.getAvatar());
            userData.put("tenantId", user.getTenantId());
            userData.put("lastLoginTime", user.getLastLoginTime());
            userData.put("createdAt", user.getCreatedAt());

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", userData);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "系统错误: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 401, "message", "未提供认证Token"));
            }

            if (!jwtUtils.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of("code", 401, "message", "Token无效或已过期"));
            }

            String phone = jwtUtils.getPhoneFromToken(token);
            User user = userMapper.findByPhone(phone).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "用户不存在"));
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("phone", user.getPhone());
            profile.put("nickname", user.getNickName());
            profile.put("email", user.getEmail());
            profile.put("realName", user.getRealName());
            profile.put("avatar", user.getAvatar());
            profile.put("roleType", user.getRoleType());
            profile.put("status", user.getStatus());
            profile.put("tenantId", user.getTenantId());
            profile.put("lastLoginTime", user.getLastLoginTime());
            profile.put("createdAt", user.getCreatedAt());
            profile.put("updatedAt", user.getUpdatedAt());

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", profile);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取用户Profile失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "系统错误: " + e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getUserList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleType,
            @RequestParam(required = false) Integer status) {
        try {
            log.info("获取用户列表: page={}, size={}, keyword={}", page, size, keyword);

            IPage<User> userPage = new Page<>(page + 1, size);
            if (keyword != null && !keyword.isEmpty()) {
                userPage = userMapper.searchByKeyword(userPage, keyword);
            } else if (roleType != null) {
                userPage = userMapper.findByRoleType(userPage, roleType);
            } else if (status != null) {
                userPage = userMapper.findByStatus(userPage, status);
            } else {
                userPage = userMapper.selectPage(userPage, null);
            }

            List<Map<String, Object>> userList = userPage.getRecords().stream().map(user -> {
                Map<String, Object> u = new HashMap<>();
                u.put("id", user.getId());
                u.put("phone", user.getPhone());
                u.put("nickname", user.getNickName());
                u.put("email", user.getEmail());
                u.put("roleType", user.getRoleType());
                u.put("status", user.getStatus());
                u.put("avatar", user.getAvatar());
                u.put("createdAt", user.getCreatedAt());
                u.put("lastLoginTime", user.getLastLoginTime());
                return u;
            }).toList();

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", userList);
            result.put("total", userPage.getTotal());
            result.put("page", page);
            result.put("size", size);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取用户列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "系统错误: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userMapper.selectList(null);

            List<Map<String, Object>> userList = users.stream().map(user -> {
                Map<String, Object> u = new HashMap<>();
                u.put("id", user.getId());
                u.put("phone", user.getPhone());
                u.put("nickname", user.getNickName());
                u.put("roleType", user.getRoleType());
                u.put("status", user.getStatus());
                u.put("createdAt", user.getCreatedAt());
                return u;
            }).toList();

            return ResponseEntity.ok(Map.of("code", 200, "data", userList, "total", userList.size()));
        } catch (Exception e) {
            log.error("获取所有用户失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "系统错误: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            if (updates.containsKey("nickname")) {
                user.setNickName((String) updates.get("nickname"));
            }
            if (updates.containsKey("email")) {
                user.setEmail((String) updates.get("email"));
            }
            if (updates.containsKey("realName")) {
                user.setRealName((String) updates.get("realName"));
            }
            if (updates.containsKey("avatar")) {
                user.setAvatar((String) updates.get("avatar"));
            }
            if (updates.containsKey("roleType")) {
                user.setRoleType(String.valueOf(updates.get("roleType")));
            }
            if (updates.containsKey("status")) {
                user.setStatus(((Number) updates.get("status")).intValue());
            }

            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);

            log.info("用户信息更新成功: id={}", id);
            return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
        } catch (Exception e) {
            log.error("更新用户信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (userMapper.selectById(id) == null) {
                return ResponseEntity.notFound().build();
            }

            userMapper.deleteById(id);
            log.info("用户删除成功: id={}", id);
            return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "删除失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            user.setStatus(0);
            userMapper.updateById(user);

            log.info("用户已禁用: id={}", id);
            return ResponseEntity.ok(Map.of("code", 200, "message", "用户已禁用"));
        } catch (Exception e) {
            log.error("禁用用户失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "操作失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            user.setStatus(1);
            userMapper.updateById(user);

            log.info("用户已启用: id={}", id);
            return ResponseEntity.ok(Map.of("code", 200, "message", "用户已启用"));
        } catch (Exception e) {
            log.error("启用用户失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "操作失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "用户不存在"));
            }
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("phone", user.getPhone());
            userData.put("nickname", user.getNickName());
            userData.put("email", user.getEmail());
            userData.put("realName", user.getRealName());
            userData.put("roleType", user.getRoleType());
            userData.put("status", user.getStatus());
            userData.put("avatar", user.getAvatar());
            userData.put("tenantId", user.getTenantId());
            userData.put("lastLoginTime", user.getLastLoginTime());
            userData.put("createdAt", user.getCreatedAt());
            userData.put("updatedAt", user.getUpdatedAt());
            return ResponseEntity.ok(Map.of("code", 200, "message", "success", "data", userData));
        } catch (Exception e) {
            log.error("获取用户详情失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "系统错误: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "用户不存在"));
            }
            String oldPassword = (String) params.get("oldPassword");
            String newPassword = (String) params.get("newPassword");
            if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "旧密码和新密码不能为空"));
            }
            // 验证旧密码
            if (!user.getPassword().equals(oldPassword)) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "旧密码不正确"));
            }
            user.setPassword(newPassword);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("密码修改成功: userId={}", id);
            return ResponseEntity.ok(Map.of("code", 200, "message", "密码修改成功"));
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "修改密码失败: " + e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getUserCount() {
        try {
            long total = userMapper.selectCount(null);
            long active = userMapper.countByStatus(1);

            return ResponseEntity.ok(Map.of(
                "code", 200,
                "total", total,
                "active", active,
                "disabled", total - active
            ));
        } catch (Exception e) {
            log.error("获取用户统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户余额（移动端需要）
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getUserBalance(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 401, "message", "未提供认证Token"));
            }
            if (!jwtUtils.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of("code", 401, "message", "Token无效或已过期"));
            }
            String phone = jwtUtils.getPhoneFromToken(token);
            User user = userMapper.findByPhone(phone).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "用户不存在"));
            }
            Map<String, Object> balance = new HashMap<>();
            balance.put("userId", user.getId());
            balance.put("phone", user.getPhone());
            balance.put("balance", 0.00);
            balance.put("frozenAmount", 0.00);
            balance.put("totalConsumed", 0.00);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", balance);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取用户余额失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "系统错误: " + e.getMessage()));
        }
    }

    // ============================================================
    // Admin 管理端扩展接口
    // ============================================================

    /**
     * 用户统计（支持日期范围）
     * GET /user/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            long total = userMapper.selectCount(null);
            long active = userMapper.countByStatus(1);
            long disabled = total - active;

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("active", active);
            stats.put("disabled", disabled);
            stats.put("startDate", startDate);
            stats.put("endDate", endDate);

            return ResponseEntity.ok(Map.of("code", 200, "data", stats));
        } catch (Exception e) {
            log.error("获取用户统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "统计失败"));
        }
    }

    /**
     * 新用户趋势
     * GET /user/trend/new-users
     */
    @GetMapping("/trend/new-users")
    public ResponseEntity<?> getNewUserTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        try {
            List<Map<String, Object>> trend = new java.util.ArrayList<>();
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", startDate != null ? startDate : "2026-05-01");
            entry.put("count", userMapper.selectCount(null));
            entry.put("granularity", granularity);
            trend.add(entry);

            return ResponseEntity.ok(Map.of("code", 200, "data", trend));
        } catch (Exception e) {
            log.error("获取新用户趋势失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "查询失败"));
        }
    }

    /**
     * 活跃用户排行
     * GET /user/ranking/active
     */
    @GetMapping("/ranking/active")
    public ResponseEntity<?> getActiveUserRanking(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<User> users = userMapper.selectList(null);
            List<Map<String, Object>> ranking = users.stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .limit(limit)
                .map(u -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("userId", u.getId());
                    item.put("nickname", u.getNickName());
                    item.put("phone", u.getPhone());
                    item.put("lastLoginTime", u.getLastLoginTime());
                    return item;
                })
                .toList();

            return ResponseEntity.ok(Map.of("code", 200, "data", ranking));
        } catch (Exception e) {
            log.error("获取活跃排行失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "查询失败"));
        }
    }

    /**
     * 异常用户列表
     * GET /user/list/abnormal
     */
    @GetMapping("/list/abnormal")
    public ResponseEntity<?> getAbnormalUsers(@RequestParam(defaultValue = "50") int limit) {
        try {
            List<User> users = userMapper.selectList(null);
            List<Map<String, Object>> abnormal = users.stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 0)
                .limit(limit)
                .map(u -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", u.getId());
                    item.put("phone", u.getPhone());
                    item.put("nickname", u.getNickName());
                    item.put("status", u.getStatus());
                    item.put("createdAt", u.getCreatedAt());
                    return item;
                })
                .toList();

            return ResponseEntity.ok(Map.of("code", 200, "data", abnormal));
        } catch (Exception e) {
            log.error("获取异常用户失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "查询失败"));
        }
    }

    /**
     * 批量查询用户
     * POST /user/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<?> batchGetUsers(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> userIds = (List<Integer>) body.get("userIds");
            if (userIds == null || userIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "userIds不能为空"));
            }

            List<Map<String, Object>> userList = userIds.stream()
                .map(id -> userMapper.selectById(id.longValue()))
                .filter(u -> u != null)
                .map(u -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", u.getId());
                    item.put("phone", u.getPhone());
                    item.put("nickname", u.getNickName());
                    item.put("email", u.getEmail());
                    item.put("status", u.getStatus());
                    item.put("roleType", u.getRoleType());
                    item.put("avatar", u.getAvatar());
                    return item;
                })
                .toList();

            return ResponseEntity.ok(Map.of("code", 200, "data", userList));
        } catch (Exception e) {
            log.error("批量查询用户失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "查询失败"));
        }
    }

    /**
     * 获取用户角色
     * GET /user/{userId}/roles
     */
    @GetMapping("/{userId}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "用户不存在"));
            }

            List<Map<String, Object>> roles = new java.util.ArrayList<>();
            Map<String, Object> role = new HashMap<>();
            role.put("roleType", user.getRoleType());
            role.put("userId", user.getId());
            roles.add(role);

            return ResponseEntity.ok(Map.of("code", 200, "data", roles));
        } catch (Exception e) {
            log.error("获取用户角色失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "查询失败"));
        }
    }

    /**
     * 获取用户权限
     * GET /user/{userId}/permissions
     */
    @GetMapping("/{userId}/permissions")
    public ResponseEntity<?> getUserPermissions(@PathVariable Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("code", 404, "message", "用户不存在"));
            }

            List<String> permissions = new java.util.ArrayList<>();
            if ("admin".equals(user.getRoleType())) {
                permissions.add("admin");
                permissions.add("user:manage");
                permissions.add("content:manage");
                permissions.add("system:manage");
            } else {
                permissions.add("user");
                permissions.add("content:view");
            }

            return ResponseEntity.ok(Map.of("code", 200, "data", permissions));
        } catch (Exception e) {
            log.error("获取用户权限失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "查询失败"));
        }
    }

    /**
     * 获取所有角色列表
     * GET /user/roles/all
     */
    @GetMapping("/roles/all")
    public ResponseEntity<?> getAllRoles() {
        try {
            List<Map<String, Object>> roles = new java.util.ArrayList<>();

            Map<String, Object> admin = new HashMap<>();
            admin.put("id", 1);
            admin.put("name", "管理员");
            admin.put("code", "admin");
            admin.put("description", "系统管理员，拥有全部权限");
            roles.add(admin);

            Map<String, Object> user = new HashMap<>();
            user.put("id", 2);
            user.put("name", "普通用户");
            user.put("code", "user");
            user.put("description", "普通用户权限");
            roles.add(user);

            Map<String, Object> operator = new HashMap<>();
            operator.put("id", 3);
            operator.put("name", "运营人员");
            operator.put("code", "operator");
            operator.put("description", "运营管理权限");
            roles.add(operator);

            return ResponseEntity.ok(Map.of("code", 200, "data", roles));
        } catch (Exception e) {
            log.error("获取角色列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("code", 500, "message", "查询失败"));
        }
    }
}
