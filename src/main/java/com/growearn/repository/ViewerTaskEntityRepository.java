package com.growearn.repository;

import com.growearn.entity.ViewerTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ViewerTaskEntityRepository extends JpaRepository<ViewerTaskEntity, Long> {
    List<ViewerTaskEntity> findByStatus(String status);
    List<ViewerTaskEntity> findByViewerId(Long viewerId);
    java.util.Optional<ViewerTaskEntity> findByTaskIdAndViewerId(Long taskId, Long viewerId);
    long countByViewerIdAndStatus(Long viewerId, String status);
    
    // Find completed tasks for a specific viewer, ordered by completion date
    List<ViewerTaskEntity> findByViewerIdAndStatusOrderByCompletedAtDesc(Long viewerId, String status);
    
    // Find tasks by viewer and status
    List<ViewerTaskEntity> findByViewerIdAndStatus(Long viewerId, String status);
}
