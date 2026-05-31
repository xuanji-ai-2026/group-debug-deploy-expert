package com.beijixing.schedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.schedule.entity.ScheduleJobRegistry;
import com.beijixing.schedule.mapper.ScheduleJobRegistryMapper;
import com.beijixing.schedule.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/job/registry")
public class JobRegistryController {

    @Autowired
    private ScheduleJobRegistryMapper registryMapper;

    @Value("${xxl.job.executor.appname:bx-schedule-executor}")
    private String appName;

    /**
     * 查询执行器列表
     */
    @GetMapping("/list")
    public Result<List<ScheduleJobRegistry>> listExecutors() {
        try {
            List<ScheduleJobRegistry> registries = registryMapper.selectOnlineRegistries(appName);
            return Result.success(registries);
        } catch (Exception e) {
            log.error("查询执行器列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 注册执行器
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody ScheduleJobRegistry registry) {
        try {
            // 查询是否已存在
            LambdaQueryWrapper<ScheduleJobRegistry> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ScheduleJobRegistry::getAppName, registry.getAppName())
                    .eq(ScheduleJobRegistry::getAddress, registry.getAddress());
            ScheduleJobRegistry existing = registryMapper.selectOne(wrapper);

            if (existing != null) {
                // 更新
                existing.setRegistryTime(LocalDateTime.now());
                existing.setUpdateTime(LocalDateTime.now());
                existing.setOnline(1);
                registryMapper.updateById(existing);
            } else {
                // 新增
                registry.setRegistryTime(LocalDateTime.now());
                registry.setUpdateTime(LocalDateTime.now());
                registry.setOnline(1);
                registryMapper.insert(registry);
            }

            return Result.success("执行器注册成功");
        } catch (Exception e) {
            log.error("执行器注册失败", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 心跳检测
     */
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@RequestParam String address) {
        try {
            LambdaQueryWrapper<ScheduleJobRegistry> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ScheduleJobRegistry::getAppName, appName)
                    .eq(ScheduleJobRegistry::getAddress, address);
            ScheduleJobRegistry registry = registryMapper.selectOne(wrapper);

            if (registry != null) {
                registry.setUpdateTime(LocalDateTime.now());
                registry.setOnline(1);
                registryMapper.updateById(registry);
            }

            return Result.success("心跳成功");
        } catch (Exception e) {
            log.error("心跳检测失败", e);
            return Result.error("心跳失败: " + e.getMessage());
        }
    }

    /**
     * 下线执行器
     */
    @PostMapping("/offline")
    public Result<Void> offline(@RequestParam String address) {
        try {
            LambdaQueryWrapper<ScheduleJobRegistry> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ScheduleJobRegistry::getAppName, appName)
                    .eq(ScheduleJobRegistry::getAddress, address);
            ScheduleJobRegistry registry = registryMapper.selectOne(wrapper);

            if (registry != null) {
                registry.setOnline(0);
                registry.setUpdateTime(LocalDateTime.now());
                registryMapper.updateById(registry);
            }

            return Result.success("执行器已下线");
        } catch (Exception e) {
            log.error("执行器下线失败", e);
            return Result.error("下线失败: " + e.getMessage());
        }
    }
}
