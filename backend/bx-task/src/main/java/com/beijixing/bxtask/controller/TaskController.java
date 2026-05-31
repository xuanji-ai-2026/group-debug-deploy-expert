package com.beijixing.bxtask.controller;

import com.beijixing.bxtask.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Map<Long, Map<String, Object>> taskStore = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @GetMapping("/")
    public Result<Map<String, Object>> serviceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "bx-task");
        info.put("version", "1.0.0");
        info.put("status", "RUNNING");
        info.put("endpoints", Arrays.asList(
            "POST /tasks/create-intercept - 创建截客任务",
            "POST /tasks/create-acquire - 创建获客任务",
            "GET /tasks/list - 获取任务列表",
            "GET /tasks/{id} - 获取任务详情",
            "POST /tasks/{id}/start - 启动任务",
            "POST /tasks/{id}/pause - 暂停任务",
            "POST /tasks/{id}/resume - 恢复任务",
            "POST /tasks/{id}/stop - 停止任务",
            "DELETE /tasks/{id} - 删除任务"
        ));
        info.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return Result.success("bx-task服务运行正常", info);
    }

    @PostMapping("/create-intercept")
    public Result<Map<String, Object>> createInterceptTask(@RequestBody Map<String, Object> request) {
        Long taskId = idCounter.getAndIncrement();
        
        Map<String, Object> task = new HashMap<>();
        task.put("taskId", taskId);
        task.put("tenantId", 1L);
        task.put("name", request.getOrDefault("name", "未命名任务"));
        task.put("type", "INTERCEPT");
        task.put("status", "PENDING");
        task.put("platforms", Collections.singletonList(request.getOrDefault("targetPlatform", "DOUYIN")));
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
        
        log.info("截客任务创建成功: taskId={}, name={}", taskId, task.get("name"));
        
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
        
        log.info("获客任务创建成功: taskId={}, name={}", taskId, task.get("name"));
        
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

    /**
     * 获取任务日志（移动端需要）
     */
    @GetMapping("/{id}/logs")
    public Result<Map<String, Object>> getTaskLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Map<String, Object> task = taskStore.get(id);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + id);
        }
        List<Map<String, Object>> logs = new ArrayList<>();
        Map<String, Object> log = new HashMap<>();
        log.put("id", System.currentTimeMillis());
        log.put("taskId", id);
        log.put("content", "任务状态变更: " + task.get("status"));
        log.put("level", "INFO");
        log.put("createdAt", LocalDateTime.now().format(FORMATTER));
        logs.add(log);

        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", 1);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    /**
     * 获取任务统计（移动端需要）
     */
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getTaskStats(@PathVariable Long id) {
        Map<String, Object> task = taskStore.get(id);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + id);
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("taskId", id);
        stats.put("totalCount", task.getOrDefault("totalCount", 0));
        stats.put("completedCount", task.getOrDefault("completedCount", 0));
        stats.put("successCount", task.getOrDefault("successCount", 0));
        stats.put("failCount", task.getOrDefault("failCount", 0));
        stats.put("progress", task.getOrDefault("progress", 0));
        stats.put("status", task.get("status"));
        return Result.success(stats);
    }

    /**
     * 获取任务摘要（移动端需要）
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getTaskSummary() {
        long running = taskStore.values().stream()
                .filter(t -> "RUNNING".equals(t.get("status"))).count();
        long pending = taskStore.values().stream()
                .filter(t -> "PENDING".equals(t.get("status"))).count();
        long completed = taskStore.values().stream()
                .filter(t -> "COMPLETED".equals(t.get("status"))).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTasks", taskStore.size());
        summary.put("runningTasks", running);
        summary.put("pendingTasks", pending);
        summary.put("completedTasks", completed);
        summary.put("todayCompleted", 0);
        summary.put("successRate", taskStore.isEmpty() ? 0 : (double) completed / taskStore.size());
        return Result.success(summary);
    }
}
