
package com.growearn.repository;

import com.growearn.entity.ViewerTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Legacy repository name retained as a thin alias to ViewerTaskEntity to avoid breaking imports.
 * New code should use ViewerTaskEntityRepository directly.
 */
public interface ViewerTaskRepository extends JpaRepository<ViewerTaskEntity, Long> {

}
