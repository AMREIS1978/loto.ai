// Todo List App - Main Script
class TodoApp {
    constructor() {
        this.tasks = [];
        this.currentFilter = 'todas';
        this.currentCategoryFilter = 'todas';
        this.editingTaskId = null;
        this.init();
    }

    init() {
        this.loadTasks();
        this.setupEventListeners();
        this.render();
    }

    // Local Storage Management
    loadTasks() {
        const saved = localStorage.getItem('tasks');
        this.tasks = saved ? JSON.parse(saved) : [];
    }

    saveTasks() {
        localStorage.setItem('tasks', JSON.stringify(this.tasks));
        this.updateStatistics();
        this.render();
    }

    // Event Listeners
    setupEventListeners() {
        // Form submission
        document.getElementById('taskForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.addTask();
        });

        // Filters
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');
                this.currentFilter = e.target.dataset.filter;
                this.render();
            });
        });

        // Category Filters
        document.querySelectorAll('.category-filter').forEach(btn => {
            btn.addEventListener('click', (e) => {
                document.querySelectorAll('.category-filter').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');
                this.currentCategoryFilter = e.target.dataset.category;
                this.render();
            });
        });

        // Action Buttons
        document.getElementById('clearCompleted').addEventListener('click', () => this.clearCompleted());
        document.getElementById('clearAll').addEventListener('click', () => this.clearAll());
        document.getElementById('exportBtn').addEventListener('click', () => this.exportTasks());
        document.getElementById('importBtn').addEventListener('click', () => this.triggerImport());

        // Modal
        document.getElementById('editForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.saveEditedTask();
        });

        document.querySelector('.close-btn').addEventListener('click', () => this.closeModal());
        document.querySelector('.btn-cancel').addEventListener('click', () => this.closeModal());

        // File input
        document.getElementById('fileInput').addEventListener('change', (e) => this.importTasks(e));
    }

    // Task Management
    addTask() {
        const taskInput = document.getElementById('taskInput');
        const category = document.getElementById('category').value;
        const priority = document.getElementById('priority').value;
        const dueDate = document.getElementById('dueDate').value;

        if (taskInput.value.trim() === '') return;

        const task = {
            id: Date.now(),
            text: taskInput.value.trim(),
            category,
            priority,
            dueDate,
            completed: false,
            createdAt: new Date().toISOString()
        };

        this.tasks.unshift(task);
        this.saveTasks();
        this.resetForm();
    }

    deleteTask(id) {
        if (confirm('Tem certeza que deseja deletar esta tarefa?')) {
            this.tasks = this.tasks.filter(task => task.id !== id);
            this.saveTasks();
        }
    }

    toggleTask(id) {
        const task = this.tasks.find(t => t.id === id);
        if (task) {
            task.completed = !task.completed;
            this.saveTasks();
        }
    }

    openEditModal(id) {
        const task = this.tasks.find(t => t.id === id);
        if (!task) return;

        this.editingTaskId = id;
        document.getElementById('editTaskInput').value = task.text;
        document.getElementById('editCategory').value = task.category;
        document.getElementById('editPriority').value = task.priority;
        document.getElementById('editDueDate').value = task.dueDate || '';

        document.getElementById('editModal').classList.add('active');
    }

    closeModal() {
        document.getElementById('editModal').classList.remove('active');
        this.editingTaskId = null;
    }

    saveEditedTask() {
        if (this.editingTaskId === null) return;

        const task = this.tasks.find(t => t.id === this.editingTaskId);
        if (!task) return;

        task.text = document.getElementById('editTaskInput').value.trim();
        task.category = document.getElementById('editCategory').value;
        task.priority = document.getElementById('editPriority').value;
        task.dueDate = document.getElementById('editDueDate').value;

        this.saveTasks();
        this.closeModal();
    }

    // Filtering
    getFilteredTasks() {
        let filtered = this.tasks;

        // Filter by status
        if (this.currentFilter === 'ativas') {
            filtered = filtered.filter(t => !t.completed);
        } else if (this.currentFilter === 'concluidas') {
            filtered = filtered.filter(t => t.completed);
        }

        // Filter by category
        if (this.currentCategoryFilter !== 'todas') {
            filtered = filtered.filter(t => t.category === this.currentCategoryFilter);
        }

        return filtered;
    }

    // Clearing
    clearCompleted() {
        if (confirm('Deseja limpar todas as tarefas concluídas?')) {
            this.tasks = this.tasks.filter(t => !t.completed);
            this.saveTasks();
        }
    }

    clearAll() {
        if (confirm('Deseja apagar TODAS as tarefas? Esta ação não pode ser desfeita!')) {
            this.tasks = [];
            this.saveTasks();
        }
    }

    // Export/Import
    exportTasks() {
        const dataStr = JSON.stringify(this.tasks, null, 2);
        const dataBlob = new Blob([dataStr], { type: 'application/json' });
        const url = URL.createObjectURL(dataBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `tasks-${new Date().toISOString().split('T')[0]}.json`;
        link.click();
        URL.revokeObjectURL(url);
    }

    triggerImport() {
        document.getElementById('fileInput').click();
    }

    importTasks(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const imported = JSON.parse(e.target.result);
                if (Array.isArray(imported)) {
                    this.tasks = imported;
                    this.saveTasks();
                    alert('Tarefas importadas com sucesso!');
                } else {
                    alert('Formato de arquivo inválido!');
                }
            } catch (error) {
                alert('Erro ao importar arquivo: ' + error.message);
            }
        };
        reader.readAsText(file);
    }

    // Statistics
    updateStatistics() {
        const total = this.tasks.length;
        const completed = this.tasks.filter(t => t.completed).length;
        const active = total - completed;
        const rate = total > 0 ? Math.round((completed / total) * 100) : 0;

        document.getElementById('totalTasks').textContent = total;
        document.getElementById('activeTasks').textContent = active;
        document.getElementById('completedTasks').textContent = completed;
        document.getElementById('completionRate').textContent = rate + '%';
    }

    // Rendering
    render() {
        this.updateStatistics();
        const filtered = this.getFilteredTasks();
        const taskList = document.getElementById('taskList');

        if (filtered.length === 0) {
            taskList.innerHTML = '<p class="empty-message">Nenhuma tarefa encontrada. 🎯</p>';
            return;
        }

        taskList.innerHTML = filtered.map(task => this.createTaskElement(task)).join('');

        // Add event listeners to task elements
        document.querySelectorAll('.task-checkbox').forEach(checkbox => {
            checkbox.addEventListener('change', () => {
                this.toggleTask(parseInt(checkbox.dataset.id));
            });
        });

        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                this.openEditModal(parseInt(btn.dataset.id));
            });
        });

        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                this.deleteTask(parseInt(btn.dataset.id));
            });
        });
    }

    createTaskElement(task) {
        const dueDate = task.dueDate ? new Date(task.dueDate) : null;
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        let dueDateHtml = '';
        if (dueDate) {
            const isOverdue = dueDate < today && !task.completed;
            const dueDateFormatted = dueDate.toLocaleDateString('pt-PT');
            const badgeClass = isOverdue ? 'badge-overdue' : 'badge-due-date';
            const dueDateLabel = isOverdue ? '⏰ Vencida' : '📅 Vence em';
            dueDateHtml = `<span class="badge ${badgeClass}">${dueDateLabel} ${dueDateFormatted}</span>`;
        }

        const categoryLabel = {
            'pessoal': '👤',
            'trabalho': '💼',
            'saude': '🏥',
            'compras': '🛒',
            'outro': '📌'
        }[task.category] || '📝';

        const priorityLabel = {
            'alta': '🔴',
            'media': '🟡',
            'baixa': '🟢'
        }[task.priority] || '';

        return `
            <div class="task-item ${task.completed ? 'completed' : ''} priority-${task.priority}">
                <input 
                    type="checkbox" 
                    class="task-checkbox" 
                    data-id="${task.id}"
                    ${task.completed ? 'checked' : ''}
                >
                <div class="task-content">
                    <div class="task-text">${this.escapeHtml(task.text)}</div>
                    <div class="task-meta">
                        <span class="badge badge-category">${categoryLabel} ${task.category.charAt(0).toUpperCase() + task.category.slice(1)}</span>
                        <span class="badge badge-priority-${task.priority}">${priorityLabel} ${task.priority.charAt(0).toUpperCase() + task.priority.slice(1)}</span>
                        ${dueDateHtml}
                    </div>
                </div>
                <div class="task-actions">
                    <button class="task-btn edit-btn" data-id="${task.id}" title="Editar">✏️</button>
                    <button class="task-btn delete delete-btn" data-id="${task.id}" title="Deletar">🗑️</button>
                </div>
            </div>
        `;
    }

    resetForm() {
        document.getElementById('taskForm').reset();
        document.getElementById('priority').value = 'media';
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new TodoApp();
});
