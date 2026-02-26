package com.jing.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jing.monitor.core.CourseCrawler;
import com.jing.monitor.model.Task;
import com.jing.monitor.model.dto.TaskReqDto;
import com.jing.monitor.model.dto.TaskRespDto;
import com.jing.monitor.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final CourseCrawler crawler;
    private final TaskRepository taskRepository;

    // 1. Get List
    public List<TaskRespDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToResp)
                .collect(Collectors.toList());
    }

    // 2. Toggle Status Logic
    public TaskRespDto toggleTaskStatus(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        // Flip the boolean
        task.setEnabled(!task.isEnabled());

        Task saved = taskRepository.save(task);
        return convertToResp(saved);
    }

    public List<TaskRespDto> SearchAndAdd(String courseName) {
        JsonNode root = crawler.searchCourse(courseName);

        if (root == null || root.path("found").asInt() == 0) {
            throw new RuntimeException("Course not found: " + courseName);
        }

        JsonNode firstHit = root.path("hits").get(0);
        String foundName = firstHit.path("courseDesignation").asText();
        if (!foundName.replace(" ", "").equalsIgnoreCase(courseName.replace(" ", ""))) {
            throw new RuntimeException("Wrong input / Course not found: " + courseName);
        }

        JsonNode reqs = firstHit.path("courseRequirements");
        Iterator<String> fieldNames = reqs.fieldNames();
        List<TaskReqDto> reqDtoList = new ArrayList<>();
        Set<String> sectionIdSet = new LinkedHashSet<>();

        // Traverse all dynamic keys under "courseRequirements" and collect all section IDs.
        while (fieldNames.hasNext()) {
            String dynamicKey = fieldNames.next(); // e.g. "018015=", "018015=100010"
            JsonNode sectionIdArray = reqs.path(dynamicKey);
            if (!sectionIdArray.isArray()) {
                continue;
            }
            for (int i = 0; i < sectionIdArray.size(); i++) {
                sectionIdSet.add(sectionIdArray.get(i).asText());
            }
        }

        for (String sectionId : sectionIdSet) {
            TaskReqDto reqDto = new TaskReqDto();
            log.info("[TaskService] Found section ID: {}", sectionId);
            reqDto.setCourseDisplayName(foundName);
            reqDto.setSectionId(sectionId);
            reqDto.setCourseId(firstHit.path("courseId").asText());
            reqDto.setEnabled(false);
            reqDtoList.add(reqDto);
        }

        return addCourse(reqDtoList);
    }

    // 3. Add new course
    public List<TaskRespDto> addCourse(List<TaskReqDto> reqDtos){
        List<TaskRespDto> respDtos = new ArrayList<>();
        if (reqDtos == null || reqDtos.isEmpty()) {
            return respDtos;
        }

        List<String> sectionIds = reqDtos.stream()
                .map(TaskReqDto::getSectionId)
                .toList();
        Map<String, Task> existingTaskMap = taskRepository.findAllBySectionIdIn(sectionIds).stream()
                .collect(Collectors.toMap(Task::getSectionId, Function.identity()));

        for (TaskReqDto req : reqDtos){
            Task task = existingTaskMap.get(req.getSectionId());
            if (task == null) {
                task = new Task();
                BeanUtils.copyProperties(req, task);
            } else {
                // Idempotent add: keep a single row per unique sectionId and refresh basic metadata.
                task.setCourseDisplayName(req.getCourseDisplayName());
                task.setCourseId(req.getCourseId());
            }
            taskRepository.save(task);
            respDtos.add(convertToResp(task));
        }
        return respDtos;
    }

    // 4. Delete course
    @Transactional
    public void deleteTask(String courseDisplayName) {
        taskRepository.deleteAllByCourseDisplayName(courseDisplayName);
    }

    // Helper: Entity -> DTO
    private TaskRespDto convertToResp(Task task) {
        TaskRespDto resp = new TaskRespDto();
        BeanUtils.copyProperties(task, resp);
        resp.setStatus(task.getLastStatus());
        // Explicit mapping if names differ, but here they match
        return resp;
    }


}
