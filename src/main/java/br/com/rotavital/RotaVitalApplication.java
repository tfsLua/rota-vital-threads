package br.com.rotavital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Rota Vital — camada de aplicação (API).
 *
 * Nesta atividade (Threads II) a API expõe a operação
 * "cruzamento requisições × estoque": para cada requisição de insumo
 * pendente, encontrar o hospital fornecedor mais adequado no país.
 * A operação existe em versão sequencial e em versões com threads.
 */
@SpringBootApplication
public class RotaVitalApplication {
    public static void main(String[] args) {
        SpringApplication.run(RotaVitalApplication.class, args);
    }
}
