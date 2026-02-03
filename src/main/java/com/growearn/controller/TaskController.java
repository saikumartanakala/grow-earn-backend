package com.growearn.controller;

import com.growearn.entity.TaskEntity;
import com.growearn.service.ViewerTaskFlowService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
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
