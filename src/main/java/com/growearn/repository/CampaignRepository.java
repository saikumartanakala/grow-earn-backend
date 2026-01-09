package com.growearn.repository;

import com.growearn.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByCreatorId(Long creatorId);
    List<Campaign> findByCreatorIdAndStatus(Long creatorId, String status);
    List<Campaign> findByStatus(String status);
}
