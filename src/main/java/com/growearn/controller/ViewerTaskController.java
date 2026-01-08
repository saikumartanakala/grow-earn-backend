package com.growearn.controller;

import com.growearn.entity.ViewerTask;
import com.growearn.repository.ViewerTaskRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/viewer/tasks")
@CrossOrigin(origins = "*")
public class ViewerTaskController {

    private final ViewerTaskRepository viewerTaskRepository;

    public ViewerTaskController(ViewerTaskRepository viewerTaskRepository) {
        this.viewerTaskRepository = viewerTaskRepository;
    }

    // ✅ THIS IS THE KEY FIX
    @GetMapping
    public List<ViewerTask> getAllPendingTasks() {
        return viewerTaskRepository.findAllPendingTasks();
    }
}
