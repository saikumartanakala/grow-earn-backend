package com.growearn.repository;

import com.growearn.entity.Earning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EarningRepository extends JpaRepository<Earning, Long> {
    List<Earning> findByViewerId(Long viewerId);

    List<Earning> findByTaskIdIn(java.util.List<Long> taskIds);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Earning e WHERE e.viewerId = :viewerId")
    Double sumEarningsByViewerId(@Param("viewerId") Long viewerId);
}
