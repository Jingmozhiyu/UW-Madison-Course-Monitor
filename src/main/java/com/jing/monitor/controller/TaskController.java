package com.jing.monitor.controller;

import com.jing.monitor.common.Result;
import com.jing.monitor.model.dto.TaskReqDto;
import com.jing.monitor.model.dto.TaskRespDto;
import com.jing.monitor.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*") // 允许 HTML 跨域调试
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public Result<List<TaskRespDto>> list() {
        return Result.success(taskService.getAllTasks());
    }

    @PostMapping
    public Result<TaskRespDto> add(@RequestBody TaskReqDto req) {
        return Result.success(taskService.addTask(req));
    }

    @PutMapping("/{id}")
    public Result<TaskRespDto> update(@PathVariable Long id, @RequestBody TaskReqDto req) {
        return Result.success(taskService.updateTask(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return Result.success(null);
    }
}