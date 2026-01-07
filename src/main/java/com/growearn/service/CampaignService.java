package com.growearn.service;

import com.growearn.entity.Campaign;
import com.growearn.repository.CampaignRepository;
import org.springframework.stereotype.Service;

@Service
public class CampaignService {

    private final CampaignRepository repo;

    public CampaignService(CampaignRepository repo) {
        this.repo = repo;
    }

    public Campaign createCampaign(Campaign campaign) {

        double amount = 0;

        amount += campaign.getSubscriberGoal() * 10;
        amount += campaign.getViewsGoal() * 0.5;
        amount += campaign.getLikesGoal() * 1;
        amount += campaign.getCommentsGoal() * 2;

        campaign.setTotalAmount(amount);
        campaign.setStatus("IN_PROGRESS");

        return repo.save(campaign);
    }
}
