package com.growearn.repository;

import com.growearn.entity.ViewerTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ViewerTaskRepository extends JpaRepository<ViewerTask, Long> {

    List<ViewerTask> findByViewerIdIsNullAndCompletedFalse();

    List<ViewerTask> findByViewerIdAndCompletedFalse(Long viewerId);
}
