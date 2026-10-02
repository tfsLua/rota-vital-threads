package br.com.rotavital.web;

import br.com.rotavital.estruturas.estoque.ItemEstoque;
import br.com.rotavital.estruturas.estoque.ListaEstoque;
import br.com.rotavital.estruturas.historico.OperacaoHistorico;
import br.com.rotavital.estruturas.historico.PilhaHistorico;
import br.com.rotavital.estruturas.requisicao.FilaRequisicoes;
import br.com.rotavital.estruturas.requisicao.Requisicao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demonstração de integração das estruturas próprias da AV1 com a camada web.
 * O controller não usa Collections para substituir as estruturas acadêmicas.
 */
@RestController
@RequestMapping("/api/av1")
public class Av1EstruturasController {
    private final ListaEstoque estoque = new ListaEstoque();
    private final FilaRequisicoes requisicoes = new FilaRequisicoes();
    private final PilhaHistorico historico = new PilhaHistorico();

    @PostMapping("/estoque")
    public ResponseEntity<ItemEstoque> inserirEstoque(@RequestBody ItemEstoque item) {
        return estoque.inserir(item) ? ResponseEntity.ok(item) : ResponseEntity.status(409).build();
    }

    @GetMapping("/estoque/{codigo}")
    public ResponseEntity<ItemEstoque> consultarEstoque(@PathVariable int codigo) {
        return ResponseEntity.of(estoque.buscar(codigo));
    }

    @DeleteMapping("/estoque/{codigo}")
    public ResponseEntity<ItemEstoque> removerEstoque(@PathVariable int codigo) {
        return ResponseEntity.of(estoque.remover(codigo));
    }

    @GetMapping("/estoque")
    public Map<String, Object> statusEstoque() {
        return Map.of("tamanho", estoque.tamanho(), "vazia", estoque.estaVazia());
    }

    @PostMapping("/requisicoes")
    public Map<String, Object> enfileirar(@RequestBody Requisicao requisicao) {
        requisicoes.enfileirar(requisicao);
        return Map.of("enfileirada", requisicao, "tamanho", requisicoes.tamanho());
    }

    @GetMapping("/requisicoes")
    public Map<String, Object> consultarFila() {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("primeira", requisicoes.consultarPrimeiro().orElse(null));
        resposta.put("tamanho", requisicoes.tamanho());
        resposta.put("vazia", requisicoes.estaVazia());
        return resposta;
    }

    @DeleteMapping("/requisicoes")
    public ResponseEntity<Requisicao> desenfileirar() {
        return ResponseEntity.of(requisicoes.desenfileirar());
    }

    @PostMapping("/historico")
    public Map<String, Object> empilhar(@RequestBody OperacaoHistorico operacao) {
        historico.empilhar(operacao);
        return Map.of("empilhada", operacao, "tamanho", historico.tamanho());
    }

    @GetMapping("/historico")
    public Map<String, Object> consultarHistorico() {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("topo", historico.consultarTopo().orElse(null));
        resposta.put("tamanho", historico.tamanho());
        resposta.put("vazia", historico.estaVazia());
        return resposta;
    }

    @DeleteMapping("/historico")
    public ResponseEntity<OperacaoHistorico> desempilhar() {
        return ResponseEntity.of(historico.desempilhar());
    }
}
