package br.com.rotavital.estruturas.requisicao;

import java.util.Optional;

/** Fila FIFO própria da AV1, com referências para início e fim. */
public final class FilaRequisicoes {
    private static final class No {
        private final Requisicao requisicao;
        private No proximo;
        private No(Requisicao requisicao) { this.requisicao = requisicao; }
    }

    private No inicio;
    private No fim;
    private int tamanho;

    public synchronized void enfileirar(Requisicao requisicao) {
        if (requisicao == null) throw new IllegalArgumentException("requisição obrigatória");
        No novo = new No(requisicao);
        if (fim == null) inicio = novo;
        else fim.proximo = novo;
        fim = novo;
        tamanho++;
    }

    public synchronized Optional<Requisicao> desenfileirar() {
        if (inicio == null) return Optional.empty();
        Requisicao valor = inicio.requisicao;
        inicio = inicio.proximo;
        if (inicio == null) fim = null;
        tamanho--;
        return Optional.of(valor);
    }

    public synchronized Optional<Requisicao> consultarPrimeiro() {
        return inicio == null ? Optional.empty() : Optional.of(inicio.requisicao);
    }

    public synchronized int tamanho() { return tamanho; }
    public synchronized boolean estaVazia() { return inicio == null; }
}
