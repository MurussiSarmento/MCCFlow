# Plano de Refinamento da UI/UX

**Status:** Proposto

## 1. Visão Geral e Princípios

A interface atual do FlowDeconstruct é funcional e possui uma identidade visual (tema escuro), mas carece de consistência e centralização. O código para estilizar e posicionar componentes é repetido em várias classes de diálogo, tornando as manutenções e alterações globais difíceis e propensas a erros.

**Princípios Orientadores:**

1.  **DRY (Don't Repeat Yourself):** Centralizar a definição de cores, fontes e a criação de componentes customizados.
2.  **Consistência:** Garantir que todos os diálogos e componentes tenham espaçamento, alinhamento e comportamento padronizados.
3.  **Feedback Aprimorado:** Fornecer ao usuário feedback mais claro e menos intrusivo sobre as ações realizadas.
4.  **Inteligência Contextual:** Tornar a UI mais inteligente, habilitando/desabilitando controles com base no contexto atual.

## 2. Proposta de Refatoração

### 2.1. Centralização de Estilos (Theming)

**Problema:** Cores, fontes e bordas são definidas como constantes estáticas em múltiplas classes (`FlowCanvas`, `ExportDialog`, `NoteDialog`, etc.), levando à duplicação de código.

**Solução Proposta:**

1.  **Criar um Pacote de Tema:** Criar um novo pacote `com.sap.flowdeconstruct.ui.theme`.

2.  **Criar a Classe `UITheme`:**
    - Mover todas as constantes de `Color` e `Font` para uma única classe `UITheme.java`.
    - Exemplo:
      ```java
      // com.sap.flowdeconstruct.ui.theme.UITheme.java
      public final class UITheme {
          public static final Color BACKGROUND_COLOR = new Color(0x2d, 0x2d, 0x2d);
          public static final Color PANEL_COLOR = new Color(0x3a, 0x3a, 0x3a);
          public static final Color TEXT_COLOR = new Color(0xcc, 0xcc, 0xcc);
          public static final Color ACCENT_COLOR = new Color(0x5f, 0x9e, 0xa0);
          public static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
          // ... etc
      }
      ```

3.  **Criar a Classe `ComponentFactory`:**
    - Criar uma fábrica de componentes `ComponentFactory.java` que use a classe `UITheme`.
    - Esta fábrica terá métodos estáticos para criar os componentes mais comuns já estilizados, eliminando a lógica de estilização repetida.
    - Exemplo:
      ```java
      // com.sap.flowdeconstruct.ui.theme.ComponentFactory.java
      public class ComponentFactory {
          public static JButton createPrimaryButton(String text) {
              JButton button = new JButton(text);
              button.setBackground(UITheme.PANEL_COLOR);
              button.setForeground(UITheme.TEXT_COLOR);
              button.setFont(UITheme.MONO_FONT);
              // ... (lógica de hover, borda, etc.)
              return button;
          }

          public static JLabel createDialogTitle(String text) { ... }
          public static JTextField createStyledTextField() { ... }
      }
      ```

**Impacto:** Classes como `ExportDialog` não terão mais métodos como `createStyledButton`. Em vez disso, chamarão `ComponentFactory.createPrimaryButton(...)`. A mudança de uma cor ou fonte em `UITheme` se refletirá em toda a aplicação.

### 2.2. Padronização de Layouts

**Problema:** Os diálogos usam uma mistura de `BorderLayout`, `FlowLayout` e `BoxLayout`, resultando em alinhamentos e espaçamentos que podem ser inconsistentes. O código de layout é complexo e difícil de manter.

**Solução Proposta:**

1.  **Adotar `GridBagLayout` como Padrão:** Para formulários e diálogos com múltiplos campos, `GridBagLayout` deve ser o padrão. Ele oferece controle granular sobre o alinhamento, peso e tamanho dos componentes, garantindo que todos os formulários tenham uma aparência coesa.

2.  **Definir Padrões de Espaçamento:** Estabelecer regras de espaçamento e usá-las de forma consistente.
    - **Margem Externa do Diálogo:** `BorderFactory.createEmptyBorder(12, 16, 12, 16)`
    - **Espaçamento Vertical entre Seções:** `Box.createVerticalStrut(16)`
    - **Espaçamento Horizontal entre Botões:** `Box.createHorizontalStrut(8)`

**Exemplo (Aplicado ao `ExportDialog.java`):**
O `contentPanel` do `ExportDialog` poderia ser reescrito com um único `GridBagLayout`, garantindo que os rótulos (`Exportar para:`, `Formato:`) e os componentes (`JTextField`, `JComboBox`) estejam perfeitamente alinhados verticalmente e horizontalmente, o que é difícil de garantir com `BoxLayout` e `BorderLayout` aninhados.

### 2.3. Melhoria no Feedback do Usuário

**Problema:** A aplicação usa `JOptionPane` para quase todo tipo de feedback (erros, sucesso, informações). Isso pode ser intrusivo e interromper o fluxo do usuário para mensagens simples.

**Solução Proposta:**

1.  **Criar uma Barra de Status:** Adicionar uma barra de status temporária na parte inferior da `MainWindow`.
    - Ações de sucesso que não requerem confirmação (ex: "Prompt copiado para a área de transferência!", "Projeto salvo") podem exibir uma mensagem nesta barra por alguns segundos e depois desaparecer.
    - Isso evita que o usuário precise clicar em "OK" em um `JOptionPane`.

2.  **Feedback Visual Direto:**
    - Ao criar um novo nó, além de selecioná-lo, ele poderia piscar brevemente ou ter uma animação de "fade-in" para chamar a atenção do usuário para sua localização.

### 2.4. Diálogos com Inteligência Contextual

**Problema:** Os diálogos apresentam todas as opções, mesmo aquelas que não fazem sentido no contexto atual.

**Solução Proposta:**

- Os diálogos devem receber o estado atual do `FlowDiagram` e ajustar a UI de acordo.
- **Exemplo (Aplicado ao `ExportDialog.java`):**
    - O construtor do `ExportDialog` deve receber o `FlowDiagram`: `public ExportDialog(Frame parent, FlowDiagram diagram)`.
    - Dentro do diálogo, verificar:
      ```java
      // Desabilitar checkbox se não houver notas no diagrama
      boolean hasNotes = diagram.getNodes().stream().anyMatch(FlowNode::hasNotes);
      includeNotesCheckBox.setEnabled(hasNotes);

      // Desabilitar checkbox se não houver subfluxos
      boolean hasSubflows = diagram.getNodes().stream().anyMatch(FlowNode::hasSubFlow);
      includeSubflowsCheckBox.setEnabled(hasSubflows);
      ```

## 4. Plano de Ação

1.  **Fase 1 (Fundação):** Implementar as classes `UITheme` e `ComponentFactory`. Refatorar uma classe de diálogo (ex: `SettingsDialog`) para usar a nova fábrica.
2.  **Fase 2 (Layouts):** Refatorar os diálogos mais complexos (`ExportDialog`, `ImportDialog`) para usar `GridBagLayout` e os padrões de espaçamento.
3.  **Fase 3 (Feedback):** Implementar a barra de status na `MainWindow` e converter as mensagens de confirmação para usá-la.
4.  **Fase 4 (Contexto):** Modificar os construtores dos diálogos para aceitar o `FlowDiagram` e implementar a lógica de habilitação/desabilitação de componentes.
