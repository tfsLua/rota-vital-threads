package br.com.rotavital.estruturas;

import br.com.rotavital.estruturas.estoque.ItemEstoque;
import br.com.rotavital.estruturas.estoque.ListaEstoque;
import br.com.rotavital.estruturas.historico.OperacaoHistorico;
import br.com.rotavital.estruturas.historico.PilhaHistorico;
import br.com.rotavital.estruturas.requisicao.FilaRequisicoes;
import br.com.rotavital.estruturas.requisicao.Requisicao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Av1EstruturasTest {
    @Test
    void listaInsereConsultaRemoveEDescartaDuplicado() {
        ListaEstoque lista = new ListaEstoque();
        ItemEstoque soro = new ItemEstoque(10, "Soro fisiologico", 50, "un");
        assertTrue(lista.inserir(soro));
        assertFalse(lista.inserir(soro));
        assertEquals(soro, lista.buscar(10).orElseThrow());
        assertEquals(soro, lista.remover(10).orElseThrow());
        assertTrue(lista.estaVazia());
        assertTrue(lista.remover(10).isEmpty());
        assertEquals(0, lista.tamanho());
    }

    @Test
    void filaPreservaOrdemFifo() {
        FilaRequisicoes fila = new FilaRequisicoes();
        Requisicao primeira = new Requisicao(1, 101, 10, 2, 1);
        Requisicao segunda = new Requisicao(2, 102, 20, 1, 0);
        fila.enfileirar(primeira);
        fila.enfileirar(segunda);
        assertEquals(primeira, fila.consultarPrimeiro().orElseThrow());
        assertEquals(primeira, fila.desenfileirar().orElseThrow());
        assertEquals(segunda, fila.desenfileirar().orElseThrow());
        assertTrue(fila.estaVazia());
        assertEquals(0, fila.tamanho());
    }

    @Test
    void pilhaPreservaOrdemLifo() {
        PilhaHistorico pilha = new PilhaHistorico();
        OperacaoHistorico entrada = new OperacaoHistorico(1, 10, "Entrada de estoque");
        OperacaoHistorico atendimento = new OperacaoHistorico(2, 1, "Requisicao atendida");
        pilha.empilhar(entrada);
        pilha.empilhar(atendimento);
        assertEquals(atendimento, pilha.consultarTopo().orElseThrow());
        assertEquals(atendimento, pilha.desempilhar().orElseThrow());
        assertEquals(entrada, pilha.desempilhar().orElseThrow());
        assertTrue(pilha.estaVazia());
        assertEquals(0, pilha.tamanho());
    }

    @Test
    void modelosRejeitamDadosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ItemEstoque(0, "", -1, ""));
        assertThrows(IllegalArgumentException.class, () -> new Requisicao(0, 0, 0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new OperacaoHistorico(1, 1, ""));
    }
}
