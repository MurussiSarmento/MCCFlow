# ADR 002: Uso de Markdown Customizado para Importação e Exportação

**Status:** Aceito

**Contexto:**

A aplicação precisa de um formato de arquivo para salvar e carregar os diagramas de fluxo de forma que eles possam ser facilmente compartilhados, versionados e até mesmo editados fora da aplicação. As opções comuns incluem formatos binários, XML, JSON ou formatos textuais como Markdown.

**Decisão:**

Foi decidido criar um **esquema de Markdown customizado** como o principal formato de intercâmbio de dados para os diagramas. A importação e exportação são gerenciadas pelas classes `MarkdownImporter` e `MarkdownExporter`.

**Justificativa:**

1.  **Legibilidade Humana:** Ao contrário de formatos binários ou JSON/XML verbosos, o Markdown é fácil de ler por um ser humano. Um usuário pode abrir um arquivo `.md` do FlowDeconstruct em qualquer editor de texto e entender a estrutura do fluxo.

2.  **Facilidade de Versionamento:** Sendo um formato de texto puro, o Markdown é ideal para sistemas de controle de versão como o Git. As alterações (diffs) são claras e significativas, permitindo rastrear a evolução de um diagrama de fluxo da mesma forma que se rastreia o código-fonte.

3.  **Edição Externa:** A simplicidade do formato permite que usuários avançados façam edições em lote ou gerem diagramas a partir de scripts, sem depender da UI da aplicação.

4.  **Integração com IA (Prompting):** O formato de texto estruturado é perfeito para ser gerado por Modelos de Linguagem Grandes (LLMs). A classe `PromptBuilder` demonstra isso: é mais fácil instruir uma IA a gerar este formato de Markdown do que a gerar um JSON complexo e aninhado, democratizando a criação de diagramas a partir de texto livre.

**Consequências:**

- **Necessidade de um Parser Customizado:** A decisão exigiu a criação de um `MarkdownImporter` específico, que sabe como interpretar as convenções do esquema (ex: `[ID] Texto do Nó`, `## Connections`). A biblioteca `commonmark` foi usada para o parsing base do Markdown, mas a lógica de interpretação é proprietária.
- **Risco de Formato Inválido:** Se um usuário editar o arquivo `.md` manualmente, ele pode quebrar a sintaxe esperada pelo importador. A implementação do `MarkdownImporter` precisa ser robusta para lidar com erros de formatação, usando valores padrão quando atributos estiverem ausentes ou malformados.
- **Manutenção do Esquema:** Qualquer nova propriedade persistente em um `FlowNode` or `FlowConnection` (ex: um novo estilo visual) precisa ser adicionada tanto ao `MarkdownExporter` quanto ao `MarkdownImporter` para garantir a consistência na serialização e desserialização (round-trip).
