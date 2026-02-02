package com.growearn.repository;

import com.growearn.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByStatus(String status);
    long countByStatus(String status);
    java.util.List<TaskEntity> findByCampaignIdIn(java.util.List<Long> campaignIds);
}
