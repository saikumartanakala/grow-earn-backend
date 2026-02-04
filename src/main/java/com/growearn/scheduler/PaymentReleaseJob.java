package com.growearn.scheduler;

import com.growearn.entity.*;
import com.growearn.repository.*;
import com.growearn.service.ViewerTaskFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to automatically release payments for tasks in HOLD status
 * Runs every hour and checks if hold period has expired
 */
@Component
public class PaymentReleaseJob {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReleaseJob.class);

    private final ViewerTaskEntityRepository viewerTaskRepository;
    private final ViewerTaskFlowService viewerTaskFlowService;
    private final TaskRepository taskRepository;
    private final WalletRepository walletRepository;
    private final EarningRepository earningRepository;
    private final CampaignRepository campaignRepository;
    private final CreatorStatsRepository creatorStatsRepository;

    public PaymentReleaseJob(
            ViewerTaskEntityRepository viewerTaskRepository,
            ViewerTaskFlowService viewerTaskFlowService,
            TaskRepository taskRepository,
            WalletRepository walletRepository,
            EarningRepository earningRepository,
            CampaignRepository campaignRepository,
            CreatorStatsRepository creatorStatsRepository) {
        this.viewerTaskRepository = viewerTaskRepository;
        this.viewerTaskFlowService = viewerTaskFlowService;
        this.taskRepository = taskRepository;
        this.walletRepository = walletRepository;
        this.earningRepository = earningRepository;
        this.campaignRepository = campaignRepository;
        this.creatorStatsRepository = creatorStatsRepository;
    }

    /**
     * Runs every hour to check for tasks ready for payment
     * Cron: 0 0 * * * * (every hour at minute 0)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void releaseHeldPayments() {
        logger.info("Running PaymentReleaseJob - checking for tasks ready for payment");

        try {
            // Find all tasks in HOLD status where hold period has expired
            List<ViewerTaskEntity> heldTasks = viewerTaskRepository.findByStatus("HOLD");
            LocalDateTime now = LocalDateTime.now();
            int processedCount = 0;
            int paidCount = 0;

            for (ViewerTaskEntity task : heldTasks) {
                processedCount++;
                
                // Check if hold period has expired
                LocalDateTime holdEndTime = task.getHoldEndTime();
                if (holdEndTime != null && holdEndTime.isBefore(now)) {
                    try {
                        // Mark task as paid and process payment
                        boolean success = markTaskPaid(task);
                        if (success) {
                            paidCount++;
                            logger.info("Released payment for task {} (viewer: {})", 
                                      task.getId(), task.getViewerId());
                        }
                    } catch (Exception e) {
                        logger.error("Error releasing payment for task {}: {}", 
                                   task.getId(), e.getMessage(), e);
                    }
                }
            }

            logger.info("PaymentReleaseJob completed. Processed: {}, Paid: {}", processedCount, paidCount);

        } catch (Exception e) {
            logger.error("Error in PaymentReleaseJob: {}", e.getMessage(), e);
        }
    }

    /**
     * Mark task as paid and credit viewer wallet
     */
    private boolean markTaskPaid(ViewerTaskEntity viewerTask) {
        // Get task details
        TaskEntity task = taskRepository.findById(viewerTask.getTaskId()).orElse(null);
        if (task == null) {
            logger.error("Task {} not found for viewerTask {}", viewerTask.getTaskId(), viewerTask.getId());
            return false;
        }

        double reward = task.getEarning() != null ? task.getEarning() : 0.0;

        // Update task status to PAID
        viewerTask.setStatus("PAID");
        viewerTask.setPaidAt(LocalDateTime.now());
        viewerTask.setPaymentTxnId(generateTxnId());
        viewerTaskRepository.save(viewerTask);

        // Credit viewer wallet
        WalletEntity wallet = walletRepository.findByViewerId(viewerTask.getViewerId())
                .orElseGet(() -> {
                    WalletEntity newWallet = new WalletEntity();
                    newWallet.setViewerId(viewerTask.getViewerId());
                    newWallet.setBalance(0.0);
                    return walletRepository.save(newWallet);
                });
        
        wallet.setBalance(wallet.getBalance() + reward);
        walletRepository.save(wallet);

        // Record earning
        Earning earning = new Earning();
        earning.setViewerId(viewerTask.getViewerId());
        earning.setAmount(reward);
        earning.setDescription("Task completed: " + task.getTaskType() + " - txn: " + viewerTask.getPaymentTxnId());
        earning.setCreatedAt(LocalDateTime.now());
        earningRepository.save(earning);

        // Update campaign and creator stats
        if (task.getCampaignId() != null) {
            updateCampaignStats(task);
        }

        logger.info("Task {} marked as PAID. Credited {} to viewer {}", 
                  viewerTask.getId(), reward, viewerTask.getViewerId());

        return true;
    }

    /**
     * Update campaign stats when task is paid
     */
    private void updateCampaignStats(TaskEntity task) {
        try {
            Campaign campaign = campaignRepository.findById(task.getCampaignId()).orElse(null);
            if (campaign == null) return;

            // Update campaign metrics
            switch (task.getTaskType().toUpperCase()) {
                case "SUBSCRIBE" -> campaign.setCurrentSubscribers(campaign.getCurrentSubscribers() + 1);
                case "VIDEO VIEW", "VIEW" -> campaign.setCurrentViews(campaign.getCurrentViews() + 1);
                case "SHORT VIEW" -> campaign.setCurrentViews(campaign.getCurrentViews() + 1);
                case "VIDEO LIKE", "LIKE" -> campaign.setCurrentLikes(campaign.getCurrentLikes() + 1);
                case "SHORT LIKE" -> campaign.setCurrentLikes(campaign.getCurrentLikes() + 1);
                case "COMMENT" -> campaign.setCurrentComments(campaign.getCurrentComments() + 1);
            }
            
            double reward = task.getEarning() != null ? task.getEarning() : 0.0;
            campaign.setCurrentAmount(campaign.getCurrentAmount() + reward);
            campaignRepository.save(campaign);

            // Update creator stats
            CreatorStats stats = creatorStatsRepository.findByCreatorId(campaign.getCreatorId())
                    .orElseGet(() -> new CreatorStats(campaign.getCreatorId()));
            
            String contentType = campaign.getContentType() != null ? campaign.getContentType() : "VIDEO";
            
            switch (task.getTaskType().toUpperCase()) {
                case "SUBSCRIBE" -> {
                    stats.setTotalFollowers(stats.getTotalFollowers() + 1);
                    stats.setSubscribers(stats.getSubscribers() + 1);
                }
                case "VIDEO VIEW", "VIEW" -> {
                    stats.setTotalViews(stats.getTotalViews() + 1);
                    stats.setVideoViews(stats.getVideoViews() + 1);
                }
                case "SHORT VIEW" -> {
                    stats.setTotalViews(stats.getTotalViews() + 1);
                    stats.setShortViews(stats.getShortViews() + 1);
                }
                case "VIDEO LIKE", "LIKE" -> {
                    stats.setTotalLikes(stats.getTotalLikes() + 1);
                    stats.setVideoLikes(stats.getVideoLikes() + 1);
                }
                case "SHORT LIKE" -> {
                    stats.setTotalLikes(stats.getTotalLikes() + 1);
                    stats.setShortLikes(stats.getShortLikes() + 1);
                }
                case "COMMENT" -> {
                    stats.setTotalComments(stats.getTotalComments() + 1);
                    if ("SHORT".equalsIgnoreCase(contentType)) {
                        stats.setShortComments(stats.getShortComments() + 1);
                    } else {
                        stats.setVideoComments(stats.getVideoComments() + 1);
                    }
                }
            }
            creatorStatsRepository.save(stats);

        } catch (Exception e) {
            logger.error("Error updating campaign stats for task {}: {}", task.getId(), e.getMessage(), e);
        }
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTxnId() {
        return "TXN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }
}
