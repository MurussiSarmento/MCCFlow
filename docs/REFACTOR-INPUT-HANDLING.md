# Plano de Refatoração: Gerenciamento de Input

**Status:** Proposto

## 1. Objetivo e Problemas Atuais

**Objetivo:** Centralizar e desacoplar a lógica de tratamento de eventos de teclado e mouse para aumentar a manutenibilidade, testabilidade e clareza do código.

**Problemas Atuais:**

1.  **Lógica Dividida:** O tratamento de eventos está espalhado por múltiplas classes. `MainWindow` lida com atalhos globais e de navegação, enquanto `FlowCanvas` lida com toda a interação do mouse e com a entrada de texto ao editar um nó. Isso torna difícil saber onde adicionar ou modificar um comportamento.
2.  **Dependência de Foco:** O sistema atual depende fortemente de qual componente Swing tem o foco do teclado, exigindo chamadas explícitas a `requestFocusInWindow()` em vários locais para garantir que os `KeyListeners` corretos sejam acionados.
3.  **Baixa Testabilidade:** A lógica de ação está diretamente acoplada aos métodos do listener de eventos do Swing (ex: `keyPressed`, `mouseClicked`), tornando-a difícil de ser testada de forma isolada (testes unitários).
4.  **Complexidade Crescente:** Os métodos `keyPressed` em `MainWindow` e `FlowCanvas` estão se tornando blocos `switch` e `if-else` grandes e complexos.

## 2. Proposta de Arquitetura

A solução proposta é refatorar o sistema de input para usar o **Padrão de Projeto Command** em conjunto com o mecanismo de **InputMap/ActionMap** do Swing.

### 2.1. O Padrão de Comando (Command Pattern)

Em vez de a lógica de uma ação residir diretamente no listener, cada ação será encapsulada em seu próprio objeto `Command`.

1.  **Criar Pacote de Ações:** Criar um novo pacote `com.sap.flowdeconstruct.actions`.
2.  **Interface `Command`:** Definir uma interface simples:
    ```java
    public interface Command {
        void execute();
    }
    ```
3.  **Classes de Comando Concretas:** Cada ação do usuário se tornará uma classe. Elas receberão os objetos necessários (ex: `ProjectManager`, `MainWindow`) em seu construtor.
    - `ExportFlowCommand(MainWindow owner, ProjectManager pm)`
    - `CreateNodeCommand(FlowCanvas canvas)`
    - `StartNodeEditingCommand(FlowCanvas canvas)`
    - `PanCanvasCommand(FlowCanvas canvas, int dx, int dy)`
    - etc.

### 2.2. O `InputManager` Centralizado

Criaremos uma nova classe `InputManager` que será a única responsável por traduzir os eventos brutos do Swing em `Commands`.

1.  **Classe `InputManager`:**
    - Será instanciada na `FlowDeconstructApp` e passada para a `MainWindow`.
    - Conterá referências ao `ProjectManager`, `MainWindow` e `FlowCanvas`.

2.  **Mapeamento de Teclado com `InputMap` e `ActionMap`:**
    - Em vez de `KeyListeners`, usaremos o `InputMap` e `ActionMap` do componente raiz da `MainWindow`. Este é o método moderno do Swing para lidar com atalhos, pois funciona independentemente do foco do componente filho.
    - **Exemplo de Mapeamento:**
      ```java
      // Dentro do InputManager
      JComponent root = mainWindow.getRootPane();
      InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      ActionMap actionMap = root.getActionMap();

      // Mapeia a tecla F1 para uma string "showHelp"
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "showHelp");

      // Mapeia a string "showHelp" para uma Ação que executa nosso Comando
      actionMap.put("showHelp", new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
              new ShowHelpCommand(mainWindow).execute();
          }
      });
      ```

3.  **Gerenciamento do Mouse:**
    - A classe `InputManager` implementará `MouseListener` e `MouseMotionListener`.
    - A `FlowCanvas` **deixará** de implementar esses listeners. Em vez disso, ela simplesmente registrará o `InputManager`: `canvas.addMouseListener(inputManager);`.
    - O `InputManager` interpretará os gestos do mouse (clique, arrasto) e executará os `Commands` apropriados (ex: `SelectNodeCommand`, `PanCanvasCommand`).

## 3. Plano de Refatoração Passo a Passo

1.  **Fase 1: Criar a Estrutura:**
    - Criar o pacote `com.sap.flowdeconstruct.actions`.
    - Definir a interface `Command`.
    - Criar as classes para 2-3 comandos iniciais (ex: `ShowHelpCommand`, `ExportFlowCommand`).

2.  **Fase 2: Implementar o `InputManager`:**
    - Criar a classe `InputManager`.
    - Instanciá-la na `FlowDeconstructApp` e injetá-la na `MainWindow`.
    - Implementar a lógica de `InputMap`/`ActionMap`.

3.  **Fase 3: Migração Incremental dos Atalhos:**
    - Migrar um atalho de cada vez. Comece com um simples, como `F1` (Ajuda).
    - Mova a lógica do `KeyListener` da `MainWindow` para a classe `ShowHelpCommand`.
    - Mapeie o atalho no `InputManager`.
    - Remova o código antigo do `KeyListener`.
    - Repita para todos os outros atalhos de teclado.

4.  **Fase 4: Migração do Gerenciamento do Mouse:**
    - Faça com que `InputManager` implemente as interfaces de mouse.
    - Mova toda a lógica dos métodos `mouseClicked`, `mousePressed`, `mouseDragged`, etc., de `FlowCanvas` para o `InputManager`.
    - Dentro do `InputManager`, chame os `Commands` apropriados (ex: `new DragNodeCommand(...)`).
    - Remova a implementação dos listeners de mouse do `FlowCanvas` e registre o `InputManager` nele.

5.  **Fase 5: Limpeza:**
    - Remover completamente os `KeyListeners` e `KeyEventDispatchers` das classes `MainWindow` e `FlowCanvas`, que se tornarão muito mais limpas e focadas apenas em suas responsabilidades de UI.

## 4. Benefícios

- **Código Desacoplado:** A lógica de ação (o *quê*) fica separada do gerenciamento de eventos (o *quando*).
- **Testabilidade:** As classes `Command` podem ser facilmente instanciadas e testadas unitariamente, pois não dependem diretamente de um evento Swing.
- **Manutenção Simplificada:** Adicionar um novo atalho se resume a criar uma nova classe `Command` e mapeá-la no `InputManager`, sem tocar em classes complexas como `MainWindow`.
- **Resolução de Problemas de Foco:** O uso de `InputMap/ActionMap` no `WHEN_IN_FOCUSED_WINDOW` resolve a maioria dos problemas de foco do teclado de forma elegante.
