# Projeto Integrador — AED AV1 — Rota Vital

**Escopo:** Unidade 1 — estruturas básicas do domínio  
**Linguagens:** C e Java 21  
**Projeto-base:** Rota Vital — Spring Boot

## 1. Objetivo e delimitação

A AV1 implementa as estruturas básicas utilizadas pelo domínio do Rota Vital: uma **lista** para o estoque, uma **fila** para requisições hospitalares e uma **pilha** para o histórico de operações. A mesma lógica foi escrita primeiro em C, com alocação manual e ponteiros, e depois reimplementada em Java para consumo pela aplicação Spring Boot.

Nesta etapa não foram implementados roteirização, FEFO, hash ou compatibilidade ABO/Rh. Esses algoritmos pertencem à Unidade 2 e foram mantidos fora do código da AV1.

## 2. Organização do código

| Responsabilidade | C | Java |
|---|---|---|
| Lista de estoque | `estruturas-c/include/estoque_lista.h` e `src/estoque_lista.c` | `estruturas/estoque/ListaEstoque.java` |
| Item de estoque | `ItemEstoque` em `estoque_lista.h` | `ItemEstoque.java` |
| Fila de requisições | `requisicoes_fila.h` e `src/requisicoes_fila.c` | `FilaRequisicoes.java` |
| Requisição | `Requisicao` em `requisicoes_fila.h` | `Requisicao.java` |
| Pilha de histórico | `historico_pilha.h` e `src/historico_pilha.c` | `PilhaHistorico.java` |
| Operação de histórico | `OperacaoHistorico` em `historico_pilha.h` | `OperacaoHistorico.java` |

## 3. Lista de estoque

### 3.1 Decisão de modelagem

O estoque precisa cadastrar, consultar e remover itens. Uma lista simplesmente encadeada é suficiente para a AV1 porque cada item é representado por um nó independente, ligado ao próximo por um ponteiro. A estrutura não ordena os itens e não implementa busca por hash; a consulta percorre a lista, como exigido pelo escopo básico.

### 3.2 Correspondência real entre C e Java

Em `estoque_lista.c`, o tipo privado `NoEstoque` contém um `ItemEstoque item` e um `struct NoEstoque *proximo`. A função `estoque_inserir` chama `malloc(sizeof(NoEstoque))`, copia o item para o nó, aponta `novo->proximo` para o antigo início e atualiza `lista->inicio`. Em `ListaEstoque.java`, a classe privada `No` possui os campos `ItemEstoque item` e `No proximo`; o método `inserir` faz a mesma ligação com `inicio = new No(item, inicio)`. Portanto, `malloc` + ponteiro `proximo` em C correspondem à criação de um objeto `No` + referência `proximo` em Java.

Na remoção, C mantém dois ponteiros, `anterior` e `atual`, percorre até encontrar o código e ajusta `anterior->proximo` ou `lista->inicio`. Depois chama `free(atual)`. Java repete os mesmos dois nós lógicos, ajusta `anterior.proximo` ou `inicio` e retorna o item removido; o coletor de lixo substitui a chamada explícita a `free`, pois não há mais referência ao nó. A consulta também percorre do início ao fim em ambas as versões. A rejeição de códigos duplicados ocorre antes da alocação em C e antes da criação do nó em Java.

### 3.3 Complexidade

- Inserção no início: `O(1)` após a consulta de duplicidade; considerando a validação, `O(n)`.
- Consulta por código: `O(n)`, no pior caso.
- Remoção por código: `O(n)`.
- Memória: `O(n)` nós.

## 4. Fila de requisições

### 4.1 Decisão de modelagem

Requisições devem ser processadas na ordem em que chegam, por isso a estrutura escolhida é uma fila FIFO. A fila mantém ponteiros para `inicio` e `fim`, permitindo enfileirar no final sem percorrer todos os nós e desenfileirar no início.

### 4.2 Correspondência real entre C e Java

Em `requisicoes_fila.c`, `NoRequisicao` guarda uma `Requisicao` e o ponteiro `proximo`. Em `fila_enfileirar`, o novo nó recebe `proximo = NULL`; se a fila estava vazia, `fila->inicio = novo`, caso contrário `fila->fim->proximo = novo`. Em seguida `fila->fim = novo`. `FilaRequisicoes.java` repete exatamente os três casos usando referências: cria `No novo`, define `inicio` quando `fim == null`, liga `fim.proximo` nos demais casos e atualiza `fim`.

No desenfileiramento, C salva o primeiro nó, move `fila->inicio` para o próximo, zera `fila->fim` quando a fila fica vazia, copia a requisição de saída e executa `free(remover)`. Java salva `inicio.requisicao`, move `inicio`, zera `fim` quando necessário e deixa o nó sem referências para ser coletado. A regra FIFO é igual e é verificada nos testes com duas requisições retiradas na mesma ordem em que foram inseridas.

### 4.3 Complexidade

- Enfileirar: `O(1)` por manter o ponteiro para o fim.
- Consultar primeiro: `O(1)`.
- Desenfileirar: `O(1)`.
- Memória: `O(n)` nós.

## 5. Pilha de histórico

### 5.1 Decisão de modelagem

O histórico de operações pode precisar desfazer ou consultar primeiro a operação mais recente. Por isso foi usada uma pilha LIFO. O topo é o único ponto de entrada e saída.

### 5.2 Correspondência real entre C e Java

Em `historico_pilha.c`, cada `NoHistorico` guarda uma `OperacaoHistorico` e `struct NoHistorico *proximo`. `pilha_empilhar` aloca um nó, copia a operação e liga `novo->proximo` ao topo anterior antes de atualizar `pilha->topo`. `PilhaHistorico.java` faz a mesma sequência: `topo = new No(operacao, topo)`. A ordem das instruções preserva a regra LIFO nas duas linguagens.

Em `pilha_desempilhar`, C verifica topo nulo, guarda o valor, move `pilha->topo` para o próximo, copia a operação e libera o nó com `free`. Java verifica `topo == null`, guarda o valor, move a referência para `topo.proximo` e retorna o valor; o nó antigo fica elegível para coleta. Os testes empilham uma entrada e depois um atendimento, confirmando que o atendimento sai primeiro.

### 5.3 Complexidade

- Empilhar: `O(1)`.
- Consultar topo: `O(1)`.
- Desempilhar: `O(1)`.
- Memória: `O(n)` nós.

## 6. Integração com Spring Boot

`Av1EstruturasController` demonstra o consumo das estruturas Java pela camada web:

- `POST/GET/DELETE /api/av1/estoque`;
- `POST/GET/DELETE /api/av1/requisicoes`;
- `POST/GET/DELETE /api/av1/historico`.

O controller instancia `ListaEstoque`, `FilaRequisicoes` e `PilhaHistorico` diretamente. Ele não troca as estruturas por `ArrayList`, `Queue`, `Deque` ou `Map`, pois a finalidade é demonstrar a implementação desenvolvida na disciplina. A lógica de negócio de Threads II permanece separada no pacote `cruzamento`.

## 7. Testes

A versão C é testada por `estruturas-c/tests/test_av1.c`, compilado pelo `estruturas-c/Makefile`. A versão Java é testada por `Av1EstruturasTest` com JUnit 5. As duas versões rejeitam dados inválidos com as mesmas regras de domínio; em Java, as operações públicas também são sincronizadas porque as estruturas são mantidas por um controller Spring singleton. Os testes cobrem:

- lista: inserir, impedir duplicidade, consultar, remover e estado vazio;
- fila: enfileirar, consultar primeiro, desenfileirar na ordem FIFO e estado vazio;
- pilha: empilhar, consultar topo, desempilhar na ordem LIFO e estado vazio;
- validação: códigos, quantidades, textos obrigatórios e reuso após esvaziamento.

## 8. Como executar

### Java e Spring Boot

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
bash ./mvnw test
bash ./mvnw spring-boot:run
```

### C

```bash
cd estruturas-c
make test
```

## 9. Demonstração rápida da API

Com o servidor em execução:

```bash
curl -X POST http://localhost:8080/api/av1/estoque \
  -H 'Content-Type: application/json' \
  -d '{"codigo":10,"nome":"Soro fisiologico","quantidade":50,"unidade":"un"}'

curl http://localhost:8080/api/av1/estoque/10

curl -X POST http://localhost:8080/api/av1/requisicoes \
  -H 'Content-Type: application/json' \
  -d '{"codigo":1,"hospitalCodigo":101,"insumoCodigo":10,"quantidade":2,"prioridade":1}'

curl -X POST http://localhost:8080/api/av1/historico \
  -H 'Content-Type: application/json' \
  -d '{"tipo":1,"referencia":10,"descricao":"Entrada de estoque"}'
```

## 10. Roteiro de apresentação

1. Apresente o problema: o Rota Vital precisa representar estoque, requisições e histórico.
2. Explique por que estoque é uma lista, requisições são uma fila e histórico é uma pilha.
3. Mostre a struct de nó em C e aponte `proximo`.
4. Mostre o `malloc` na inserção e o `free` na remoção/liberação.
5. Mostre a classe `No` equivalente em Java.
6. Explique que referência Java ocupa o papel lógico do ponteiro, mas a memória é administrada pelo coletor de lixo.
7. Execute `make test` e `bash ./mvnw test`.
8. Demonstre um endpoint `/api/av1`.
9. Reforce que roteirização, FEFO, hash e ABO/Rh não foram implementados porque pertencem à AV2.

## 11. Limites conscientes da AV1

A lista não faz ordenação; a fila não prioriza requisições; a pilha não implementa busca no histórico. Essas limitações são intencionais: adicionar ordenação, FEFO, hash, busca ou compatibilidade ABO/Rh anteciparia o conteúdo da Unidade 2 e descaracterizaria o escopo solicitado para a AV1.
