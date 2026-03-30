package br.com.dio.service;

import br.com.dio.model.Board;
import br.com.dio.model.Space;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de dicas para o jogo de Sudoku.
 *
 * <p>Cada dica revela o valor correto de uma célula vazia aleatória.
 * O jogador tem um número limitado de dicas ({@value #MAX_HINTS}).
 * Cada dica usada adiciona uma penalidade de {@value #PENALTY_SECONDS}s no cronômetro.</p>
 */
public class HintService {

    public static final int MAX_HINTS = 3;
    public static final int PENALTY_SECONDS = 30;

    private int hintsUsed = 0;
    private final Board board;
    private final TimerService timerService;

    public record HintResult(int col, int row, int value, int hintsRemaining, boolean penaltyApplied) {}

    public HintService(final Board board, final TimerService timerService) {
        this.board = board;
        this.timerService = timerService;
    }

    /**
     * Solicita uma dica.
     *
     * @return {@link Optional} com o resultado da dica, ou vazio se não houver dicas disponíveis
     *         ou não existirem células vazias.
     */
    public Optional<HintResult> requestHint() {
        if (hintsUsed >= MAX_HINTS) {
            return Optional.empty();
        }

        var emptySpaces = findEmptySpaces();
        if (emptySpaces.isEmpty()) {
            return Optional.empty();
        }

        Collections.shuffle(emptySpaces);
        var chosen = emptySpaces.getFirst();

        board.changeValue(chosen[0], chosen[1],
                board.getSpaces().get(chosen[0]).get(chosen[1]).getExpected());

        hintsUsed++;
        applyPenalty();

        return Optional.of(new HintResult(
                chosen[0], chosen[1],
                board.getSpaces().get(chosen[0]).get(chosen[1]).getExpected(),
                getHintsRemaining(),
                true
        ));
    }

    public int getHintsRemaining() {
        return MAX_HINTS - hintsUsed;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    /** Reseta o contador de dicas (usado ao reiniciar o jogo). */
    public void reset() {
        hintsUsed = 0;
    }

    // -------------------------------------------------------------------------

    private List<int[]> findEmptySpaces() {
        List<int[]> empty = new ArrayList<>();
        var spaces = board.getSpaces();
        for (int col = 0; col < spaces.size(); col++) {
            for (int row = 0; row < spaces.get(col).size(); row++) {
                Space s = spaces.get(col).get(row);
                if (!s.isFixed() && s.getActual() == null) {
                    empty.add(new int[]{col, row});
                }
            }
        }
        return empty;
    }

    private void applyPenalty() {
        if (!timerService.isRunning()) return;
        // Adiciona penalidade "avançando" o tempo de início para trás
        // (equivalente a somar PENALTY_SECONDS ao tempo acumulado)
        timerService.pause();
        addPenaltyMillis(PENALTY_SECONDS * 1000L);
        timerService.resume();
    }

    private void addPenaltyMillis(long millis) {
        // Acessa via reflexão seria frágil; delegamos ao próprio serviço
        // através de um método package-private adicionado ao TimerService.
        timerService.addPenalty(millis);
    }
}
