package com.growearn.controller;

import com.growearn.entity.Campaign;
import com.growearn.security.JwtUtil;
import com.growearn.service.CampaignService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creator/campaign")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CampaignController {

    private final CampaignService service;
    private final JwtUtil jwtUtil;

    public CampaignController(CampaignService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create")
    public Campaign createCampaign(
            @RequestBody Campaign campaign,
            HttpServletRequest request
    ) {
        String token = request.getHeader("Authorization").substring(7);
        Long creatorId = jwtUtil.extractUserId(token);

        campaign.setCreatorId(creatorId);
        return service.createCampaign(campaign);
    }
}
