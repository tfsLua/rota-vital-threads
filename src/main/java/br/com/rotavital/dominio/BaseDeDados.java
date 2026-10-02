package br.com.rotavital.dominio;

/**
 * Snapshot em memória (somente leitura) do que a camada de aplicação lê do
 * banco para executar o cruzamento: hospitais com estoque e requisições
 * pendentes.
 *
 * Os dados ficam em "estrutura de arrays" (um array por coluna) em vez de
 * milhões de objetos: menos memória, menos GC e acesso sequencial amigável
 * ao cache da CPU. Como NINGUÉM escreve nesses arrays durante o
 * processamento, várias threads podem lê-los ao mesmo tempo sem lock.
 */
public final class BaseDeDados {

    public static final int NUM_PRIORIDADES = 4;          // 0=emergência, 1=urgente, 2=alta, 3=eletiva
    public static final String[] NOME_PRIORIDADE = {"EMERGENCIA", "URGENTE", "ALTA", "ELETIVA"};

    // ----- Hospitais (fornecedores) -----
    public final int numHospitais;
    public final int numInsumos;
    public final double[] hospLatRad;
    public final double[] hospLonRad;
    public final double[] hospCosLat;       // cos(lat) pré-calculado (Haversine)
    public final byte[] hospUf;
    /** estoque[h * numInsumos + i] = unidades do insumo i no hospital h. */
    public final int[] estoque;
    /** Para cada insumo, os ids (crescentes) dos hospitais que têm algum estoque dele. */
    public final int[][] candidatosPorInsumo;

    // ----- Requisições pendentes -----
    public final int numRequisicoes;
    public final double[] reqLatRad;
    public final double[] reqLonRad;
    public final double[] reqCosLat;
    public final byte[] reqUf;
    public final short[] reqInsumo;
    public final short[] reqQuantidade;
    public final byte[] reqPrioridade;

    public final long seed;

    public BaseDeDados(long seed, int numHospitais, int numInsumos,
                       double[] hospLatRad, double[] hospLonRad, double[] hospCosLat, byte[] hospUf,
                       int[] estoque, int[][] candidatosPorInsumo,
                       int numRequisicoes, double[] reqLatRad, double[] reqLonRad, double[] reqCosLat,
                       byte[] reqUf, short[] reqInsumo, short[] reqQuantidade, byte[] reqPrioridade) {
        this.seed = seed;
        this.numHospitais = numHospitais;
        this.numInsumos = numInsumos;
        this.hospLatRad = hospLatRad;
        this.hospLonRad = hospLonRad;
        this.hospCosLat = hospCosLat;
        this.hospUf = hospUf;
        this.estoque = estoque;
        this.candidatosPorInsumo = candidatosPorInsumo;
        this.numRequisicoes = numRequisicoes;
        this.reqLatRad = reqLatRad;
        this.reqLonRad = reqLonRad;
        this.reqCosLat = reqCosLat;
        this.reqUf = reqUf;
        this.reqInsumo = reqInsumo;
        this.reqQuantidade = reqQuantidade;
        this.reqPrioridade = reqPrioridade;
    }

    /** Média de hospitais candidatos por insumo (o "H efetivo" da Big-O). */
    public double mediaCandidatosPorInsumo() {
        long soma = 0;
        for (int[] c : candidatosPorInsumo) soma += c.length;
        return (double) soma / candidatosPorInsumo.length;
    }
}
