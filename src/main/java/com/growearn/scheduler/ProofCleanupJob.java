package com.growearn.scheduler;

import com.growearn.entity.ViewerTaskEntity;
import com.growearn.repository.ViewerTaskEntityRepository;
import com.growearn.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to clean up proof images from Cloudinary after payment + retention period
 * Runs daily and deletes images older than 30 days after payment
 */
@Component
public class ProofCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(ProofCleanupJob.class);
    private static final int RETENTION_DAYS = 30;

    private final ViewerTaskEntityRepository viewerTaskRepository;
    private final CloudinaryService cloudinaryService;

    public ProofCleanupJob(
            ViewerTaskEntityRepository viewerTaskRepository,
            CloudinaryService cloudinaryService) {
        this.viewerTaskRepository = viewerTaskRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Runs daily at 2 AM to clean up old proof images
     * Cron: 0 0 2 * * * (every day at 2:00 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldProofs() {
        logger.info("Running ProofCleanupJob - deleting old proof images");

        try {
            // Find all PAID tasks with proofUrl still present
            List<ViewerTaskEntity> paidTasks = viewerTaskRepository.findByStatus("PAID");
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
            
            int processedCount = 0;
            int deletedCount = 0;
            int skippedCount = 0;

            for (ViewerTaskEntity task : paidTasks) {
                // Check if task has been paid and retention period has passed
                LocalDateTime paidAt = task.getPaidAt();
                if (paidAt != null && paidAt.isBefore(cutoffDate)) {
                    processedCount++;
                    
                    // Check if proof still exists
                    if (task.getProofUrl() != null && !task.getProofUrl().isEmpty()) {
                        String publicId = task.getProofPublicId();
                        
                        if (publicId != null && !publicId.isEmpty()) {
                            try {
                                // Delete image from Cloudinary
                                boolean deleted = cloudinaryService.deleteImage(publicId);
                                
                                if (deleted) {
                                    // Clear proofUrl but keep metadata
                                    task.setProofUrl(null);
                                    viewerTaskRepository.save(task);
                                    deletedCount++;
                                    
                                    logger.info("Deleted proof for task {} (viewer: {}, publicId: {})",
                                              task.getId(), task.getViewerId(), publicId);
                                } else {
                                    skippedCount++;
                                    logger.warn("Failed to delete proof for task {} (publicId: {})",
                                              task.getId(), publicId);
                                }
                            } catch (Exception e) {
                                skippedCount++;
                                logger.error("Error deleting proof for task {}: {}", 
                                           task.getId(), e.getMessage(), e);
                            }
                        } else {
                            // No publicId, just clear the URL
                            task.setProofUrl(null);
                            viewerTaskRepository.save(task);
                            skippedCount++;
                            logger.warn("Task {} has proofUrl but no publicId, clearing URL only", task.getId());
                        }
                    }
                }
            }

            logger.info("ProofCleanupJob completed. Processed: {}, Deleted: {}, Skipped: {}", 
                      processedCount, deletedCount, skippedCount);

        } catch (Exception e) {
            logger.error("Error in ProofCleanupJob: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual cleanup method that can be called via admin endpoint
     * Cleans up proofs for a specific task
     */
    public boolean cleanupTaskProof(Long taskId) {
        try {
            ViewerTaskEntity task = viewerTaskRepository.findById(taskId).orElse(null);
            if (task == null) {
                logger.warn("Task {} not found for cleanup", taskId);
                return false;
            }

            if (!"PAID".equals(task.getStatus())) {
                logger.warn("Task {} is not PAID, cannot cleanup proof", taskId);
                return false;
            }

            String publicId = task.getProofPublicId();
            if (publicId != null && !publicId.isEmpty()) {
                boolean deleted = cloudinaryService.deleteImage(publicId);
                if (deleted) {
                    task.setProofUrl(null);
                    viewerTaskRepository.save(task);
                    logger.info("Manually cleaned up proof for task {}", taskId);
                    return true;
                }
            } else {
                // Just clear URL if no publicId
                task.setProofUrl(null);
                viewerTaskRepository.save(task);
                logger.info("Cleared proofUrl for task {} (no publicId)", taskId);
                return true;
            }

            return false;
        } catch (Exception e) {
            logger.error("Error cleaning up proof for task {}: {}", taskId, e.getMessage(), e);
            return false;
        }
    }
}
