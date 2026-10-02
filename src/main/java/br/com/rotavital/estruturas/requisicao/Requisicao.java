package br.com.rotavital.estruturas.requisicao;

/** Dados armazenados em cada nó da fila de requisições. */
public record Requisicao(int codigo, int hospitalCodigo, int insumoCodigo, int quantidade, int prioridade) {
    public Requisicao {
        if (codigo <= 0 || hospitalCodigo <= 0 || insumoCodigo <= 0)
            throw new IllegalArgumentException("códigos devem ser positivos");
        if (quantidade <= 0) throw new IllegalArgumentException("quantidade deve ser positiva");
        if (prioridade < 0) throw new IllegalArgumentException("prioridade inválida");
    }
}
