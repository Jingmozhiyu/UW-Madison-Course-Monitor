package com.jing.monitor.repository;

import com.jing.monitor.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Task} persistence and ownership-scoped queries.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Finds all enabled tasks that are owned by a user.
     *
     * @return enabled task list
     */
    List<Task> findByEnabledTrueAndUserIdIsNotNull();

    /**
     * Finds all tasks for a specific course under one user.
     *
     * @param courseId course id
     * @param userId owner user id
     * @return task list
     */
    List<Task> findAllByCourseIdAndUserId(String courseId, Long userId);

    /**
     * Lists all tasks owned by one user.
     *
     * @param userId owner user id
     * @return task list
     */
    List<Task> findAllByUserId(Long userId);

    /**
     * Finds one task by id under a specific user.
     *
     * @param id task id
     * @param userId owner user id
     * @return optional task
     */
    Optional<Task> findByIdAndUserId(Long id, Long userId);

    /**
     * Finds tasks by section ids under one owner.
     *
     * @param sectionIds section id collection
     * @param userId owner user id
     * @return matching tasks
     */
    List<Task> findAllBySectionIdInAndUserId(Collection<String> sectionIds, Long userId);

    /**
     * Deletes all tasks by display name under one owner.
     *
     * @param courseDisplayName display name
     * @param userId owner user id
     */
    void deleteAllByCourseDisplayNameAndUserId(String courseDisplayName, Long userId);

    /**
     * Backfills null ownership to a target user id.
     *
     * @param userId target user id
     * @return number of rows updated
     */
    @Modifying
    @Transactional
    @Query("update Task t set t.userId = :userId where t.userId is null")
    int assignUserIdToNullTasks(@Param("userId") Long userId);

    /**
     * Reassigns tasks from one user to another.
     *
     * @param sourceUserId previous owner id
     * @param targetUserId new owner id
     * @return number of rows updated
     */
    @Modifying
    @Transactional
    @Query("update Task t set t.userId = :targetUserId where t.userId = :sourceUserId")
    int reassignTasksToUser(@Param("sourceUserId") Long sourceUserId, @Param("targetUserId") Long targetUserId);
}
