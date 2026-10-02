package br.com.rotavital.cruzamento;

import br.com.rotavital.dominio.BaseDeDados;
import br.com.rotavital.dominio.Uf;

/**
 * Acumulador de UMA fatia. Cada thread tem o seu (confinamento de thread):
 * durante o processamento não há nenhum dado compartilhado sendo escrito,
 * então não há lock nem race condition. Ao final, as parciais são somadas
 * (agregação/redução) pela thread que atendeu a requisição HTTP.
 *
 * Todos os campos são inteiros (distância em METROS, inteiros): soma de
 * inteiros é associativa e comutativa, então a ordem da agregação não muda
 * o resultado — a versão paralela devolve EXATAMENTE o mesmo que a
 * sequencial. (Com soma de double a ordem mudaria os últimos dígitos.)
 */
public final class Parcial {

    long requisicoes;
    long atendiveis;
    long naoAtendiveis;
    long interestaduais;
    long somaDistanciaMetros;
    /** Soma (mod 2^64) de um hash de cada par (requisição, hospital escolhido): independe da ordem. */
    long checksum;

    final long[] ufTotal = new long[Uf.TODAS.length];
    final long[] ufAtendiveis = new long[Uf.TODAS.length];
    final long[] ufSomaDistanciaMetros = new long[Uf.TODAS.length];
    final long[] prioridadeTotal = new long[BaseDeDados.NUM_PRIORIDADES];
    final long[] prioridadeDentroSla = new long[BaseDeDados.NUM_PRIORIDADES];

    /** Nome da thread que processou esta fatia (só para exibição). */
    String thread;

    /** Redução: this += outra. Custo O(UFs + prioridades), desprezível. */
    void somar(Parcial o) {
        requisicoes += o.requisicoes;
        atendiveis += o.atendiveis;
        naoAtendiveis += o.naoAtendiveis;
        interestaduais += o.interestaduais;
        somaDistanciaMetros += o.somaDistanciaMetros;
        checksum += o.checksum;
        for (int i = 0; i < ufTotal.length; i++) {
            ufTotal[i] += o.ufTotal[i];
            ufAtendiveis[i] += o.ufAtendiveis[i];
            ufSomaDistanciaMetros[i] += o.ufSomaDistanciaMetros[i];
        }
        for (int i = 0; i < prioridadeTotal.length; i++) {
            prioridadeTotal[i] += o.prioridadeTotal[i];
            prioridadeDentroSla[i] += o.prioridadeDentroSla[i];
        }
    }
}
