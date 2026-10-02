package br.com.rotavital.cruzamento;

import br.com.rotavital.dominio.BaseDeDados;
import br.com.rotavital.dominio.Uf;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Núcleo da operação "cruzamento requisições × estoque" (Java puro, sem
 * Spring, para poder ser testado isoladamente).
 *
 * Para cada requisição pendente r (insumo i, quantidade q, prioridade p):
 *   percorre os hospitais que têm o insumo i; descarta os com estoque < q;
 *   calcula a distância geodésica (Haversine) até cada um e uma pontuação
 *   = distância + penalidade se o estoque ficaria criticamente baixo;
 *   escolhe o de menor pontuação (empate -> menor id, determinístico).
 * Depois agrega indicadores nacionais: atendíveis, distância média, % de
 * requisições dentro do SLA de cada prioridade, por UF e interestaduais.
 *
 * Big-O sequencial: O(R · C), C = hospitais candidatos por insumo (∝ H),
 * ou seja O(R · H). Cada requisição é independente das demais (o estoque
 * é só lido) -> dá para partir R em fatias e processá-las em paralelo.
 */
public final class MotorCruzamento {

    static final double RAIO_TERRA_KM = 6371.0;
    /** Penalidade (km equivalentes) quando o fornecedor ficaria com menos que o dobro do pedido. */
    static final double PENALIDADE_ESTOQUE_BAIXO_KM = 80.0;
    static final double VELOCIDADE_MEDIA_KMH = 60.0;
    static final double TEMPO_PREPARO_MIN = 30.0;
    /** SLA de entrega por prioridade, em minutos: 1h, 4h, 12h, 48h. */
    static final double[] SLA_MIN = {60, 240, 720, 2880};

    private MotorCruzamento() {}

    // =====================================================================
    // Trabalho de UMA fatia [inicio, fim) — usado por todas as versões.
    // =====================================================================
    static Parcial processarFatia(BaseDeDados b, int inicio, int fim) {
        Parcial p = new Parcial();
        p.thread = Thread.currentThread().isVirtual()
                ? "virtual:" + Thread.currentThread().getName()
                : Thread.currentThread().getName();
        final int nIns = b.numInsumos;

        // acumuladores locais (registradores) — gravados na Parcial no fim
        long atend = 0, nao = 0, inter = 0, somaDist = 0, chk = 0;

        for (int r = inicio; r < fim; r++) {
            final int insumo = b.reqInsumo[r];
            final int qtd = b.reqQuantidade[r];
            final double lat = b.reqLatRad[r], lon = b.reqLonRad[r], cosLat = b.reqCosLat[r];
            final int[] cand = b.candidatosPorInsumo[insumo];

            int melhor = -1;
            double melhorPontuacao = Double.MAX_VALUE;
            double melhorDist = 0;

            for (int k = 0; k < cand.length; k++) {                 // O(C) por requisição
                final int h = cand[k];
                final int est = b.estoque[h * nIns + insumo];
                if (est < qtd) continue;
                final double d = haversineKm(lat, lon, cosLat, b.hospLatRad[h], b.hospLonRad[h], b.hospCosLat[h]);
                final double pont = est < 2 * qtd ? d + PENALIDADE_ESTOQUE_BAIXO_KM : d;
                if (pont < melhorPontuacao) {                        // estrito: empate mantém o menor id
                    melhorPontuacao = pont;
                    melhor = h;
                    melhorDist = d;
                }
            }

            final int uf = b.reqUf[r];
            final int pri = b.reqPrioridade[r];
            p.ufTotal[uf]++;
            p.prioridadeTotal[pri]++;
            if (melhor < 0) {
                nao++;
            } else {
                atend++;
                long metros = Math.round(melhorDist * 1000.0);
                somaDist += metros;
                p.ufAtendiveis[uf]++;
                p.ufSomaDistanciaMetros[uf] += metros;
                if (b.hospUf[melhor] != uf) inter++;
                double minutos = TEMPO_PREPARO_MIN + melhorDist / VELOCIDADE_MEDIA_KMH * 60.0;
                if (minutos <= SLA_MIN[pri]) p.prioridadeDentroSla[pri]++;
            }
            chk += mix64(((long) r << 20) ^ (melhor + 1));
        }

        p.requisicoes = fim - inicio;
        p.atendiveis = atend;
        p.naoAtendiveis = nao;
        p.interestaduais = inter;
        p.somaDistanciaMetros = somaDist;
        p.checksum = chk;
        return p;
    }

    /** Haversine com cos(lat) pré-calculados. */
    static double haversineKm(double lat1, double lon1, double cos1, double lat2, double lon2, double cos2) {
        double sDLat = Math.sin((lat2 - lat1) * 0.5);
        double sDLon = Math.sin((lon2 - lon1) * 0.5);
        double a = sDLat * sDLat + cos1 * cos2 * sDLon * sDLon;
        return 2.0 * RAIO_TERRA_KM * Math.asin(Math.sqrt(Math.min(1.0, a)));
    }

    /** Finalizador do SplitMix64: espalha bits para o checksum. */
    static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    // =====================================================================
    // Versão 1 — SEQUENCIAL: uma única fatia [0, R) na thread da requisição.
    // =====================================================================
    public static ResultadoCruzamento sequencial(BaseDeDados b) {
        long t0 = System.nanoTime();
        Parcial total = processarFatia(b, 0, b.numRequisicoes);
        return montar("sequencial", 1, b, total, List.of(total.thread), System.nanoTime() - t0);
    }

    // =====================================================================
    // Versão 2 — THREADS DE PLATAFORMA (ExecutorService com N threads do SO).
    // Cada thread recebe UMA fatia contígua; agregação ao final.
    // =====================================================================
    public static ResultadoCruzamento comExecutor(String modo, BaseDeDados b, int nThreads, ExecutorService executor) {
        long t0 = System.nanoTime();
        int[][] fatias = fatiar(b.numRequisicoes, nThreads);

        List<Callable<Parcial>> tarefas = new ArrayList<>(fatias.length);
        for (int[] f : fatias) tarefas.add(() -> processarFatia(b, f[0], f[1]));

        Parcial total = new Parcial();
        LinkedHashSet<String> nomes = new LinkedHashSet<>();
        try {
            // invokeAll: dispara as N tarefas e espera todas (é o "join")
            for (Future<Parcial> f : executor.invokeAll(tarefas)) {
                Parcial p = f.get();
                total.somar(p);                     // agregação: só esta thread escreve em "total"
                nomes.add(p.thread);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha em uma das fatias", e.getCause());
        }
        return montar(modo, nThreads, b, total, List.copyOf(nomes), System.nanoTime() - t0);
    }

    /** Versão com um pool de threads de plataforma criado para a chamada (uso fora do Spring/testes). */
    public static ResultadoCruzamento plataforma(BaseDeDados b, int nThreads) {
        ExecutorService pool = Executors.newFixedThreadPool(nThreads, fabricaPlataforma(nThreads));
        try {
            return comExecutor("plataforma", b, nThreads, pool);
        } finally {
            pool.shutdown();
        }
    }

    // =====================================================================
    // Versão 3 (opcional) — VIRTUAL THREADS do Java 21: uma virtual thread
    // por fatia. Elas rodam montadas sobre um pool de "carrier threads"
    // (ForkJoinPool com paralelismo = núcleos da máquina).
    // =====================================================================
    public static ResultadoCruzamento virtual(BaseDeDados b, int nThreads) {
        ThreadFactory fabrica = Thread.ofVirtual().name("rv-virtual-" + nThreads + "-", 1).factory();
        try (ExecutorService vt = Executors.newThreadPerTaskExecutor(fabrica)) {
            return comExecutor("virtual", b, nThreads, vt);
        }
    }

    // =====================================================================
    // DEMONSTRAÇÃO DIDÁTICA — como NÃO fazer: N threads escrevendo num único
    // acumulador compartilhado, sem sincronização. "x++" é ler-somar-gravar
    // (três passos); duas threads intercaladas perdem incrementos
    // (race condition) e o resultado passa a divergir do sequencial.
    // =====================================================================
    public static ResultadoCruzamento inseguro(BaseDeDados b, int nThreads) {
        long t0 = System.nanoTime();
        final Parcial compartilhado = new Parcial();       // <- estado mutável compartilhado
        int[][] fatias = fatiar(b.numRequisicoes, nThreads);
        Thread[] ts = new Thread[fatias.length];
        for (int i = 0; i < fatias.length; i++) {
            int[] f = fatias[i];
            ts[i] = new Thread(() -> {
                for (int r = f[0]; r < f[1]; r++) {
                    Parcial um = processarFatia(b, r, r + 1);
                    compartilhado.somar(um);             // <- SEM lock: race condition
                }
            }, "rv-inseguro-" + i);
            ts[i].start();
        }
        for (Thread t : ts) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        return montar("inseguro", nThreads, b, compartilhado, List.of("(" + nThreads + " threads sem sincronização)"),
                System.nanoTime() - t0);
    }

    // =====================================================================
    // Utilitários
    // =====================================================================

    /** Divide [0, n) em k fatias contíguas de tamanhos que diferem no máximo em 1. */
    public static int[][] fatiar(int n, int k) {
        k = Math.max(1, Math.min(k, Math.max(1, n)));
        int[][] fatias = new int[k][2];
        int base = n / k, resto = n % k, ini = 0;
        for (int i = 0; i < k; i++) {
            int tam = base + (i < resto ? 1 : 0);
            fatias[i][0] = ini;
            fatias[i][1] = ini + tam;
            ini += tam;
        }
        return fatias;
    }

    public static ThreadFactory fabricaPlataforma(int nThreads) {
        return Thread.ofPlatform().name("rv-plataforma-" + nThreads + "-", 1).daemon(true).factory();
    }

    private static ResultadoCruzamento montar(String modo, int nThreads, BaseDeDados b, Parcial t,
                                              List<String> threads, long nanos) {
        List<ResultadoCruzamento.IndicadorUf> porUf = new ArrayList<>();
        for (Uf uf : Uf.TODAS) {
            int i = uf.ordinal();
            porUf.add(new ResultadoCruzamento.IndicadorUf(uf.name(), t.ufTotal[i], t.ufAtendiveis[i],
                    kmMedio(t.ufSomaDistanciaMetros[i], t.ufAtendiveis[i])));
        }
        List<ResultadoCruzamento.IndicadorPrioridade> porPri = new ArrayList<>();
        for (int i = 0; i < BaseDeDados.NUM_PRIORIDADES; i++) {
            double pct = t.prioridadeTotal[i] == 0 ? 0 : Math.round(10000.0 * t.prioridadeDentroSla[i] / t.prioridadeTotal[i]) / 100.0;
            porPri.add(new ResultadoCruzamento.IndicadorPrioridade(BaseDeDados.NOME_PRIORIDADE[i],
                    t.prioridadeTotal[i], t.prioridadeDentroSla[i], pct));
        }
        return new ResultadoCruzamento(modo, nThreads, b.numRequisicoes, b.numHospitais, b.numInsumos,
                t.atendiveis, t.naoAtendiveis, t.interestaduais, kmMedio(t.somaDistanciaMetros, t.atendiveis),
                porUf, porPri, String.format("%016x", t.checksum), assinatura(t),
                nanos / 1_000_000.0, threads);
    }

    private static double kmMedio(long somaMetros, long n) {
        return n == 0 ? 0 : Math.round((double) somaMetros / n) / 1000.0;
    }

    /** Hash de TODOS os números do resultado: se dois modos têm a mesma assinatura, a resposta é idêntica. */
    static String assinatura(Parcial t) {
        long h = 0x5bd1e995L;
        long[] escalares = {t.requisicoes, t.atendiveis, t.naoAtendiveis, t.interestaduais, t.somaDistanciaMetros, t.checksum};
        for (long v : escalares) h = mix64(h ^ v) + 0x9e3779b97f4a7c15L;
        for (long[] arr : new long[][]{t.ufTotal, t.ufAtendiveis, t.ufSomaDistanciaMetros, t.prioridadeTotal, t.prioridadeDentroSla})
            for (long v : arr) h = mix64(h ^ v) + 0x9e3779b97f4a7c15L;
        return String.format("%016x", h);
    }
}
