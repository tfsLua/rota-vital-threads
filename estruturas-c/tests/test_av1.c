#include <assert.h>
#include <stdio.h>
#include <string.h>
#include "estoque_lista.h"
#include "requisicoes_fila.h"
#include "historico_pilha.h"

static void testar_lista(void) {
    ListaEstoque lista;
    estoque_inicializar(&lista);
    ItemEstoque soro = {10, "Soro fisiologico", 50, "un"};
    ItemEstoque sangue = {20, "Bolsa de sangue", 8, "bolsas"};
    ItemEstoque invalido = {0, "", -1, ""};
    assert(estoque_vazia(&lista));
    assert(estoque_inserir(&lista, soro));
    assert(estoque_inserir(&lista, sangue));
    assert(!estoque_inserir(&lista, soro));
    assert(!estoque_inserir(&lista, invalido));
    assert(estoque_tamanho(&lista) == 2);
    assert(estoque_buscar(&lista, 20)->quantidade == 8);
    ItemEstoque removido;
    assert(estoque_remover(&lista, 10, &removido));
    assert(removido.codigo == 10 && estoque_tamanho(&lista) == 1);
    assert(!estoque_remover(&lista, 999, NULL));
    estoque_liberar(&lista);
    assert(estoque_vazia(&lista));
    assert(estoque_inserir(&lista, soro));
    estoque_liberar(&lista);
}

static void testar_fila(void) {
    FilaRequisicoes fila;
    fila_inicializar(&fila);
    Requisicao a = {1, 101, 10, 2, 1};
    Requisicao b = {2, 102, 20, 1, 0};
    Requisicao invalida = {0, 0, 0, 0, -1};
    assert(!fila_enfileirar(&fila, invalida));
    assert(fila_enfileirar(&fila, a));
    assert(fila_enfileirar(&fila, b));
    assert(fila_consultar_primeiro(&fila)->codigo == 1);
    Requisicao removida;
    assert(fila_desenfileirar(&fila, &removida) && removida.codigo == 1);
    assert(fila_desenfileirar(&fila, &removida) && removida.codigo == 2);
    assert(fila_vazia(&fila));
    assert(!fila_desenfileirar(&fila, NULL));
    fila_liberar(&fila);
}

static void testar_pilha(void) {
    PilhaHistorico pilha;
    pilha_inicializar(&pilha);
    OperacaoHistorico a = {1, 10, "Entrada de estoque"};
    OperacaoHistorico b = {2, 1, "Requisicao atendida"};
    OperacaoHistorico invalida = {0, 0, ""};
    assert(!pilha_empilhar(&pilha, invalida));
    assert(pilha_empilhar(&pilha, a));
    assert(pilha_empilhar(&pilha, b));
    assert(strcmp(pilha_consultar_topo(&pilha)->descricao, "Requisicao atendida") == 0);
    OperacaoHistorico removida;
    assert(pilha_desempilhar(&pilha, &removida) && removida.tipo == 2);
    assert(pilha_desempilhar(&pilha, &removida) && removida.tipo == 1);
    assert(pilha_vazia(&pilha));
    assert(!pilha_desempilhar(&pilha, NULL));
    pilha_liberar(&pilha);
}

int main(void) {
    testar_lista();
    testar_fila();
    testar_pilha();
    puts("AV1 C: todos os testes passaram.");
    return 0;
}
