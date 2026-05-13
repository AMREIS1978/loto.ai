# 📋 Todo List - Gestor de Tarefas Inteligente

**Uma aplicação web completa para gerenciar suas tarefas diárias com sincronização local (localStorage).**

## ✨ Funcionalidades

### Gestão de Tarefas
- ✅ **Adicionar tarefas** com descrição completa
- ✏️ **Editar tarefas** já criadas
- 🗑️ **Deletar tarefas** individualmente
- ☑️ **Marcar como concluída** com checkbox
- 💾 **Sincronização automática** via localStorage

### Organização Avançada
- 📂 **Categorias**: Pessoal, Trabalho, Saúde, Compras, Outro
- 🎯 **Prioridades**: Baixa, Média, Alta
- 📅 **Data de vencimento** com alertas de atraso
- 🏷️ **Tags e badges** visuais para melhor identificação

### Filtros e Pesquisa
- 📊 **Filtros de status**: Todas / Ativas / Concluídas
- 🔍 **Filtros por categoria**: Todos / Pessoal / Trabalho / Saúde / Compras / Outro
- 📈 **Estatísticas em tempo real**: Total, Ativas, Concluídas, Taxa de conclusão

### Dados e Backup
- 📥 **Exportar tarefas** em formato JSON
- 📤 **Importar tarefas** de arquivo JSON
- 💿 **Local Storage**: Todos os dados salvos no navegador
- 🗑️ **Limpar completadas**: Remove todas as tarefas concluídas

### Interface
- 🎨 **Design responsivo** (funciona em mobile, tablet, desktop)
- 🌈 **UI intuitiva** com gradientes e animações
- 🎭 **Dark/Light compatible** (segue o tema do sistema)
- ⚡ **Performance otimizada**: Sem dependências externas

## 🚀 Como Usar

### Instalação

1. **Clone o repositório**
   ```bash
   git clone https://github.com/AMREIS1978/loto.ai.git
   cd loto.ai
   ```

2. **Abra no navegador**
   - Simplesmente abra `index.html` no seu navegador
   - Ou use um servidor local:
     ```bash
     python -m http.server 8000
     # Acesse http://localhost:8000
     ```

### Funcionalidades Detalhadas

#### Adicionar Tarefa
1. Digite a descrição no campo "O que precisa fazer?"
2. Selecione a categoria
3. Escolha a prioridade
4. (Opcional) Defina a data de vencimento
5. Clique em "+ Adicionar Tarefa"

#### Editar Tarefa
1. Clique no ícone ✏️ na tarefa
2. Altere os dados no modal
3. Clique em "Salvar"

#### Filtrar Tarefas
1. Use os botões de status: Todas / Ativas / Concluídas
2. Use os filtros de categoria para agrupar por tipo
3. Os filtros trabalham juntos (compatível)

#### Exportar/Importar
1. **Exportar**: Clique em "📥 Exportar" para salvar um arquivo JSON
2. **Importar**: Clique em "📤 Importar" para carregar um arquivo JSON salvo anteriormente

## 💾 Local Storage

Todas as tarefas são **automaticamente salvas** no localStorage do navegador. Isso significa:
- ✅ Dados persistem após fechar o navegador
- ✅ Funcionam offline (sem internet)
- ❌ Dados específicos por navegador/dispositivo
- 💡 Use a função de exportar para fazer backup em outro dispositivo

## 📱 Compatibilidade

- ✅ Chrome/Edge (versão 60+)
- ✅ Firefox (versão 55+)
- ✅ Safari (versão 11+)
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

## 🎨 Estrutura de Código

```
loto.ai/
├── index.html      # Estrutura HTML
├── styles.css      # Estilos CSS responsivos
├── script.js       # Lógica JavaScript (Classe TodoApp)
└── README.md       # Este arquivo
```

### Classe TodoApp

A aplicação usa uma classe `TodoApp` que gerencia:
- **Carregamento/Salvamento** de tarefas (localStorage)
- **CRUD operations** (Create, Read, Update, Delete)
- **Filtros dinâmicos** (status e categoria)
- **Renderização** de elementos DOM
- **Exportação/Importação** de dados

## 🔧 Desenvolvimento

### Adicionar Nova Funcionalidade

1. **Nova categoria**: Edite o `<select>` em HTML e a função `createTaskElement()`
2. **Novo filtro**: Adicione um novo botão e sua lógica em `getFilteredTasks()`
3. **Novo badge**: Crie uma classe CSS `badge-*` e use em `createTaskElement()`

### Exemplo: Adicionar Categoria

```html
<!-- Em index.html -->
<option value="financeiro">Financeiro</option>
```

```javascript
// Em script.js - em createTaskElement()
const categoryLabel = {
    'pessoal': '👤',
    'trabalho': '💼',
    'saude': '🏥',
    'compras': '🛒',
    'financeiro': '💰',  // ← Nova
    'outro': '📌'
}[task.category] || '📝';
```

## 🐛 Troubleshooting

### As tarefas não aparecem após reload
- Verifique se o localStorage está habilitado no navegador
- Tente limpar o cache do navegador
- Importe o backup JSON (se tiver)

### Os dados sumiram
- Clique em "📤 Importar" se tiver um backup
- As tarefas podem estar em outro navegador/aba

### Performance lenta com muitas tarefas
- Limpe as tarefas concluídas regularmente
- Exporte e importe para "compactar" os dados

## 📈 Roadmap Futuro

- [ ] Subtarefas/checklist dentro de tarefas
- [ ] Sincronização em nuvem (Firebase)
- [ ] Lembretes por email/notificação
- [ ] Tema escuro automático
- [ ] Colaboração entre usuários
- [ ] Integração com calendário
- [ ] Reconhecimento de voz
- [ ] Sugestões de IA

## 📄 Licença

MIT - Sinta-se livre para usar, modificar e distribuir!

## 👨‍💻 Autor

Desenvolvido com ❤️ por **AMREIS1978**

---

**Dica**: Use a função de exportar regularmente para fazer backup de suas tarefas! 💾
