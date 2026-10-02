#ifndef ESTOQUE_LISTA_H
#define ESTOQUE_LISTA_H

#include <stddef.h>

/* Item de estoque: estrutura de domínio da AV1. */
typedef struct {
    int codigo;
    char nome[80];
    int quantidade;
    char unidade[16];
} ItemEstoque;

typedef struct NoEstoque NoEstoque;
typedef struct {
    NoEstoque *inicio;
    size_t tamanho;
} ListaEstoque;

void estoque_inicializar(ListaEstoque *lista);
int estoque_inserir(ListaEstoque *lista, ItemEstoque item);
int estoque_remover(ListaEstoque *lista, int codigo, ItemEstoque *removido);
ItemEstoque *estoque_buscar(ListaEstoque *lista, int codigo);
size_t estoque_tamanho(const ListaEstoque *lista);
int estoque_vazia(const ListaEstoque *lista);
void estoque_liberar(ListaEstoque *lista);

#endif
