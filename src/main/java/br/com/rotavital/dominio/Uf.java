package br.com.rotavital.dominio;

/**
 * As 27 unidades federativas com a coordenada da capital (centro de
 * geração dos pontos), um peso proporcional à população (milhões, aprox.)
 * e o espalhamento em graus usado ao sortear pontos no estado.
 */
public enum Uf {
    AC(-9.97, -67.81, 0.83, 1.5), AL(-9.67, -35.74, 3.1, 0.6), AP(0.03, -51.07, 0.73, 1.2),
    AM(-3.12, -60.02, 3.9, 3.0), BA(-12.97, -38.51, 14.1, 2.5), CE(-3.73, -38.52, 8.8, 1.3),
    DF(-15.79, -47.88, 2.8, 0.3), ES(-20.32, -40.34, 3.8, 0.8), GO(-16.68, -49.26, 7.1, 1.8),
    MA(-2.53, -44.30, 6.8, 2.0), MT(-15.60, -56.10, 3.7, 3.0), MS(-20.47, -54.62, 2.8, 2.0),
    MG(-19.92, -43.94, 20.5, 2.5), PA(-1.46, -48.50, 8.1, 3.0), PB(-7.12, -34.86, 4.0, 0.8),
    PR(-25.43, -49.27, 11.4, 1.5), PE(-8.05, -34.88, 9.1, 1.2), PI(-5.09, -42.80, 3.3, 1.8),
    RJ(-22.91, -43.17, 16.1, 0.8), RN(-5.79, -35.21, 3.3, 0.7), RS(-30.03, -51.23, 10.9, 1.8),
    RO(-8.76, -63.90, 1.6, 1.8), RR(2.82, -60.67, 0.64, 1.5), SC(-27.59, -48.55, 7.6, 1.0),
    SP(-23.55, -46.63, 44.4, 1.8), SE(-10.91, -37.07, 2.2, 0.5), TO(-10.18, -48.33, 1.5, 1.8);

    public final double latCapital;
    public final double lonCapital;
    public final double peso;
    public final double espalhamentoGraus;

    Uf(double lat, double lon, double peso, double espalhamento) {
        this.latCapital = lat;
        this.lonCapital = lon;
        this.peso = peso;
        this.espalhamentoGraus = espalhamento;
    }

    public static final Uf[] TODAS = values();
}
