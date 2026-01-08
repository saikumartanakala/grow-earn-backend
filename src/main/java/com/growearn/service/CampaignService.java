package com.growearn.service;

import com.growearn.entity.*;
import com.growearn.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final ViewerTaskRepository viewerTaskRepository;

    public CampaignService(
            CampaignRepository campaignRepository,
            UserRepository userRepository,
            ViewerTaskRepository viewerTaskRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.viewerTaskRepository = viewerTaskRepository;
    }

    @Transactional
    public Campaign createCampaign(Campaign campaign) {

        Campaign savedCampaign = campaignRepository.save(campaign);

        List<User> viewers = userRepository.findByRole(Role.USER);

        for (User viewer : viewers) {

            if (savedCampaign.getSubscriberGoal() > 0) {
                createTask(savedCampaign, viewer, "SUBSCRIBE");
            }
            if (savedCampaign.getViewsGoal() > 0) {
                createTask(savedCampaign, viewer, "VIEW");
            }
            if (savedCampaign.getLikesGoal() > 0) {
                createTask(savedCampaign, viewer, "LIKE");
            }
            if (savedCampaign.getCommentsGoal() > 0) {
                createTask(savedCampaign, viewer, "COMMENT");
            }
        }

        return savedCampaign;
    }

    private void createTask(Campaign campaign, User viewer, String type) {

        ViewerTask task = new ViewerTask();
        task.setCampaignId(campaign.getId());
        task.setCreatorId(campaign.getCreatorId());
        task.setViewerId(viewer.getId());
        task.setTaskType(type);
        task.setStatus("PENDING");
        task.setCompleted(false);

        viewerTaskRepository.save(task);
    }
}
