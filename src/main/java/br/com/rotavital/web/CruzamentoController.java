package br.com.rotavital.web;

import br.com.rotavital.cruzamento.ResultadoCruzamento;
import br.com.rotavital.servico.CruzamentoService;
import br.com.rotavital.servico.RepositorioDeDados;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints REST do Rota Vital para esta atividade.
 *
 *   POST /api/dados/gerar?requisicoes=1000000        gera/carrega o snapshot
 *   GET  /api/cruzamento?modo=sequencial&requisicoes=100000
 *   GET  /api/cruzamento?modo=plataforma&threads=4&requisicoes=1000000
 *   GET  /api/cruzamento?modo=virtual&threads=8&requisicoes=1000000
 *   GET  /api/cruzamento?modo=inseguro&threads=8&requisicoes=100000  (demo de race condition)
 *   GET  /api/info                                    dados do ambiente de medição
 */
@RestController
@RequestMapping("/api")
public class CruzamentoController {

    private static final int MAX_REQUISICOES = 5_000_000;

    private final CruzamentoService servico;
    private final RepositorioDeDados repositorio;

    public CruzamentoController(CruzamentoService servico, RepositorioDeDados repositorio) {
        this.servico = servico;
        this.repositorio = repositorio;
    }

    @GetMapping("/cruzamento")
    public ResultadoCruzamento cruzamento(@RequestParam(defaultValue = "sequencial") String modo,
                                          @RequestParam(defaultValue = "1") int threads,
                                          @RequestParam(defaultValue = "100000") int requisicoes) {
        validarTamanho(requisicoes);
        if (!CruzamentoService.THREADS_PERMITIDAS.contains(threads)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "threads deve ser um de " + CruzamentoService.THREADS_PERMITIDAS);
        }
        try {
            return servico.executar(modo, threads, requisicoes);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/dados/gerar")
    public Map<String, Object> gerar(@RequestParam(defaultValue = "100000") int requisicoes) {
        validarTamanho(requisicoes);
        long t0 = System.nanoTime();
        boolean jaExistia = repositorio.carregada(requisicoes);
        var base = repositorio.obter(requisicoes);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("requisicoes", base.numRequisicoes);
        r.put("hospitais", base.numHospitais);
        r.put("insumos", base.numInsumos);
        r.put("mediaHospitaisCandidatosPorInsumo", Math.round(base.mediaCandidatosPorInsumo() * 10) / 10.0);
        r.put("seed", base.seed);
        r.put("jaEstavaEmCache", jaExistia);
        r.put("tempoMs", (System.nanoTime() - t0) / 1_000_000.0);
        return r;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("servidor", "Spring Boot + Tomcat embutido");
        r.put("nucleosDisponiveis", rt.availableProcessors());
        r.put("java", System.getProperty("java.version"));
        r.put("jvm", System.getProperty("java.vm.name"));
        r.put("sistemaOperacional", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        r.put("heapMaximoMb", rt.maxMemory() / (1024 * 1024));
        r.put("argumentosJvm", ManagementFactory.getRuntimeMXBean().getInputArguments());
        r.put("basesCarregadas", repositorio.todas().keySet());
        return r;
    }

    private static void validarTamanho(int requisicoes) {
        if (requisicoes < 1 || requisicoes > MAX_REQUISICOES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "requisicoes deve estar entre 1 e " + MAX_REQUISICOES);
        }
    }
}
