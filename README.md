# Algoritmo do Banqueiro — Trabalho Prático 1

## Descrição

Implementação multithreaded do **Algoritmo do Banqueiro** em Java, conforme proposto na Seção 7.5.3 do livro *Fundamentos de Sistemas Operacionais* (Silberschatz, Galvin, Gagne, 9ª ed.).

O programa simula um banqueiro que gerencia recursos compartilhados entre múltiplos clientes (threads). Antes de atender qualquer solicitação, o banqueiro executa o **algoritmo de segurança** para garantir que o sistema permaneça em um estado seguro — prevenindo deadlocks.

---

## Funcionalidades

- **5 threads de clientes** solicitando e liberando recursos concorrentemente.
- **Mutex (ReentrantLock)** para exclusão mútua e prevenção de condições de corrida.
- **Algoritmo de segurança** que verifica se existe uma sequência segura antes de alocar recursos.
- Inicialização dos recursos disponíveis via **argumentos de linha de comando**.

---

## Requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java JDK   | 11 ou superior |

Verificar instalação:
```bash
java -version
javac -version
```

---

## Compilação

```bash
javac BankersAlgorithm.java
```

---

## Execução

O programa recebe como argumentos o número de instâncias de cada tipo de recurso.

```bash
java BankersAlgorithm <recursos_tipo1> <recursos_tipo2> <recursos_tipo3>
```

**Exemplo** (10 instâncias do recurso A, 5 do B, 7 do C):

```bash
java BankersAlgorithm 10 5 7
```

> O número de argumentos deve ser exatamente igual a `NUMBER_OF_RESOURCES` (padrão: 3).

---

## Estrutura do Projeto

```
.
├── BankersAlgorithm.java   # Código-fonte principal
└── README.md               # Este arquivo
```

---

## Saída Esperada

```
============================================================
Estado inicial do sistema:
  Disponível:  [10, 5, 7]
  Máximo por cliente:
    Cliente 0: [5, 3, 1]
    ...
============================================================
[Cliente 0] ALOCADO:   [3, 1, 0]
[Cliente 1] AGUARDANDO: recursos insuficientes disponíveis.
[Cliente 2] NEGADO (estado inseguro): [8, 2, 1]
[Cliente 3] LIBERADO:  [0, 1, 2]
...
============================================================
Todas as threads finalizaram. Sistema encerrado com sucesso.
============================================================
```

### Legenda dos estados

| Mensagem      | Significado                                           |
|---------------|-------------------------------------------------------|
| `ALOCADO`     | Solicitação aprovada — estado seguro garantido        |
| `NEGADO`      | Solicitação recusada — resultaria em estado inseguro  |
| `AGUARDANDO`  | Recursos insuficientes no momento                     |
| `LIBERADO`    | Recursos devolvidos ao pool disponível                |

---

## Parâmetros Configuráveis (no código-fonte)

| Constante              | Padrão | Descrição                              |
|------------------------|--------|----------------------------------------|
| `NUMBER_OF_CUSTOMERS`  | 5      | Número de threads clientes             |
| `NUMBER_OF_RESOURCES`  | 3      | Tipos de recursos                      |
| `MAX_CYCLES`           | 10     | Ciclos de solicitar/liberar por cliente|
| `SLEEP_MS`             | 200    | Pausa entre operações (milissegundos)  |

---

## Autores
Vinícius Oliveira Ramos
Thiago Costa Soares
Trabalho desenvolvido para a disciplina de Sistemas Operacionais.
