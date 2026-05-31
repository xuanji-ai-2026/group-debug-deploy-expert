package com.beijixing.bxlead.controller;

import com.beijixing.bxlead.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/lead-tasks")
public class LeadTaskController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Map<Long, Map<String, Object>> taskStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @PostMapping("/create-intercept")
    public Result<Map<String, Object>> createInterceptTask(@RequestBody Map<String, Object> request) {
        Long taskId = idCounter.getAndIncrement();
        Map<String, Object> task = new HashMap<>();
        task.put("taskId", taskId);
        task.put("tenantId", 1L);
        task.put("name", request.getOrDefault("name", "未命名任务"));
        task.put("type", "INTERCEPT");
        task.put("status", "PENDING");
        task.put("platforms", request.getOrDefault("targetPlatform", "DOUYIN"));
        task.put("keywords", request.getOrDefault("keywords", Collections.emptyList()));
        task.put("totalCount", 0);
        task.put("completedCount", 0);
        task.put("successCount", 0);
        task.put("failCount", 0);
        task.put("progress", 0);
        task.put("startTime", request.get("startTime"));
        task.put("createTime", LocalDateTime.now().format(FORMATTER));
        task.put("updateTime", LocalDateTime.now().format(FORMATTER));
        taskStore.put(taskId, task);
        return Result.success("截客任务创建成功", task);
    }

    @PostMapping("/create-acquire")
    public Result<Map<String, Object>> createAcquireTask(@RequestBody Map<String, Object> request) {
        Long taskId = idCounter.getAndIncrement();
        Map<String, Object> task = new HashMap<>();
        task.put("taskId", taskId);
        task.put("tenantId", 1L);
        task.put("name", request.getOrDefault("name", "未命名任务"));
        task.put("type", "ACTIVE_CAPTURE");
        task.put("status", "PENDING");
        task.put("platforms", request.getOrDefault("platforms", Collections.emptyList()));
        task.put("keywords", request.getOrDefault("keywords", Collections.emptyList()));
        task.put("totalCount", 0);
        task.put("completedCount", 0);
        task.put("successCount", 0);
        task.put("failCount", 0);
        task.put("progress", 0);
        task.put("startTime", request.get("startTime"));
        task.put("endTime", request.get("endTime"));
        task.put("createTime", LocalDateTime.now().format(FORMATTER));
        task.put("updateTime", LocalDateTime.now().format(FORMATTER));
        taskStore.put(taskId, task);
        return Result.success("获客任务创建成功", task);
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> getTaskList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> allTasks = new ArrayList<>(taskStore.values());
        if (type != null && !type.isEmpty()) {
            allTasks.removeIf(t -> !type.equals(t.get("type")));
        }
        if (status != null && !status.isEmpty()) {
            allTasks.removeIf(t -> !status.equals(t.get("status")));
        }
        int total = allTasks.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pagedTasks = fromIndex < total ?
            allTasks.subList(fromIndex, toIndex) : Collections.emptyList();
        Map<String, Object> result = new HashMap<>();
        result.put("list", pagedTasks);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getTaskDetail(@PathVariable Long id) {
        Map<String, Object> task = taskStore.get(id);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + id);
        }
        return Result.success(task);
    }

    @PostMapping("/{id}/start")
    public Result<Void> startTask(@PathVariable Long id) {
        Map<String, Object> task = taskStore.get(id);
        if (task != null) {
            task.put("status", "RUNNING");
            task.put("startTime", LocalDateTime.now().format(FORMATTER));
            task.put("updateTime", LocalDateTime.now().format(FORMATTER));
        }
        return Result.success("任务已启动");
    }

    @PostMapping("/{id}/pause")
    public Result<Void> pauseTask(@PathVariable Long id) {
        Map<String, Object> task = taskStore.get(id);
        if (task != null) {
            task.put("status", "PAUSED");
            task.put("updateTime", LocalDateTime.now().format(FORMATTER));
        }
        return Result.success("任务已暂停");
    }

    @PostMapping("/{id}/resume")
    public Result<Void> resumeTask(@PathVariable Long id) {
        Map<String, Object> task = taskStore.get(id);
        if (task != null) {
            task.put("status", "RUNNING");
            task.put("updateTime", LocalDateTime.now().format(FORMATTER));
        }
        return Result.success("任务已恢复");
    }

    @PostMapping("/{id}/stop")
    public Result<Void> stopTask(@PathVariable Long id) {
        Map<String, Object> task = taskStore.get(id);
        if (task != null) {
            task.put("status", "COMPLETED");
            task.put("actualEndTime", LocalDateTime.now().format(FORMATTER));
            task.put("updateTime", LocalDateTime.now().format(FORMATTER));
        }
        return Result.success("任务已停止");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        taskStore.remove(id);
        return Result.success("任务已删除");
    }
}