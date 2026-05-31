package com.beijixing.bxlead.service;

import com.beijixing.bxlead.dto.task.CreateAcquireTaskDTO;
import com.beijixing.bxlead.dto.task.CreateInterceptTaskDTO;
import com.beijixing.bxlead.vo.task.TaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class TaskService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Map<Long, TaskVO> taskStore = new HashMap<>();
    private long idCounter = 1;

    public synchronized TaskVO createInterceptTask(CreateInterceptTaskDTO dto) {
        TaskVO task = new TaskVO();
        task.setTaskId(idCounter++);
        task.setTenantId(1L);
        task.setName(dto.getName());
        task.setType("INTERCEPT");
        task.setStatus("PENDING");
        task.setPlatforms(Collections.singletonList(dto.getTargetPlatform()));
        task.setKeywords(dto.getKeywords());
        task.setTotalCount(0);
        task.setCompletedCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setProgress(0);
        task.setStartTime(dto.getStartTime());
        task.setCreateTime(LocalDateTime.now().format(FORMATTER));
        task.setUpdateTime(task.getCreateTime());

        taskStore.put(task.getTaskId(), task);
        log.info("截客任务创建成功: taskId={}, name={}", task.getTaskId(), task.getName());
        return task;
    }

    public synchronized TaskVO createAcquireTask(CreateAcquireTaskDTO dto) {
        TaskVO task = new TaskVO();
        task.setTaskId(idCounter++);
        task.setTenantId(1L);
        task.setName(dto.getName());
        task.setType("ACTIVE_CAPTURE");
        task.setStatus("PENDING");
        task.setPlatforms(dto.getPlatforms());
        task.setKeywords(dto.getKeywords());
        task.setTotalCount(0);
        task.setCompletedCount(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setProgress(0);
        task.setStartTime(dto.getStartTime());
        task.setEndTime(dto.getEndTime());
        task.setCreateTime(LocalDateTime.now().format(FORMATTER));
        task.setUpdateTime(task.getCreateTime());

        taskStore.put(task.getTaskId(), task);
        log.info("获客任务创建成功: taskId={}, name={}", task.getTaskId(), task.getName());
        return task;
    }

    public synchronized Map<String, Object> getTaskList(int page, int size, String type, String status) {
        List<TaskVO> allTasks = new ArrayList<>(taskStore.values());

        if (type != null && !type.isEmpty()) {
            allTasks.removeIf(t -> !type.equals(t.getType()));
        }
        if (status != null && !status.isEmpty()) {
            allTasks.removeIf(t -> !status.equals(t.getStatus()));
        }

        int total = allTasks.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<TaskVO> pagedTasks = fromIndex < total ? allTasks.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", pagedTasks);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        log.info("查询任务列表: total={}", total);
        return result;
    }

    public TaskVO getTaskDetail(Long id) {
        TaskVO task = taskStore.get(id);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + id);
        }
        return task;
    }

    public synchronized void startTask(Long id) {
        TaskVO task = taskStore.get(id);
        if (task != null) {
            task.setStatus("RUNNING");
            task.setStartTime(LocalDateTime.now().format(FORMATTER));
            task.setUpdateTime(task.getStartTime());
            log.info("任务已启动: taskId={}", id);
        }
    }

    public synchronized void pauseTask(Long id) {
        TaskVO task = taskStore.get(id);
        if (task != null) {
            task.setStatus("PAUSED");
            task.setUpdateTime(LocalDateTime.now().format(FORMATTER));
            log.info("任务已暂停: taskId={}", id);
        }
    }

    public synchronized void resumeTask(Long id) {
        TaskVO task = taskStore.get(id);
        if (task != null) {
            task.setStatus("RUNNING");
            task.setUpdateTime(LocalDateTime.now().format(FORMATTER));
            log.info("任务已恢复: taskId={}", id);
        }
    }

    public synchronized void stopTask(Long id) {
        TaskVO task = taskStore.get(id);
        if (task != null) {
            task.setStatus("COMPLETED");
            task.setActualEndTime(LocalDateTime.now().format(FORMATTER));
            task.setUpdateTime(task.getActualEndTime());
            log.info("任务已停止: taskId={}", id);
        }
    }

    public synchronized void deleteTask(Long id) {
        taskStore.remove(id);
        log.info("任务已删除: taskId={}", id);
    }
}
