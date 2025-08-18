# Projeto: Correção da Movimentação de Nós

## Análise do Problema

### Problemas Identificados

1. **Erro de Sintaxe no MainWindow.java**
   - O método `moveSelectedNode` está declarado dentro do switch case, causando erro de compilação
   - Linha 624: `private void moveSelectedNode(int dx, int dy) {` está no local incorreto

2. **Variável moveMode não declarada**
   - A variável `moveMode` é usada mas não foi declarada como campo da classe
   - Necessária para controlar quando o movimento por teclado está ativo

3. **Método wouldOverlap não é público**
   - O método `wouldOverlap` no FlowCanvas é privado
   - MainWindow não consegue acessá-lo para verificar sobreposições

4. **Movimento por mouse pode não estar funcionando**
   - Implementação de arrastar com mouse existe mas pode ter problemas de coordenadas
   - Transformação de coordenadas pode estar incorreta

## Plano de Correção

### Fase 1: Correção de Erros de Sintaxe
1. Mover o método `moveSelectedNode` para fora do switch case
2. Declarar a variável `moveMode` como campo da classe
3. Tornar o método `wouldOverlap` público no FlowCanvas

### Fase 2: Melhorias na Movimentação
1. Corrigir transformação de coordenadas no movimento por mouse
2. Adicionar feedback visual durante o movimento
3. Implementar snap-to-grid opcional
4. Adicionar limites de canvas para evitar nós fora da área visível

### Fase 3: Testes e Validação
1. Testar movimento por mouse (arrastar e soltar)
2. Testar movimento por teclado (Ctrl+M + Shift+Setas)
3. Verificar prevenção de sobreposição
4. Validar persistência das posições

## Implementação Detalhada

### 1. Correções no MainWindow.java

#### Adicionar campo moveMode:
```java
private boolean moveMode = false;
```

#### Mover método moveSelectedNode para local correto:
```java
private void moveSelectedNode(int dx, int dy) {
    FlowNode node = currentFlow.getSelectedNode();
    int newX = (int)node.getX() + dx;
    int newY = (int)node.getY() + dy;
    if (!canvas.wouldOverlap(node, newX, newY)) {
        node.setPosition(newX, newY);
        canvas.repaint();
    }
}
```

### 2. Correções no FlowCanvas.java

#### Tornar wouldOverlap público:
```java
public boolean wouldOverlap(FlowNode movingNode, int newX, int newY) {
    // implementação existente
}
```

#### Melhorar movimento por mouse:
```java
@Override
public void mouseDragged(MouseEvent e) {
    if (lastMousePos == null) return;
    
    if (draggingNode != null) {
        // Calcular nova posição em coordenadas do mundo
        Point2D.Double currentWorld = screenToWorld(e.getPoint());
        Point2D.Double lastWorld = screenToWorld(lastMousePos);
        
        int dx = (int)(currentWorld.x - lastWorld.x);
        int dy = (int)(currentWorld.y - lastWorld.y);
        
        int newX = (int)draggingNode.getX() + dx;
        int newY = (int)draggingNode.getY() + dy;
        
        // Aplicar limites do canvas
        newX = Math.max(CANVAS_MARGIN, Math.min(newX, 2000 - NODE_WIDTH - CANVAS_MARGIN));
        newY = Math.max(CANVAS_MARGIN, Math.min(newY, 1500 - NODE_HEIGHT - CANVAS_MARGIN));
        
        if (!wouldOverlap(draggingNode, newX, newY)) {
            draggingNode.setPosition(newX, newY);
            repaint();
        }
    } else {
        // Pan da visualização
        int dx = e.getX() - lastMousePos.x;
        int dy = e.getY() - lastMousePos.y;
        viewOffset.x += dx;
        viewOffset.y += dy;
        repaint();
    }
    
    lastMousePos = e.getPoint();
    dragging = true;
}
```

### 3. Melhorias Adicionais

#### Feedback visual durante movimento:
- Destacar o nó sendo movido
- Mostrar posição de destino
- Indicar quando movimento é bloqueado por sobreposição

#### Controles de teclado aprimorados:
- Ctrl+M: Ativar/desativar modo de movimento
- Shift+Setas: Mover nó selecionado (10px por vez)
- Ctrl+Shift+Setas: Movimento fino (1px por vez)
- Alt+Shift+Setas: Movimento rápido (50px por vez)

## Casos de Teste

### Teste 1: Movimento por Mouse
1. Criar alguns nós
2. Selecionar um nó
3. Arrastar com o mouse
4. Verificar se o nó se move suavemente
5. Verificar se não sobrepõe outros nós

### Teste 2: Movimento por Teclado
1. Ativar modo de movimento (Ctrl+M)
2. Selecionar um nó
3. Usar Shift+Setas para mover
4. Verificar movimento em todas as direções
5. Verificar prevenção de sobreposição

### Teste 3: Limites do Canvas
1. Mover nó para as bordas do canvas
2. Verificar se não sai da área visível
3. Testar com zoom aplicado

### Teste 4: Persistência
1. Mover nós para novas posições
2. Salvar projeto
3. Recarregar projeto
4. Verificar se posições foram mantidas

## Riscos e Mitigações

### Risco 1: Performance
- **Problema**: Movimento pode ser lento com muitos nós
- **Mitigação**: Otimizar repaint apenas da área afetada

### Risco 2: Coordenadas Incorretas
- **Problema**: Transformação de coordenadas pode estar errada
- **Mitigação**: Testes extensivos com diferentes níveis de zoom

### Risco 3: Conflitos de Eventos
- **Problema**: Eventos de mouse e teclado podem conflitar
- **Mitigação**: Priorização clara de eventos e estados

## Cronograma

- **Dia 1**: Correção de erros de sintaxe e compilação
- **Dia 2**: Implementação de movimento por mouse melhorado
- **Dia 3**: Implementação de movimento por teclado aprimorado
- **Dia 4**: Testes e refinamentos
- **Dia 5**: Documentação e validação final

## Critérios de Sucesso

1. ✅ Código compila sem erros
2. ✅ Nós podem ser movidos com mouse (arrastar e soltar)
3. ✅ Nós podem ser movidos com teclado (Shift+Setas)
4. ✅ Prevenção de sobreposição funciona
5. ✅ Movimento respeita limites do canvas
6. ✅ Posições são persistidas corretamente
7. ✅ Performance adequada com múltiplos nós