package br.com.rotavital.cruzamento;

import java.util.List;

/** Corpo JSON devolvido pelo endpoint de cruzamento. */
public record ResultadoCruzamento(
        String modo,
        int threads,
        int requisicoes,
        int hospitais,
        int insumos,
        long atendiveis,
        long naoAtendiveis,
        long interestaduais,
        double distanciaMediaKm,
        List<IndicadorUf> porUf,
        List<IndicadorPrioridade> porPrioridade,
        String checksum,
        String assinatura,
        double tempoProcessamentoMs,
        List<String> threadsUtilizadas) {

    public record IndicadorUf(String uf, long requisicoes, long atendiveis, double distanciaMediaKm) {}

    public record IndicadorPrioridade(String prioridade, long requisicoes, long dentroDoSla, double percentualSla) {}
}
