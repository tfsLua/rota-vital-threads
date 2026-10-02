package br.com.rotavital.servico;

import br.com.rotavital.dominio.BaseDeDados;
import br.com.rotavital.dominio.GeradorDeDados;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Faz o papel da camada de dados nesta atividade: mantém em memória os
 * snapshots gerados (ex.: 100 mil e 1 milhão de requisições).
 *
 * Em produção o snapshot viria do banco com UMA consulta de leitura
 * (custo O(R) de E/S, feito uma vez e reaproveitado pelos painéis); aqui
 * ele é gerado, para que a medição isole o custo de PROCESSAMENTO — que é
 * o gargalo estudado.
 */
@Component
public class RepositorioDeDados {

    public static final int HOSPITAIS_PADRAO = 1_000;
    public static final int INSUMOS_PADRAO = 30;
    public static final long SEED_PADRAO = 42L;

    private final Map<Integer, BaseDeDados> porTamanho = new ConcurrentHashMap<>();

    /** Gera (ou devolve do cache) a base com R requisições. Thread-safe: computeIfAbsent. */
    public BaseDeDados obter(int requisicoes) {
        return porTamanho.computeIfAbsent(requisicoes,
                r -> GeradorDeDados.gerar(r, HOSPITAIS_PADRAO, INSUMOS_PADRAO, SEED_PADRAO));
    }

    public boolean carregada(int requisicoes) {
        return porTamanho.containsKey(requisicoes);
    }

    public Map<Integer, BaseDeDados> todas() {
        return Map.copyOf(porTamanho);
    }
}
