#include "historico_pilha.h"
#include <stdlib.h>

typedef struct NoHistorico {
    OperacaoHistorico operacao;
    struct NoHistorico *proximo;
} NoHistorico;

static int operacao_valida(const OperacaoHistorico *operacao) {
    return operacao != NULL && operacao->descricao[0] != '\0';
}

void pilha_inicializar(PilhaHistorico *pilha) {
    if (pilha == NULL) return;
    pilha->topo = NULL;
    pilha->tamanho = 0;
}

int pilha_empilhar(PilhaHistorico *pilha, OperacaoHistorico operacao) {
    if (pilha == NULL || !operacao_valida(&operacao)) return 0;
    NoHistorico *novo = malloc(sizeof(NoHistorico));
    if (novo == NULL) return 0;
    novo->operacao = operacao;
    novo->proximo = pilha->topo;
    pilha->topo = novo;
    pilha->tamanho++;
    return 1;
}

int pilha_desempilhar(PilhaHistorico *pilha, OperacaoHistorico *removida) {
    if (pilha == NULL) return 0;
    if (pilha->topo == NULL) return 0;
    NoHistorico *remover = pilha->topo;
    pilha->topo = remover->proximo;
    if (removida != NULL) *removida = remover->operacao;
    free(remover);
    pilha->tamanho--;
    return 1;
}

OperacaoHistorico *pilha_consultar_topo(PilhaHistorico *pilha) {
    if (pilha == NULL) return NULL;
    return pilha->topo == NULL ? NULL : &pilha->topo->operacao;
}

size_t pilha_tamanho(const PilhaHistorico *pilha) {
    return pilha == NULL ? 0 : pilha->tamanho;
}
int pilha_vazia(const PilhaHistorico *pilha) {
    return pilha == NULL || pilha->topo == NULL;
}

void pilha_liberar(PilhaHistorico *pilha) {
    if (pilha == NULL) return;
    OperacaoHistorico descartada;
    while (pilha_desempilhar(pilha, &descartada)) { }
}
