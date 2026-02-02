package com.growearn.controller;

import com.growearn.entity.Campaign;
import com.growearn.repository.CampaignRepository;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    private final TaskService taskService;
    private final CampaignRepository campaignRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;

    public TestController(TaskService taskService, CampaignRepository campaignRepository,
                          ViewerTaskEntityRepository viewerTaskEntityRepository) {
        this.taskService = taskService;
        this.campaignRepository = campaignRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
    }

    @GetMapping("/api/test")
    public String test() {
        return "Backend is running successfully!";
    }

    @GetMapping("/api/protected")
    public String protectedApi() {
        return "You accessed a protected API!";
    }

    /**
     * Generate tasks for all ACTIVE campaigns that don't have tasks yet.
     * This is a PUBLIC endpoint for testing purposes.
     * GET /api/test/generate-tasks (changed to GET for easy browser access)
     */
    @GetMapping("/api/test/generate-tasks")
    public Map<String, Object> generateTasks() {
        List<Campaign> activeCampaigns = campaignRepository.findByStatus("ACTIVE");
        int generatedCount = 0;
        for (Campaign c : activeCampaigns) {
            if (!taskService.hasTasksForCampaign(c.getId())) {
                taskService.createTasksForCampaign(c);
                generatedCount++;
            }
        }
        return Map.of(
            "message", "Tasks generated successfully",
            "campaignsProcessed", generatedCount,
            "totalActiveCampaigns", activeCampaigns.size(),
            "openTasksCount", taskService.countOpenTasks()
        );
    }

    /**
     * Reset all stuck tasks (ASSIGNED, UNDER_VERIFICATION) back to OPEN.
     * Also clears viewer_tasks entries for those tasks.
     * GET /api/test/reset-tasks (changed to GET for easy browser access)
     */
    @GetMapping("/api/test/reset-tasks")
    public Map<String, Object> resetTasks() {
        // Delete all viewer_task entries that are not completed
        var allViewerTasks = viewerTaskEntityRepository.findAll();
        int deletedViewerTasks = 0;
        for (var vt : allViewerTasks) {
            if (!"COMPLETED".equalsIgnoreCase(vt.getStatus())) {
                viewerTaskEntityRepository.delete(vt);
                deletedViewerTasks++;
            }
        }
        
        // Reset all stuck tasks to OPEN
        int resetCount = taskService.resetStuckTasks();
        
        return Map.of(
            "message", "Tasks reset successfully",
            "tasksReset", resetCount,
            "viewerTasksDeleted", deletedViewerTasks,
            "openTasksCount", taskService.countOpenTasks()
        );
    }
}
