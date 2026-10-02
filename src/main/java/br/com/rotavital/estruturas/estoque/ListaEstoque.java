package br.com.rotavital.estruturas.estoque;

import java.util.Optional;

/** Lista encadeada própria da AV1; não usa java.util.List para preservar a lógica dos nós. */
public final class ListaEstoque {
    private static final class No {
        private final ItemEstoque item;
        private No proximo;
        private No(ItemEstoque item, No proximo) { this.item = item; this.proximo = proximo; }
    }

    private No inicio;
    private int tamanho;

    public synchronized boolean inserir(ItemEstoque item) {
        if (item == null || buscar(item.codigo()).isPresent()) return false;
        inicio = new No(item, inicio);
        tamanho++;
        return true;
    }

    public synchronized Optional<ItemEstoque> remover(int codigo) {
        No anterior = null;
        No atual = inicio;
        while (atual != null && atual.item.codigo() != codigo) {
            anterior = atual;
            atual = atual.proximo;
        }
        if (atual == null) return Optional.empty();
        if (anterior == null) inicio = atual.proximo;
        else anterior.proximo = atual.proximo;
        tamanho--;
        return Optional.of(atual.item);
    }

    public synchronized Optional<ItemEstoque> buscar(int codigo) {
        for (No atual = inicio; atual != null; atual = atual.proximo)
            if (atual.item.codigo() == codigo) return Optional.of(atual.item);
        return Optional.empty();
    }

    public synchronized int tamanho() { return tamanho; }
    public synchronized boolean estaVazia() { return inicio == null; }
}
