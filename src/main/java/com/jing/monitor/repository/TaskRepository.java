package com.jing.monitor.repository;

import com.jing.monitor.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TaskRepository
 * 继承 JpaRepository 后，Spring 会自动实现 CRUD + 分页 + 排序
 * <Task, Long> : 第一个参数是实体类，第二个参数是主键(@Id)的类型
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // 🔥 进阶魔法：自定义查询方法
    // 你只需要按照 "findBy + 字段名" 的规则命名方法，Spring 就会自动生成 SQL！

    // 自动生成 SQL: SELECT * FROM tasks WHERE enabled = true;
    List<Task> findByEnabledTrue();

    // 自动生成 SQL: SELECT * FROM tasks WHERE section_id = ?;
    // Task findBySectionId(String sectionId);
}