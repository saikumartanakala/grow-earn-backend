package com.growearn.repository;

import com.growearn.entity.ViewerTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ViewerTaskRepository extends JpaRepository<ViewerTask, Long> {

    // ✅ FETCH ALL PENDING TASKS FOR ALL VIEWERS
    @Query("""
        SELECT vt FROM ViewerTask vt
        WHERE vt.completed = false
        AND vt.status = 'PENDING'
    """)
    List<ViewerTask> findAllPendingTasks();

    // ✅ FETCH TASKS ASSIGNED TO A SPECIFIC VIEWER
    List<ViewerTask> findByViewerId(Long viewerId);

    // ✅ FETCH COMPLETED TASKS BY A VIEWER
    List<ViewerTask> findByViewerIdAndCompleted(Long viewerId, boolean completed);

    // ✅ FETCH AVAILABLE TASKS (not assigned to anyone)
    @Query("""
        SELECT vt FROM ViewerTask vt
        WHERE vt.viewerId IS NULL
        AND vt.completed = false
        AND vt.status = 'PENDING'
    """)
    List<ViewerTask> findAvailableTasks();
}
