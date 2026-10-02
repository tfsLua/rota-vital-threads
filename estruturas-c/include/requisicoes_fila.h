#ifndef REQUISICOES_FILA_H
#define REQUISICOES_FILA_H

#include <stddef.h>

typedef struct {
    int codigo;
    int hospital_codigo;
    int insumo_codigo;
    int quantidade;
    int prioridade;
} Requisicao;

typedef struct NoRequisicao NoRequisicao;
typedef struct {
    NoRequisicao *inicio;
    NoRequisicao *fim;
    size_t tamanho;
} FilaRequisicoes;

void fila_inicializar(FilaRequisicoes *fila);
int fila_enfileirar(FilaRequisicoes *fila, Requisicao requisicao);
int fila_desenfileirar(FilaRequisicoes *fila, Requisicao *removida);
Requisicao *fila_consultar_primeiro(FilaRequisicoes *fila);
size_t fila_tamanho(const FilaRequisicoes *fila);
int fila_vazia(const FilaRequisicoes *fila);
void fila_liberar(FilaRequisicoes *fila);

#endif
