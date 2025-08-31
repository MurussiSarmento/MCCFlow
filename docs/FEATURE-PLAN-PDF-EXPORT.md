# Plano de Feature: Exportação para PDF

**Status:** Proposto

## 1. Objetivo

Permitir que os usuários exportem a visualização de seu diagrama de fluxo, incluindo nós, conexões e anotações, para um arquivo PDF. Isso facilitará o compartilhamento com stakeholders não-técnicos e a criação de documentação formal.

## 2. Análise Técnica

- **Biblioteca:** O projeto já inclui a dependência `com.itextpdf:itext7-core` no `pom.xml`, que é a biblioteca principal para a criação de PDFs com iText7.
- **Código Existente:**
    - `ExportDialog.java` já possui a opção "PDF Document" no seletor de formato, mas a lógica não está implementada.
    - `FlowCanvas.java` contém toda a lógica de renderização 2D no método `paintComponent` e seus auxiliares (`drawNodes`, `drawConnections`, `drawArrowHead`, etc.).
- **Desafio Principal:** A renderização do Swing é feita em um `Graphics2D` de um componente na tela. A exportação para PDF requer que essa mesma lógica de desenho seja aplicada a um `Graphics2D` que, em vez de desenhar na tela, desenha em um canvas de PDF.

## 3. Plano de Implementação

A implementação será dividida em 4 etapas principais.

### Etapa 1: Criar a Classe `PdfExporter`

1.  **Criar o Arquivo:** No pacote `com.sap.flowdeconstruct.export`, criar uma nova classe `PdfExporter.java`.
2.  **Estrutura da Classe:** A classe terá um método público principal:
    ```java
    public class PdfExporter {
        public void export(
            FlowDiagram diagram,
            String filePath,
            boolean includeNotes,
            boolean includeSubflows) throws IOException {
            // Lógica de exportação principal aqui
        }
    }
    ```

### Etapa 2: Integrar `PdfExporter` com a UI

1.  **Modificar `MainWindow.java`:** No método `exportFlow()`, após o diálogo de exportação ser confirmado, adicionar uma verificação do formato selecionado.
    ```java
    // Dentro de exportFlow() em MainWindow.java
    if (dialog.isConfirmed()) {
        String filePath = dialog.getFilePath();
        ExportDialog.ExportFormat format = dialog.getSelectedFormat();

        if (format == ExportDialog.ExportFormat.MARKDOWN) {
            // Lógica existente para Markdown
            projectManager.saveToMarkdown(filePath, ...);
        } else if (format == ExportDialog.ExportFormat.PDF) {
            try {
                PdfExporter pdfExporter = new PdfExporter();
                pdfExporter.export(projectManager.getCurrentProject(), filePath, dialog.isIncludeNotes(), dialog.isIncludeSubflows());
                JOptionPane.showMessageDialog(this, "Exportado para PDF com sucesso!", ...);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao exportar para PDF: " + ex.getMessage(), ...);
            }
        }
    }
    ```

### Etapa 3: Implementar a Lógica de Renderização em PDF

Esta é a etapa central, a ser implementada dentro do método `export()` da classe `PdfExporter`.

1.  **Calcular Dimensões do Fluxo:** Antes de criar o PDF, é preciso determinar o tamanho total do diagrama. Itere por todos os nós do `FlowDiagram` para encontrar as coordenadas `minX`, `minY`, `maxX`, e `maxY`, criando um retângulo (`Rectangle`) que engloba todo o fluxo.

2.  **Inicializar o Documento PDF:**
    - Crie um `PdfWriter` com o `filePath` fornecido.
    - Crie um `PdfDocument` a partir do `writer`.
    - Determine o tamanho da página. Use `PageSize.A4.rotate()` para uma página A4 em modo paisagem, que é mais adequado para diagramas de fluxo.
    - Crie uma `PdfPage` no documento.

3.  **Criar o Canvas Gráfico 2D:**
    - A biblioteca `itext.svg` (uma dependência transitiva ou a ser adicionada) fornece a classe `SvgCanvas` que pode ser usada para criar um `Graphics2D` a partir de um `PdfCanvas`. Esta é a ponte entre o iText e a API de desenho do Java 2D.
    - Obtenha o `PdfCanvas` da página recém-criada.
    - Crie uma instância de `Graphics2D` a partir do `PdfCanvas`.

4.  **Reutilizar e Adaptar a Lógica de Desenho:**
    - **Refatoração Leve:** Mova os métodos de desenho puro (como `drawNode`, `drawConnection`, `drawArrowHead`) de `FlowCanvas` para uma nova classe utilitária, por exemplo, `FlowRendererUtils.java`. Esses métodos devem aceitar um `Graphics2D` como primeiro argumento.
    - **Desenhar no PDF:** No `PdfExporter`, chame esses métodos utilitários, passando o `Graphics2D` do PDF como argumento. A lógica de iteração sobre os nós e conexões será similar à do método `paintComponent` do `FlowCanvas`.

5.  **Escalonamento (Scaling):**
    - Calcule a escala necessária para que o retângulo do fluxo (calculado na etapa 1) caiba nas dimensões da página A4, mantendo uma margem.
    - Aplique uma transformação de escala (`g2d.scale(scaleFactor, scaleFactor)`) ao objeto `Graphics2D` do PDF antes de começar a desenhar.

6.  **Finalizar o Documento:** Feche o `PdfDocument`. Isso salva o arquivo no disco.

### Etapa 4: Lidar com Fontes e Opções

1.  **Fontes:** Para garantir que o texto apareça corretamente em qualquer sistema, as fontes devem ser embutidas. Use `PdfFontFactory.createFont()` com as fontes padrão (ex: `StandardFonts.HELVETICA`) para criar objetos `PdfFont` e configure-os no `PdfCanvas` antes de desenhar o texto.

2.  **Opção `includeNotes`:** Se esta opção estiver ativa, após desenhar cada nó, desenhe o texto das anotações (`node.getNotes()`) abaixo ou ao lado do respectivo nó, usando uma fonte menor.

3.  **Opção `includeSubflows`:** A primeira versão da implementação deve focar no fluxo principal. A presença de um subfluxo será indicada pelo ícone que já é desenhado no nó. A renderização de subfluxos em páginas separadas pode ser uma melhoria futura.

## 4. Verificação

Após a implementação, a funcionalidade deve ser verificada:
- Exporte um diagrama simples e verifique se o PDF é gerado corretamente.
- Exporte um diagrama com nós customizados (cores, formas) e verifique se a aparência é mantida.
- Teste a opção de incluir notas.
- Verifique se o arquivo PDF é aberto corretamente nos leitores de PDF mais comuns.
