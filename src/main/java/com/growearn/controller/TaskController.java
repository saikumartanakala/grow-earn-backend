package com.growearn.controller;

import com.growearn.entity.TaskEntity;
import com.growearn.service.ViewerTaskFlowService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ViewerTaskFlowService viewerTaskFlowService;

    public TaskController(ViewerTaskFlowService viewerTaskFlowService) {
        this.viewerTaskFlowService = viewerTaskFlowService;
    }

    @GetMapping("/active")
    public List<TaskEntity> getActiveTasks() {
        return viewerTaskFlowService.getActiveTasks();
    }
}
