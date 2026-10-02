# Estruturas da AV1 em C

Este diretório contém a implementação manual das estruturas básicas do domínio Rota Vital.

## Estruturas

- `ListaEstoque`: lista simplesmente encadeada para itens de estoque.
- `FilaRequisicoes`: fila FIFO encadeada para requisições hospitalares.
- `PilhaHistorico`: pilha LIFO encadeada para registrar operações.

Cada inserção cria um nó com `malloc`. Cada remoção e a função de encerramento liberam o nó com `free`.
Os ponteiros `inicio`, `fim` e `topo` são manipulados explicitamente.

## Compilar e testar

Com GCC e Make instalados:

```bash
make test
```

O teste usa `assert` e verifica inserir, consultar, remover, ordem FIFO/LIFO, duplicidade e liberação.

Não há roteirização, FEFO, hash ou compatibilidade ABO/Rh nesta pasta: esses algoritmos pertencem à Unidade 2.
