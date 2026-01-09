package com.growearn.repository;

import com.growearn.entity.Earning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EarningRepository extends JpaRepository<Earning, Long> {
    List<Earning> findByViewerId(Long viewerId);

    @Query("SELECT SUM(e.amount) FROM Earning e WHERE e.viewerId = :viewerId")
    Double sumEarningsByViewerId(Long viewerId);
}
