// ...existing code...
package com.growearn.service;

import com.growearn.entity.*;
import com.growearn.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.transaction.Transactional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CreatorStatsRepository creatorStatsRepository;
    private final TaskService taskService;

    public CampaignService(
            CampaignRepository campaignRepository,
            UserRepository userRepository,
            CreatorStatsRepository creatorStatsRepository,
            TaskService taskService
    ) {
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.creatorStatsRepository = creatorStatsRepository;
        this.taskService = taskService;
    }


    @Transactional
    public Campaign createCampaign(Campaign campaign) {
        // Ensure creator dashboard/stats entry exists
        creatorStatsRepository.findByCreatorId(campaign.getCreatorId())
            .orElseGet(() -> creatorStatsRepository.save(new CreatorStats(campaign.getCreatorId())));
        // Initialize important runtime fields
        if (campaign.getGoalAmount() == 0.0) campaign.setGoalAmount(campaign.getTotalAmount());
        campaign.setCurrentAmount(0.0);
        campaign.setCurrentSubscribers(0);
        campaign.setCurrentViews(0);
        campaign.setCurrentLikes(0);
        campaign.setCurrentComments(0);
        if (campaign.getUpdatedAt() == null) campaign.setUpdatedAt(java.time.LocalDateTime.now());
        if (campaign.getTitle() == null) campaign.setTitle("");
        if (campaign.getDescription() == null) campaign.setDescription("");
        // Ensure campaign is ACTIVE by default when created
        if (campaign.getStatus() == null || campaign.getStatus().isEmpty()) campaign.setStatus("ACTIVE");
        // Always update updatedAt
        campaign.setUpdatedAt(java.time.LocalDateTime.now());

        // Save campaign (do NOT pre-assign tasks to viewers)
        Campaign savedCampaign = campaignRepository.save(campaign);
        // Create tasks for this campaign
        if (taskService != null) {
            taskService.createTasksForCampaign(savedCampaign);
        }

        return savedCampaign;
    }

    // legacy per-viewer task creation removed; tasks are created globally via TaskService.createTasksForCampaign
    public List<Campaign> getCampaignsByCreatorAndStatus(Long creatorId, String status) {
        return campaignRepository.findByCreatorIdAndStatus(creatorId, status);
    }

    // Expose repository method so controllers can fetch campaigns by creator id
    public List<Campaign> findByCreatorId(Long creatorId) {
        return campaignRepository.findByCreatorId(creatorId);
    }
}
