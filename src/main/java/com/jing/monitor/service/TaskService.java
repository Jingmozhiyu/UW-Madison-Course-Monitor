package com.jing.monitor.service;

import com.jing.monitor.model.Task;
import com.jing.monitor.model.dto.TaskReqDto;
import com.jing.monitor.model.dto.TaskRespDto;
import com.jing.monitor.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    // 1. 新增任务
    public TaskRespDto addTask(TaskReqDto req) {
        // 1. 直接调用你定义的构造函数，一行搞定所有初始化逻辑
        Task task = new Task(
                req.getSubject(),
                req.getCatalogNumber(),
                req.getSectionId(),
                "004289", // 暂时 Mock 的 courseId
                null      // 新任务初始状态为 null
        );

        // 2. 如果还有 DTO 里有但构造函数里没有的字段（比如 enabled），再手动 set
        if (req.getEnabled() != null) {
            task.setEnabled(req.getEnabled());
        }

        // 3. 保存并返回
        Task saved = taskRepository.save(task);
        return convertToResp(saved);
    }

    // 2. 查询列表
    public List<TaskRespDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    // 3. 更新任务 (PUT)
    public TaskRespDto updateTask(Long id, TaskReqDto req) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // 只允许更新部分字段
        if (req.getSectionId() != null) task.setSectionId(req.getSectionId());
        if (req.getEnabled() != null) task.setEnabled(req.getEnabled());

        // 如果改了 Section，最好把名字也改了
        if (req.getSubject() != null && req.getCatalogNumber() != null) {
            task.setCourseDisplayName(req.getSubject() + " " + req.getCatalogNumber());
        }

        Task updated = taskRepository.save(task);
        return convertToResp(updated);
    }

    // 4. 删除任务
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    // 💡 Helper: Entity -> Output DTO
    private TaskRespDto convertToResp(Task task) {
        TaskRespDto resp = new TaskRespDto();
        BeanUtils.copyProperties(task, resp);
        resp.setStatus(task.getLastStatus()); // 映射 status 字段
        return resp;
    }
}