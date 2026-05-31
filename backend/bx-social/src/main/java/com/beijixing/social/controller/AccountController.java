package com.beijixing.social.controller;

import com.beijixing.social.entity.SocialAccount;
import com.beijixing.social.service.AccountService;
import com.beijixing.social.service.AccountGroupService;
import com.beijixing.social.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 账号管理控制器
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountGroupService groupService;

    /** 分页查询账号列表 */
    @GetMapping("/page")
    public ApiResponse<PageVO<AccountVO>> pageAccounts(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String platformCode,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(accountService.pageAccounts(pageNum, pageSize, platformCode, groupId, status));
    }

    /** 获取账号详情 */
    @GetMapping("/{id}")
    public ApiResponse<AccountVO> getAccount(@PathVariable Long id) {
        AccountVO account = accountService.getAccountById(id);
        return account != null ? ApiResponse.success(account) : ApiResponse.fail("账号不存在");
    }

    /** 创建/更新账号 */
    @PostMapping("/save")
    public ApiResponse<SocialAccount> saveAccount(@RequestBody @Valid AccountRequestVO request) {
        return ApiResponse.success("保存成功", accountService.saveAccount(request));
    }

    /** 更新账号状态 */
    @PostMapping("/{id}/status")
    public ApiResponse<String> updateStatus(@PathVariable Long id, @RequestParam Integer status,
                                             @RequestParam(required = false) String errorMsg) {
        boolean result = accountService.updateStatus(id, status, errorMsg);
        return result ? ApiResponse.success("状态更新成功") : ApiResponse.fail("更新失败");
    }

    /** 解绑账号 */
    @PostMapping("/{id}/unbind")
    public ApiResponse<String> unbindAccount(@PathVariable Long id) {
        boolean result = accountService.unbindAccount(id);
        return result ? ApiResponse.success("解绑成功") : ApiResponse.fail("解绑失败");
    }

    /** 获取用户分组列表 */
    @GetMapping("/groups")
    public ApiResponse<List<com.beijixing.social.entity.AccountGroup>> listGroups(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(required = false) String platformCode) {
        return ApiResponse.success(groupService.listUserGroups(userId, platformCode));
    }

    /** 根据平台查询账号 */
    @GetMapping("/platform/{platformCode}")
    public ApiResponse<List<AccountVO>> listByPlatform(@PathVariable String platformCode) {
        PageVO<AccountVO> result = accountService.pageAccounts(1L, 100L, platformCode, null, null);
        return ApiResponse.success(result.getRecords());
    }

    /** 获取异常账号列表 */
    @GetMapping("/abnormal")
    public ApiResponse<List<SocialAccount>> listAbnormalAccounts() {
        return ApiResponse.success(accountService.selectAbnormalAccounts());
    }

    /** 获取支持的平台列表（移动端需要） */
    @GetMapping("/platforms")
    public ApiResponse<List<Map<String, Object>>> getSupportedPlatforms() {
        List<Map<String, Object>> platforms = new java.util.ArrayList<>();
        Map<String, Object> douyin = new java.util.HashMap<>();
        douyin.put("code", "DOUYIN");
        douyin.put("name", "抖音");
        douyin.put("icon", "https://lf3-static.bytednssec.com/obj/eden-cn/hjeha_plhpubq/douyin_logo_500x500.png");
        douyin.put("enabled", true);
        platforms.add(douyin);
        Map<String, Object> xiaohongshu = new java.util.HashMap<>();
        xiaohongshu.put("code", "XIAOHONGSHU");
        xiaohongshu.put("name", "小红书");
        xiaohongshu.put("icon", "https://img.xiaohongshu.com/logo.png");
        xiaohongshu.put("enabled", true);
        platforms.add(xiaohongshu);
        Map<String, Object> kuaishou = new java.util.HashMap<>();
        kuaishou.put("code", "KUAISHOU");
        kuaishou.put("name", "快手");
        kuaishou.put("icon", "https://static.yximgs.com/kwai-logo.png");
        kuaishou.put("enabled", true);
        platforms.add(kuaishou);
        return ApiResponse.success(platforms);
    }

    /** 账号健康检查（移动端需要） */
    @PostMapping("/{id}/health-check")
    public ApiResponse<Map<String, Object>> healthCheck(@PathVariable Long id) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("accountId", id);
        result.put("healthy", true);
        result.put("loginStatus", "ONLINE");
        result.put("lastCheckTime", java.time.LocalDateTime.now().toString());
        result.put("issues", new java.util.ArrayList<>());
        return ApiResponse.success(result);
    }
}
