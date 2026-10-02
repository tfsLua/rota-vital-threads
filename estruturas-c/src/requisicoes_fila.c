#include "requisicoes_fila.h"
#include <stdlib.h>

typedef struct NoRequisicao {
    Requisicao requisicao;
    struct NoRequisicao *proximo;
} NoRequisicao;

static int requisicao_valida(const Requisicao *requisicao) {
    return requisicao != NULL
        && requisicao->codigo > 0
        && requisicao->hospital_codigo > 0
        && requisicao->insumo_codigo > 0
        && requisicao->quantidade > 0
        && requisicao->prioridade >= 0;
}

void fila_inicializar(FilaRequisicoes *fila) {
    if (fila == NULL) return;
    fila->inicio = NULL;
    fila->fim = NULL;
    fila->tamanho = 0;
}

int fila_enfileirar(FilaRequisicoes *fila, Requisicao requisicao) {
    if (fila == NULL || !requisicao_valida(&requisicao)) return 0;
    NoRequisicao *novo = malloc(sizeof(NoRequisicao));
    if (novo == NULL) return 0;
    novo->requisicao = requisicao;
    novo->proximo = NULL;
    if (fila->fim == NULL) fila->inicio = novo;
    else fila->fim->proximo = novo;
    fila->fim = novo;
    fila->tamanho++;
    return 1;
}

int fila_desenfileirar(FilaRequisicoes *fila, Requisicao *removida) {
    if (fila == NULL) return 0;
    if (fila->inicio == NULL) return 0;
    NoRequisicao *remover = fila->inicio;
    fila->inicio = remover->proximo;
    if (fila->inicio == NULL) fila->fim = NULL;
    if (removida != NULL) *removida = remover->requisicao;
    free(remover);
    fila->tamanho--;
    return 1;
}

Requisicao *fila_consultar_primeiro(FilaRequisicoes *fila) {
    if (fila == NULL) return NULL;
    return fila->inicio == NULL ? NULL : &fila->inicio->requisicao;
}

size_t fila_tamanho(const FilaRequisicoes *fila) {
    return fila == NULL ? 0 : fila->tamanho;
}
int fila_vazia(const FilaRequisicoes *fila) {
    return fila == NULL || fila->inicio == NULL;
}

void fila_liberar(FilaRequisicoes *fila) {
    if (fila == NULL) return;
    Requisicao descartada;
    while (fila_desenfileirar(fila, &descartada)) { }
}
