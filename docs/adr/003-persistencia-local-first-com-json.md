# ADR 003: Persistência Local-First com JSON para Salvamento Automático

**Status:** Aceito

**Contexto:**

A aplicação precisa salvar o trabalho do usuário de forma confiável. Era necessário decidir onde os dados seriam armazenados (localmente ou na nuvem) e em que formato seriam persistidos para o estado interno da aplicação (diferente do formato de import/export).

**Decisão:**

Foi decidido implementar uma estratégia de **persistência local-first**, onde os projetos são salvos automaticamente em um diretório de dados do aplicativo no sistema de arquivos do usuário. O formato de arquivo para essa persistência é **JSON**.

**Justificativa:**

1.  **Velocidade e Disponibilidade Offline:** A abordagem local-first garante que a aplicação seja extremamente rápida, pois não há latência de rede para salvar ou carregar projetos. A aplicação é totalmente funcional sem uma conexão com a internet.

2.  **Privacidade:** Os dados do usuário (diagramas de fluxo, que podem conter informações sensíveis sobre sistemas) permanecem no computador do usuário, o que é uma grande vantagem de segurança e privacidade.

3.  **Simplicidade:** Evita a complexidade e o custo de manter uma infraestrutura de backend e autenticação de usuários.

4.  **Escolha do JSON:**
    - **Estrutura e Aninhamento:** O JSON mapeia naturalmente a estrutura de objetos aninhados do modelo de dados (`FlowDiagram` contendo `FlowNode`, que por sua vez pode conter um `FlowDiagram` de subfluxo).
    - **Ecossistema Java:** A biblioteca **Jackson (`jackson-databind`)** é o padrão de fato para manipulação de JSON em Java. Ela é poderosa, performática e facilita a conversão de objetos Java (POJOs) para JSON e vice-versa com pouca configuração.
    - **Depuração:** Embora não seja tão legível quanto o Markdown customizado, o JSON formatado (com indentação, como configurado no `ProjectManager`) é relativamente fácil de inspecionar para fins de depuração, caso seja necessário verificar um arquivo de projeto (`.flowproj`) corrompido.

**Consequências:**

- **Nenhum Acesso Multi-dispositivo:** A abordagem local-first significa que os projetos não são sincronizados automaticamente entre diferentes computadores. O compartilhamento depende do usuário exportar para Markdown e mover o arquivo manualmente.
- **Gestão de Caminhos de Arquivo:** A classe `ProjectManager` precisa conter lógica para determinar o diretório de dados do aplicativo de forma compatível com diferentes sistemas operacionais (Windows, macOS, Linux).
- **Migração de Esquema:** Se a estrutura dos modelos de dados (`FlowNode`, etc.) mudar no futuro, os arquivos JSON antigos podem não ser mais compatíveis. Seria necessário implementar uma lógica de migração para carregar projetos antigos e convertê-los para o novo formato.
