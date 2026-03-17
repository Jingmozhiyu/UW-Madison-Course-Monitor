const API_URL = 'http://localhost:8080/api/tasks';

// 1. Load Tasks
const loadTasks = () => {
    const tbody = document.getElementById('taskTableBody');
    axios.get(API_URL)
        .then(response => {
            const tasks = response.data.data;
            document.getElementById('totalCount').innerText = tasks.length;
            renderTable(tasks);
        })
        .catch(err => {
            console.error(err);
            tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:red">Backend Error. Is Spring Boot running?</td></tr>`;
        });
};

const renderTable = (tasks) => {
    const tbody = document.getElementById('taskTableBody');
    tbody.innerHTML = '';

    if (tasks.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:#666">No active snipers. Add a course above!</td></tr>';
        return;
    }

    // Sort: Enabled first, then Name, then Section ID
    tasks.sort((a, b) => {
        if (a.enabled !== b.enabled) return a.enabled ? -1 : 1;
        if (a.courseDisplayName !== b.courseDisplayName) return a.courseDisplayName.localeCompare(b.courseDisplayName);
        return a.sectionId.localeCompare(b.sectionId);
    });

    tasks.forEach(task => {
        const row = document.createElement('tr');

        let badgeClass = 'bg-unknown';
        if (task.status === 'OPEN') badgeClass = 'bg-open';
        else if (task.status === 'WAITLISTED') badgeClass = 'bg-waitlist';
        else if (task.status === 'CLOSED') badgeClass = 'bg-closed';

        row.innerHTML = `
            <td><b>${task.courseDisplayName}</b></td>
            <td>${task.sectionId}</td>
            <td><span class="badge ${badgeClass}">${task.status || 'Checking...'}</span></td>
            <td>
                <label class="switch">
                    <input type="checkbox" ${task.enabled ? 'checked' : ''} onchange="toggleTask(${task.id})">
                    <span class="slider"></span>
                </label>
            </td>
            <td>
                <button class="btn-del" title="Delete entire course" onclick="deleteCourse('${task.courseDisplayName}')">🗑️</button>
            </td>
        `;
        tbody.appendChild(row);
    });
};

// 2. Toggle Status
const toggleTask = (id) => {
    axios.patch(`${API_URL}/${id}/toggle`)
        .then(res => console.log(`Task ${id} toggled`))
        .catch(err => {
            alert("Failed to toggle status");
            loadTasks(); // revert UI on error
        });
};

// 3. Search & Add (Core Function)
const searchAndAdd = () => {
    const input = document.getElementById('searchInput');
    const btn = document.getElementById('btnAdd');
    const courseName = input.value.trim();

    if (!courseName) { alert("Please enter a course name/section id (e.g. COMP SCI 577, 76101)"); return; }

    // UI Feedback: Disable button to prevent double-click abuse
    btn.disabled = true;
    btn.innerHTML = "⏳ Searching...";

    // Use params to match backend @RequestParam
    axios.post(API_URL, null, { params: { courseName: courseName } })
        .then(res => {
            alert(`✅ Sniper deployed! Found ${res.data.data.length} sections.`);
            input.value = ''; // clear input
            loadTasks(); // refresh list
        })
        .catch(err => {
            console.error(err);
            // Handle backend exceptions (e.g. Course Not Found)
            const msg = err.response?.data?.msg || "Search failed. Check console.";
            alert("❌ Error: " + msg);
        })
        .finally(() => {
            btn.disabled = false;
            btn.innerHTML = "🔍 Snipe!";
        });
};

// 4. Delete Course
const deleteCourse = (courseDisplayName) => {
    if (!confirm(`Are you sure you want to delete ALL sections for "${courseDisplayName}"?`)) return;

    axios.delete(API_URL, { params: { courseDisplayName: courseDisplayName } })
        .then(res => {
            // alert("Deleted."); // Optional
            loadTasks();
        })
        .catch(err => {
            alert("Delete failed.");
            console.error(err);
        });
}

// Init
window.onload = loadTasks;