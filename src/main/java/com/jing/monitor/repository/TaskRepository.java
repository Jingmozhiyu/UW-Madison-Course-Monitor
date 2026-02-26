package com.jing.monitor.repository;

import com.jing.monitor.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * TaskRepository
 * 继承 JpaRepository 后，Spring 会自动实现 CRUD + 分页 + 排序
 * <Task, Long> : 第一个参数是实体类，第二个参数是主键(@Id)的类型
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByEnabledTrue();

    Task findBySectionId(String sectionId);

    List<Task> findAllByCourseId(String courseId);

    List<Task> findAllBySectionIdIn(Collection<String> sectionIds);

    void deleteAllByCourseDisplayName(String courseDisplayName);
}
