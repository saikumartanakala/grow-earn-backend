package com.growearn.service;

import com.growearn.entity.Campaign;
import com.growearn.entity.TaskEntity;
import com.growearn.entity.ViewerTaskEntity;
import com.growearn.repository.CampaignRepository;
import com.growearn.repository.TaskRepository;
import com.growearn.repository.ViewerTaskEntityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TaskTimerService {

    private final TaskRepository taskRepository;
    private final CampaignRepository campaignRepository;
    private final ViewerTaskEntityRepository viewerTaskEntityRepository;

    public TaskTimerService(TaskRepository taskRepository,
                            CampaignRepository campaignRepository,
                            ViewerTaskEntityRepository viewerTaskEntityRepository) {
        this.taskRepository = taskRepository;
        this.campaignRepository = campaignRepository;
        this.viewerTaskEntityRepository = viewerTaskEntityRepository;
    }

    public Map<String, Object> startTimer(Long taskId, Long viewerId) {
        TaskEntity task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));

        ViewerTaskEntity viewerTask = viewerTaskEntityRepository.findByTaskIdAndViewerId(taskId, viewerId)
            .orElseGet(() -> {
                ViewerTaskEntity created = new ViewerTaskEntity();
                created.setTaskId(taskId);
                created.setViewerId(viewerId);
                created.setAssignedAt(LocalDateTime.now());
                created.setStatus("ASSIGNED");
                created.setTaskType(task.getTaskType());
                return created;
            });

        if (viewerTask.getTaskStartTime() != null && viewerTask.getTaskUnlockTime() != null) {
            int requiredWatchSeconds = calculateRequiredWatchSeconds(task);
            return Map.of(
                "unlockTime", viewerTask.getTaskUnlockTime(),
                "requiredWatchSeconds", requiredWatchSeconds
            );
        }

        int requiredWatchSeconds = calculateRequiredWatchSeconds(task);
        LocalDateTime now = LocalDateTime.now();
        viewerTask.setTaskStartTime(now);
        viewerTask.setTaskUnlockTime(now.plusSeconds(requiredWatchSeconds));
        viewerTaskEntityRepository.save(viewerTask);

        return Map.of(
            "unlockTime", viewerTask.getTaskUnlockTime(),
            "requiredWatchSeconds", requiredWatchSeconds
        );
    }

    private int calculateRequiredWatchSeconds(TaskEntity task) {
        if (task == null || task.getCampaignId() == null) {
            return 30;
        }
        Campaign campaign = campaignRepository.findById(task.getCampaignId())
            .orElse(null);
        if (campaign == null) {
            return 30;
        }
        Integer duration = campaign.getContentDuration();
        if (duration == null || duration <= 0) {
            return 30;
        }
        String platformType = campaign.getPlatformType() != null
            ? campaign.getPlatformType().trim().toLowerCase()
            : "";
        if (platformType.isEmpty()) {
            return duration;
        }
        return switch (platformType) {
            case "youtube_video", "facebook_video", "twitter_video",
                 "youtube_shorts", "instagram_reel" -> duration;
            default -> duration;
        };
    }
}

