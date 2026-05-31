package com.beijixing.social.controller;

import com.beijixing.social.entity.AccountGroup;
import com.beijixing.social.service.AccountGroupService;
import com.beijixing.social.vo.ApiResponse;
import com.beijixing.social.vo.GroupRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 账号分组控制器
 */
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class AccountGroupController {

    private final AccountGroupService groupService;

    /** 获取用户的分组列表 */
    @GetMapping("/list")
    public ApiResponse<List<AccountGroup>> listGroups(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @RequestParam(required = false) String platformCode) {
        return ApiResponse.success(groupService.listUserGroups(userId, platformCode));
    }

    /** 创建分组 */
    @PostMapping("/create")
    public ApiResponse<AccountGroup> createGroup(
            @RequestBody @Valid GroupRequestVO request,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ApiResponse.success("分组创建成功", groupService.createGroup(request, userId));
    }

    /** 更新分组 */
    @PostMapping("/update")
    public ApiResponse<String> updateGroup(@RequestBody @Valid GroupRequestVO request) {
        boolean result = groupService.updateGroup(request);
        return result ? ApiResponse.success("分组更新成功") : ApiResponse.fail("更新失败");
    }

    /** 删除分组 */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteGroup(@PathVariable Long id) {
        boolean result = groupService.deleteGroup(id);
        return result ? ApiResponse.success("分组删除成功") : ApiResponse.fail("删除失败");
    }
}
