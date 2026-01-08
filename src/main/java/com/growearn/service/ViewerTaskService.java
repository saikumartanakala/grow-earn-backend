package com.growearn.service;

import com.growearn.entity.CreatorStats;
import com.growearn.entity.ViewerTask;
import com.growearn.repository.CreatorStatsRepository;
import com.growearn.repository.ViewerTaskRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ViewerTaskService {

    private final ViewerTaskRepository viewerTaskRepository;
    private final CreatorStatsRepository creatorStatsRepository;

    public ViewerTaskService(
            ViewerTaskRepository viewerTaskRepository,
            CreatorStatsRepository creatorStatsRepository
    ) {
        this.viewerTaskRepository = viewerTaskRepository;
        this.creatorStatsRepository = creatorStatsRepository;
    }

    public void completeTask(Long taskId) {

        ViewerTask task = viewerTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.isCompleted()) {
            return;
        }

        // mark completed
        task.setCompleted(true);
        task.setStatus("COMPLETED");
        viewerTaskRepository.save(task);

        // update creator stats
        CreatorStats stats = creatorStatsRepository
                .findByCreatorId(task.getCreatorId())
                .orElseGet(() -> new CreatorStats(task.getCreatorId()));

        switch (task.getTaskType()) {
            case "SUBSCRIBE" ->
                    stats.setTotalFollowers(stats.getTotalFollowers() + 1);

            case "VIEW" ->
                    stats.setTotalViews(stats.getTotalViews() + 1);

            case "LIKE" ->
                    stats.setTotalLikes(stats.getTotalLikes() + 1);

            case "COMMENT" ->
                    stats.setTotalComments(stats.getTotalComments() + 1);
        }

        creatorStatsRepository.save(stats);
    }
}
