CC ?= gcc
CFLAGS ?= -std=c11 -Wall -Wextra -Wpedantic -Werror -Iinclude
BUILD = build
SOURCES = src/estoque_lista.c src/requisicoes_fila.c src/historico_pilha.c
TEST = $(BUILD)/test_av1

.PHONY: all test clean

all: test

test: $(TEST)
	./$(TEST)

$(BUILD):
	mkdir -p $(BUILD)

$(TEST): $(SOURCES) tests/test_av1.c | $(BUILD)
	$(CC) $(CFLAGS) $(SOURCES) tests/test_av1.c -o $(TEST)

clean:
	rm -rf $(BUILD)
