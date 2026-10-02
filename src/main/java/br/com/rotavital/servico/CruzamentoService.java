package br.com.rotavital.servico;

import br.com.rotavital.cruzamento.MotorCruzamento;
import br.com.rotavital.cruzamento.ResultadoCruzamento;
import br.com.rotavital.dominio.BaseDeDados;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Serviço da camada de aplicação que executa o cruzamento requisições ×
 * estoque no modo pedido.
 *
 * Os pools de threads de plataforma são criados uma vez por tamanho
 * (2, 4, 8...) e reaproveitados entre requisições HTTP — criar e destruir
 * threads do SO a cada chamada custaria caro e distorceria a medição.
 */
@Service
public class CruzamentoService {

    public static final List<Integer> THREADS_PERMITIDAS = List.of(1, 2, 4, 8, 16, 32);

    private final RepositorioDeDados repositorio;
    private final Map<Integer, ExecutorService> pools = new ConcurrentHashMap<>();

    public CruzamentoService(RepositorioDeDados repositorio) {
        this.repositorio = repositorio;
    }

    public ResultadoCruzamento executar(String modo, int threads, int requisicoes) {
        BaseDeDados base = repositorio.obter(requisicoes);
        return switch (modo) {
            case "sequencial" -> MotorCruzamento.sequencial(base);
            case "plataforma" -> MotorCruzamento.comExecutor("plataforma", base, threads, pool(threads));
            case "virtual" -> MotorCruzamento.virtual(base, threads);
            case "inseguro" -> MotorCruzamento.inseguro(base, threads);
            default -> throw new IllegalArgumentException(
                    "modo inválido: " + modo + " (use sequencial | plataforma | virtual | inseguro)");
        };
    }

    private ExecutorService pool(int n) {
        return pools.computeIfAbsent(n, k -> Executors.newFixedThreadPool(k, MotorCruzamento.fabricaPlataforma(k)));
    }

    @PreDestroy
    public void encerrar() {
        pools.values().forEach(ExecutorService::shutdown);
    }
}
