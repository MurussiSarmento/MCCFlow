package com.sap.flowdeconstruct.ai;

/**
 * Gera um prompt (em PT-BR/PT-PT) para orientar um modelo de IA
 * a converter uma transcrição textual em um Markdown compatível com o
 * esquema de importação do FlowDeconstruct (MarkdownImporter/MarkdownExporter).
 *
 * O objetivo é que o modelo gere "caixinhas" (nós) automaticamente
 * ao produzir um arquivo Markdown no formato suportado pela aplicação.
 */
public class PromptBuilder {

    /**
     * Constrói o prompt completo a partir de uma transcrição livre.
     * @param transcription Texto livre (transcrição) com sistemas/etapas/conexões.
     * @return Prompt final para enviar ao LLM.
     */
    public String buildPrompt(String transcription) {
        StringBuilder sb = new StringBuilder();

        // Contexto e papel
        sb.append("Você é um assistente especialista em mapeamento de fluxos técnicos.\n");
        sb.append("Sua tarefa é transformar a transcrição abaixo em UM arquivo Markdown no ESQUEMA EXATO do FlowDeconstruct,\n");
        sb.append("de forma que o arquivo possa ser importado diretamente e renderize as caixinhas (nós) e conexões.\n\n");

        // Instruções de alto nível
        sb.append("Regras obrigatórias:\n");
        sb.append("1) Saída APENAS o Markdown final (sem explicações, sem cercas de código).\n");
        sb.append("2) Use um TÍTULO H1 (#) para o nome do fluxo.\n");
        sb.append("3) Cada NÓ deve ser uma linha no formato: [ID] Texto do nó\n");
        sb.append("   - IDs devem ser simples e únicos (ex.: N1, N2, N3).\n");
        sb.append("   - O texto do nó é o rótulo da caixinha.\n");
        sb.append("4) Após a lista de nós, crie a seção '## Connections' e liste as conexões uma por linha no formato:\n");
        sb.append("   From: <ID_ORIGEM> To: <ID_DESTINO> (NORMAL) Direction: FORWARD\n");
        sb.append("   - Você pode usar tipos diferentes (ex.: NORMAL), e direção (ex.: FORWARD, BIDIRECTIONAL) quando fizer sentido.\n");
        sb.append("   - FORWARD significa origem→destino; BIDIRECTIONAL significa via dupla. (Também são aceitos FROM_TO, TO_FROM, NONE.)\n");
        sb.append("5) Não use marcações extras (sem bullets/listas), apenas linhas de texto.\n");
        sb.append("6) Se necessário, crie nós intermediários para representar etapas citadas na transcrição.\n");
        sb.append("7) IDs usados nas conexões DEVEM existir na lista de nós.\n");
        sb.append("8) Opcional: você pode adicionar atributos por nó nas linhas seguintes (mesma indentação), como:\n");
        sb.append("   Position: 100, 100\n");
        sb.append("   Size: 160, 60\n");
        sb.append("   Shape: RECTANGLE|SQUARE|CIRCLE|OVAL|DIAMOND\n");
        sb.append("   *Notes: Texto da nota*\n");
        sb.append("   (Esses atributos são opcionais; se não tiver certeza, omita para usar padrões).\n\n");

        // Esquema de referência (derivado do Exporter/Importer)
        sb.append("Esquema de referência (resumo):\n");
        sb.append("# <Nome do Fluxo>\n");
        sb.append("[ID] Texto do Nó\n");
        sb.append("  Position: X, Y  (opcional)\n");
        sb.append("  Size: Largura, Altura  (opcional)\n");
        sb.append("  Shape: RECTANGLE|SQUARE|CIRCLE|OVAL|DIAMOND  (opcional)\n");
        sb.append("  FillColor: #RRGGBB  (opcional)\n");
        sb.append("  BorderColor: #RRGGBB  (opcional)\n");
        sb.append("  TextColor: #RRGGBB  (opcional)\n");
        sb.append("  *Notes: Observação opcional*  (opcional)\n");
        sb.append("\n");
        sb.append("## Connections\n");
        sb.append("From: ID1 To: ID2 (NORMAL) Direction: FORWARD LineColor: #RRGGBB ArrowColor: #RRGGBB Protocol: Texto opcional\n\n");

        // Passos de raciocínio (instruções)
        sb.append("Passos que você deve seguir (internamente, mas NÃO mostre o raciocínio):\n");
        sb.append("- Identifique entidades/sistemas/etapas na transcrição e crie um nó para cada um.\n");
        sb.append("- Defina uma ordem lógica das conexões com base no fluxo descrito (origem → destino).\n");
        sb.append("- Use IDs simples N1..N9. Mantenha consistência.\n");
        sb.append("- Se houver menção de protocolo (HTTP, SFTP, etc.), você pode incluir em Protocol: na conexão.\n");
        sb.append("- Se houver direções dupla via, use Direction: BIDIRECTIONAL. Caso contrário, FORWARD.\n");
        sb.append("- Se existirem subfluxos complexos, priorize primeiro o fluxo principal neste Markdown.\n\n");

        // Exemplo mínimo
        sb.append("Exemplo mínimo (NÃO copiar, apenas seguir o formato):\n");
        sb.append("# Fluxo de Integração\n");
        sb.append("[N1] Sistema A\n");
        sb.append("[N2] Gateway\n");
        sb.append("[N3] Sistema B\n");
        sb.append("\n");
        sb.append("## Connections\n");
        sb.append("From: N1 To: N2 (NORMAL) Direction: FORWARD\n");
        sb.append("From: N2 To: N3 (NORMAL) Direction: FORWARD\n\n");

        // Transcrição do usuário
        sb.append("Transcrição (conteúdo a transformar):\n");
        sb.append("\"\"\"\n");
        if (transcription != null) {
            sb.append(transcription.trim()).append("\n");
        } else {
            sb.append("(vazio)\n");
        }
        sb.append("\"\"\"\n");

        // Reforço final
        sb.append("Gere SOMENTE o Markdown final conforme o esquema. Nada além disso.\n");

        return sb.toString();
    }
}