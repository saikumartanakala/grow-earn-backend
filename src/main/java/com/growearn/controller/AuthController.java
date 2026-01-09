package com.growearn.controller;

import com.growearn.entity.Role;
import com.growearn.entity.User;
import com.growearn.repository.UserRepository;
import com.growearn.repository.CampaignRepository;
import com.growearn.repository.CreatorStatsRepository;
import com.growearn.repository.ViewerTaskRepository;
import com.growearn.entity.CreatorStats;
import com.growearn.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import com.growearn.entity.Campaign;
import com.growearn.entity.ViewerTask;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")

public class AuthController {
        private final UserRepository userRepo;
        private final JwtUtil jwtUtil;
        private final PasswordEncoder passwordEncoder;
        private final CreatorStatsRepository creatorStatsRepo;
        private final ViewerTaskRepository viewerTaskRepo;
        private final CampaignRepository campaignRepo;

                public AuthController(
                        UserRepository userRepo,
                        JwtUtil jwtUtil,
                        PasswordEncoder passwordEncoder,
                        CreatorStatsRepository creatorStatsRepo,
                        ViewerTaskRepository viewerTaskRepo,
                        CampaignRepository campaignRepo
                ) {
                        this.userRepo = userRepo;
                        this.jwtUtil = jwtUtil;
                        this.passwordEncoder = passwordEncoder;
                        this.creatorStatsRepo = creatorStatsRepo;
                        this.viewerTaskRepo = viewerTaskRepo;
                        this.campaignRepo = campaignRepo;
                }

    // ✅ CHECK EMAIL + ROLE (PUBLIC)
    @PostMapping("/check-email-role")
    public ResponseEntity<?> checkEmailRole(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String roleStr = req.get("role");

        Role role = Role.valueOf(roleStr.toUpperCase());

        boolean exists =
                userRepo.findByEmailAndRole(email, role).isPresent();

        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ✅ SIGNUP
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");
        Role role = Role.valueOf(req.get("role").toUpperCase());

        if (userRepo.findByEmailAndRole(email, role).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Account already exists"));
        }

        User user = new User(
                email,
                passwordEncoder.encode(password),
                role
        );

                userRepo.save(user);

                // Auto-create CreatorStats for new creators
                if (role == Role.CREATOR) {
                        // Only create if not exists
                        creatorStatsRepo.findByCreatorId(user.getId())
                                .orElseGet(() -> creatorStatsRepo.save(new CreatorStats(user.getId())));
                }

                // Assign tasks to new viewers for all IN_PROGRESS campaigns
                if (role == Role.USER) {
                        List<Campaign> inProgressCampaigns = campaignRepo.findByStatus("IN_PROGRESS");
                        for (Campaign campaign : inProgressCampaigns) {
                                if (campaign.getSubscriberGoal() > 0) {
                                        ViewerTask subscribeTask = new ViewerTask();
                                        subscribeTask.setCampaignId(campaign.getId());
                                        subscribeTask.setCreatorId(campaign.getCreatorId());
                                        subscribeTask.setViewerId(user.getId());
                                        subscribeTask.setTaskType("SUBSCRIBE");
                                        subscribeTask.setStatus("PENDING");
                                        subscribeTask.setCompleted(false);
                                        subscribeTask.setTargetLink(campaign.getChannelLink());
                                        viewerTaskRepo.save(subscribeTask);
                                }
                                if (campaign.getViewsGoal() > 0) {
                                        ViewerTask viewTask = new ViewerTask();
                                        viewTask.setCampaignId(campaign.getId());
                                        viewTask.setCreatorId(campaign.getCreatorId());
                                        viewTask.setViewerId(user.getId());
                                        viewTask.setTaskType("VIEW");
                                        viewTask.setStatus("PENDING");
                                        viewTask.setCompleted(false);
                                        viewTask.setTargetLink(campaign.getVideoLink());
                                        viewerTaskRepo.save(viewTask);
                                }
                                if (campaign.getLikesGoal() > 0) {
                                        ViewerTask likeTask = new ViewerTask();
                                        likeTask.setCampaignId(campaign.getId());
                                        likeTask.setCreatorId(campaign.getCreatorId());
                                        likeTask.setViewerId(user.getId());
                                        likeTask.setTaskType("LIKE");
                                        likeTask.setStatus("PENDING");
                                        likeTask.setCompleted(false);
                                        likeTask.setTargetLink(campaign.getVideoLink());
                                        viewerTaskRepo.save(likeTask);
                                }
                                if (campaign.getCommentsGoal() > 0) {
                                        ViewerTask commentTask = new ViewerTask();
                                        commentTask.setCampaignId(campaign.getId());
                                        commentTask.setCreatorId(campaign.getCreatorId());
                                        commentTask.setViewerId(user.getId());
                                        commentTask.setTaskType("COMMENT");
                                        commentTask.setStatus("PENDING");
                                        commentTask.setCompleted(false);
                                        commentTask.setTargetLink(campaign.getVideoLink());
                                        viewerTaskRepo.save(commentTask);
                                }
                        }
                }

                String token = jwtUtil.generateToken(user.getId(), role.name());

                return ResponseEntity.ok(
                                Map.of("token", token, "role", role.name())
                );
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {

        String email = req.get("email");
        String password = req.get("password");
        Role role = Role.valueOf(req.get("role").toUpperCase());

        User user = userRepo.findByEmailAndRole(email, role)
                .orElse(null);

        if (user == null ||
                !passwordEncoder.matches(password, user.getPassword())) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getId(), role.name());

        return ResponseEntity.ok(
                Map.of("token", token, "role", role.name())
        );
    }
}
