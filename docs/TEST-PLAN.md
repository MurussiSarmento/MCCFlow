# Plano de Testes Abrangente

**Status:** Proposto

## 1. Visão Geral e Estratégia

**Objetivo:** Aumentar a confiança nas alterações de código, prevenir regressões e garantir que as funcionalidades principais do FlowDeconstruct operem conforme o esperado. Um conjunto de testes robusto é essencial para permitir refatorações seguras e a adição de novas features.

**Ferramentas:** O projeto já está configurado com **JUnit 5** (`junit-jupiter`), que será a base para os testes.

**Estratégia (Pirâmide de Testes):**

1.  **Base - Testes Unitários (Muitos):** Testes rápidos e focados que verificam uma única classe ou método em isolamento. Serão a grande maioria dos testes.
2.  **Meio - Testes de Integração (Alguns):** Verificam a interação entre duas ou mais partes do sistema, como a interação do `ProjectManager` com o sistema de arquivos ou o ciclo completo de exportação/importação.
3.  **Topo - Testes de UI (Poucos):** Testes lentos e complexos que simulam a interação do usuário com a interface gráfica. Serão usados para os fluxos mais críticos.

## 2. Plano de Testes Unitários

O foco é testar a lógica de negócio pura, isolando-a de dependências externas como a UI ou o sistema de arquivos.

#### Prioridade Alta

-   **`ProjectManager`:**
    -   **Técnica:** Usar a funcionalidade de diretórios temporários do JUnit 5 (`@TempDir`) para não interagir com o sistema de arquivos real do usuário.
    -   **Cenários:**
        -   `createNewProject()`: Verifica se um novo `FlowDiagram` não nulo é criado.
        -   `loadProject()`: Testa o carregamento de um arquivo JSON válido, o tratamento de um arquivo JSON malformado (deve lançar exceção) e o que acontece se o arquivo não existir.
        -   `saveCurrentProject()`: Verifica se o arquivo `.flowproj` é escrito corretamente no diretório temporário.

-   **`MarkdownImporter`:**
    -   **Técnica:** Criar arquivos de recurso `.md` na pasta `src/test/resources` com diferentes cenários.
    -   **Cenários:**
        -   Importar um arquivo Markdown válido e verificar se o objeto `FlowDiagram` resultante contém todos os nós, conexões e atributos corretos (cores, formas, etc.).
        -   Importar um arquivo com sintaxe inválida (ex: ID de conexão que não existe) e garantir que ele não quebre, mas lide com o erro de forma graciosa.
        -   Testar a importação de notas com caracteres especiais e quebras de linha.

-   **`FlowDiagram` (Lógica do Modelo):**
    -   **Técnica:** Testes de POJO puros.
    -   **Cenários:**
        -   `addNode()` / `removeNode()`: Verificar se a lista de nós é atualizada corretamente e se as conexões associadas ao nó removido também são limpas.
        -   `addConnection()`: Testar a adição de uma conexão válida e a tentativa de adicionar uma conexão duplicada (deve ser ignorada).
        -   `setSelectedNode()`: Verificar se o nó anterior é desmarcado e o novo é marcado como selecionado.

#### Prioridade Média

-   **`PromptBuilder`:** Expandir os testes existentes para cobrir mais variações de transcrições de texto, incluindo casos sem nenhuma entidade clara ou com ambiguidades.
-   **`I18n`:** Expandir os testes para verificar o fallback para o idioma padrão (`messages.properties`) quando uma chave não existe no idioma selecionado (ex: `messages_pt_BR.properties`).

## 3. Plano de Testes de Integração

Estes testes garantem que os componentes principais funcionam bem em conjunto.

-   **`MarkdownRoundtripTest` (Teste de Ciclo Completo - Prioridade Altíssima):**
    -   **Estratégia:** Este é o teste mais importante para a integridade dos dados. O teste existente deve ser expandido.
    -   **Cenário:**
        1.  Criar programaticamente um objeto `FlowDiagram` complexo, com múltiplos nós, conexões, customizações de cor/forma, notas e subfluxos.
        2.  Usar o `MarkdownExporter` para salvar este objeto em uma string ou arquivo temporário.
        3.  Usar o `MarkdownImporter` para carregar essa string/arquivo de volta para um novo objeto `FlowDiagram`.
        4.  **Assert:** Fazer uma comparação profunda (`deep equals`) entre o objeto original e o objeto reimportado para garantir que nenhuma informação foi perdida no processo.

-   **`ProjectLifecycleTest` (Prioridade Alta):**
    -   **Estratégia:** Simular uma sessão de usuário completa, testando a integração do `ProjectManager` com o sistema de arquivos.
    -   **Cenário:**
        1.  Usar um diretório `@TempDir`.
        2.  Criar um novo projeto.
        3.  Adicionar/modificar nós.
        4.  Chamar `saveCurrentProject()`.
        5.  Verificar se o arquivo `.flowproj` existe e seu conteúdo parece correto.
        6.  Instanciar um novo `ProjectManager` e chamar `loadProject()` com o caminho do arquivo salvo.
        7.  Verificar se o projeto carregado é idêntico ao que foi salvo.

## 4. Plano de Testes de UI (End-to-End)

Testar UIs Swing é complexo. A abordagem será focar nos fluxos mais críticos usando uma biblioteca de automação.

-   **Ferramenta Recomendada:** **AssertJ-Swing**.
    -   É uma biblioteca fluente e intuitiva para testes de UI em Swing. Exemplo: `window.button("okButton").click();`

-   **Cenários Críticos para Automação:**
    1.  **Criação de Nó e Conexão:**
        -   Iniciar a aplicação.
        -   Simular o pressionar da tecla `Tab`.
        -   Verificar se um novo `FlowNode` foi adicionado ao modelo de dados do `FlowDiagram`.
        -   Simular a digitação de texto e `Enter`.
        -   Verificar se o texto do nó foi atualizado.

    2.  **Exportação via UI:**
        -   Abrir a aplicação com um projeto de exemplo.
        -   Simular cliques no menu `Arquivo` -> `Exportar`.
        -   Interagir com o `ExportDialog` para preencher um caminho de arquivo (em um diretório temporário) e clicar em "Exportar".
        -   Verificar se o arquivo `.md` foi efetivamente criado no disco.

    3.  **Mudança de Idioma:**
        -   Abrir a aplicação.
        -   Abrir o `SettingsDialog`.
        -   Selecionar "Português (Brasil)" no `JComboBox` e clicar em "OK".
        -   Verificar se o título da `MainWindow` ou o texto do menu `Arquivo` foi alterado para a string em português.

**Nota sobre Refatoração:** A implementação do plano de refatoração do `InputManager` (Tarefa 4) simplificará enormemente os testes. Será possível testar as `Actions` e `Commands` diretamente, com menos necessidade de simular cliques de baixo nível na UI.
