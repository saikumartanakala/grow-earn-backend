package com.growearn.repository;

import com.growearn.entity.CreatorStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreatorStatsRepository extends JpaRepository<CreatorStats, Long> {

    Optional<CreatorStats> findByCreatorId(Long creatorId);
}
