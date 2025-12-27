package com.jing.monitor;

import com.jing.monitor.model.Task;
import com.jing.monitor.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

// 这是一个集成测试，它会启动整个 Spring 容器，连接真实的 MySQL
@SpringBootTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void testCrudOperations() {
        System.out.println("====== 开始测试 MySQL CRUD ======");

        // 1. Create (增)
        Task newTask = new Task("SPR BT", "200","99999","009999",null);
        // 保存到数据库，save() 方法会返回保存后的对象（包含了生成的 ID）
        Task savedTask = taskRepository.save(newTask);

        System.out.println("1. 写入成功，生成的 ID 是: " + savedTask.getId());

        // 断言（Assertion）：作为测试，必须验证结果是否符合预期
        assert savedTask.getId() != null;

        // 2. Read (查)
        // findAll() 返回所有
        List<Task> allTasks = taskRepository.findAll();
        System.out.println("2. 查询所有任务，当前数量: " + allTasks.size());

        // findById() 返回 Optional (可能为空)
        Optional<Task> fetchResult = taskRepository.findById(savedTask.getId());
        if (fetchResult.isPresent()) {
            System.out.println("3. 根据 ID 查到了: " + fetchResult.get().getCourseDisplayName());
        } else {
            System.err.println("3. 奇怪，没查到！");
        }

        // 3. Update (改)
        Task taskToUpdate = fetchResult.get();
        taskToUpdate.setCourseDisplayName("SPR BT 300");
        // save() 很智能：如果对象有 ID，它就执行 UPDATE；如果没有 ID，它就执行 INSERT
        taskRepository.save(taskToUpdate);
        System.out.println("4. 修改成功，新名字: " + taskRepository.findById(savedTask.getId()).get().getCourseDisplayName());

        // 4. Delete (删)
        // 为了不弄脏你的数据库，测试完最好删掉
        taskRepository.deleteById(savedTask.getId());
        System.out.println("5. 删除测试数据成功");

        // 验证一下是不是真删了
        boolean exists = taskRepository.existsById(savedTask.getId());
        System.out.println("6. 数据还存在吗? " + exists);

        System.out.println("====== 测试结束 ======");
    }
}