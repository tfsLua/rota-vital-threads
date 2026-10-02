#include "estoque_lista.h"
#include <stdlib.h>
#include <string.h>

typedef struct NoEstoque {
    ItemEstoque item;
    struct NoEstoque *proximo;
} NoEstoque;

static int item_valido(const ItemEstoque *item) {
    return item != NULL
        && item->codigo > 0
        && item->nome[0] != '\0'
        && item->quantidade >= 0
        && item->unidade[0] != '\0';
}

void estoque_inicializar(ListaEstoque *lista) {
    if (lista == NULL) return;
    lista->inicio = NULL;
    lista->tamanho = 0;
}

int estoque_inserir(ListaEstoque *lista, ItemEstoque item) {
    if (lista == NULL || !item_valido(&item)) return 0;
    if (estoque_buscar(lista, item.codigo) != NULL) return 0;
    NoEstoque *novo = malloc(sizeof(NoEstoque));
    if (novo == NULL) return 0;
    novo->item = item;
    novo->proximo = lista->inicio;
    lista->inicio = novo;
    lista->tamanho++;
    return 1;
}

int estoque_remover(ListaEstoque *lista, int codigo, ItemEstoque *removido) {
    if (lista == NULL) return 0;
    NoEstoque *anterior = NULL;
    NoEstoque *atual = lista->inicio;
    while (atual != NULL && atual->item.codigo != codigo) {
        anterior = atual;
        atual = atual->proximo;
    }
    if (atual == NULL) return 0;
    if (anterior == NULL) lista->inicio = atual->proximo;
    else anterior->proximo = atual->proximo;
    if (removido != NULL) *removido = atual->item;
    free(atual);
    lista->tamanho--;
    return 1;
}

ItemEstoque *estoque_buscar(ListaEstoque *lista, int codigo) {
    if (lista == NULL) return NULL;
    for (NoEstoque *atual = lista->inicio; atual != NULL; atual = atual->proximo)
        if (atual->item.codigo == codigo) return &atual->item;
    return NULL;
}

size_t estoque_tamanho(const ListaEstoque *lista) {
    return lista == NULL ? 0 : lista->tamanho;
}
int estoque_vazia(const ListaEstoque *lista) {
    return lista == NULL || lista->inicio == NULL;
}

void estoque_liberar(ListaEstoque *lista) {
    if (lista == NULL) return;
    NoEstoque *atual = lista->inicio;
    while (atual != NULL) {
        NoEstoque *proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }
    lista->inicio = NULL;
    lista->tamanho = 0;
}
