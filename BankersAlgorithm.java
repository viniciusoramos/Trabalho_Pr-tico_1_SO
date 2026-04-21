/**
 * Algoritmo do Banqueiro - Trabalho Prático 1
 * Disciplina: Sistemas Operacionais
 *
 * Implementação multithreaded do Algoritmo do Banqueiro com:
 *  - Múltiplas threads de clientes
 *  - Locks mutex (ReentrantLock) para prevenir condições de corrida
 *  - Verificação de estado seguro para evitar deadlocks
 *
 * Uso: java BankersAlgorithm <recursos_tipo1> <recursos_tipo2> ... <recursos_tipoN>
 * Exemplo: java BankersAlgorithm 10 5 7
 */

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class BankersAlgorithm {

    // ─── Constantes ────────────────────────────────────────────────────────────
    static final int NUMBER_OF_CUSTOMERS  = 5;
    static final int NUMBER_OF_RESOURCES  = 3;
    static final int MAX_CYCLES           = 10;  // ciclos por thread cliente
    static final int SLEEP_MS             = 200; // pausa entre operações (ms)

    // ─── Estruturas de dados compartilhadas ────────────────────────────────────
    static int[] available  = new int[NUMBER_OF_RESOURCES];
    static int[][] maximum  = new int[NUMBER_OF_CUSTOMERS][NUMBER_OF_RESOURCES];
    static int[][] allocation = new int[NUMBER_OF_CUSTOMERS][NUMBER_OF_RESOURCES];
    static int[][] need     = new int[NUMBER_OF_CUSTOMERS][NUMBER_OF_RESOURCES];

    // ─── Mutex para acesso exclusivo às estruturas compartilhadas ──────────────
    static final ReentrantLock mutex = new ReentrantLock();

    static final Random random = new Random();

    // ───────────────────────────────────────────────────────────────────────────
    // Solicita recursos para o cliente customer_num
    // Retorna 0 se bem-sucedido, -1 se negado ou inválido
    // ───────────────────────────────────────────────────────────────────────────
    static int request_resources(int customer_num, int[] request) {
        mutex.lock();
        try {
            // 1. Verificar se a solicitação excede a necessidade do cliente
            for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                if (request[j] > need[customer_num][j]) {
                    System.out.printf(
                        "[Cliente %d] ERRO: solicitação excede necessidade declarada.%n",
                        customer_num);
                    return -1;
                }
            }

            // 2. Verificar se os recursos disponíveis são suficientes
            for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                if (request[j] > available[j]) {
                    System.out.printf(
                        "[Cliente %d] AGUARDANDO: recursos insuficientes disponíveis.%n",
                        customer_num);
                    return -1;
                }
            }

            // 3. Simular alocação temporariamente
            for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                available[j]             -= request[j];
                allocation[customer_num][j] += request[j];
                need[customer_num][j]    -= request[j];
            }

            // 4. Verificar se o estado resultante é seguro
            if (is_safe()) {
                System.out.printf(
                    "[Cliente %d] ALOCADO:   %s%n",
                    customer_num, arrayToString(request));
                return 0;
            } else {
                // Reverter a simulação — estado inseguro
                for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                    available[j]             += request[j];
                    allocation[customer_num][j] -= request[j];
                    need[customer_num][j]    += request[j];
                }
                System.out.printf(
                    "[Cliente %d] NEGADO (estado inseguro): %s%n",
                    customer_num, arrayToString(request));
                return -1;
            }
        } finally {
            mutex.unlock();
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Libera recursos do cliente customer_num
    // Retorna 0 se bem-sucedido, -1 se inválido
    // ───────────────────────────────────────────────────────────────────────────
    static int release_resources(int customer_num, int[] release) {
        mutex.lock();
        try {
            // Verificar se cliente tenta liberar mais do que tem alocado
            for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                if (release[j] > allocation[customer_num][j]) {
                    System.out.printf(
                        "[Cliente %d] ERRO: tentativa de liberar mais do que foi alocado.%n",
                        customer_num);
                    return -1;
                }
            }

            for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                available[j]             += release[j];
                allocation[customer_num][j] -= release[j];
                need[customer_num][j]    += release[j];
            }

            System.out.printf(
                "[Cliente %d] LIBERADO:  %s%n",
                customer_num, arrayToString(release));
            return 0;
        } finally {
            mutex.unlock();
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Algoritmo de segurança (Seção 7.5.3.1)
    // Deve ser chamado com o mutex já adquirido
    // ───────────────────────────────────────────────────────────────────────────
    static boolean is_safe() {
        int[] work     = available.clone();
        boolean[] finish = new boolean[NUMBER_OF_CUSTOMERS];

        // Tentar encontrar uma sequência segura
        boolean progress = true;
        while (progress) {
            progress = false;
            for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
                if (!finish[i] && canFinish(i, work)) {
                    // Cliente i pode terminar: libera seus recursos
                    for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                        work[j] += allocation[i][j];
                    }
                    finish[i] = true;
                    progress   = true;
                }
            }
        }

        for (boolean f : finish) {
            if (!f) return false;
        }
        return true;
    }

    // Verifica se os recursos de 'work' satisfazem a necessidade do cliente i
    static boolean canFinish(int i, int[] work) {
        for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
            if (need[i][j] > work[j]) return false;
        }
        return true;
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Thread de cliente
    // ───────────────────────────────────────────────────────────────────────────
    static class CustomerThread implements Runnable {
        private final int id;

        CustomerThread(int id) { this.id = id; }

        @Override
        public void run() {
            for (int cycle = 0; cycle < MAX_CYCLES; cycle++) {
                try {
                    // Gerar uma solicitação aleatória dentro da necessidade atual
                    int[] req = randomRequest(id);
                    request_resources(id, req);

                    Thread.sleep(SLEEP_MS);

                    // Liberar tudo que está alocado para este cliente
                    int[] rel = currentAllocation(id);
                    if (hasAny(rel)) {
                        release_resources(id, rel);
                    }

                    Thread.sleep(SLEEP_MS);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("[Cliente %d] Finalizado.%n", id);
        }
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ───────────────────────────────────────────────────────────────────────────
    static int[] randomRequest(int customer_num) {
        mutex.lock();
        try {
            int[] req = new int[NUMBER_OF_RESOURCES];
            for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                int maxReq = need[customer_num][j];
                req[j] = (maxReq > 0) ? random.nextInt(maxReq + 1) : 0;
            }
            return req;
        } finally {
            mutex.unlock();
        }
    }

    static int[] currentAllocation(int customer_num) {
        mutex.lock();
        try {
            return allocation[customer_num].clone();
        } finally {
            mutex.unlock();
        }
    }

    static boolean hasAny(int[] arr) {
        for (int v : arr) if (v > 0) return true;
        return false;
    }

    static String arrayToString(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]);
            if (i < arr.length - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    static void printState() {
        System.out.println("=".repeat(60));
        System.out.println("Estado inicial do sistema:");
        System.out.printf("  Disponível:  %s%n", arrayToString(available));
        System.out.println("  Máximo por cliente:");
        for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
            System.out.printf("    Cliente %d: %s%n", i, arrayToString(maximum[i]));
        }
        System.out.println("  Necessidade por cliente:");
        for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
            System.out.printf("    Cliente %d: %s%n", i, arrayToString(need[i]));
        }
        System.out.println("=".repeat(60));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Ponto de entrada
    // ───────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        // Validar argumentos
        if (args.length != NUMBER_OF_RESOURCES) {
            System.out.printf(
                "Uso: java BankersAlgorithm <r1> <r2> ... <r%d>%n",
                NUMBER_OF_RESOURCES);
            System.out.printf(
                "Exemplo: java BankersAlgorithm 10 5 7%n");
            System.exit(1);
        }

        // Inicializar recursos disponíveis
        for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
            available[j] = Integer.parseInt(args[j]);
        }

        // Inicializar demanda máxima de cada cliente (aleatória mas respeitando disponível)
        for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
            for (int j = 0; j < NUMBER_OF_RESOURCES; j++) {
                // Máximo entre 1 e available[j] (evita nextInt(0) se recurso = 0)
                maximum[i][j] = available[j] > 0 ? 1 + random.nextInt(available[j]) : 0;
                need[i][j]    = maximum[i][j];
                allocation[i][j] = 0;
            }
        }

        printState();

        // Criar e iniciar threads de clientes
        Thread[] threads = new Thread[NUMBER_OF_CUSTOMERS];
        for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
            threads[i] = new Thread(new CustomerThread(i), "Cliente-" + i);
            threads[i].start();
        }

        // Aguardar todas as threads terminarem
        for (int i = 0; i < NUMBER_OF_CUSTOMERS; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("=".repeat(60));
        System.out.println("Todas as threads finalizaram. Sistema encerrado com sucesso.");
        System.out.println("=".repeat(60));
    }
}
