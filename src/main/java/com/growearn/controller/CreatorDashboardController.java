package com.growearn.controller;


import com.growearn.entity.Campaign;
import com.growearn.entity.CreatorStats;
import com.growearn.security.JwtUtil;
import com.growearn.service.CampaignService;
import com.growearn.service.CreatorStatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/creator/dashboard")
@CrossOrigin(origins = {"http://localhost:5173", "http://192.168.55.104:5173"}, allowCredentials = "true")
public class CreatorDashboardController {
    private final JwtUtil jwtUtil;
    private final CreatorStatsService creatorStatsService;
    private final CampaignService campaignService;
    private static final Logger logger = LoggerFactory.getLogger(CreatorDashboardController.class);

    public CreatorDashboardController(JwtUtil jwtUtil, CreatorStatsService creatorStatsService, CampaignService campaignService) {
        this.jwtUtil = jwtUtil;
        this.creatorStatsService = creatorStatsService;
        this.campaignService = campaignService;
    }

    @GetMapping
    public Map<String, Object> getDashboard(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return Map.of("error", "Unauthorized");
        }
        Long creatorId;
        try {
            creatorId = Long.parseLong(auth.getPrincipal().toString());
        } catch (Exception e) {
            return Map.of("error", "Invalid principal");
        }
        CreatorStats stats = creatorStatsService.getOrCreateStats(creatorId);
        // Defensive: always return 0 if any stat is null
        int subscribers = stats.getSubscribers() != null ? stats.getSubscribers() : 0;
        int videoViews = stats.getVideoViews() != null ? stats.getVideoViews() : 0;
        int shortViews = stats.getShortViews() != null ? stats.getShortViews() : 0;
        int videoLikes = stats.getVideoLikes() != null ? stats.getVideoLikes() : 0;
        int shortLikes = stats.getShortLikes() != null ? stats.getShortLikes() : 0;
        int videoComments = stats.getVideoComments() != null ? stats.getVideoComments() : 0;

        String updatedAt = java.time.Instant.now().toString();

        Map<String, Object> result = Map.of(
            "subscribers", subscribers,
            "video_views", videoViews,
            "short_views", shortViews,
            "video_likes", videoLikes,
            "short_likes", shortLikes,
            "video_comments", videoComments,
            "updatedAt", updatedAt
        );

        logger.info("Creator dashboard for creatorId={} -> subscribers={}, videoViews={}, shortViews={}, videoLikes={}, shortLikes={}, videoComments={}",
            creatorId, subscribers, videoViews, shortViews, videoLikes, shortLikes, videoComments);

        return result;
    }
}
