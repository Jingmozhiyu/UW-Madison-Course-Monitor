// 设置后端 API 基础路径
const API_URL = 'http://localhost:8080/api/tasks'; // 确保端口和你 Spring Boot 配置一致

// 获取表单数据
const getData = () => {
    return {
        subject: document.getElementById("inputSubject").value,
        catalogNumber: document.getElementById("inputCatalog").value,
        sectionId: document.getElementById("inputSectionId").value
    };
}

// 1. 创建任务 (POST)
document.querySelector('#btnCreate').addEventListener('click', () => {
    const taskReq = getData();
    if(!taskReq.sectionId) { alert("Section ID is required!"); return; }

    axios.post(API_URL, taskReq)
        .then((result) => {
            console.log(result.data);
            alert("Task Added: " + result.data.msg);
            loadList(); // 自动刷新列表
        })
        .catch((err) => console.error(err));
})

// 2. 读取列表 (GET)
const loadList = () => {
    axios.get(API_URL)
        .then((result) => {
            const tasks = result.data.data; // Result<List<TaskRespDto>>
            const output = document.getElementById('outputArea');

            if (tasks.length === 0) {
                output.innerHTML = "No active tasks.";
                return;
            }

            // 简单的渲染成文本列表
            let html = '<ul style="text-align: left;">';
            tasks.forEach(t => {
                // 根据状态显示不同 Emoji
                const statusIcon = t.status === 'OPEN' ? '🟢' : (t.status === 'WAITLISTED' ? '🟡' : '🔴');
                html += `
                    <li>
                        <b>[ID: ${t.id}]</b> ${t.courseDisplayName} - Sec: ${t.sectionId} <br/>
                        Status: ${statusIcon} ${t.status || 'Checking...'}
                    </li><br/>
                `;
            });
            html += '</ul>';
            output.innerHTML = html;
        })
        .catch((err) => console.error(err));
}
// 绑定按钮
document.querySelector('#btnRead').addEventListener('click', loadList);

// 3. 更新任务 (PUT)
document.querySelector('#btnUpdate').addEventListener('click', () => {
    const id = document.getElementById('taskId').value;
    const taskReq = getData(); // 允许用户修改输入框的内容来更新

    if(!id) { alert("Please enter Task ID"); return; }

    axios.put(`${API_URL}/${id}`, taskReq)
        .then((result) => {
            alert("Updated!");
            loadList();
        })
        .catch((err) => console.error(err));
})

// 4. 删除任务 (DELETE)
document.querySelector('#btnDelete').addEventListener('click', () => {
    const id = document.getElementById('taskId').value;
    if(!id) { alert("Please enter Task ID"); return; }

    axios.delete(`${API_URL}/${id}`)
        .then((result) => {
            alert("Deleted!");
            loadList();
        })
        .catch((err) => console.error(err));
})

// 页面加载时自动拉取一次列表
window.onload = loadList;