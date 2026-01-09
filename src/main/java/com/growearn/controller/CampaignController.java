package com.growearn.controller;

import com.growearn.entity.Campaign;
import com.growearn.service.CampaignService;
import com.growearn.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/creator/campaign", "/api/creator/campaigns"})
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CampaignController {

    private final CampaignService campaignService;
    private final JwtUtil jwtUtil;

    public CampaignController(CampaignService campaignService, JwtUtil jwtUtil) {
        this.campaignService = campaignService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCampaign(@RequestBody Campaign campaign, HttpServletRequest request) {
        // Improved validation using isNullOrEmpty and <= 0 checks, including videoDuration for VIDEO/SHORT
        StringBuilder missingFields = new StringBuilder();
        if (isNullOrEmpty(campaign.getPlatform())) missingFields.append("platform, ");
        if (isNullOrEmpty(campaign.getGoalType())) missingFields.append("goalType, ");
        if (isNullOrEmpty(campaign.getChannelName())) missingFields.append("channelName, ");
        if (isNullOrEmpty(campaign.getChannelLink())) missingFields.append("channelLink, ");
        if (isNullOrEmpty(campaign.getContentType())) missingFields.append("contentType, ");
        if (isNullOrEmpty(campaign.getVideoLink())) missingFields.append("videoLink, ");
        if (campaign.getSubscriberGoal() <= 0) missingFields.append("subscriberGoal, ");
        if (campaign.getViewsGoal() <= 0) missingFields.append("viewsGoal, ");
        if (campaign.getLikesGoal() <= 0) missingFields.append("likesGoal, ");
        if (campaign.getCommentsGoal() <= 0) missingFields.append("commentsGoal, ");
        if (campaign.getTotalAmount() <= 0.0) missingFields.append("totalAmount, ");
        if (isNullOrEmpty(campaign.getStatus())) missingFields.append("status, ");
        if (("VIDEO".equalsIgnoreCase(campaign.getContentType()) || "SHORT".equalsIgnoreCase(campaign.getContentType())) && isNullOrEmpty(campaign.getVideoDuration())) missingFields.append("videoDuration, ");
        if (missingFields.length() > 0) {
            String missing = missingFields.substring(0, missingFields.length() - 2);
            System.out.println("[CampaignController] Missing required campaign fields: " + missing + " | Campaign: " + campaign);
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Missing required fields: " + missing));
        }

        // Log campaign creation attempt
        System.out.println("[CampaignController] Attempting to create campaign: " + campaign);

        // Assign creatorId from JWT
        String token = request.getHeader("Authorization").substring(7);
        Long creatorId = jwtUtil.extractUserId(token);
        campaign.setCreatorId(creatorId);

        try {
            Campaign saved = campaignService.createCampaign(campaign);
            System.out.println("[CampaignController] Campaign created for creatorId: " + creatorId + ", campaignId: " + (saved != null ? saved.getId() : "null"));
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.out.println("[CampaignController] Failed to create campaign: " + e.getMessage());
            return ResponseEntity.status(400).body("Failed to create campaign: " + e.getMessage());
        }
    }

    // Helper method for validation
    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCampaignsByStatus(@RequestParam("status") String status) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("[CampaignController] Principal from SecurityContext (GET): " + principal + " (" + (principal != null ? principal.getClass().getName() : "null") + ")");
        Long creatorId = null;
        try {
            if (principal instanceof Long) {
                creatorId = (Long) principal;
            } else if (principal instanceof String) {
                creatorId = Long.parseLong((String) principal);
            }
        } catch (Exception e) {
            System.out.println("[CampaignController] Error parsing principal (GET): " + e.getMessage());
            return ResponseEntity.status(403).body("Invalid JWT principal: " + principal);
        }
        if (creatorId == null) {
            System.out.println("[CampaignController] Missing or invalid creatorId in JWT principal. (GET)");
            return ResponseEntity.status(403).body("Missing or invalid creatorId in JWT principal.");
        }
        String normalizedStatus = status.equalsIgnoreCase("in-progress") ? "IN_PROGRESS" : status.equalsIgnoreCase("completed") ? "COMPLETED" : status.toUpperCase();
        return ResponseEntity.ok(campaignService.getCampaignsByCreatorAndStatus(creatorId, normalizedStatus));
    }

    // Support /api/creator/campaigns?status=... and /api/creator/campaign?status=...
    @GetMapping("")
    public ResponseEntity<?> getCampaignsByStatusRoot(@RequestParam("status") String status) {
        return getCampaignsByStatus(status);
    }

    @GetMapping("/")
    public ResponseEntity<?> getCampaignsByStatusRootSlash(@RequestParam("status") String status) {
        return getCampaignsByStatus(status);
    }
}