# ADR 001: Escolha de Java Swing para a Interface de Usuário

**Status:** Aceito

**Contexto:**

A aplicação FlowDeconstruct é uma ferramenta de desktop que requer uma interface gráfica (GUI) para renderizar o canvas de fluxos, os nós, as conexões e os diálogos de interação. A escolha da tecnologia de UI é uma decisão fundamental que impacta o desenvolvimento, a performance, a portabilidade e a manutenção do projeto.

**Decisão:**

Foi decidido utilizar **Java Swing** como o framework para a construção de toda a interface de usuário.

**Justificativa:**

1.  **Portabilidade:** Sendo parte do Java Standard Edition, o Swing é inerentemente multiplataforma (Windows, macOS, Linux), alinhado com a natureza de uma ferramenta para desenvolvedores que podem usar diferentes sistemas operacionais.

2.  **Renderização Customizada:** O requisito principal da aplicação é um `FlowCanvas` com renderização 2D customizada. O Swing oferece um controle de baixo nível sobre o pipeline de renderização através do método `paintComponent`, o que é ideal para desenhar formas, textos e linhas de forma eficiente e customizada, como o projeto exige.

3.  **Performance:** Para uma aplicação focada em velocidade ("ultra-fast"), o Swing é uma tecnologia madura e com performance consolidada para aplicações 2D. A aceleração de hardware (via OpenGL pipeline, como ativado na `FlowDeconstructApp`) garante que a renderização do canvas seja fluida.

4.  **Nenhuma Dependência Web:** A aplicação foi concebida como uma ferramenta local-first, sem a necessidade de um servidor web ou de componentes de navegador. O uso de Swing evita a complexidade de frameworks que misturam tecnologias web (HTML/CSS/JS) com o backend Java (como JavaFX com WebView ou Electron), mantendo a aplicação autocontida.

5.  **Ecossistema e Maturidade:** O Swing é uma API estável e bem documentada. Embora seja mais antiga, sua maturidade significa que há poucos bugs inesperados no framework em si, e uma vasta quantidade de recursos disponíveis.

**Consequências:**

- **Aparência (Look and Feel):** O Swing pode ter uma aparência datada em comparação com frameworks mais modernos. Isso foi mitigado no projeto através da criação de um tema escuro customizado e da estilização manual dos componentes para garantir uma UI coesa e agradável, em vez de depender do Look and Feel padrão do sistema.
- **Curva de Aprendizagem:** Desenvolvedores mais novos podem não ter familiaridade com o Swing. No entanto, a API é direta e o padrão de componentização é claro, facilitando o aprendizado no contexto do projeto.
- **Manutenção:** O código da UI é verboso, como é característico do Swing. A separação clara entre os componentes da UI (diálogos, canvas, janela principal) é crucial para manter o código organizado.
