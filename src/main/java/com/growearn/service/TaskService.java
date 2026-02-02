package com.growearn.service;

import com.growearn.entity.Campaign;
import com.growearn.entity.TaskEntity;
import com.growearn.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("taskService")
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void createTasksForCampaign(Campaign c) {
        // create multiple tasks based on campaign goals
        // Each viewer gets one task, so create as many tasks as the goal requires
        double rewardPerTask = calculateReward(c);
        
        // Create subscriber tasks (one per subscriber goal)
        int subCount = Math.max(1, c.getSubscriberGoal());
        if (c.getSubscriberGoal() > 0) {
            for (int i = 0; i < subCount; i++) {
                TaskEntity t = new TaskEntity();
                t.setCampaignId(c.getId());
                t.setTaskType("SUBSCRIBE");
                t.setTargetLink(c.getChannelLink());
                t.setEarning(rewardPerTask);
                t.setStatus("OPEN");
                taskRepository.save(t);
            }
        }
        
        // Create view tasks
        int viewCount = Math.max(1, c.getViewsGoal());
        if (c.getViewsGoal() > 0) {
            for (int i = 0; i < viewCount; i++) {
                TaskEntity t = new TaskEntity();
                t.setCampaignId(c.getId());
                t.setTaskType("VIEW");
                t.setTargetLink(c.getVideoLink());
                t.setEarning(rewardPerTask);
                t.setStatus("OPEN");
                taskRepository.save(t);
            }
        }
        
        // Create like tasks
        int likeCount = Math.max(1, c.getLikesGoal());
        if (c.getLikesGoal() > 0) {
            for (int i = 0; i < likeCount; i++) {
                TaskEntity t = new TaskEntity();
                t.setCampaignId(c.getId());
                t.setTaskType("LIKE");
                t.setTargetLink(c.getVideoLink());
                t.setEarning(rewardPerTask);
                t.setStatus("OPEN");
                taskRepository.save(t);
            }
        }
        
        // Create comment tasks
        int commentCount = Math.max(1, c.getCommentsGoal());
        if (c.getCommentsGoal() > 0) {
            for (int i = 0; i < commentCount; i++) {
                TaskEntity t = new TaskEntity();
                t.setCampaignId(c.getId());
                t.setTaskType("COMMENT");
                t.setTargetLink(c.getVideoLink());
                t.setEarning(rewardPerTask);
                t.setStatus("OPEN");
                taskRepository.save(t);
            }
        }
    }

    private double calculateReward(Campaign c) {
        int totalUnits = c.getSubscriberGoal() + c.getViewsGoal() + c.getLikesGoal() + c.getCommentsGoal();
        if (totalUnits <= 0 || c.getTotalAmount() <= 0) return 0.0;
        return Math.round((c.getTotalAmount() / (double) totalUnits) * 100.0) / 100.0;
    }

    /**
     * Check if tasks already exist for a campaign
     */
    public boolean hasTasksForCampaign(Long campaignId) {
        return taskRepository.findByCampaignIdIn(java.util.List.of(campaignId)).size() > 0;
    }

    /**
     * Get count of open tasks
     */
    public long countOpenTasks() {
        return taskRepository.countByStatus("OPEN");
    }

    /**
     * Reset all non-completed tasks back to OPEN status.
     * This is useful for testing when tasks get stuck.
     */
    @Transactional
    public int resetStuckTasks() {
        java.util.List<TaskEntity> stuckTasks = taskRepository.findAll();
        int resetCount = 0;
        for (TaskEntity t : stuckTasks) {
            if (!"OPEN".equalsIgnoreCase(t.getStatus()) && 
                !"COMPLETED".equalsIgnoreCase(t.getStatus())) {
                t.setStatus("OPEN");
                taskRepository.save(t);
                resetCount++;
            }
        }
        return resetCount;
    }
}
