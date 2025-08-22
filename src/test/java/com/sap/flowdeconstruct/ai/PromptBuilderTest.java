package com.sap.flowdeconstruct.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PromptBuilderTest {

    @Test
    public void testBuildPromptBasicStructure() {
        String transcription = "alpha envia dados para beta; beta processa e envia para gamma";
        PromptBuilder builder = new PromptBuilder();
        String prompt = builder.buildPrompt(transcription);

        // Estrutura e instruções essenciais
        Assertions.assertTrue(prompt.contains("Você é um assistente especialista"));
        Assertions.assertTrue(prompt.contains("transformar a transcrição abaixo em UM arquivo Markdown"));
        Assertions.assertTrue(prompt.contains("Regras obrigatórias:"));
        Assertions.assertTrue(prompt.contains("1) Saída APENAS o Markdown final"));
        Assertions.assertTrue(prompt.contains("2) Use um TÍTULO H1 (#) para o nome do fluxo."));
        Assertions.assertTrue(prompt.contains("## Connections"));
        Assertions.assertTrue(prompt.contains("Esquema de referência (resumo):"));
        Assertions.assertTrue(prompt.contains("Exemplo mínimo (NÃO copiar, apenas seguir o formato):"));
        Assertions.assertTrue(prompt.contains("Transcrição (conteúdo a transformar):"));

        // A transcrição deve estar embutida no prompt
        Assertions.assertTrue(prompt.contains("alpha envia dados para beta"));
        Assertions.assertTrue(prompt.contains("beta processa e envia para gamma"));

        // Reforço final
        Assertions.assertTrue(prompt.contains("Gere SOMENTE o Markdown final"));
    }

    @Test
    public void testBuildPromptHandlesNullTranscription() {
        PromptBuilder builder = new PromptBuilder();
        String prompt = builder.buildPrompt(null);
        Assertions.assertTrue(prompt.contains("(vazio)"));
    }
}