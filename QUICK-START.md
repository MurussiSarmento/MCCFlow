# FlowDeconstruct - Quick Start Guide

Este guia te ajudará a começar a usar o FlowDeconstruct em menos de 5 minutos.

## ⚡ Início Rápido (2 minutos)

### 1. 🚀 Execute a Aplicação
```batch
# Opção mais simples - duplo click no arquivo:
FlowDeconstruct.bat

# Ou via PowerShell:
.\run-jar.ps1
```

### 2. 🎯 Primeiro Uso
1. **Aplicação inicia na bandeja** - Procure o ícone ao lado do relógio
2. **Abra a interface** - Pressione `Ctrl+Shift+F` ou duplo-click no ícone
3. **Crie seu primeiro nó** - Pressione `Tab`
4. **Digite o nome** - Ex: "ECC" e pressione `Enter`
5. **Crie nós conectados** - Com nó selecionado, pressione `Tab` novamente

### 3. ⌨️ Atalhos Essenciais
| Tecla | Ação |
|-------|------|
| `Tab` | Criar nó conectado |
| `Enter` | Editar nó selecionado |
| `Setas` | Navegar entre nós |
| `Ctrl+Enter` | Drill-down (subfluxo) |
| `?` | Mostrar ajuda |

**Pronto!** Você já pode mapear seus fluxos. 🎉

---

## 🔧 Configuração Inicial (Primeira vez)

### Pré-requisitos
- **Windows 10/11**
- **SAP JVM 8** instalado
- **Permissões de Administrador** (apenas para setup)

### Setup Automático
```powershell
# Execute como Administrador (UMA VEZ APENAS):
.\setup-environment.ps1

# Build da aplicação (UMA VEZ APENAS):
.\build-jar.ps1
```

Isso configura:
- ✅ SAP JVM 8
- ✅ Apache Maven
- ✅ Variáveis de ambiente
- ✅ Build da aplicação

---

## 📋 Fluxo de Trabalho Típico

### Cenário: Mapear um Sistema SAP

1. **Abrir FlowDeconstruct** - `Ctrl+Shift+F`
2. **Mapear fluxo principal**:
   - `Tab` → Digite "ECC" → `Enter`
   - `Tab` → Digite "CIG" → `Enter`  
   - `Tab` → Digite "SCT" → `Enter`
3. **Adicionar detalhes**:
   - Navegue para "CIG" com setas
   - `Ctrl+N` → Adicione nota sobre o problema
4. **Drill-down** (se necessário):
   - `Ctrl+Enter` → Mapeia internos do CIG
   - `Esc` → Volta ao fluxo principal
5. **Exportar resultado**:
   - `Ctrl+E` → Escolha PDF ou Markdown

**Tempo total: 2-3 minutos**

---

## 🎯 Casos de Uso Principais

### 1. 🚨 Triagem de Incidentes
**Cenário**: Incidente P1 em produção
- **Velocidade**: Mapeamento em tempo real durante call
- **Foco**: Sistema de nós hierárquico para drill-down
- **Resultado**: PDF para escalation ou Markdown para ticket

### 2. 📋 Documentação de Processos
**Cenário**: Documentar arquitetura complexa
- **Estrutura**: Múltiplos níveis de subfluxos
- **Colaboração**: Export para Markdown/PDF
- **Manutenção**: Salvamento automático local

### 3. 🎓 Treinamento Técnico
**Cenário**: Transferência de conhecimento
- **Visual**: Mapeamento claro de dependências
- **Navegação**: Drill-down para detalhes
- **Compartilhamento**: Exports profissionais

---

## 🔧 Troubleshooting Rápido

### ❌ Aplicação não inicia
```powershell
# Verificar SAP JVM:
java -version
# Deve mostrar: SAP Java Server VM

# Se não funcionar:
.\setup-environment.ps1
```

### ❌ Ícone não aparece na bandeja
- Verifique se bandeja do Windows está habilitada
- Reinicie a aplicação
- Tente executar como Administrador

### ❌ JAR não encontrado
```powershell
# Rebuild da aplicação:
.\build-jar.ps1
```

### ❌ Atalhos não funcionam
- Certifique-se que a janela tem foco
- Pressione `Ctrl+Shift+F` para focar
- Verifique se não há conflito com outras aplicações

---

## 📚 Próximos Passos

Depois do quick start, confira:

1. **README.md** - Documentação completa
2. **design.md** - Especificações visuais
3. **SAP-SETUP-INSTRUCTIONS.md** - Setup detalhado para SAP
4. **CHANGELOG.md** - Histórico de mudanças

---

## 💡 Dicas Pro

### ⚡ Máxima Velocidade
- Use apenas atalhos de teclado
- Mantenha a aplicação sempre na bandeja
- Use `Ctrl+Shift+F` para acesso instantâneo

### 🎯 Workflow Eficiente
- Comece sempre mapeando o fluxo principal
- Use drill-down apenas quando necessário
- Adicione notas nos pontos problemáticos
- Exporte sempre no final

### 🔄 Boas Práticas
- Nomes de nós concisos (ex: "ECC", "CIG")
- Use notas para contexto detalhado
- Subfluxos para complexidade interna
- Export PDF para apresentações formais

---

**FlowDeconstruct** - *"Analysis at the speed of thought"* ⚡