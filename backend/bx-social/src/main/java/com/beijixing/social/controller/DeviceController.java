package com.beijixing.social.controller;

import com.beijixing.social.entity.AccountDevice;
import com.beijixing.social.service.DeviceService;
import com.beijixing.social.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 设备管理控制器
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /** 注册设备 */
    @PostMapping("/register")
    public ApiResponse<AccountDevice> registerDevice(
            @RequestParam String deviceId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String phoneModel,
            @RequestParam(required = false) String osVersion,
            @RequestParam(required = false) String ipAddress) {
        AccountDevice device = deviceService.registerDevice(deviceId, deviceName, deviceType, phoneModel, osVersion, ipAddress);
        return ApiResponse.success(device);
    }

    /** 获取设备信息 */
    @GetMapping("/{deviceId}")
    public ApiResponse<AccountDevice> getDevice(@PathVariable String deviceId) {
        AccountDevice device = deviceService.getByDeviceId(deviceId);
        return device != null ? ApiResponse.success(device) : ApiResponse.fail("设备不存在");
    }

    /** 查询账号的设备列表 */
    @GetMapping("/account/{accountId}")
    public ApiResponse<List<AccountDevice>> listByAccount(@PathVariable Long accountId) {
        return ApiResponse.success(deviceService.listByAccountId(accountId));
    }

    /** 占用设备 */
    @PostMapping("/occupy")
    public ApiResponse<String> occupyDevice(
            @RequestParam String deviceId,
            @RequestParam Long accountId) {
        boolean result = deviceService.occupyDevice(deviceId, accountId);
        return result ? ApiResponse.success("设备占用成功") : ApiResponse.fail("设备占用失败");
    }

    /** 释放设备 */
    @PostMapping("/release")
    public ApiResponse<String> releaseDevice(@RequestParam String deviceId) {
        boolean result = deviceService.releaseDevice(deviceId);
        return result ? ApiResponse.success("设备释放成功") : ApiResponse.fail("设备释放失败");
    }

    /** 获取空闲设备列表 */
    @GetMapping("/free")
    public ApiResponse<List<AccountDevice>> listFreeDevices() {
        return ApiResponse.success(deviceService.listFreeDevices());
    }
}
