package com.beijixing.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.social.entity.AccountDevice;
import com.beijixing.social.mapper.AccountDeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备服务
 */
@Service
@RequiredArgsConstructor
public class DeviceService extends ServiceImpl<AccountDeviceMapper, AccountDevice> {

    /** 根据设备ID查询 */
    public AccountDevice getByDeviceId(String deviceId) {
        LambdaQueryWrapper<AccountDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountDevice::getDeviceId, deviceId);
        return getOne(wrapper);
    }

    /** 查询账号的设备列表 */
    public List<AccountDevice> listByAccountId(Long accountId) {
        LambdaQueryWrapper<AccountDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountDevice::getAccountId, accountId);
        wrapper.orderByDesc(AccountDevice::getLastUsedTime);
        return list(wrapper);
    }

    /** 注册设备 */
    public AccountDevice registerDevice(String deviceId, String deviceName, String deviceType,
                                        String phoneModel, String osVersion, String ipAddress) {
        AccountDevice device = getByDeviceId(deviceId);
        if (device == null) {
            device = new AccountDevice();
            device.setDeviceId(deviceId);
            device.setStatus(0);
        }
        device.setDeviceName(deviceName);
        device.setDeviceType(deviceType);
        device.setPhoneModel(phoneModel);
        device.setOsVersion(osVersion);
        device.setIpAddress(ipAddress);
        device.setLastUsedTime(LocalDateTime.now());
        saveOrUpdate(device);
        return device;
    }

    /** 占用设备 */
    public boolean occupyDevice(String deviceId, Long accountId) {
        AccountDevice device = getByDeviceId(deviceId);
        if (device == null) return false;
        device.setAccountId(accountId);
        device.setStatus(1);
        device.setLastUsedTime(LocalDateTime.now());
        return updateById(device);
    }

    /** 释放设备 */
    public boolean releaseDevice(String deviceId) {
        AccountDevice device = getByDeviceId(deviceId);
        if (device == null) return false;
        device.setStatus(0);
        return updateById(device);
    }

    /** 查询空闲设备 */
    public List<AccountDevice> listFreeDevices() {
        LambdaQueryWrapper<AccountDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountDevice::getStatus, 0);
        return list(wrapper);
    }
}
