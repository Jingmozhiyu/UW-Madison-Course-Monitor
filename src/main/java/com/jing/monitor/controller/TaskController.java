package com.jing.monitor.controller;

import com.jing.monitor.common.Result;

import com.jing.monitor.model.dto.TaskRespDto;
import com.jing.monitor.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for authenticated task operations.
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Lists all tasks for the current authenticated user.
     *
     * @return task list response
     */
    @GetMapping
    public Result<List<TaskRespDto>> list() {
        return Result.success(taskService.getAllTasks());
    }

    /**
     * Toggles task enabled state by id.
     *
     * @param id task id
     * @return updated task
     */
    @PatchMapping("/{id}/toggle")
    public Result<TaskRespDto> toggleStatus(@PathVariable Long id) {
        return Result.success(taskService.toggleTaskStatus(id));
    }

    /**
     * Searches a course and adds discovered sections as tasks.
     *
     * @param courseName course keyword from client
     * @return created or updated tasks
     */
    @PostMapping
    public Result<List<TaskRespDto>> searchAndAdd(@RequestParam String courseName){
        return Result.success(taskService.SearchAndAdd(courseName));
    }

    /**
     * Deletes all tasks for a course display name under current user.
     *
     * @param courseDisplayName display name to delete
     * @return empty success result
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam String courseDisplayName){
        taskService.deleteTask(courseDisplayName);
        return Result.success();
    }
}
