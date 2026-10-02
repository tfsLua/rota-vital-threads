package br.com.rotavital.estruturas.estoque;

/** Dados armazenados em cada nó da lista de estoque. */
public record ItemEstoque(int codigo, String nome, int quantidade, String unidade) {
    public ItemEstoque {
        if (codigo <= 0) throw new IllegalArgumentException("codigo deve ser positivo");
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("nome obrigatório");
        if (quantidade < 0) throw new IllegalArgumentException("quantidade não pode ser negativa");
        if (unidade == null || unidade.isBlank()) throw new IllegalArgumentException("unidade obrigatória");
    }
}
