package com.growearn.controller;

import com.growearn.entity.Campaign;
import com.growearn.service.CampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creator/campaign")
@CrossOrigin(origins = "*")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping("/create")
    public ResponseEntity<Campaign> createCampaign(
            @RequestBody Campaign campaign
    ) {

        // ✅ Extract creatorId from JWT (JwtFilter sets userId as principal)
        Object principal =
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal();

        Long creatorId;

        if (principal instanceof Long) {
            creatorId = (Long) principal;
        } else {
            throw new RuntimeException("Invalid JWT principal");
        }

        // ✅ VERY IMPORTANT
        campaign.setCreatorId(creatorId);

        Campaign saved = campaignService.createCampaign(campaign);

        return ResponseEntity.ok(saved);
    }
}
