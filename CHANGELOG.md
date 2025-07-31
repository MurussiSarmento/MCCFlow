# FlowDeconstruct - Changelog

Todas as mudanças importantes do projeto serão documentadas neste arquivo.

## [v1.0.1] - 2025-07-31

### ✨ Melhorias na Edição de Nós
- **Enter para Edição**: A tecla Enter agora inicia a edição do nó selecionado ao invés de criar um novo nó
- **Edição Automática**: Ao digitar texto com um nó selecionado, o modo de edição inicia automaticamente
- **Campo Limpo**: Quando inicia a edição, o campo começa vazio para permitir substituição completa do texto
- **Tab para Criar**: Apenas a tecla Tab cria novos nós conectados

### 🔧 Mudanças Técnicas
- Refatoração do sistema de eventos de teclado no `MainWindow.java`
- Melhorias no método `startEditingNode()` em `FlowCanvas.java`
- Adição de método `startEditingSelectedNode()` para melhor separação de responsabilidades
- Atualização da lógica de `finishEditingNode()` para permitir campos vazios

### 📚 Documentação
- **README.md**: Completamente reescrito com formatação adequada e informações atualizadas
- **design.md**: Reformatado para melhor legibilidade e incluídas mudanças recentes
- **Shortcuts atualizados**: Todos os documentos agora refletem os novos atalhos de teclado
- Remoção de arquivos desnecessários (`prompt.md`)

### 🐛 Correções
- Corrigido problema onde Enter criava nodos indevidamente
- Corrigido comportamento de edição que não limpava texto anterior
- Corrigido problema de reversão para texto original ao limpar campo

## [v1.0.0] - 2025-07-30

### 🎉 Release Inicial
- **Core Flow Mapping**: Funcionalidade básica de mapeamento de fluxos
- **Navegação Hierárquica**: Sistema de drill-down com Ctrl+Enter
- **System Tray**: Integração completa com bandeja do sistema Windows
- **Atalhos de Teclado**: Sistema completo de atalhos para navegação sem mouse
- **Persistência**: Salvamento automático de projetos em JSON
- **Exportação**: Suporte a PDF e Markdown com preservação hierárquica
- **Interface Dark**: Tema escuro otimizado para reduzir fadiga visual
- **SAP JVM 8**: Integração específica com SAP JVM para ambientes empresariais

### 🛠️ Funcionalidades Técnicas
- Auto-layout de nós e conexões
- Sistema de eventos baseado em Observer pattern
- Build automation com scripts PowerShell
- Configuração Maven para SAP JVM 8
- Sistema de logs para debugging

### 📋 Documentação Inicial
- Documento de Requisitos do Produto (PRD)
- Especificação de Design Visual
- Instruções de Setup para SAP JVM
- Scripts de build e execução

---

## Convenções de Versionamento

Este projeto segue [Semantic Versioning](https://semver.org/):
- **MAJOR**: Mudanças incompatíveis na API
- **MINOR**: Funcionalidades adicionadas de forma compatível
- **PATCH**: Correções de bugs compatíveis

## Tipos de Mudanças

- 🎉 **Added**: Novas funcionalidades
- ✨ **Enhanced**: Melhorias em funcionalidades existentes
- 🔧 **Changed**: Mudanças em funcionalidades existentes
- 🐛 **Fixed**: Correções de bugs
- 🗑️ **Removed**: Funcionalidades removidas
- 🔒 **Security**: Correções de segurança
- 📚 **Documentation**: Mudanças apenas na documentação