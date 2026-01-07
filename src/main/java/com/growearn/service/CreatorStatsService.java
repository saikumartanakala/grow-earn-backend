package com.growearn.service;

import com.growearn.entity.CreatorStats;
import com.growearn.repository.CreatorStatsRepository;
import org.springframework.stereotype.Service;

@Service
public class CreatorStatsService {

    private final CreatorStatsRepository repo;

    public CreatorStatsService(CreatorStatsRepository repo) {
        this.repo = repo;
    }

    public CreatorStats getOrCreateStats(Long creatorId) {
        return repo.findByCreatorId(creatorId)
                .orElseGet(() -> repo.save(new CreatorStats(creatorId)));
    }
}
