#ifndef HISTORICO_PILHA_H
#define HISTORICO_PILHA_H

#include <stddef.h>

typedef struct {
    int tipo;
    int referencia;
    char descricao[120];
} OperacaoHistorico;

typedef struct NoHistorico NoHistorico;
typedef struct {
    NoHistorico *topo;
    size_t tamanho;
} PilhaHistorico;

void pilha_inicializar(PilhaHistorico *pilha);
int pilha_empilhar(PilhaHistorico *pilha, OperacaoHistorico operacao);
int pilha_desempilhar(PilhaHistorico *pilha, OperacaoHistorico *removida);
OperacaoHistorico *pilha_consultar_topo(PilhaHistorico *pilha);
size_t pilha_tamanho(const PilhaHistorico *pilha);
int pilha_vazia(const PilhaHistorico *pilha);
void pilha_liberar(PilhaHistorico *pilha);

#endif
