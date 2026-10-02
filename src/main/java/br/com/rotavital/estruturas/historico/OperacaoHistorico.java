package br.com.rotavital.estruturas.historico;

/** Dados armazenados em cada nó da pilha de histórico. */
public record OperacaoHistorico(int tipo, int referencia, String descricao) {
    public OperacaoHistorico {
        if (descricao == null || descricao.isBlank()) throw new IllegalArgumentException("descrição obrigatória");
    }
}
