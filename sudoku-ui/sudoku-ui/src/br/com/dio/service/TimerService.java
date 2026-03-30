package br.com.dio.service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Serviço de cronômetro para o jogo de Sudoku.
 * Controla início, pausa, retomada e parada do tempo de jogo.
 */
public class TimerService {

    private long startTimeMillis;
    private long accumulatedMillis = 0;
    private boolean running = false;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;

    /** Inicia o cronômetro. Ignora se já estiver rodando. */
    public void start() {
        if (running) return;
        startTimeMillis = System.currentTimeMillis();
        running = true;
    }

    /** Pausa o cronômetro acumulando o tempo decorrido. */
    public void pause() {
        if (!running) return;
        accumulatedMillis += System.currentTimeMillis() - startTimeMillis;
        running = false;
        stopTick();
    }

    /** Retoma o cronômetro de onde parou. */
    public void resume() {
        if (running) return;
        startTimeMillis = System.currentTimeMillis();
        running = true;
    }

    /** Para o cronômetro completamente e zera o tempo. */
    public void stop() {
        running = false;
        accumulatedMillis = 0;
        stopTick();
    }

    /** Retorna o tempo total decorrido como {@link Duration}. */
    public Duration getElapsed() {
        long total = accumulatedMillis;
        if (running) {
            total += System.currentTimeMillis() - startTimeMillis;
        }
        return Duration.ofMillis(total);
    }

    /** Retorna o tempo formatado como MM:SS. */
    public String getFormattedTime() {
        Duration elapsed = getElapsed();
        long minutes = elapsed.toMinutes();
        long seconds = elapsed.toSecondsPart();
        return "%02d:%02d".formatted(minutes, seconds);
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Registra um callback que será invocado a cada segundo.
     * Útil para atualizar a UI em tempo real.
     *
     * @param onTick Consumer que recebe o tempo formatado a cada tick
     */
    public void startTicking(Consumer<String> onTick) {
        stopTick();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sudoku-timer");
            t.setDaemon(true);
            return t;
        });
        tickTask = scheduler.scheduleAtFixedRate(
                () -> onTick.accept(getFormattedTime()),
                1, 1, TimeUnit.SECONDS
        );
    }

    /**
     * Adiciona uma penalidade de tempo (em milissegundos) ao cronômetro.
     * Usado pelo {@link HintService} para penalizar o uso de dicas.
     */
    void addPenalty(long millis) {
        accumulatedMillis += millis;
    }

    /** Para os ticks periódicos sem zerar o tempo acumulado. */
    public void stopTick() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel(false);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
