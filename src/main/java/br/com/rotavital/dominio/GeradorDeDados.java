package br.com.rotavital.dominio;

import java.util.SplittableRandom;

/**
 * Gera uma base sintética, porém realista, em escala nacional.
 * Com a mesma semente, gera SEMPRE os mesmos dados (reprodutibilidade das
 * medições e comparação exata entre as versões).
 *
 * - Hospitais e requisições distribuídos pelas 27 UFs proporcionalmente à
 *   população, espalhados em torno das capitais.
 * - Cada insumo tem uma "raridade": insumos comuns estão em ~50% dos
 *   hospitais, insumos raros (ex.: sangue O-, antiveneno) em ~2%, e com
 *   estoques menores — por isso algumas requisições grandes não têm
 *   fornecedor possível ("não atendíveis").
 */
public final class GeradorDeDados {

    private GeradorDeDados() {}

    public static BaseDeDados gerar(int numRequisicoes, int numHospitais, int numInsumos, long seed) {
        SplittableRandom rnd = new SplittableRandom(seed);
        double[] acumulado = pesosAcumulados();

        // ---------- Hospitais ----------
        double[] hLat = new double[numHospitais];
        double[] hLon = new double[numHospitais];
        double[] hCos = new double[numHospitais];
        byte[] hUf = new byte[numHospitais];
        for (int h = 0; h < numHospitais; h++) {
            Uf uf = sortearUf(rnd, acumulado);
            double[] p = pontoNoEstado(rnd, uf);
            hLat[h] = Math.toRadians(p[0]);
            hLon[h] = Math.toRadians(p[1]);
            hCos[h] = Math.cos(hLat[h]);
            hUf[h] = (byte) uf.ordinal();
        }

        // ---------- Estoque ----------
        double[] probPresenca = new double[numInsumos];
        for (int i = 0; i < numInsumos; i++) {
            // de 0,50 (comum) até 0,02 (raro), decaindo com o índice do insumo
            probPresenca[i] = 0.02 + 0.48 * Math.pow(1.0 - (double) i / Math.max(1, numInsumos - 1), 1.5);
        }
        // insumos raros também têm estoques menores (25..~35 un.); comuns até ~200 un.
        int[] maxEstoque = new int[numInsumos];
        for (int i = 0; i < numInsumos; i++) maxEstoque[i] = 25 + (int) (380 * probPresenca[i]);
        int[] estoque = new int[numHospitais * numInsumos];
        int[] contagem = new int[numInsumos];
        for (int h = 0; h < numHospitais; h++) {
            for (int i = 0; i < numInsumos; i++) {
                if (rnd.nextDouble() < probPresenca[i]) {
                    estoque[h * numInsumos + i] = 1 + rnd.nextInt(maxEstoque[i]);
                    contagem[i]++;
                }
            }
        }
        int[][] candidatos = new int[numInsumos][];
        for (int i = 0; i < numInsumos; i++) {
            candidatos[i] = new int[contagem[i]];
            int k = 0;
            for (int h = 0; h < numHospitais; h++) {
                if (estoque[h * numInsumos + i] > 0) candidatos[i][k++] = h;
            }
        }

        // ---------- Requisições ----------
        double[] rLat = new double[numRequisicoes];
        double[] rLon = new double[numRequisicoes];
        double[] rCos = new double[numRequisicoes];
        byte[] rUf = new byte[numRequisicoes];
        short[] rIns = new short[numRequisicoes];
        short[] rQtd = new short[numRequisicoes];
        byte[] rPri = new byte[numRequisicoes];
        for (int r = 0; r < numRequisicoes; r++) {
            Uf uf = sortearUf(rnd, acumulado);
            double[] p = pontoNoEstado(rnd, uf);
            rLat[r] = Math.toRadians(p[0]);
            rLon[r] = Math.toRadians(p[1]);
            rCos[r] = Math.cos(rLat[r]);
            rUf[r] = (byte) uf.ordinal();
            rIns[r] = (short) rnd.nextInt(numInsumos);
            // quantidade: maioria pequena, cauda longa (1..120)
            rQtd[r] = (short) (1 + (int) (Math.pow(rnd.nextDouble(), 3) * 120));
            double u = rnd.nextDouble();
            rPri[r] = (byte) (u < 0.10 ? 0 : u < 0.35 ? 1 : u < 0.70 ? 2 : 3);
        }

        return new BaseDeDados(seed, numHospitais, numInsumos, hLat, hLon, hCos, hUf, estoque, candidatos,
                numRequisicoes, rLat, rLon, rCos, rUf, rIns, rQtd, rPri);
    }

    private static double[] pesosAcumulados() {
        double[] acc = new double[Uf.TODAS.length];
        double soma = 0;
        for (int i = 0; i < acc.length; i++) {
            soma += Uf.TODAS[i].peso;
            acc[i] = soma;
        }
        for (int i = 0; i < acc.length; i++) acc[i] /= soma;
        return acc;
    }

    private static Uf sortearUf(SplittableRandom rnd, double[] acumulado) {
        double u = rnd.nextDouble();
        for (int i = 0; i < acumulado.length; i++) {
            if (u <= acumulado[i]) return Uf.TODAS[i];
        }
        return Uf.TODAS[acumulado.length - 1];
    }

    /** Ponto ~normal em torno da capital (Box-Muller), limitado ao território aproximado do Brasil. */
    private static double[] pontoNoEstado(SplittableRandom rnd, Uf uf) {
        double u1 = Math.max(1e-12, rnd.nextDouble());
        double u2 = rnd.nextDouble();
        double mag = Math.sqrt(-2.0 * Math.log(u1)) * uf.espalhamentoGraus * 0.5;
        double lat = uf.latCapital + mag * Math.cos(2 * Math.PI * u2);
        double lon = uf.lonCapital + mag * Math.sin(2 * Math.PI * u2);
        lat = Math.max(-33.7, Math.min(5.2, lat));
        lon = Math.max(-73.9, Math.min(-34.8, lon));
        return new double[]{lat, lon};
    }
}
