## Rota Vital — Threads II e AV1

### Pré-requisitos

- JDK **21 completo** (o comando `javac` precisa estar disponível; somente o JRE não é suficiente).
- GCC e `make` para os testes da implementação em C.
- Python 3 para os scripts de benchmark e relatório.

Se o Java 21 estiver instalado em outro caminho, configure `JAVA_HOME` antes de executar o Maven:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

## AV1 — Algoritmos e Estruturas de Dados

O projeto também contém a entrega da Unidade 1, com as mesmas estruturas implementadas em C e Java:

- `estruturas-c/`: lista encadeada de estoque, fila de requisições e pilha de histórico usando `malloc`, `free` e ponteiros.
- `src/main/java/br/com/rotavital/estruturas/`: reimplementação equivalente em Java, sem substituir as estruturas acadêmicas por Collections.
- `src/test/java/br/com/rotavital/estruturas/`: testes JUnit das operações básicas.
- `docs/Relatorio_AED_AV1_RotaVital.md`: justificativa da modelagem, equivalência entre as versões, complexidade e roteiro de apresentação.

### Testar a AV1

```bash
cd estruturas-c
make test
cd ..
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
bash ./mvnw test
```

### Endpoints de demonstração

Com o servidor Spring Boot em execução, as estruturas Java podem ser consumidas por:

- `POST /api/av1/estoque`, `GET /api/av1/estoque/{codigo}`, `DELETE /api/av1/estoque/{codigo}` e `GET /api/av1/estoque`
- `POST` e `GET /api/av1/requisicoes`, `DELETE /api/av1/requisicoes`
- `POST` e `GET /api/av1/historico`, `DELETE /api/av1/historico`

A AV1 não implementa roteirização, FEFO, hash ou compatibilidade ABO/Rh. Esses algoritmos ficam reservados para a Unidade 2, conforme o enunciado.

### Publicar no GitHub

O projeto já contém `.gitignore` e `.gitattributes` para não enviar arquivos de build, caches da IDE ou diferenças de quebra de linha. A partir da pasta raiz do projeto:

```bash
git init
git add .
git commit -m "Corrige projeto Rota Vital"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/SEU_REPOSITORIO.git
git push -u origin main
```

Antes do primeiro `git add`, confirme que o remoto aponta para o repositório correto com `git remote -v`.
