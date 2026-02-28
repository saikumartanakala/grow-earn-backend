package com.growearn.repository;

import com.growearn.entity.ViewerTaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ViewerTaskEntityRepository extends JpaRepository<ViewerTaskEntity, Long> {
    List<ViewerTaskEntity> findByStatus(String status);
    Page<ViewerTaskEntity> findByStatus(String status, Pageable pageable);
    
    // Find tasks by status ordered by submitted date DESC (newest first)
    List<ViewerTaskEntity> findByStatusOrderBySubmittedAtDesc(String status);
    
    List<ViewerTaskEntity> findByViewerId(Long viewerId);
    java.util.Optional<ViewerTaskEntity> findByTaskIdAndViewerId(Long taskId, Long viewerId);
    long countByViewerIdAndStatus(Long viewerId, String status);
    
    // Find completed tasks for a specific viewer, ordered by completion date
    List<ViewerTaskEntity> findByViewerIdAndStatusOrderByCompletedAtDesc(Long viewerId, String status);
    
    // Find tasks by viewer and status
    List<ViewerTaskEntity> findByViewerIdAndStatus(Long viewerId, String status);
    
    // Find viewer tasks by task IDs and status (for creator to see tasks related to their campaigns)
    List<ViewerTaskEntity> findByTaskIdInAndStatus(List<Long> taskIds, String status);
    
    // Find viewer tasks by task IDs (all statuses)
    List<ViewerTaskEntity> findByTaskIdIn(List<Long> taskIds);

    @Modifying
    @Transactional
    @Query("""
        UPDATE ViewerTaskEntity v
        SET v.proofUrl = :proofUrl,
            v.proofPublicId = :proofPublicId,
            v.proofText = :proofText,
            v.proof = :proof,
            v.deviceFingerprint = :deviceFingerprint,
            v.ipAddress = :ipAddress,
            v.submittedAt = :submittedAt,
            v.proofSubmitted = :proofSubmitted,
            v.watchSeconds = :watchSeconds,
            v.status = :status,
            v.taskType = :taskType,
            v.rejectionReason = :rejectionReason,
            v.riskScore = :riskScore,
            v.autoFlag = :autoFlag
        WHERE v.taskId = :taskId AND v.viewerId = :viewerId
        """)
    int updateProofSubmission(@Param("taskId") Long taskId,
                              @Param("viewerId") Long viewerId,
                              @Param("proofUrl") String proofUrl,
                              @Param("proofPublicId") String proofPublicId,
                              @Param("proofText") String proofText,
                              @Param("proof") String proof,
                              @Param("deviceFingerprint") String deviceFingerprint,
                              @Param("ipAddress") String ipAddress,
                              @Param("submittedAt") java.time.LocalDateTime submittedAt,
                              @Param("proofSubmitted") Boolean proofSubmitted,
                              @Param("watchSeconds") Integer watchSeconds,
                              @Param("status") String status,
                              @Param("taskType") String taskType,
                              @Param("rejectionReason") String rejectionReason,
                              @Param("riskScore") Double riskScore,
                              @Param("autoFlag") Boolean autoFlag);
}
