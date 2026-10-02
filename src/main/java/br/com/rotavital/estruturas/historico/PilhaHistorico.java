package br.com.rotavital.estruturas.historico;

import java.util.Optional;

/** Pilha LIFO própria da AV1, com referência para o topo. */
public final class PilhaHistorico {
    private static final class No {
        private final OperacaoHistorico operacao;
        private final No proximo;
        private No(OperacaoHistorico operacao, No proximo) { this.operacao = operacao; this.proximo = proximo; }
    }

    private No topo;
    private int tamanho;

    public synchronized void empilhar(OperacaoHistorico operacao) {
        if (operacao == null) throw new IllegalArgumentException("operação obrigatória");
        topo = new No(operacao, topo);
        tamanho++;
    }

    public synchronized Optional<OperacaoHistorico> desempilhar() {
        if (topo == null) return Optional.empty();
        OperacaoHistorico valor = topo.operacao;
        topo = topo.proximo;
        tamanho--;
        return Optional.of(valor);
    }

    public synchronized Optional<OperacaoHistorico> consultarTopo() {
        return topo == null ? Optional.empty() : Optional.of(topo.operacao);
    }

    public synchronized int tamanho() { return tamanho; }
    public synchronized boolean estaVazia() { return topo == null; }
}
