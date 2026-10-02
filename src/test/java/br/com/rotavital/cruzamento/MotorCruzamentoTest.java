package br.com.rotavital.cruzamento;

import br.com.rotavital.dominio.BaseDeDados;
import br.com.rotavital.dominio.GeradorDeDados;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** As versões com threads devem devolver EXATAMENTE a mesma resposta da sequencial. */
class MotorCruzamentoTest {

    static BaseDeDados base;
    static ResultadoCruzamento referencia;

    @BeforeAll
    static void preparar() {
        base = GeradorDeDados.gerar(30_001, 400, 30, 7L);   // tamanho ímpar: fatias desiguais
        referencia = MotorCruzamento.sequencial(base);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 8, 16})
    void plataformaIgualSequencial(int n) {
        assertIgual(MotorCruzamento.plataforma(base, n));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8})
    void virtualIgualSequencial(int n) {
        assertIgual(MotorCruzamento.virtual(base, n));
    }

    @Test
    void fatiasCobremTudoSemSobreposicao() {
        int[][] f = MotorCruzamento.fatiar(10, 4);
        assertEquals(0, f[0][0]);
        assertEquals(10, f[3][1]);
        for (int i = 1; i < f.length; i++) assertEquals(f[i - 1][1], f[i][0]);
    }

    private static void assertIgual(ResultadoCruzamento r) {
        assertEquals(referencia.assinatura(), r.assinatura());
        assertEquals(referencia.checksum(), r.checksum());
        assertEquals(referencia.atendiveis(), r.atendiveis());
        assertEquals(referencia.distanciaMediaKm(), r.distanciaMediaKm());
        assertEquals(referencia.porUf(), r.porUf());
        assertEquals(referencia.porPrioridade(), r.porPrioridade());
    }
}
